package org.example.productservice.domain;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.productservice.api.dto.request.ProductSearchFilter;
import org.example.productservice.api.dto.response.ProductPageResponse;
import org.example.productservice.api.dto.response.ProductResponse;
import org.example.productservice.api.dto.response.SearchParams;
import org.example.productservice.db.Category;
import org.example.productservice.db.ProductCacheRepository;
import org.example.productservice.db.ProductEntity;
import org.example.productservice.db.ProductRepository;
import org.example.productservice.domain.mapper.ProductMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductFindManager {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductCacheRepository productCacheRepository;
    private final ProductImageService productImageService;

    @Transactional(readOnly = true)
    public ProductPageResponse findProductsFilter(ProductSearchFilter filter) {
        try {
            SearchParams params = isValidArgument(filter);
            Pageable pageable = Pageable.ofSize(params.pageSize()).withPage(params.pageNumber());
            Page<ProductEntity> productsPage = fetchProductsPage(params.category(), params.query(), pageable);

            ProductPageResponse productPage = productMapper.convertPageEntityToDTO(productsPage);

            List<ProductResponse> contentWithUrls = productPage.content().stream()
                    .map(dto -> {
                        List<String> activeUrls = productImageService.findImages(dto.images());
                        return dto.withImages(activeUrls);
                    })
                    .toList();

            return productMapper.replaceContent(productPage,contentWithUrls);
        } catch (Exception ex) {
            log.error("Ошибка при загрузке продуктов: {}", ex.getMessage(), ex);
            throw new RuntimeException("Ошибка при загрузке продуктов", ex);
        }
    }

    private SearchParams isValidArgument(ProductSearchFilter filter){
        Category category = filter.category() != null ? Category.valueOf(filter.category()) : null;
        int pageSize = filter.size() != null ? filter.size() : 10;
        int pageNumber = filter.page() != null ? filter.page() : 0;
        String query = filter.query() != null ? filter.query() : "";
        return new SearchParams(
                category, pageSize, pageNumber, query);
    }

    private Page<ProductEntity> fetchProductsPage(Category category, String query, Pageable pageable){
        long startTime = System.currentTimeMillis();
        Page<ProductEntity> productsPage = productRepository.findProductsFilter(category, query, pageable);
        long endTime = System.currentTimeMillis();

        log.info("Поиск завершен за {} мс, найдено: {} товаров",
                (endTime - startTime), productsPage.getTotalElements());
        return productsPage;
    }


    @Transactional(readOnly = true)
    public ProductResponse getProductDto(Long id) {
        try {
            ProductResponse cached = productCacheRepository.getProduct(id);
            if (cached != null) {
                List<String> activeUrls = productImageService.findImages(cached.images());
                return cached.withImages(activeUrls);
            }

            ProductEntity productEntity = productRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Продукт не найден с id=" + id));
            ProductResponse productResponse = productMapper.convertEntityToDTO(productEntity);
            productCacheRepository.save(productResponse);
            List<String> activeUrls = productImageService.findImages(productResponse.images());
            return productResponse.withImages(activeUrls);
        } catch (Exception e) {
            log.error("Не удалось получить dto продукта ex={}", e.getMessage());
            throw new RuntimeException(e);
        }
    }


    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsBySeller(Long sellerId) {
        List<ProductEntity> products = productRepository.findBySellerId(sellerId);
        return products.stream()
                .map(productMapper::convertEntityToDTO)
                .map(dto -> {
                    List<String> activeUrls = productImageService.findImages(dto.images());
                    return dto.withImages(activeUrls);
                })
                .toList();
    }

}
