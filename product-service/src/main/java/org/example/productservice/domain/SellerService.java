package org.example.productservice.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.productservice.api.dto.request.ProductRequest;
import org.example.productservice.api.dto.response.ProductResponse;
import org.example.productservice.db.ProductEntity;
import org.example.productservice.domain.mapper.ProductMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerService {//TODO проверять что это товар у продавца
    private final ProductService productService;
    private final ProductMapper productMapper;


    public List<ProductResponse> getMyProducts(Long userId) {
        try {
            return productService.getProductsBySeller(userId);
        } catch (Exception e) {
            log.error("Ошибка при получении товаров, ex={}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public ProductResponse getProduct(Long id) {
        try {
            ProductResponse product = productService.getById(id);
            if(product == null) {
                log.warn("Продукт не является товарам продавца");
                throw new IllegalArgumentException("Продукт не является товарам продавца");
            }
            return product;
        } catch (Exception e) {
            log.error("Ошибка поиска продукта, ex={}", e.getMessage());
            return null;
        }
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request,Long userId) {
        try {
            ProductEntity product = ProductEntity.builder()
                    .name(request.name())
                    .price(request.price())
                    .count(request.count())
                    .description(request.description())
                    .category(request.category())
                    .sellerId(userId)
                    .build();

            if (request.imageFile() != null && !request.imageFile().isEmpty()) {
                String imageName = saveImage(request.imageFile());
                product.setImage(imageName);
            }

            ProductEntity createdProduct = productService.create(product);
            return productMapper.convertEntityToDTO(createdProduct);
        } catch (Exception e) {
            log.error("Ошибка при создании товара, ex={} ", e.getMessage());
            throw new RuntimeException(e);
        }
    }


    public ProductResponse updateProduct(Long id, ProductRequest request) {
        try {
            ProductEntity existingProduct = productService.getByIdEntity(id);

            existingProduct.setName(request.name());
            existingProduct.setPrice(request.price());
            existingProduct.setCount(request.count());
            existingProduct.setDescription(request.description());
            existingProduct.setCategory(request.category());

            if (request.imageFile() != null && !request.imageFile().isEmpty()) {
                if (existingProduct.getImage() != null) {
                    deleteImage(existingProduct.getImage());
                }

                String imageName = saveImage(request.imageFile());
                existingProduct.setImage(imageName);
            }

            ProductEntity updatedProduct = productService.update(id, existingProduct);
            return productMapper.convertEntityToDTO(updatedProduct);
        } catch (Exception e) {
            log.error("Ошибка при обновлении товара, ex={}", e.getMessage());
            return null;
        }
    }

    @Transactional
    public boolean deleteProduct(Long id) {//TODO Поменять Return
        try {
            ProductEntity product = productService.getByIdEntity(id);
            if (product.getImage() != null && !product.getImage().isEmpty()) {
                deleteImage(product.getImage());
            }

            productService.deleted(id);
            return true;
        } catch (Exception e) {
            log.error("Ошибка при удалении товара, ex={}",e.getMessage());
            throw new RuntimeException(e);
        }
    }



    private String saveImage(MultipartFile imageFile)  {//TODO ДОБАВИТЬ МИНИО И СЖИМАТЬ ФАЙЛЫ
        try {
            String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            Path uploadPath = Paths.get("uploads/images");

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(fileName);

            try (InputStream inputStream = imageFile.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            return fileName;
        }
        catch (IOException e) {
            log.error("Не удалось сохранить картинку товара");
            return null;
        }
    }

    private void deleteImage(String imageName) {
        try {
            Path uploadPath = Paths.get("uploads/images");
            Path filePath = uploadPath.resolve(imageName);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Не удалось удалить изображение image={} ", imageName);
        }
    }
}

