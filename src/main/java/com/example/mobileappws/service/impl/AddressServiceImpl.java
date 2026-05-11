package com.example.mobileappws.service.impl;

import com.example.mobileappws.io.repository.AddressRepository;
import com.example.mobileappws.io.repository.UserRepository;
import com.example.mobileappws.io.entity.AddressEntity;
import com.example.mobileappws.io.entity.UserEntity;
import com.example.mobileappws.service.AddressService;
import com.example.mobileappws.shared.dto.AddressDto;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    AddressRepository addressRepository;

    @Override
    public List<AddressDto> getUserAddresses(String userId) {
        List<AddressDto> returnValue = new ArrayList<>();
        UserEntity userEntity = userRepository.findByUserId(userId);
        if (userEntity == null)
            throw new RuntimeException("Error");

        Iterable<AddressEntity> addresses = addressRepository.findAllByUserDetails(userEntity);
        ModelMapper modelMapper = new ModelMapper();
        for (AddressEntity addressEntity : addresses) {
            AddressDto addressDto = modelMapper.map(addressEntity, AddressDto.class);
            returnValue.add(addressDto);
        }
        return returnValue;
    }

    @Override
    public AddressDto getAddress(String addressId) {
        ModelMapper modelMapper = new ModelMapper();
        AddressEntity addressEntity = addressRepository.findByAddressId(addressId);
        if (addressEntity == null)
            throw new RuntimeException("AddressEntity not found");
        AddressDto returnValue = modelMapper.map(addressEntity, AddressDto.class);
        return returnValue;
    }
}
