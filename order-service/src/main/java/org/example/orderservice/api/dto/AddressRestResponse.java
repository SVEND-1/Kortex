package org.example.orderservice.api.dto;

public record AddressRestResponse(
        String region,
        String city,
        String street,
        String house,
        String apartment
) {
}
