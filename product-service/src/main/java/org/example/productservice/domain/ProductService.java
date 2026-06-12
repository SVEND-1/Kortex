package org.example.productservice.domain;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.productservice.api.dto.request.ProductCreateRequest;
import org.example.productservice.api.dto.request.ProductUpdateRequest;
import org.example.productservice.api.dto.response.ProductPageResponse;
import org.example.productservice.api.dto.response.ProductResponse;
import org.example.productservice.api.dto.request.ProductSearchFilter;
import org.example.productservice.db.ProductCacheRepository;
import org.example.productservice.db.ProductEntity;
import org.example.productservice.db.ProductRepository;
import org.example.productservice.domain.mapper.ProductMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductCacheRepository productCacheRepository;
    private final ProductImageService productImageService;
    private final ProductFindManager productFindManager;

    @Async("asyncExecutor")
    public CompletableFuture<ProductPageResponse> findProductsFilter(ProductSearchFilter filter) {
        return CompletableFuture.completedFuture(productFindManager.findProductsFilter(filter));
    }

    public ProductResponse getProductDto(Long id) {
        return productFindManager.getProductDto(id);
    }

    public List<ProductResponse> getProductsBySeller(Long sellerId) {
        return productFindManager.getProductsBySeller(sellerId);
    }

    public ProductEntity getByIdEntity(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Продукт не найден"));
    }

    //TODO ПОСЛЕ КОММИТА АСИНХРОНО ВЫПОЛНЯТЬ
    //   TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    //            @Override
    //            public void afterCommit() {
    //                String cacheKey = CACHE_KEY_PREFIX + productId;
    //                redisTemplate.delete(cacheKey);
    //            }
    //        });

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void productSubtractQuantity(Long productId, int quantity) {//TODO тут в кафка при создание заказа
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


    @Transactional
    public ProductEntity create(ProductCreateRequest request,Long sellerId) {
        try {
            List<String> imagePaths =  productImageService.saveImages(request.imageFiles());
            ProductEntity product = ProductEntity.builder()
                    .name(request.name())
                    .price(request.price())
                    .count(request.count())
                    .description(request.description())
                    .category(request.category())
                    .sellerId(sellerId)
                    .images(imagePaths)
                    .build();
            return productRepository.save(product);
        }catch (Exception e){
            log.error("Ошибка сохранение продукта");
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public ProductEntity update(Long id, ProductUpdateRequest request) {
        try {
            ProductEntity existingProduct = productRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Продукт не найден"));

            productMapper.updateEntity(existingProduct,request);
            ProductEntity productUpdated = productRepository.save(existingProduct);
            productCacheRepository.remove(id);
            return productUpdated;
        }
        catch (Exception e){
            log.error("Ошибка обновление продукта с id: {}, ex={}", id, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public ProductEntity updateImages(Long productId, List<MultipartFile> imageFiles) {
        try {
            ProductEntity product = getByIdEntity(productId);

            List<String> imagePathsUpdated = productImageService.updateImages(product.getImages(),imageFiles);
            product.setImages(imagePathsUpdated);

            productRepository.save(product);
            productCacheRepository.remove(productId);
            return product;
        }catch (Exception e){
            log.error("Не получилось обновить фотографии у продукта, ex={}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void deleted(Long id) {
        try {
            ProductEntity product = getByIdEntity(id);
            productRepository.deleteById(id);
            productImageService.deleteImages(product.getImages());
            productCacheRepository.remove(id);
        }
        catch (Exception e){
            log.error("Ошибка удаление продукта с id: {}, ex={}", id, e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

