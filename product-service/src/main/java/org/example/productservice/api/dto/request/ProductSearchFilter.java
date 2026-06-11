package org.example.productservice.api.dto.request;



public record ProductSearchFilter(String category,
                                  String query,
                                  Integer size,
                                  Integer page) {
}
