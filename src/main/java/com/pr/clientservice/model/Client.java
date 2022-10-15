package com.pr.clientservice.model;

import com.pr.clientservice.constants.DinningHallServiceUrl;
import com.pr.clientservice.constants.FoodOrderingServiceUrls;
import com.pr.clientservice.service.ClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.pr.clientservice.ClientServiceApplication.TIME_UNIT;

@Slf4j
public class Client {

    private final Long id;
    private static final AtomicLong atomicLong = new AtomicLong();

    private static final RestTemplate restTemplate = new RestTemplate();
    private static final Random random = new Random();

    private OrderResponse orderResponse;
    private final List<SubOrderRatingRequest> subOrderRatingRequests = new ArrayList<>();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public Client() {
        this.id = atomicLong.incrementAndGet();
    }

    public void init() {
        executor.submit(() -> {
            MenuResponse menuResponse = requestMenu();
            OrderRequest orderRequest = generateRandomOrder(menuResponse);
            orderResponse = submitOrderToFoodOrderingService(orderRequest);
            log.info("Order sent : "+orderResponse);
            for (SubOrderResponse subOrderResponse : orderResponse.getSubOrderResponses()) {
                String restaurantUrl = menuResponse.getRestaurantData().stream()
                        .filter(r->r.getRestaurantId().equals(subOrderResponse.getRestaurantId()))
                        .map(Restaurant::getAddress)
                        .findAny().orElseThrow();
                SubOrderRatingRequest subOrderRatingRequest = new SubOrderRatingRequest();
                subOrderRatingRequest.setOrderId(subOrderResponse.getOrderId());
                subOrderRatingRequest.setRestaurantId(subOrderResponse.getRestaurantId());
                subOrderRatingRequest.setEstimatedWaitingTime(subOrderResponse.getEstimatedWaitingTime());
                subOrderRatingRequest.setWaitingTime(subOrderResponse.getEstimatedWaitingTime());
                executor.schedule(() -> checkIfSubOrderIsReady(subOrderRatingRequest, restaurantUrl),
                        subOrderResponse.getEstimatedWaitingTime().longValue() * TIME_UNIT + 5, TimeUnit.MILLISECONDS);
            }
        });
    }

    private void checkIfSubOrderIsReady(SubOrderRatingRequest subOrderRatingRequest, String restaurantUrl) {
        log.info("Requesting order status for suborder "+subOrderRatingRequest);
        ResponseEntity<OrderExternal> response = restTemplate.getForEntity(restaurantUrl + DinningHallServiceUrl.CHECK_ORDER + subOrderRatingRequest.getOrderId(), OrderExternal.class);
        OrderExternal orderExternal = response.getBody();
        log.info("Order status : "+orderExternal);
        if (orderExternal.getIsReady()) {
            log.info("Order is ready : "+orderExternal);
            subOrderRatingRequest.setRating(getRatingForSuborder(orderExternal));
            subOrderRatingRequests.add(subOrderRatingRequest);
            if (orderResponse.getSubOrderResponses().size() == subOrderRatingRequests.size()) {
                submitOrderRating();
            }
        } else {
            subOrderRatingRequest.setWaitingTime(subOrderRatingRequest.getWaitingTime() + orderExternal.getEstimatedWaitingTime());
            executor.schedule(() -> checkIfSubOrderIsReady(subOrderRatingRequest, restaurantUrl), orderExternal.getEstimatedWaitingTime().longValue() * TIME_UNIT + 5, TimeUnit.MILLISECONDS);
        }
    }

    public Integer getRatingForSuborder(OrderExternal orderExternal) {
        long prepTime = orderExternal.getPreparedTime() - orderExternal.getCreatedTime();
        double maxWaitTime = orderExternal.getMaximumWaitTime() * TIME_UNIT;
        int rating;
        if (prepTime < maxWaitTime) {
            rating = 5;
        } else if (prepTime < maxWaitTime * 1.1) {
            rating = 4;
        } else if (prepTime < maxWaitTime * 1.2) {
            rating = 3;
        } else if (prepTime < maxWaitTime * 1.3) {
            rating = 2;
        } else if (prepTime < maxWaitTime * 1.4) {
            rating = 1;
        } else {
            rating = 0;
        }
        log.info("Rating for order with id : "+orderExternal.getOrderId()+" is : "+rating+" order max waiting time : "+maxWaitTime+" order actual wait time : "+prepTime);
        return  rating;
    }

    private void submitOrderRating() {
        OrderRating orderRating = new OrderRating();
        orderRating.setClientId(id);
        orderRating.setOrders(subOrderRatingRequests);
        orderRating.setOrderId(orderResponse.getId());
        restTemplate.postForEntity(ClientService.FOOD_ORDER_SERVICE_URL + FoodOrderingServiceUrls.RATING, orderRating, Void.class);

        new Client().init();
    }

    private OrderResponse submitOrderToFoodOrderingService(OrderRequest orderRequest) {
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(ClientService.FOOD_ORDER_SERVICE_URL + FoodOrderingServiceUrls.ORDER, orderRequest, OrderResponse.class);

        return response.getBody();
    }

    private MenuResponse requestMenu() {
        ResponseEntity<MenuResponse> response = restTemplate.getForEntity(ClientService.FOOD_ORDER_SERVICE_URL + FoodOrderingServiceUrls.GET_ALL_MENUS, MenuResponse.class);

        return response.getBody();
    }

    private OrderRequest generateRandomOrder(MenuResponse menuResponse) {
        OrderRequest orderRequest = new OrderRequest();
        int numberOfRestaurants = Math.max(1,random.nextInt(menuResponse.getRestaurants()));
        List<Restaurant> restaurants = new ArrayList<>(menuResponse.getRestaurantData());
        List<SubOrderRequest> subOrderRequests = new ArrayList<>();
        for (int i = 0; i < numberOfRestaurants; i++) {
            SubOrderRequest subOrderRequest = new SubOrderRequest();
            subOrderRequests.add(subOrderRequest);
            int restaurantIndex = random.nextInt(menuResponse.getRestaurants());
            Restaurant restaurant = restaurants.get(restaurantIndex);
            restaurants.remove(restaurant);
            int numberOfItems = random.nextInt(2) + 1;
            int maxPrepTime = -1;
            for (int j = 0; j < numberOfItems; j++) {
                int itemIndex = random.nextInt(restaurant.getMenuItems());
                MenuItem menuItem = restaurant.getMenu().get(itemIndex);
                subOrderRequest.getItems().add(menuItem.getId());
                maxPrepTime = Math.max(maxPrepTime, menuItem.getPreparationTime());
            }
            subOrderRequest.setPriority(0);
            subOrderRequest.setMaximumWaitTime(maxPrepTime * 1.8);
            subOrderRequest.setRestaurantId(restaurant.getRestaurantId());
        }
        orderRequest.setSubOrderRequests(subOrderRequests);
        orderRequest.setClientId(id);

        return orderRequest;
    }
}
