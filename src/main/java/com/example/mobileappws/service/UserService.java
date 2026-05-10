package com.example.mobileappws.service;

import com.example.mobileappws.shared.dto.UserDto;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface UserService extends UserDetailsService {

    UserDto createUser(UserDto user);
    UserDto getUser(String email);
    List<UserDto> getUsers(int page, int limit);
    UserDto getUserByUserId(String userId);
    UserDto updateUser(String userId, UserDto user);
    void deleteUser(String userId);
    boolean verifyEmailToken(String token);
    boolean requestPasswordReset(String email);
}
