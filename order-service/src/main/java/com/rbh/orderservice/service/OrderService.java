package com.rbh.orderservice.service;

import com.rbh.orderservice.dto.InventoryResponse;
import com.rbh.orderservice.dto.OrderLineItemsDto;
import com.rbh.orderservice.dto.OrderRequest;
import com.rbh.orderservice.model.Order;
import com.rbh.orderservice.model.OrderLineItems;
import com.rbh.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;

    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItemsList = orderRequest.getOrderLineItemsDtoList().stream()
                .map(this::mapToDto)
                .toList();
        order.setOrderLineItems(orderLineItemsList);

        List<String> skuCodes = order.getOrderLineItems().stream().map(OrderLineItems::getSkuCode).toList();

        InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();
        assert inventoryResponses != null;
        if(inventoryResponses.length == 0){
            throw new IllegalArgumentException("Product in not in stock");
        }
        boolean allInStock = Arrays.stream(inventoryResponses).allMatch(InventoryResponse::isInStock);

        if (allInStock) {
            orderRepository.save(order);
            return "Order Placed Successfully";
        }
        else {
            throw new IllegalArgumentException("Product in not in stock");
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        return orderLineItems;
    }
}
