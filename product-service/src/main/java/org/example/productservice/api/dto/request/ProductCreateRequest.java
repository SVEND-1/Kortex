package org.example.productservice.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.productservice.db.Category;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public record ProductCreateRequest(
        @NotNull
        @Size(min = 1, max = 128)
        String name,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal price,

        @NotNull
        @Min(value = 0)
        Integer count,

        @NotNull
        @Size(min = 1, max = 3000)
        String description,

        @NotNull
        Category category,

        @NotNull
        List<MultipartFile> imageFiles
) {
}
