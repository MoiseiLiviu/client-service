package com.pr.clientservice.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MenuResponse {

    private Integer restaurants;

    @JsonAlias("restaurants_data")
    private List<Restaurant> restaurantData;
}
