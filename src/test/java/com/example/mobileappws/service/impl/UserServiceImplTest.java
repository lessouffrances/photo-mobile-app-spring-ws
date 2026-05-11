package com.example.mobileappws.service.impl;

import com.example.mobileappws.io.entity.UserEntity;
import com.example.mobileappws.io.repository.PasswordResetTokenRepository;
import com.example.mobileappws.io.repository.UserRepository;
import com.example.mobileappws.service.UserService;
import com.example.mobileappws.shared.AmazonSES;
import com.example.mobileappws.shared.Utils;
import com.example.mobileappws.shared.dto.AddressDto;
import com.example.mobileappws.shared.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

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
        userEntity.setEncryptedPassword(encryptedPassword);
        userEntity.setEmail("test@test.com");
        userEntity.setEmailVerificationToken("zkjsdfhbsuerggieanrkj");
    }

    @Test
    void testCreateUser() {
        when(userRepository.findByEmail(anyString())).thenReturn(null);
        when(utils.generateAddressId(anyInt())).thenReturn("addressId");
        when(utils.generateUserId(anyInt())).thenReturn(userId);
        when(bCryptPasswordEncoder.encode(anyString())).thenReturn(encryptedPassword);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.generateEmailVerificationToken(anyString()))
                .thenReturn("mockEmailVerificationToken");

            AddressDto addressDto = new AddressDto();
            addressDto.setType("Shipping");
            List<AddressDto> addresses = new ArrayList<>();
            addresses.add(addressDto);
            UserDto userDto = new UserDto();
            userDto.setAddresses(addresses);

            UserDto storedUserDetails = userService.createUser(userDto);
            assertNotNull(storedUserDetails);
            assertEquals(userEntity.getFirstName(), storedUserDetails.getFirstName());
        }
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
}