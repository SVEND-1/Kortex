package org.example.adminservice.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.adminservice.domain.exception.IncorrectUpdateRoleException;
import org.example.adminservice.kafka.KafkaProducer;
import org.example.kafkaEvent.Role;
import org.example.kafkaEvent.RoleUpdateEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
@Service
public class AdminService {
    private final KafkaProducer kafkaProducer;

    @Transactional
    public void appoint(Long userId, Role currentRole, Role updatedRole) {
        try {
            if (isValidRoleAppoint(currentRole, updatedRole)) {
                throw new IncorrectUpdateRoleException("Нельзя назначить на роль" + updatedRole.name() + " пользователя с ролью: " + currentRole);
            }

            kafkaProducer.sendMessageToKafkaAdmin(new RoleUpdateEvent(userId,updatedRole));
        }
        catch(Exception e) {
            log.error("Ошибка при повышение пользователя, ex={}", e.getMessage());
            throw new IncorrectUpdateRoleException(e.getMessage());
        }
    }

    @Transactional
    public void downgrade(Long userId, Role currentRole, Role roleUpdated) {
        try {
            if (isValidRoleDowngrade(currentRole, roleUpdated)) {
                throw new IncorrectUpdateRoleException("Нельзя забрать роль " + roleUpdated + " у пользователя с ролью: " + currentRole);
            }

            kafkaProducer.sendMessageToKafkaAdmin(new RoleUpdateEvent(userId,roleUpdated));
        }
        catch(Exception e) {
            log.error("Ошибка при повышение понижении, ex={}", e.getMessage());
            throw new IncorrectUpdateRoleException(e.getMessage());
        }
    }


    private boolean isValidRoleAppoint(Role userRole, Role updateRole) {
        if(Role.COURIER.equals(updateRole)) {
            if(userRole.equals(Role.ADMIN) ||
                    userRole.equals(Role.SELLER)) {
                log.warn("Нельзя назначить курьером пользователя с ролью: {}", userRole);
                return true;
            }
        }
        if(Role.SELLER.equals(updateRole)) {
            if(userRole.equals(Role.ADMIN) ||
                    userRole.equals(Role.COURIER)) {
                log.warn("Нельзя назначить продавцом пользователя с ролью: {}", userRole);
                return true;
            }
        }
        return false;
    }

    private boolean isValidRoleDowngrade(Role userRole, Role updateRole) {
        if (userRole.equals(updateRole)) {
            log.warn("Нельзя забрать роль {} у пользователя с ролью: {}", updateRole, userRole);
            return true;
        }
        return false;
    }


}
