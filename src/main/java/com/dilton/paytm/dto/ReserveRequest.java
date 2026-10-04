package com.dilton.paytm.dto;

import java.util.List;

public record ReserveRequest(
        List<String> seats,
        String idempotencyKey
) {
}