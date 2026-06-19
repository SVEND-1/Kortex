package org.example.rest;

public record AddressRestResponse(
        String region,
        String city,
        String street,
        String house,
        String apartment
) {
}