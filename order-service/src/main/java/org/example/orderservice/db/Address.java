package org.example.orderservice.db;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {
    private String region;
    private String city;
    private String street;
    private String house;
    private String apartment;
}
