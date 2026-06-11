package org.example.productservice.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.productservice.api.dto.request.ProductSearchFilter;
import org.example.productservice.api.dto.response.ProductPageResponse;
import org.example.productservice.domain.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/products")//TODO В API GATEWAY ДОБАВИТЬ БЕЗ РЕГИСТРАЦИИ ЧТОБЫ РАБОТАЛО
@Tag(name = "Product",description = "Работа с товарами")
public class ProductController {
    private final ProductService productService;

    @Operation(summary = "Получение списка продуктов с фильтром")
    @GetMapping
    public CompletableFuture<ResponseEntity<ProductPageResponse>> getProducts(@ModelAttribute ProductSearchFilter filter) {
        return productService.findProductsFilter(filter)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @Operation(summary = "Получение деталей товара")
    @GetMapping("/{id}")
    public ResponseEntity<?> productDetailPage(@PathVariable String id)  {
        return ResponseEntity.ok(productService.getProductDto(Long.parseLong(id)));
    }
}
