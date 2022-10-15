package com.pr.clientservice.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderRequest {

    @JsonAlias("client_id")
    private Long clientId;

    private List<SubOrderRequest> subOrderRequests;
}
