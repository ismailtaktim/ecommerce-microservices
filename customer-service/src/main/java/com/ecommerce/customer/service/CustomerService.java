package com.ecommerce.customer.service;

import com.ecommerce.customer.dto.request.*;
import com.ecommerce.customer.dto.response.*;

import java.util.List;
import java.util.UUID;

public interface CustomerService {

    // Auth
    CustomerResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void logout(UUID customerId);

    // Profile
    CustomerResponse getProfile(UUID customerId);
    CustomerResponse updateProfile(UUID customerId, UpdateProfileRequest request);
    void changePassword(UUID customerId, ChangePasswordRequest request);

    // Address
    List<AddressResponse> getAddresses(UUID customerId);
    AddressResponse addAddress(UUID customerId, AddressRequest request);
    AddressResponse updateAddress(UUID customerId, UUID addressId, AddressRequest request);
    void deleteAddress(UUID customerId, UUID addressId);

    // Verification
    void verifyEmail(String token);
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
}