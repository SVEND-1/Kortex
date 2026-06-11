package org.example.productservice.domain;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.productservice.api.dto.response.ProductPageResponse;
import org.example.productservice.api.dto.response.ProductResponse;
import org.example.productservice.api.dto.request.ProductSearchFilter;
import org.example.productservice.db.Category;
import org.example.productservice.db.ProductCacheRepository;
import org.example.productservice.db.ProductEntity;
import org.example.productservice.db.ProductRepository;
import org.example.productservice.domain.mapper.ProductMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductCacheRepository productCacheRepository;

    @Async("asyncExecutor")
    public CompletableFuture<ProductPageResponse> findProductsFilter(ProductSearchFilter filter) {
        try {
            Category category = filter.category() != null ? Category.valueOf(filter.category()) : null;
            int pageSize = filter.size() != null ? filter.size() : 10;
            int pageNumber = filter.page() != null ? filter.page() : 0;
            String query = filter.query() != null ? filter.query() : "";

            Pageable pageable = Pageable.ofSize(pageSize).withPage(pageNumber);

            long startTime = System.currentTimeMillis();
            Page<ProductEntity> productsPage = productRepository.findProductsFilter(category, query, pageable);
            long endTime = System.currentTimeMillis();

            log.info("Поиск завершен за {} мс, найдено: {} товаров",
                    (endTime - startTime), productsPage.getTotalElements());

            ProductPageResponse response = productMapper.convertPageEntityToDTO(productsPage);
            return CompletableFuture.completedFuture(response);
        } catch (Exception ex) {
            log.error("Ошибка при загрузке продуктов: {}", ex.getMessage(), ex);
            throw new RuntimeException("Ошибка при загрузке продуктов",ex);
        }
    }

    public ProductResponse getProductDto(Long id) {
        return getById(id);
    }


    public ProductEntity getByIdEntity(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Продукт не найден"));
    }

    public ProductResponse getById(Long id) {
        try {
            ProductResponse product = productCacheRepository.getProduct(id);
            if (product != null) {
                log.debug("Продукт найден в кэше key={}", id);
                return product;
            }

            product = getProductDto(id);
            productCacheRepository.save(product);

            return product;
        }
        catch (Exception e){
            log.error("REDIS ex={}",e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public List<ProductResponse> getProductsBySeller(Long sellerId) {
        return productMapper.convertEntityListToDTO(productRepository.findBySellerId(sellerId));
    }

    //ПОСЛЕ КОММИТА АСИНХРОНО ВЫПОЛНЯТЬ
    //   TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    //            @Override
    //            public void afterCommit() {
    //                String cacheKey = CACHE_KEY_PREFIX + productId;
    //                redisTemplate.delete(cacheKey);
    //            }
    //        });

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void productSubtractQuantity(Long productId, int quantity) {
        try {
            ProductEntity product = getByIdEntity(productId);
            product.setCount(product.getCount() - quantity);
            ProductEntity productEntity = productRepository.save(product);

            productCacheRepository.save(
                    productMapper.convertEntityToDTO(productEntity)
            );
        }catch (Exception e){
            log.error("Ошибка уменьшение количество продукта productId: {}, ex={}", productId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void productAddQuantity(Long productId, int quantity) {
        try {
            ProductEntity product = getByIdEntity(productId);
            product.setCount(product.getCount() + quantity);
            ProductEntity productEntity = productRepository.save(product);

            productCacheRepository.save(//ВЫПОЛНЯТЬ АСИНХРОНО НАДО
                    productMapper.convertEntityToDTO(productEntity)
            );
        }catch (Exception e){
            log.error("Ошибка добавление количество продукта productId: {}, ex={}", productId, e.getMessage());
            throw new RuntimeException(e);
        }
    }


    public ProductEntity create(ProductEntity productToCreate) {
        try {
            return productRepository.save(productToCreate);
        }catch (Exception e){
            log.error("Ошибка сохранение продукта");
            throw new RuntimeException(e);
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ProductEntity update(Long id, ProductEntity productToUpdate) {//TODO ВРОДЕ ПОВТОРЯЕТСЯ С SELLER
        try {
            ProductEntity existingProduct = productRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Продукт не найден"));

            existingProduct.setName(productToUpdate.getName());
            existingProduct.setPrice(productToUpdate.getPrice());
            existingProduct.setCount(productToUpdate.getCount());
            existingProduct.setDescription(productToUpdate.getDescription());
            existingProduct.setCategory(productToUpdate.getCategory());

            if (productToUpdate.getImage() != null && !productToUpdate.getImage().isEmpty()) {
                existingProduct.setImage(productToUpdate.getImage());
            }

            ProductEntity productUpdated = productRepository.save(existingProduct);

            productCacheRepository.remove(id);//TODO по моему логично сразу и добавить будет в кэш

            return productUpdated;
        }
        catch (Exception e){
            log.error("Ошибка обновление продукта с id: {}, ex={}", id, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void deleted(Long id) {
        try {//TODO УДАЛИТЬ ВСЕ КАРТИНКИ
            if (!productRepository.existsById(id)) {
                log.info("Продукт не найден id={}",id);
                throw new NoSuchElementException("Продукт не найден");
            }
            productRepository.deleteById(id);

            productCacheRepository.remove(id);
        }
        catch (Exception e){
            log.error("Ошибка удаление продукта с id: {}, ex={}", id, e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

