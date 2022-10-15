package com.pr.clientservice.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderRating {

    @JsonAlias("client_id")
    private Long clientId;

    @JsonAlias("order_id")
    private Long orderId;

    private List<SubOrderRatingRequest> orders;
}
