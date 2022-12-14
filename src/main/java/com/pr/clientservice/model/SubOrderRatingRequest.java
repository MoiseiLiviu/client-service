package com.pr.clientservice.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SubOrderRatingRequest {

    @JsonAlias("restaurant_id")
    private Long restaurantId;

    @JsonAlias("order_id")
    private Long orderId;

    private Integer rating;

    @JsonAlias("estimated_waiting_time")
    private Double estimatedWaitingTime;

    @JsonAlias("waiting_time")
    private Double waitingTime;
}
