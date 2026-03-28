package com.commercex.order.service;

import com.commercex.cart.entity.Cart;
import com.commercex.cart.entity.CartItem;
import com.commercex.cart.repository.CartRepository;
import com.commercex.common.EmptyCartException;
import com.commercex.common.InsufficientStockException;
import com.commercex.common.InvalidOrderStateException;
import com.commercex.common.InvalidPromoCodeException;
import com.commercex.common.ResourceNotFoundException;
import com.commercex.order.discount.DiscountStrategyFactory;
import com.commercex.order.dto.CreateOrderRequest;
import com.commercex.order.dto.OrderResponseDTO;
import com.commercex.order.entity.Order;
import com.commercex.order.entity.OrderItem;
import com.commercex.order.entity.OrderStatus;
import com.commercex.order.event.OrderEventPublisher;
import com.commercex.order.repository.OrderRepository;
import com.commercex.product.entity.Product;
import com.commercex.product.repository.ProductRepository;
import com.commercex.user.entity.User;
import com.commercex.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private DiscountStrategyFactory discountStrategyFactory;
    @Mock private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User testUser;
    private Product iPhone;
    private Product airpods;

    @BeforeEach
    void setUp() {
        testUser = User.builder().name("Test").email("test@test.com").password("pass").build();
        testUser.setId(1L);

        iPhone = Product.builder().name("iPhone").price(new BigDecimal("999.99")).stock(20).build();
        iPhone.setId(10L);

        airpods = Product.builder().name("AirPods").price(new BigDecimal("549.00")).stock(15).build();
        airpods.setId(20L);
    }

    private Cart buildCartWithItems(User user, CartItem... items) {
        Cart cart = Cart.builder().user(user).items(new ArrayList<>(List.of(items))).build();
        cart.setId(1L);
        for (CartItem item : items) {
            item.setCart(cart);
        }
        return cart;
    }

    private CartItem buildCartItem(Product product, int qty) {
        CartItem item = CartItem.builder().product(product).quantity(qty).build();
        item.setId(product.getId()); // use product id for simplicity
        return item;
    }

    // ================================
    // createOrder tests
    // ================================
    @Nested
    @DisplayName("createOrder")
    class CreateOrderTests {

        @Test
        @DisplayName("Happy path: creates order, deducts stock, clears cart")
        void createOrder_happyPath() {
            Cart cart = buildCartWithItems(testUser,
                    buildCartItem(iPhone, 2),
                    buildCartItem(airpods, 1));

            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(productRepository.findAllById(Set.of(10L, 20L))).thenReturn(List.of(iPhone, airpods));
            when(discountStrategyFactory.getStrategy(null))
                    .thenReturn(new com.commercex.order.discount.NoDiscount());
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(100L);
                return o;
            });

            OrderResponseDTO result = orderService.createOrder(1L, null);

            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(2);
            // Total = (999.99 * 2) + 549.00 = 2548.98
            assertThat(result.getTotalAmount()).isEqualByComparingTo("2548.98");
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("0");
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);

            // Stock deducted
            assertThat(iPhone.getStock()).isEqualTo(18); // 20 - 2
            assertThat(airpods.getStock()).isEqualTo(14); // 15 - 1

            // Cart cleared
            assertThat(cart.getItems()).isEmpty();

            // Event published
            verify(orderEventPublisher).publishOrderCreated(eq(100L), eq(OrderStatus.PENDING));
        }

        @Test
        @DisplayName("With SAVE10 promo: applies 10% discount")
        void createOrder_withPromoCode() {
            Cart cart = buildCartWithItems(testUser, buildCartItem(iPhone, 1));
            CreateOrderRequest request = CreateOrderRequest.builder().promoCode("SAVE10").build();

            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
            when(discountStrategyFactory.isValidPromoCode("SAVE10")).thenReturn(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(productRepository.findAllById(Set.of(10L))).thenReturn(List.of(iPhone));
            when(discountStrategyFactory.getStrategy("SAVE10"))
                    .thenReturn(new com.commercex.order.discount.PercentageDiscount(new BigDecimal("0.10")));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(101L);
                return o;
            });

            OrderResponseDTO result = orderService.createOrder(1L, request);

            // 999.99 * 10% = 100.00 (rounded)
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("100.00");
            // 999.99 - 100.00 = 899.99
            assertThat(result.getTotalAmount()).isEqualByComparingTo("899.99");
            assertThat(result.getPromoCode()).isEqualTo("SAVE10");
        }

        @Test
        @DisplayName("Empty cart throws EmptyCartException")
        void createOrder_emptyCart_throws() {
            Cart emptyCart = Cart.builder().user(testUser).items(new ArrayList<>()).build();
            emptyCart.setId(1L);

            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(emptyCart));

            assertThatThrownBy(() -> orderService.createOrder(1L, null))
                    .isInstanceOf(EmptyCartException.class);
        }

        @Test
        @DisplayName("No cart at all throws EmptyCartException")
        void createOrder_noCart_throws() {
            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.createOrder(1L, null))
                    .isInstanceOf(EmptyCartException.class);
        }

        @Test
        @DisplayName("Insufficient stock throws InsufficientStockException")
        void createOrder_insufficientStock_throws() {
            iPhone.setStock(1); // Only 1 in stock
            Cart cart = buildCartWithItems(testUser, buildCartItem(iPhone, 5)); // wants 5

            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(productRepository.findAllById(Set.of(10L))).thenReturn(List.of(iPhone));

            assertThatThrownBy(() -> orderService.createOrder(1L, null))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Insufficient stock");

            // Stock should NOT be deducted (transaction would roll back)
            assertThat(iPhone.getStock()).isEqualTo(1);
        }

        @Test
        @DisplayName("Invalid promo code throws InvalidPromoCodeException")
        void createOrder_invalidPromo_throws() {
            Cart cart = buildCartWithItems(testUser, buildCartItem(iPhone, 1));
            CreateOrderRequest request = CreateOrderRequest.builder().promoCode("BOGUS").build();

            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
            when(discountStrategyFactory.isValidPromoCode("BOGUS")).thenReturn(false);

            assertThatThrownBy(() -> orderService.createOrder(1L, request))
                    .isInstanceOf(InvalidPromoCodeException.class)
                    .hasMessageContaining("BOGUS");
        }

        @Test
        @DisplayName("Product deleted during checkout throws ResourceNotFoundException")
        void createOrder_productDeleted_throws() {
            Cart cart = buildCartWithItems(testUser, buildCartItem(iPhone, 1));

            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(productRepository.findAllById(Set.of(10L))).thenReturn(List.of());

            assertThatThrownBy(() -> orderService.createOrder(1L, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("no longer available");
        }

        @Test
        @DisplayName("Snapshots product data into OrderItem")
        void createOrder_snapshotsProductData() {
            Cart cart = buildCartWithItems(testUser, buildCartItem(iPhone, 1));

            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(productRepository.findAllById(Set.of(10L))).thenReturn(List.of(iPhone));
            when(discountStrategyFactory.getStrategy(null))
                    .thenReturn(new com.commercex.order.discount.NoDiscount());

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            when(orderRepository.save(captor.capture())).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(102L);
                return o;
            });

            orderService.createOrder(1L, null);

            Order savedOrder = captor.getValue();
            OrderItem item = savedOrder.getItems().get(0);
            assertThat(item.getProductId()).isEqualTo(10L);
            assertThat(item.getProductName()).isEqualTo("iPhone");
            assertThat(item.getUnitPrice()).isEqualByComparingTo("999.99");
            assertThat(item.getQuantity()).isEqualTo(1);
        }
    }

    // ================================
    // getOrderById tests
    // ================================
    @Nested
    @DisplayName("getOrderById")
    class GetOrderByIdTests {

        @Test
        @DisplayName("Customer can view their own order")
        void getOrderById_ownOrder_succeeds() {
            Order order = Order.builder().user(testUser).totalAmount(BigDecimal.TEN)
                    .status(OrderStatus.PENDING).items(new ArrayList<>()).build();
            order.setId(1L);

            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

            OrderResponseDTO result = orderService.getOrderById(1L, 1L);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Customer cannot view another user's order")
        void getOrderById_otherUserOrder_throws() {
            User otherUser = User.builder().name("Other").email("other@test.com").password("p").build();
            otherUser.setId(999L);

            Order order = Order.builder().user(otherUser).totalAmount(BigDecimal.TEN)
                    .status(OrderStatus.PENDING).items(new ArrayList<>()).build();
            order.setId(1L);

            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.getOrderById(1L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Admin overload returns any order")
        void getOrderById_adminOverload_succeeds() {
            Order order = Order.builder().user(testUser).totalAmount(BigDecimal.TEN)
                    .status(OrderStatus.PENDING).items(new ArrayList<>()).build();
            order.setId(1L);

            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

            OrderResponseDTO result = orderService.getOrderById(1L);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Non-existent order throws ResourceNotFoundException")
        void getOrderById_notFound_throws() {
            when(orderRepository.findByIdWithItems(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderById(999L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ================================
    // updateOrderStatus tests
    // ================================
    @Nested
    @DisplayName("updateOrderStatus")
    class UpdateOrderStatusTests {

        private Order pendingOrder() {
            Order order = Order.builder().user(testUser).totalAmount(BigDecimal.TEN)
                    .status(OrderStatus.PENDING).items(new ArrayList<>()).build();
            order.setId(1L);
            return order;
        }

        @Test
        @DisplayName("PENDING → CONFIRMED succeeds")
        void pendingToConfirmed() {
            Order order = pendingOrder();
            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OrderResponseDTO result = orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

            assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            verify(orderEventPublisher).publishStatusChanged(1L, OrderStatus.PENDING, OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("PENDING → SHIPPED fails (invalid transition)")
        void pendingToShipped_fails() {
            Order order = pendingOrder();
            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.SHIPPED))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("Cannot transition");
        }

        @Test
        @DisplayName("DELIVERED → any fails (terminal state)")
        void deliveredToAnything_fails() {
            Order order = pendingOrder();
            order.setStatus(OrderStatus.DELIVERED);
            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.CANCELLED))
                    .isInstanceOf(InvalidOrderStateException.class);
        }

        @Test
        @DisplayName("CANCELLED → any fails (terminal state)")
        void cancelledToAnything_fails() {
            Order order = pendingOrder();
            order.setStatus(OrderStatus.CANCELLED);
            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED))
                    .isInstanceOf(InvalidOrderStateException.class);
        }

        @Test
        @DisplayName("CONFIRMED → SHIPPED succeeds")
        void confirmedToShipped() {
            Order order = pendingOrder();
            order.setStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OrderResponseDTO result = orderService.updateOrderStatus(1L, OrderStatus.SHIPPED);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        }

        @Test
        @DisplayName("SHIPPED → DELIVERED succeeds")
        void shippedToDelivered() {
            Order order = pendingOrder();
            order.setStatus(OrderStatus.SHIPPED);
            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OrderResponseDTO result = orderService.updateOrderStatus(1L, OrderStatus.DELIVERED);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        }

        @Test
        @DisplayName("Cancellation restores stock")
        void cancellation_restoresStock() {
            OrderItem item = OrderItem.builder().productId(10L).productName("iPhone")
                    .unitPrice(new BigDecimal("999.99")).quantity(3).build();
            Order order = Order.builder().user(testUser).totalAmount(BigDecimal.TEN)
                    .status(OrderStatus.PENDING).items(new ArrayList<>(List.of(item))).build();
            order.setId(1L);
            item.setOrder(order);

            iPhone.setStock(17); // After 3 were deducted

            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
            when(productRepository.findById(10L)).thenReturn(Optional.of(iPhone));
            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            orderService.updateOrderStatus(1L, OrderStatus.CANCELLED);

            assertThat(iPhone.getStock()).isEqualTo(20); // 17 + 3 restored
        }
    }

    // ================================
    // cancelOrder tests
    // ================================
    @Nested
    @DisplayName("cancelOrder")
    class CancelOrderTests {

        @Test
        @DisplayName("Customer can cancel their own PENDING order")
        void cancelOwnPendingOrder() {
            Order order = Order.builder().user(testUser).totalAmount(BigDecimal.TEN)
                    .status(OrderStatus.PENDING).items(new ArrayList<>()).build();
            order.setId(1L);

            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OrderResponseDTO result = orderService.cancelOrder(1L, 1L);

            assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("Customer cannot cancel another user's order")
        void cancelOtherUserOrder_throws() {
            User other = User.builder().name("Other").email("o@test.com").password("p").build();
            other.setId(999L);
            Order order = Order.builder().user(other).totalAmount(BigDecimal.TEN)
                    .status(OrderStatus.PENDING).items(new ArrayList<>()).build();
            order.setId(1L);

            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Cannot cancel CONFIRMED order")
        void cancelConfirmedOrder_throws() {
            Order order = Order.builder().user(testUser).totalAmount(BigDecimal.TEN)
                    .status(OrderStatus.CONFIRMED).items(new ArrayList<>()).build();
            order.setId(1L);

            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("Only PENDING");
        }
    }

    // ================================
    // getMyOrders tests
    // ================================
    @Nested
    @DisplayName("getMyOrders")
    class GetMyOrdersTests {

        @Test
        @DisplayName("Returns paginated orders for user")
        void returnsPagedOrders() {
            Order order = Order.builder().user(testUser).totalAmount(BigDecimal.TEN)
                    .status(OrderStatus.PENDING).items(new ArrayList<>()).build();
            order.setId(1L);

            Page<Order> page = new PageImpl<>(List.of(order));
            when(orderRepository.findByUserId(eq(1L), any(PageRequest.class))).thenReturn(page);

            Page<OrderResponseDTO> result = orderService.getMyOrders(1L, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
        }
    }
}
