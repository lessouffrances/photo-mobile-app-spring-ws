package com.example.mobileappws.ui.controller;

import com.example.mobileappws.exceptions.UserServiceException;
import com.example.mobileappws.service.UserService;
import com.example.mobileappws.service.AddressService;
import com.example.mobileappws.shared.dto.AddressDto;
import com.example.mobileappws.shared.dto.UserDto;
import com.example.mobileappws.ui.model.request.PasswordResetModel;
import com.example.mobileappws.ui.model.request.PasswordResetRequestModel;
import com.example.mobileappws.ui.model.request.UserDetailsRequestModel;
import com.example.mobileappws.ui.model.response.*;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("users") // http://localhost:8080/users
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    AddressService addressService;

    // http://localhost:8080/users/publicUserId
    @GetMapping(path="/{id}", produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE })
    public UserRest getUser(@PathVariable String id)
    {
        ModelMapper modelMapper = new ModelMapper();
        UserDto userDto = userService.getUserByUserId(id);
        return modelMapper.map(userDto, UserRest.class);
    }

    // http://localhost:8080/users?page=0&limit=50
    @GetMapping(produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE })
    public List<UserRest> getUsers(@RequestParam(value = "page", defaultValue = "0") int page,
                                   @RequestParam(value = "limit", defaultValue = "2") int limit)
    {
        ModelMapper modelMapper = new ModelMapper();
        List<UserRest> returnValue = new ArrayList<>();
        List<UserDto> users = userService.getUsers(page, limit);

        for (UserDto userDto : users) {
            UserRest userModel = modelMapper.map(userDto, UserRest.class);
            returnValue.add(userModel);
        }
        return returnValue;
    }

    // http://localhost:8080/users/publicUserId/addresses
    @GetMapping(path = "/{id}/addresses",
        produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public CollectionModel<AddressRest> getUserAddresses(@PathVariable String id)
    {
        List<AddressDto> addressesDto = addressService.getUserAddresses(id);
        ModelMapper modelMapper = new ModelMapper();

        Type listType = new TypeToken<List<AddressRest>>() {}.getType();
        List<AddressRest> returnValue = modelMapper.map(addressesDto, listType);
        for (AddressRest addressRest : returnValue) {
            Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(UserController.class)
                    .getUserAddress(addressRest.getAddressId(), id))
                    .withSelfRel();
            addressRest.add(selfLink); // AddressRest extends RepresentationModel
        }

        Link userLink = WebMvcLinkBuilder.linkTo(UserController.class).slash(id).withRel("user");
        Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(UserController.class)
                .getUserAddresses(id))
                .withSelfRel();

        return CollectionModel.of(returnValue, userLink, selfLink);
    }

    // http://localhost:8080/users/publicUserId/addresses/publicAddressId
    @GetMapping(path = "/{userId}/addresses/{addressId}",
        produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public EntityModel<AddressRest> getUserAddress(@PathVariable String addressId, @PathVariable String userId)
    {
        AddressDto addressDto = addressService.getAddress(addressId);
        ModelMapper modelMapper = new ModelMapper();
        AddressRest returnValue = modelMapper.map(addressDto, AddressRest.class);

        // http://localhost::8081/users/<userId>
        Link userLink = WebMvcLinkBuilder.linkTo(UserController.class).slash(userId).withRel("user");
        Link userAddressesLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(UserController.class)
                  .getUserAddresses(userId))
//                .slash(userId)
//                .slash("addresses")
                  .withRel("addresses");

        Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(UserController.class)
                  .getUserAddress(addressId, userId))
//                .slash(userId)
//                .slash("addresses")
//                .slash(addressId)
                  .withSelfRel();

