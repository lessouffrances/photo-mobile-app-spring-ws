package com.example.mobileappws.ui.controller;

import com.example.mobileappws.service.AddressService;
import com.example.mobileappws.service.UserService;
import com.example.mobileappws.shared.dto.AddressDto;
import com.example.mobileappws.shared.dto.UserDto;
import com.example.mobileappws.ui.model.response.UserRest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class UserControllerTest {

    @InjectMocks
    UserController userController;

    @Mock
    UserService userService;

    @Mock
    AddressService addressService;

    UserDto userDto = new UserDto();
    final String USER_ID = "626348279345JBSDHFU";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        userDto.setFirstName("Layla");
        userDto.setLastName("D");
        userDto.setEmail("test@test.com");
        userDto.setEmailVerificationStatus(Boolean.FALSE);
        userDto.setEmailVerificationToken(null);
        userDto.setUserId(USER_ID);
        userDto.setAddresses(getAddressesDto());
        userDto.setEncryptedPassword("KAJWHEF87Q3Y424");
    }

    @Test
    void getUser() {
        when(userService.getUserByUserId(anyString())).thenReturn(userDto);

        UserRest returnValue = userController.getUser(USER_ID);

        assertNotNull(returnValue);
        assertEquals(USER_ID, returnValue.getUserId());
        assertEquals(userDto.getFirstName(), returnValue.getFirstName());
        assertEquals(userDto.getLastName(), returnValue.getLastName());
        assertEquals(userDto.getAddresses().size(), returnValue.getAddresses().size());
    }

    private List<AddressDto> getAddressesDto()
    {
        AddressDto shippingAddressDto = new AddressDto();
        shippingAddressDto.setType("Shipping");
        shippingAddressDto.setCity("Vancouver");
        shippingAddressDto.setCountry("Canada");
        shippingAddressDto.setPostalCode("M5J 0B1");
        shippingAddressDto.setStreetName("14 York st");

        AddressDto billingAddressDto = new AddressDto();
        billingAddressDto.setType("Shipping");
        billingAddressDto.setCity("Vancouver");
        billingAddressDto.setCountry("Canada");
        billingAddressDto.setPostalCode("M5J 0B1");
        billingAddressDto.setStreetName("14 York st");

        List<AddressDto> addresses = new ArrayList<>();
        addresses.add(shippingAddressDto);
        addresses.add(billingAddressDto);
        return addresses;
    }
}