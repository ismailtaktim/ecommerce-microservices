package com.ecommerce.product.service;

import com.ecommerce.product.dto.request.ProductImageRequest;
import com.ecommerce.product.dto.request.ProductRequest;
import com.ecommerce.product.dto.request.ProductSearchRequest;
import com.ecommerce.product.dto.response.ProductListResponse;
import com.ecommerce.product.dto.response.ProductResponse;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductImage;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.exception.BadRequestException;
import com.ecommerce.product.exception.ResourceNotFoundException;
import com.ecommerce.product.mapper.CategoryMapper;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductImageRepository;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final ProductImageRepository productImageRepository;

    public ProductResponse create(ProductRequest request) {
        // 1. SKU benzersiz mi?
        if (productRepository.existsBySku(request.getSku())) {
            throw new BadRequestException("Bu SKU zaten kullanılıyor");
        }

        // 2. Slug benzersiz mi?
        if (productRepository.existsBySlug(request.getSlug())) {
            throw new BadRequestException("Bu slug zaten kullanılıyor");
        }

        // 3. Kategori var mı?
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategori bulunamadı"));

        // 4. Kategori aktif mi?
        if (!category.getIsActive()) {
            throw new BadRequestException("Pasif kategoriye ürün eklenemez");
        }

        // 5. Fiyat kontrolü (liste fiyatı >= satış fiyatı)
        if (request.getListPrice() != null &&
                request.getListPrice().compareTo(request.getPrice()) < 0) {
            throw new BadRequestException("Liste fiyatı satış fiyatından düşük olamaz");
        }

        // 6. Kaydet
        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .slug(request.getSlug())
                .shortDescription(request.getShortDescription())
                .description(request.getDescription())
                .price(request.getPrice())
                .listPrice(request.getListPrice())
                .currency(request.getCurrency())
                .taxRate(request.getTaxRate())
                .category(category)
                .weightGrams(request.getWeightGrams())
                .widthCm(request.getWidthCm())
                .heightCm(request.getHeightCm())
                .depthCm(request.getDepthCm())
                .mainImageUrl(request.getMainImageUrl())
                .status(ProductStatus.DRAFT)
                .build();

        product = productRepository.save(product);
        return productMapper.toResponse(product);
    }

    public ProductResponse update(UUID id, ProductRequest request) {
        // 1. Ürün var mı?
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı"));

        // 2. SKU değiştiyse, benzersiz mi?
        if (!product.getSku().equals(request.getSku()) &&
                productRepository.existsBySku(request.getSku())) {
            throw new BadRequestException("Bu SKU zaten kullanılıyor");
        }

        // 3. Slug değiştiyse, benzersiz mi?
        if (!product.getSlug().equals(request.getSlug()) &&
                productRepository.existsBySlug(request.getSlug())) {
            throw new BadRequestException("Bu slug zaten kullanılıyor");
        }

        // 4. Kategori değiştiyse, yeni kategori var mı?
        if (!product.getCategory().getId().equals(request.getCategoryId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Kategori bulunamadı"));

            if (!category.getIsActive()) {
                throw new BadRequestException("Pasif kategoriye ürün taşınamaz");
            }
            product.setCategory(category);
        }

        // 5. Fiyat kontrolü
        if (request.getListPrice() != null &&
                request.getListPrice().compareTo(request.getPrice()) < 0) {
            throw new BadRequestException("Liste fiyatı satış fiyatından düşük olamaz");
        }

        // 6. Güncelle
        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setSlug(request.getSlug());
        product.setShortDescription(request.getShortDescription());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setListPrice(request.getListPrice());
        product.setCurrency(request.getCurrency());
        product.setTaxRate(request.getTaxRate());
        product.setWeightGrams(request.getWeightGrams());
        product.setWidthCm(request.getWidthCm());
        product.setHeightCm(request.getHeightCm());
        product.setDepthCm(request.getDepthCm());
        product.setMainImageUrl(request.getMainImageUrl());

        product = productRepository.save(product);
        return productMapper.toResponse(product);
    }

    public void delete(UUID id) {
        // 1. Ürün var mı?
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı"));

        // 2. Soft delete - durumu DISCONTINUED yap
        product.setStatus(ProductStatus.DISCONTINUED);
        productRepository.save(product);

        // Veya hard delete:
        // productRepository.delete(product);
    }

    @Override
    public ProductResponse getById(UUID id) {
        return null;
    }

    @Override
    public ProductResponse getBySku(String sku) {
        return null;
    }

    @Override
    public ProductResponse getBySlug(String slug) {
        return null;
    }

    @Override
    public Page<ProductListResponse> getAll(Pageable pageable) {
        return null;
    }

    @Override
    public Page<ProductListResponse> getByCategory(UUID categoryId, Pageable pageable) {
        return null;
    }

    @Override
    public Page<ProductListResponse> search(ProductSearchRequest request) {
        // 1. Pageable oluştur
        Sort sort = Sort.by(
                request.getSortDirection().equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC,
                request.getSortBy()
        );
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        // 2. Arama yap
        Page<Product> products;

        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            products = productRepository.searchByKeyword(request.getKeyword(), request.getStatus(), pageable);
        } else if (request.getCategoryId() != null) {
            products = productRepository.findByCategoryIdAndStatus(request.getCategoryId(), request.getStatus(), pageable);
        } else if (request.getMinPrice() != null && request.getMaxPrice() != null) {
            products = productRepository.findByPriceRange(request.getMinPrice(), request.getMaxPrice(), pageable);
        } else {
            products = productRepository.findByStatus(request.getStatus(), pageable);
        }

        // 3. Map ve dön
        return products.map(productMapper::toListResponse);
    }

    public ProductResponse updateStatus(UUID id, ProductStatus status) {
        // 1. Ürün var mı?
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı"));

        // 2. ACTIVE yapılacaksa kontroller
        if (status == ProductStatus.ACTIVE) {
            // Fiyat var mı?
            if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Fiyatı olmayan ürün aktif edilemez");
            }

            // Kategori aktif mi?
            if (!product.getCategory().getIsActive()) {
                throw new BadRequestException("Pasif kategorideki ürün aktif edilemez");
            }
        }

        product.setStatus(status);
        product = productRepository.save(product);
        return productMapper.toResponse(product);
    }

    @Override
    public ProductResponse addImage(UUID productId, ProductImageRequest request) {
        // 1. Ürün var mı?
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı"));

        // 2. Maksimum 10 görsel kontrolü
        if (product.getImages().size() >= 10) {
            throw new BadRequestException("Bir ürüne en fazla 10 görsel eklenebilir");
        }

        // 3. Primary yapılacaksa, diğerlerini kaldır
        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            productImageRepository.clearPrimaryImage(productId);
        }

        // 4. İlk görsel otomatik primary olsun
        boolean isPrimary = product.getImages().isEmpty() || Boolean.TRUE.equals(request.getIsPrimary());

        // 5. Kaydet
        ProductImage image = ProductImage.builder()
                .product(product)
                .imageUrl(request.getImageUrl())
                .altText(request.getAltText())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isPrimary(isPrimary)
                .build();

        productImageRepository.save(image);

        // 6. Main image güncelle
        if (isPrimary) {
            product.setMainImageUrl(request.getImageUrl());
            productRepository.save(product);
        }

        return productMapper.toResponse(product);
    }

    @Override
    public void removeImage(UUID productId, UUID imageId) {
        // 1. Ürün var mı?
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı"));

        // 2. Görsel bu ürüne ait mi?
        ProductImage image = product.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Görsel bulunamadı"));

        boolean wasPrimary = image.getIsPrimary();

        // 3. Sil
        productImageRepository.delete(image);

        // 4. Silinen primary idiyse, yeni primary belirle
        if (wasPrimary) {
            List<ProductImage> remainingImages = productImageRepository.findByProductIdOrderBySortOrder(productId);
            if (!remainingImages.isEmpty()) {
                ProductImage newPrimary = remainingImages.get(0);
                newPrimary.setIsPrimary(true);
                productImageRepository.save(newPrimary);
                product.setMainImageUrl(newPrimary.getImageUrl());
            } else {
                product.setMainImageUrl(null);
            }
            productRepository.save(product);
        }
    }
}
