package org.example.productservice.domain.mapper;


import org.example.productservice.api.dto.response.ProductPageResponse;
import org.example.productservice.api.dto.response.ProductResponse;
import org.example.productservice.db.ProductEntity;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse convertEntityToDTO(ProductEntity productEntity);


    List<ProductResponse> convertEntityListToDTO(List<ProductEntity> productEntities);

    default ProductPageResponse convertPageEntityToDTO(Page<ProductEntity> productEntities) {
        if (productEntities == null) {
            return null;
        }

        return new ProductPageResponse(
                convertEntityListToDTO(productEntities.getContent()),
                productEntities.getNumber(),
                productEntities.getSize(),
                productEntities.getTotalElements(),
                productEntities.getTotalPages(),
                productEntities.isFirst(),
                productEntities.isLast(),
                productEntities.isEmpty()
        );
    }
}
