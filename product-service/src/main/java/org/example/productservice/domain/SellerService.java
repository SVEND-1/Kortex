package org.example.productservice.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.productservice.api.dto.request.ProductCreateRequest;
import org.example.productservice.api.dto.request.ProductUpdateRequest;
import org.example.productservice.api.dto.response.ProductResponse;
import org.example.productservice.db.ProductEntity;
import org.example.productservice.db.ProductRepository;
import org.example.productservice.domain.exceptions.ProductAccessDeniedException;
import org.example.productservice.domain.mapper.ProductMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerService {

    private final ProductService productService;
    private final ProductMapper productMapper;
    private final ProductRepository productRepository;

    public List<ProductResponse> getMyProducts(Long userId) {
        try {
            return productService.getProductsBySeller(userId);
        } catch (Exception e) {
            log.error("Ошибка при получении товаров, ex={}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public ProductResponse getProduct(Long id,Long sellerId) {
        try {
            validateProductBelongsToSeller(id,sellerId);
            return productService.getProductDto(id);
        } catch (Exception e) {
            log.error("Ошибка поиска продукта, ex={}", e.getMessage());
            return null;
        }
    }

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request, Long sellerId) {
        try {
            ProductEntity product = productService.create(request, sellerId);
            return productMapper.convertEntityToDTO(product);
        } catch (Exception e) {
            log.error("Ошибка при создании товара, ex={} ", e.getMessage());
            throw new RuntimeException(e);
        }
    }


    public ProductResponse updateProduct(Long id, ProductUpdateRequest request,Long sellerId) {
        try {
            validateProductBelongsToSeller(id,sellerId);
            ProductEntity updatedProduct = productService.update(id, request);
            return productMapper.convertEntityToDTO(updatedProduct);
        } catch (Exception e) {
            log.error("Ошибка при обновлении товара, ex={}", e.getMessage());
            return null;
        }
    }

    public ProductResponse updateImages(Long productId, List<MultipartFile> imageFiles, Long sellerId) {
        try {
            validateProductBelongsToSeller(productId,sellerId);
            ProductEntity product = productService.updateImages(productId, imageFiles);
            return productMapper.convertEntityToDTO(product);
        }catch (Exception e) {
            log.error("Ошибка при обновление фотографий,ex={}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public boolean deleteProduct(Long id,Long sellerId) {
        try {
            validateProductBelongsToSeller(id,sellerId);
            productService.deleted(id);
            return true;
        } catch (Exception e) {
            log.error("Ошибка при удалении товара, ex={}",e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void validateProductBelongsToSeller(Long productId, Long sellerId) {
        if(!productRepository.existsByIdAndSellerId(productId,sellerId)) {
            log.warn("Пользователь не является владельцем продукта");
            throw new ProductAccessDeniedException("Пользователь не является владельцем продукта");
        }
    }
}

