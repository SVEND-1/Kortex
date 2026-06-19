package org.example.userservice.domain.mapper;

import org.example.kafkaEvent.UserRegisterEvent;
import org.example.rest.UserRestResponse;
import org.example.userservice.api.dto.response.UserResponse;
import org.example.userservice.db.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse convertEntityToDto(UserEntity user);
    UserEntity convertDtoToEntity(UserRegisterEvent event);
    UserRestResponse convertEntityToRest(UserEntity user);
}
