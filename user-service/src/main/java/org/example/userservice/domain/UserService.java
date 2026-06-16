package org.example.userservice.domain;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kafkaEvent.Role;
import org.example.kafkaEvent.RoleUpdateEvent;
import org.example.kafkaEvent.UserRegisterEvent;
import org.example.userservice.api.dto.request.AddressUpdatedRequest;
import org.example.userservice.api.dto.response.UserResponse;
import org.example.userservice.api.dto.response.UserRestResponse;
import org.example.userservice.db.Address;
import org.example.userservice.db.UserEntity;
import org.example.userservice.db.UserRepository;
import org.example.userservice.domain.mapper.AddressMapper;
import org.example.userservice.domain.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;

    public UserEntity getByIdEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с таким Id не найден"));
    }

    public UserRestResponse getByIdRest(Long id){
        return userMapper.convertEntityToRest(getByIdEntity(id));
    }

    public UserResponse getByIdDto(Long id) {
        return userMapper.convertEntityToDto(getByIdEntity(id));
    }

    public void save(UserRegisterEvent event) {
        try {
            UserEntity saved = userMapper.convertDtoToEntity(event);
            saved.setRole(Role.USER);
            userRepository.save(saved);
        }catch (Exception e){
            throw new RuntimeException("Не удалось сохранить пользователя");
        }
    }

    public void roleUpdate(RoleUpdateEvent event) {
        try {
            UserEntity saved = getByIdEntity(event.id());
            saved.setRole(event.updatedRole());
            userRepository.save(saved);
        }catch (Exception e){
            log.error("Ошибка обновление роли, ex={}", e.getMessage());
            throw new RuntimeException();
        }
    }

    @Transactional
    public void changeAddress(AddressUpdatedRequest address,Long userId) {
        try {
            UserEntity user = getByIdEntity(userId);
            Address addressEntity = addressMapper.convertDtoToEntity(address);

            user.setAddress(addressEntity);
            userRepository.save(user);
        }catch (Exception  e){
            log.error("Ошибка при обновление адреса, ex={}", e.getMessage());
            throw new RuntimeException("Не удалось обновить пароль", e);
        }
    }
}
