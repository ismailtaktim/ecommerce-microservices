package com.ecommerce.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateInventoryRequest {

    @NotNull(message = "Product ID boş olamaz")
    private UUID productId;

    @NotBlank(message = "Ürün adı boş olamaz")
    private String productName;

    @NotBlank(message = "SKU boş olamaz")
    private String sku;

    @Min(value = 0, message = "Miktar 0'dan küçük olamaz")
    private Integer initialQuantity = 0;

    @Min(value = 0, message = "Minimum stok seviyesi 0'dan küçük olamaz")
    private Integer minStockLevel = 10;
}