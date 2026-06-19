package org.example.command;

import org.example.rest.AddressRestResponse;
import org.example.saga.OrderCreateRequest;

import java.util.List;


public record DeliveryCommand(
        Long orderId,
        Long userId,
        AddressRestResponse address,
        List<ItemsDelivery> items,
        String comment
) {
}
