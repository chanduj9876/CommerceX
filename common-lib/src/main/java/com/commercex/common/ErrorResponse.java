package com.commercex.common;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Standardized error response returned to the client.
 *
 * Why a structured error response?
 * - Consistent format for ALL errors (404, 400, 409, 500...)
 * - Client can parse errors programmatically instead of guessing the format
 * - Includes timestamp for debugging and log correlation
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    private int status;
    private String message;
    private LocalDateTime timestamp;
}
