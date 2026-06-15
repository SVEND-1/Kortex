package org.example.userservice.api.dto.request;

public record AddressUpdatedRequest(
        String region,
        String city,
        String street,
        String house,
        String apartment
) {
}
