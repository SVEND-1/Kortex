package org.example.authservice.domain.mapper;

import org.example.authservice.api.dto.request.UserCreateRequest;
import org.example.authservice.api.dto.response.UserRegistrationResponse;
import org.example.authservice.db.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserRegistrationResponse convertEntityToDto(UserEntity user);

    UserCreateRequest convertDtoToCreateRequest(UserEntity user);

}
