package org.example.userservice.api.dto.response;

public record AddressRestResponse(
        String region,
        String city,
        String street,
        String house,
        String apartment
) {
}
