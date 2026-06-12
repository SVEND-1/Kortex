package org.example.productservice.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.productservice.api.dto.request.ProductCreateRequest;
import org.example.productservice.api.dto.request.ProductUpdateRequest;
import org.example.productservice.api.dto.response.ProductResponse;
import org.example.productservice.domain.SellerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/api/sellers")
@Tag(name = "Seller",description = "Работа с продавцами")
public class SellerController {
    private final SellerService sellerService;

    @Autowired
    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @Operation(summary = "Получение товаров продавца")
    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> getMyProducts(
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return ResponseEntity.ok(sellerService.getMyProducts(userId));
    }

    @Operation(summary = "Получение деталей товара")
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponse> getProduct(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return ResponseEntity.ok(sellerService.getProduct(id,userId));
    }

    @Operation(summary = "Создание товара")
    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> createProduct(
            @ModelAttribute @Valid ProductCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return ResponseEntity.ok(sellerService.createProduct(request,userId));
    }

    @Operation(summary = "Обновление товара")
    @PutMapping(value = "/products/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestBody @Valid ProductUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(sellerService.updateProduct(id, request,userId));
    }

    @Operation(summary = "Обновление картинок продукта")
    @PutMapping(value = "/images/{productId}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> updateProductImage(
            @PathVariable Long productId,
            @ModelAttribute List<MultipartFile> imageFiles,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
            ){
        return ResponseEntity.ok(sellerService.updateImages(productId,imageFiles,userId));
    }

    @Operation(summary = "Удаление товара")
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Boolean> deleteProduct(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return ResponseEntity.ok(sellerService.deleteProduct(id,userId));
    }

}
