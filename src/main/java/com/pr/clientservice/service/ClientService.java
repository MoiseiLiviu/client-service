package com.pr.clientservice.service;

import com.pr.clientservice.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class ClientService {

    private static final Integer NUMBER_OF_CLIENTS = 3;

    public static String DINNNING_HALL_URL;
    public static String FOOD_ORDER_SERVICE_URL;

    @Value("${dinning-hall-service.url}")
    public void setDinningHallServiceUrl(String url){
        DINNNING_HALL_URL = url;
    }

    @Value("${food-order-service.url}")
    public void setFoodOrderServiceUrl(String url){
        FOOD_ORDER_SERVICE_URL = url;
    }

    @PostConstruct
    public void initClients(){

        for(int i = 0;i<NUMBER_OF_CLIENTS;i++){
            new Client().init();
        }
    }
}
