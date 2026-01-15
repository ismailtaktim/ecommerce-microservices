package com.ecommerce.customer.controller;

import com.ecommerce.customer.dto.request.*;
import com.ecommerce.customer.dto.response.*;
import com.ecommerce.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // ==================== PROFILE ====================

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CustomerResponse>> getProfile(@AuthenticationPrincipal UUID customerId) {
        CustomerResponse customer = customerService.getProfile(customerId);
        return ResponseEntity.ok(ApiResponse.success(customer));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateProfile(
            @AuthenticationPrincipal UUID customerId,
            @Valid @RequestBody UpdateProfileRequest request) {
        CustomerResponse customer = customerService.updateProfile(customerId, request);
        return ResponseEntity.ok(ApiResponse.success("Profil güncellendi", customer));
    }

    @PostMapping("/me/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UUID customerId,
            @Valid @RequestBody ChangePasswordRequest request) {
        customerService.changePassword(customerId, request);
        return ResponseEntity.ok(ApiResponse.success("Şifre değiştirildi", null));
    }

    @PostMapping("/me/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UUID customerId) {
        customerService.logout(customerId);
        return ResponseEntity.ok(ApiResponse.success("Çıkış yapıldı", null));
    }

    // ==================== ADDRESSES ====================

    @GetMapping("/me/addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(@AuthenticationPrincipal UUID customerId) {
        List<AddressResponse> addresses = customerService.getAddresses(customerId);
        return ResponseEntity.ok(ApiResponse.success(addresses));
    }

    @PostMapping("/me/addresses")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @AuthenticationPrincipal UUID customerId,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse address = customerService.addAddress(customerId, request);
        return ResponseEntity.ok(ApiResponse.success("Adres eklendi", address));
    }

    @PutMapping("/me/addresses/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal UUID customerId,
            @PathVariable UUID addressId,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse address = customerService.updateAddress(customerId, addressId, request);
        return ResponseEntity.ok(ApiResponse.success("Adres güncellendi", address));
    }

    @DeleteMapping("/me/addresses/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal UUID customerId,
            @PathVariable UUID addressId) {
        customerService.deleteAddress(customerId, addressId);
        return ResponseEntity.ok(ApiResponse.success("Adres silindi", null));
    }
}