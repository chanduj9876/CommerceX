-- CommerceX: Initialize all service databases
-- This script runs automatically when the postgres container starts for the first time.

CREATE DATABASE commercex_users;
CREATE DATABASE commercex_products;
CREATE DATABASE commercex_orders;
CREATE DATABASE commercex_payments;
CREATE DATABASE commercex_shipping;
