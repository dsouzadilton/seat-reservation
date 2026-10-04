package com.dilton.paytm.dto;

import java.util.List;

public record ShowResponse(
        Long id,
        String name,
        Long pricePaise,
        Integer perUserLimit,
        List<ShowSeatResponse> seats,
        long available,
        long held,
        long confirmed,
        long total
) {
}