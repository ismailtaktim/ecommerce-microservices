package com.ecommerce.customer.service;

import com.ecommerce.customer.dto.request.*;
import com.ecommerce.customer.dto.response.*;
import com.ecommerce.customer.entity.*;
import com.ecommerce.customer.exception.*;
import com.ecommerce.customer.repository.*;
import com.ecommerce.customer.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository addressRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // ==================== AUTH ====================

    @Override
    public CustomerResponse register(RegisterRequest request) {
        // Email kontrolü
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Bu e-posta adresi zaten kayıtlı");
        }

        // Customer oluştur
        Customer customer = Customer.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .status(CustomerStatus.ACTIVE) // Şimdilik direkt aktif
                .role(CustomerRole.CUSTOMER)
                .emailVerified(true) // Şimdilik direkt onaylı
                .build();

        customer = customerRepository.save(customer);
        log.info("Yeni müşteri kaydedildi: {}", customer.getEmail());

        return mapToCustomerResponse(customer);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // Müşteri bul
        Customer customer = customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Geçersiz e-posta veya şifre"));

        // Hesap kilitli mi?
        if (customer.isLocked()) {
            throw new UnauthorizedException("Hesabınız kilitli. Lütfen daha sonra tekrar deneyin");
        }

        // Hesap aktif mi?
        if (!customer.isActive()) {
            throw new UnauthorizedException("Hesabınız aktif değil");
        }

        // Şifre kontrolü
        if (!passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
            // Başarısız giriş sayısını artır
            customer.setFailedLoginAttempts(customer.getFailedLoginAttempts() + 1);

            // 5 başarısız denemede kilitle
            if (customer.getFailedLoginAttempts() >= 5) {
                customer.setLockedUntil(LocalDateTime.now().plusMinutes(15));
                log.warn("Hesap kilitlendi: {}", customer.getEmail());
            }

            customerRepository.save(customer);
            throw new UnauthorizedException("Geçersiz e-posta veya şifre");
        }

        // Başarılı giriş - sayacı sıfırla
        customer.setFailedLoginAttempts(0);
        customer.setLockedUntil(null);
        customer.setLastLoginAt(LocalDateTime.now());
        customerRepository.save(customer);

        // Token oluştur
        String accessToken = jwtService.generateAccessToken(customer);
        String refreshToken = jwtService.generateRefreshToken(customer);

        // Refresh token'ı kaydet
        saveRefreshToken(customer, refreshToken);

        log.info("Müşteri giriş yaptı: {}", customer.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration())
                .customer(mapToCustomerResponse(customer))
                .build();
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Geçersiz refresh token"));

        if (!refreshToken.isValid()) {
            throw new UnauthorizedException("Refresh token geçersiz veya süresi dolmuş");
        }

        Customer customer = refreshToken.getCustomer();

        // Eski token'ı iptal et
        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        // Yeni token'lar oluştur
        String newAccessToken = jwtService.generateAccessToken(customer);
        String newRefreshToken = jwtService.generateRefreshToken(customer);

        // Yeni refresh token'ı kaydet
        saveRefreshToken(customer, newRefreshToken);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration())
                .customer(mapToCustomerResponse(customer))
                .build();
    }

    @Override
    public void logout(UUID customerId) {
        refreshTokenRepository.revokeAllByCustomerId(customerId, LocalDateTime.now());
        log.info("Müşteri çıkış yaptı: {}", customerId);
    }

    // ==================== PROFILE ====================

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getProfile(UUID customerId) {
        Customer customer = findCustomerById(customerId);
        return mapToCustomerResponse(customer);
    }

    @Override
    public CustomerResponse updateProfile(UUID customerId, UpdateProfileRequest request) {
        Customer customer = findCustomerById(customerId);

        if (request.getFirstName() != null) {
            customer.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            customer.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            customer.setPhone(request.getPhone());
        }

        customer = customerRepository.save(customer);
        log.info("Profil güncellendi: {}", customerId);

        return mapToCustomerResponse(customer);
    }

    @Override
    public void changePassword(UUID customerId, ChangePasswordRequest request) {
        Customer customer = findCustomerById(customerId);

        // Mevcut şifre kontrolü
        if (!passwordEncoder.matches(request.getCurrentPassword(), customer.getPasswordHash())) {
            throw new BadRequestException("Mevcut şifre yanlış");
        }

        // Yeni şifreyi kaydet
        customer.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        customerRepository.save(customer);

        // Tüm refresh token'ları iptal et
        refreshTokenRepository.revokeAllByCustomerId(customerId, LocalDateTime.now());

        log.info("Şifre değiştirildi: {}", customerId);
    }

    // ==================== ADDRESS ====================

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(UUID customerId) {
        List<CustomerAddress> addresses = addressRepository.findByCustomerIdAndIsActiveTrue(customerId);
        return addresses.stream()
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AddressResponse addAddress(UUID customerId, AddressRequest request) {
        Customer customer = findCustomerById(customerId);

        // Maksimum 10 adres kontrolü
        long addressCount = addressRepository.countByCustomerIdAndIsActiveTrue(customerId);
        if (addressCount >= 10) {
            throw new BadRequestException("En fazla 10 adres ekleyebilirsiniz");
        }

        // İlk adres varsayılan olsun
        boolean isDefault = addressCount == 0 || Boolean.TRUE.equals(request.getIsDefault());

        // Eğer yeni adres varsayılan yapılacaksa, diğerlerini kaldır
        if (isDefault) {
            addressRepository.clearDefaultAddress(customerId);
        }

        CustomerAddress address = CustomerAddress.builder()
                .customer(customer)
                .title(request.getTitle())
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .district(request.getDistrict())
                .city(request.getCity())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .isDefault(isDefault)
                .isActive(true)
                .build();

        address = addressRepository.save(address);
        log.info("Yeni adres eklendi: {} - {}", customerId, address.getId());

        return mapToAddressResponse(address);
    }

    @Override
    public AddressResponse updateAddress(UUID customerId, UUID addressId, AddressRequest request) {
        CustomerAddress address = addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Adres bulunamadı"));

        address.setTitle(request.getTitle());
        address.setRecipientName(request.getRecipientName());
        address.setRecipientPhone(request.getRecipientPhone());
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setDistrict(request.getDistrict());
        address.setCity(request.getCity());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());

        // Varsayılan adres değişikliği
        if (Boolean.TRUE.equals(request.getIsDefault()) && !address.getIsDefault()) {
            addressRepository.clearDefaultAddress(customerId);
            address.setIsDefault(true);
        }

        address = addressRepository.save(address);
        log.info("Adres güncellendi: {} - {}", customerId, addressId);

        return mapToAddressResponse(address);
    }

    @Override
    public void deleteAddress(UUID customerId, UUID addressId) {
        CustomerAddress address = addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Adres bulunamadı"));

        // Soft delete
        address.setIsActive(false);

        // Eğer silinen varsayılan adresse, başka bir adresi varsayılan yap
        if (address.getIsDefault()) {
            address.setIsDefault(false);
            addressRepository.save(address);

            // İlk aktif adresi varsayılan yap
            List<CustomerAddress> activeAddresses = addressRepository.findByCustomerIdAndIsActiveTrue(customerId);
            if (!activeAddresses.isEmpty()) {
                CustomerAddress newDefault = activeAddresses.get(0);
                newDefault.setIsDefault(true);
                addressRepository.save(newDefault);
            }
        } else {
            addressRepository.save(address);
        }

        log.info("Adres silindi: {} - {}", customerId, addressId);
    }

    // ==================== VERIFICATION ====================

    @Override
    public void verifyEmail(String token) {
        // TODO: Email doğrulama implementasyonu
        log.info("Email doğrulama: {}", token);
    }

    @Override
    public void forgotPassword(String email) {
        // TODO: Şifre sıfırlama e-postası gönderme
        log.info("Şifre sıfırlama talebi: {}", email);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        // TODO: Şifre sıfırlama implementasyonu
        log.info("Şifre sıfırlama: {}", token);
    }

    // ==================== HELPER METHODS ====================

    private Customer findCustomerById(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Müşteri bulunamadı"));
    }

    private void saveRefreshToken(Customer customer, String refreshToken) {
        RefreshToken token = RefreshToken.builder()
                .customer(customer)
                .tokenHash(hashToken(refreshToken))
                .expiresAt(LocalDateTime.now().plusSeconds(jwtService.getRefreshTokenExpiration() / 1000))
                .build();
        refreshTokenRepository.save(token);
    }

    private String hashToken(String token) {
        // Basit hash - production'da daha güvenli bir yöntem kullanılmalı
        return String.valueOf(token.hashCode());
    }

    private CustomerResponse mapToCustomerResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .email(customer.getEmail())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .phone(customer.getPhone())
                .status(customer.getStatus())
                .role(customer.getRole())
                .emailVerified(customer.getEmailVerified())
                .lastLoginAt(customer.getLastLoginAt())
                .createdAt(customer.getCreatedAt())
                .build();
    }

    private AddressResponse mapToAddressResponse(CustomerAddress address) {
        return AddressResponse.builder()
                .id(address.getId())
                .title(address.getTitle())
                .recipientName(address.getRecipientName())
                .recipientPhone(address.getRecipientPhone())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .district(address.getDistrict())
                .city(address.getCity())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .isDefault(address.getIsDefault())
                .build();
    }
}