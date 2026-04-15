package com.example.mobileappws.service;

import com.example.mobileappws.shared.dto.AddressDto;

import java.util.List;

public interface AddressService {
    List<AddressDto> getUserAddresses(String userId);
    AddressDto getAddress(String addressId);
}
