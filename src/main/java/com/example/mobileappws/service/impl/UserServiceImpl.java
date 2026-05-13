package com.example.mobileappws.service.impl;

import com.example.mobileappws.io.repository.PasswordResetTokenRepository;
import com.example.mobileappws.io.repository.UserRepository;
import com.example.mobileappws.exceptions.UserServiceException;
import com.example.mobileappws.io.entity.PasswordResetTokenEntity;
import com.example.mobileappws.io.entity.UserEntity;
import com.example.mobileappws.service.UserService;
import com.example.mobileappws.shared.AmazonSES;
import com.example.mobileappws.shared.Utils;
import com.example.mobileappws.shared.dto.AddressDto;
import com.example.mobileappws.shared.dto.UserDto;
import com.example.mobileappws.ui.model.response.ErrorMessages;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService  {

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    Utils utils;

    @Autowired
    BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    AmazonSES amazonSES;

    @Override
    public UserDto createUser(UserDto user) {
        if (userRepository.findByEmail(user.getEmail()) != null)
            throw new RuntimeException("Email already exists!");

        // generate public address Id and set it back to the user
        for (int i=0; i<user.getAddresses().size(); i++) {
            AddressDto address = user.getAddresses().get(i);
            address.setUserDetails(user);
            address.setAddressId(utils.generateAddressId(30));
            user.getAddresses().set(i, address);
        }

        ModelMapper modelMapper = new ModelMapper();
        UserEntity userEntity = modelMapper.map(user, UserEntity.class);
        String publicUserId = utils.generateUserId(30);
        userEntity.setUserId(publicUserId);
        userEntity.setEncryptedPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        userEntity.setEmailVerificationToken(utils.generateEmailVerificationToken(publicUserId));
        userEntity.setEmailVerificationStatus(false);

        UserEntity storedUserDetails = userRepository.save(userEntity);
        UserDto returnValue = modelMapper.map(storedUserDetails, UserDto.class);

        // Send email message to user to verify their email address
        amazonSES.verifyEmail(returnValue);
        return returnValue;
    }

    @Override
    public UserDto getUser(String email) {
        UserEntity userEntity = userRepository.findByEmail(email);
        if (userEntity == null) throw new UsernameNotFoundException(email);

        UserDto returnValue = new UserDto();
        BeanUtils.copyProperties(userEntity, returnValue);
        return returnValue;
    }

    @Override
    public List<UserDto> getUsers(int page, int limit) {
        List<UserDto> returnValue = new ArrayList<>();

        if (page > 0) page -= 1;
        Pageable pageableRequest = PageRequest.of(page, limit);
        Page<UserEntity> usersPage = userRepository.findAll(pageableRequest);
        List<UserEntity> users = usersPage.getContent();

        ModelMapper modelMapper = new ModelMapper();
        for (UserEntity userEntity : users) {
            UserDto userDto = modelMapper.map(userEntity, UserDto.class);
            returnValue.add(userDto);
        }
        return returnValue;
    }

    // UserDetailsService will load the UserDetails by username
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByEmail(username);
        if (userEntity == null) throw new UsernameNotFoundException(username);

        // User class from spring framework
        // spring will handle the password
        // third param is user's permissions
        return new User(userEntity.getEmail(), userEntity.getEncryptedPassword(),
            userEntity.getEmailVerificationStatus(),
            true, true, true,
            new ArrayList<>());
    }

    public UserDto getUserByUserId(String userId)
    {
        ModelMapper modelMapper = new ModelMapper();
        UserEntity userEntity = userRepository.findByUserId(userId);
        if (userEntity == null) throw new UsernameNotFoundException(userId);
        UserDto returnValue = modelMapper.map(userEntity, UserDto.class);
        return returnValue;
    }

    @Override
    public UserDto updateUser(String userId, UserDto user) {
        UserDto returnValue = new UserDto();
        UserEntity userEntity = userRepository.findByUserId(userId);
        if (userEntity == null) throw new UserServiceException(ErrorMessages.NO_RECORD_FOUND.getErrorMessage());

        // update firstName
        userEntity.setFirstName(user.getFirstName());
        // update lastName
        userEntity.setLastName(user.getLastName());
        // save entity
        UserEntity updatedUserDetails = userRepository.save(userEntity);
        BeanUtils.copyProperties(updatedUserDetails, returnValue);
        return returnValue;
    }

    @Override
    public void deleteUser(String userId) {
        UserEntity userEntity = userRepository.findByUserId(userId);
        if (userEntity == null) throw new UserServiceException(ErrorMessages.NO_RECORD_FOUND.getErrorMessage());
        userRepository.delete(userEntity);
    }

    @Override
    public boolean verifyEmailToken(String token) {
        boolean returnValue = false;
        UserEntity userEntity = userRepository.findUserByEmailVerificationToken(token);

        if (userEntity != null) {
            boolean hasTokenExpired = Utils.hasTokenExpired(token);
            if (!hasTokenExpired) {
                userEntity.setEmailVerificationToken(null);
                userEntity.setEmailVerificationStatus(Boolean.TRUE);
                userRepository.save(userEntity);
                returnValue = true;
            }
        }
        return returnValue;
    }

    @Override
    public boolean requestPasswordReset(String email) {
        boolean returnValue = false;
        UserEntity userEntity = userRepository.findByEmail(email);
        if (userEntity == null) {
            return returnValue;
        }

        String token = Utils.generatePasswordResetToken(userEntity.getUserId());
        PasswordResetTokenEntity passwordResetTokenEntity = new PasswordResetTokenEntity();
        passwordResetTokenEntity.setToken(token);
        passwordResetTokenEntity.setUserDetails(userEntity);
        passwordResetTokenRepository.save(passwordResetTokenEntity);

        returnValue = amazonSES.sendPasswordResetRequest(
            userEntity.getFirstName(),
            userEntity.getEmail(),
            token
        );

        return returnValue;
    }

    @Override
    public boolean resetPassword(String token, String password) {
        boolean returnValue = false;

        if (Utils.hasTokenExpired(token)) {
            return returnValue;
        }

        PasswordResetTokenEntity passwordResetTokenEntity = passwordResetTokenRepository.findByToken(token);
        if (passwordResetTokenEntity == null) {
            return returnValue;
        }

        String encodedPassword = bCryptPasswordEncoder.encode(password);
        UserEntity userEntity = passwordResetTokenEntity.getUserDetails();
        userEntity.setEncryptedPassword(encodedPassword);
        UserEntity savedUserEntity = userRepository.save(userEntity);

        if (savedUserEntity != null && savedUserEntity.getEncryptedPassword().equals(encodedPassword)) {
            returnValue = true;
        }

        passwordResetTokenRepository.delete(passwordResetTokenEntity);
        return returnValue;
    }
}