//        returnValue.add(userLink);
//        returnValue.add(userAddressesLink);
//        returnValue.add(selfLink);

        return EntityModel.of(returnValue, Arrays.asList(userLink, userAddressesLink, selfLink));
    }

    // http://localhost:8080/mobile-app-ws/users/email-verification?token=sometokenstring
    @GetMapping(path = "/email-verification",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public OperationStatusModel verifyEmailToken(@RequestParam(value = "token") String token)
    {
        OperationStatusModel returnValue = new OperationStatusModel();
        returnValue.setOperationName(RequestOperationName.VERIFY_EMAIL.name());
        boolean isVerified = userService.verifyEmailToken(token);

        if (isVerified) {
            returnValue.setOperationResult(RequestOperationStatus.SUCCESS.name());
        } else {
            returnValue.setOperationResult(RequestOperationStatus.FAIL.name());
        }
        return returnValue;
    }

    @PostMapping(
        consumes = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE },
        produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE }
    )
    public UserRest createUser(@RequestBody UserDetailsRequestModel userDetails) throws UserServiceException
    {
        if (userDetails.getFirstName().isEmpty())
            throw new UserServiceException(ErrorMessages.MISSING_REQUIRED_FILED.getErrorMessage());

        ModelMapper modelMapper = new ModelMapper();
        UserDto userDto = modelMapper.map(userDetails, UserDto.class);
        UserDto createdUser = userService.createUser(userDto);
        return modelMapper.map(createdUser, UserRest.class);
    }

    // http://localhost:8080/mobile-app-ws/users/password-reset-request
    @PostMapping(
        path = "/password-reset-request",
        produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE },
        consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE }
    )
    public OperationStatusModel requestReset(@RequestBody PasswordResetRequestModel passwordResetRequestModel)
    {
        OperationStatusModel returnValue = new OperationStatusModel();
        boolean operationResult = userService.requestPasswordReset(passwordResetRequestModel.getEmail());
        returnValue.setOperationName(RequestOperationName.REQUEST_PASSWORD_RESET.name());
        returnValue.setOperationResult(RequestOperationStatus.FAIL.name());

        if (operationResult) {
            returnValue.setOperationResult(RequestOperationStatus.SUCCESS.name());
        }
        return returnValue;
    }

    // http://localhost:8080/mobile-app-ws/users/password-reset
    @PostMapping(
        path = "/password-reset",
        produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE },
        consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE }
    )
    public OperationStatusModel resetPassword(@RequestBody PasswordResetModel passwordResetModel)
    {
        OperationStatusModel returnValue = new OperationStatusModel();

        boolean operationResult = userService.resetPassword(
            passwordResetModel.getToken(),
            passwordResetModel.getPassword()
        );

        returnValue.setOperationName(RequestOperationName.PASSWORD_RESET.name());
        returnValue.setOperationResult(RequestOperationStatus.FAIL.name());

        if (operationResult) {
            returnValue.setOperationResult(RequestOperationStatus.SUCCESS.name());
        }
        return returnValue;
    }

    @PutMapping(
        path="/{id}",
        consumes = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE },
        produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE }
    )
    public UserRest updateUser(@RequestBody UserDetailsRequestModel userDetails,
                             @PathVariable String id)
    {
        UserRest returnValue = new UserRest();

        if (userDetails.getFirstName().isEmpty()) throw new UserServiceException(ErrorMessages.MISSING_REQUIRED_FILED.getErrorMessage());
        if (userDetails.getLastName().isEmpty()) throw new UserServiceException(ErrorMessages.MISSING_REQUIRED_FILED.getErrorMessage());
        if (userDetails.getEmail().isEmpty()) throw new UserServiceException(ErrorMessages.MISSING_REQUIRED_FILED.getErrorMessage());
        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(userDetails, userDto);
        UserDto updatedUser = userService.updateUser(id, userDto);
        BeanUtils.copyProperties(updatedUser, returnValue);
        return returnValue;
    }

    @DeleteMapping(path="/{id}",
        produces = { MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE }
    )
    public OperationStatusModel deleteUser(@PathVariable String id)
    {
        OperationStatusModel returnValue = new OperationStatusModel();
        returnValue.setOperationName(RequestOperationName.DELETE.name());
        userService.deleteUser(id);
        returnValue.setOperationResult(RequestOperationStatus.SUCCESS.name());
        return returnValue;
    }
}
