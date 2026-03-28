package com.commercex.user.dto;

import lombok.*;

/**
 * Simple message response for endpoints that don't return data
 * (e.g., password changed, reset email sent).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    private String message;
}
