package com.ecommerce.product.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductRequest {

    @NotBlank(message = "SKU boş olamaz")
    private String sku;

    @NotBlank(message = "Ürün adı boş olamaz")
    private String name;

    @NotBlank(message = "Slug boş olamaz")
    private String slug;

    private String shortDescription;

    private String description;

    @NotNull(message = "Fiyat boş olamaz")
    @DecimalMin(value = "0.0", inclusive = false, message = "Fiyat 0'dan büyük olmalıdır")
    private BigDecimal price;

    @DecimalMin(value = "0.0", message = "Liste fiyatı 0 veya daha büyük olmalıdır")
    private BigDecimal listPrice;

    private String currency = "TRY";

    @DecimalMin(value = "0.0", message = "KDV oranı 0 veya daha büyük olmalıdır")
    @DecimalMax(value = "100.0", message = "KDV oranı 100'den büyük olamaz")
    private BigDecimal taxRate = new BigDecimal("18.00");

    @NotNull(message = "Kategori ID boş olamaz")
    private UUID categoryId;

    private Integer weightGrams;

    private BigDecimal widthCm;

    private BigDecimal heightCm;

    private BigDecimal depthCm;

    private String mainImageUrl;
}