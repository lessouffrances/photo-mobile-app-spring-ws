package com.example.mobileappws.service.impl;

import com.example.mobileappws.io.entity.AddressEntity;
import com.example.mobileappws.io.entity.UserEntity;
import com.example.mobileappws.io.repository.UserRepository;
import com.example.mobileappws.shared.AmazonSES;
import com.example.mobileappws.shared.Utils;
import com.example.mobileappws.shared.dto.AddressDto;
import com.example.mobileappws.shared.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    // SUT: system under test
    @InjectMocks
    UserServiceImpl userService;

    @Mock
    UserRepository userRepository;

    @Mock
    Utils utils;

    @Mock
    AmazonSES amazonSES;

    @Mock
    BCryptPasswordEncoder bCryptPasswordEncoder;

    // reusable stubs
    private String userId = "userId";
    private String encryptedPassword = "zkjsdfgjwR78";
    private UserEntity userEntity = new UserEntity();

    @BeforeEach
    void setUp() {
        // activates the Mockito Annotations for this test class
        MockitoAnnotations.initMocks(this);

        userEntity.setId(1L);
        userEntity.setUserId(userId);
        userEntity.setFirstName("Layla");
        userEntity.setLastName("D");
        userEntity.setEncryptedPassword(encryptedPassword);
        userEntity.setEmail("test@test.com");
        userEntity.setEmailVerificationToken("zkjsdfhbsuerggieanrkj");
        userEntity.setAddresses(getAddressesEntity());
    }

    @Test
    void testCreateUser() {
        when(userRepository.findByEmail(anyString())).thenReturn(null);
        when(utils.generateAddressId(anyInt())).thenReturn("addressId");
        when(utils.generateUserId(anyInt())).thenReturn(userId);
        when(bCryptPasswordEncoder.encode(anyString())).thenReturn(encryptedPassword);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        Mockito.doNothing().when(amazonSES).verifyEmail(any(UserDto.class));

        UserDto userDto = new UserDto();
        userDto.setAddresses(getAddressesDto());
        userDto.setFirstName("Layla");
        userDto.setLastName("D");
        userDto.setPassword("123456");
        userDto.setEmail("test@testm.com");

        UserDto storedUserDetails = userService.createUser(userDto);

        assertNotNull(storedUserDetails);
        assertEquals(userEntity.getFirstName(), storedUserDetails.getFirstName());
        assertEquals(userEntity.getLastName(), storedUserDetails.getLastName());
        assertNotNull(storedUserDetails.getUserId());
        assertEquals(userEntity.getAddresses().size(), storedUserDetails.getAddresses().size());
        verify(utils, times(2)).generateAddressId(30);
        verify(utils, times(1)).generateUserId(30);
        verify(bCryptPasswordEncoder, times(1)).encode("123456");
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    @Test
    void testGetUserHappyPath() {
        // stub fake object
        when(userRepository.findByEmail(anyString())).thenReturn(userEntity);
        UserDto userDto = userService.getUser("test@test.com");

        assertNotNull(userDto);
        assertEquals(1L, userDto.getId());
        assertEquals("userId", userDto.getUserId());
        assertEquals("Layla", userDto.getFirstName());
        assertEquals("zkjsdfgjwR78", userDto.getEncryptedPassword());
    }

    @Test
    void testGetUserThrowsUsernameNotFoundException() {
        when(userRepository.findByEmail(anyString())).thenReturn(null);

        assertThrows(UsernameNotFoundException.class,
            ()-> {
            userService.getUser("test@test.com");
            });
    }

    @Test
    void testGetUserThrowsRuntimeExceptionDuplicateEmail() {
        when(userRepository.findByEmail(anyString())).thenReturn(userEntity);

        UserDto userDto = new UserDto();
        userDto.setAddresses(getAddressesDto());
        userDto.setFirstName("Layla");
        userDto.setLastName("D");
        userDto.setPassword("123456");
        userDto.setEmail("test@testm.com");

        assertThrows(RuntimeException.class,
            ()-> {
                userService.createUser(userDto);
            });
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

    private List<AddressEntity> getAddressesEntity()
    {
        List<AddressDto> addressDtos = getAddressesDto();
        Type listType = new TypeToken<List<AddressEntity>>() {}.getType();
        return new ModelMapper().map(addressDtos, listType);
    }
}