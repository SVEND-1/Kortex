package org.example.adminservice.domain;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.adminservice.api.UserFeignClient;
import org.example.adminservice.api.dto.response.RolePageResponse;
import org.example.adminservice.api.dto.request.RoleRequestFilter;
import org.example.adminservice.api.dto.response.RoleRequestResponse;
import org.example.adminservice.api.dto.response.UserRestResponse;
import org.example.adminservice.db.RoleRequest;
import org.example.adminservice.db.RoleRequestRepository;
import org.example.adminservice.domain.exception.IncorrectUpdateRoleException;
import org.example.adminservice.domain.exception.PendingRequestException;
import org.example.adminservice.domain.mapper.RoleRequestMapper;
import org.example.adminservice.kafka.KafkaProducer;
import org.example.kafkaEvent.NotifyEvent;
import org.example.kafkaEvent.NotifyType;
import org.example.kafkaEvent.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleRequestService {
    private final RoleRequestRepository roleRequestRepository;
    private final RoleRequestMapper roleRequestMapper;
    private final AdminService adminService;
    private final KafkaProducer kafkaProducer;
    private final UserFeignClient userFeignClient;

    public UserRestResponse findUserDTO(Long userId){
        return userFeignClient.getUserById(userId,"ADMIN");
    }

    public List<RoleRequestResponse> getAllRoleRequestsByUserId(Long userId) {
        return roleRequestMapper.convertListEntityToDto(roleRequestRepository.getAllByUserId(userId));
    }

    public RoleRequestResponse getRoleRequest(Long roleRequestId) {
        return roleRequestMapper.convertEntityToDto(roleRequestRepository.findById(roleRequestId).orElseThrow(() -> new EntityNotFoundException("Заявка не найдена")));
    }

    @Async("asyncExecutor")
    public CompletableFuture<RolePageResponse> getRoleRequestsPage(RoleRequestFilter filter) {//TODO Разбить на методы
        return CompletableFuture.supplyAsync(() -> {
            int pageSize = filter.pageSize() != null ? filter.pageSize() : 10;
            int pageNumber = filter.pageNumber() != null ? filter.pageNumber() : 0;//TODO SearchDTO
            Pageable pageable = Pageable.ofSize(pageSize).withPage(pageNumber);

            Page<RoleRequest> roleRequests = roleRequestRepository.findSearchFilter(
                    filter.role(),
                    filter.status(),
                    filter.actionType(),
                    pageable
            );

            RolePageResponse response = roleRequestMapper.convertPageEntityToDto(roleRequests);
            log.info("Успешно получено {} заявок", roleRequests.getTotalElements());

            return response;
        });
    }

    public RoleRequestResponse create(Role updatedRole, RoleRequest.TypeAction typeAction,//TODO Request dto
                                      String message, Long userId) {
        try {
            UserRestResponse user = findUserDTO(userId);
            if (hasPendingRequestForSameAction(userId)) {
                log.warn("У вас уже есть активная заявка на это действие");
                throw new PendingRequestException("У вас уже есть активная заявка на это действие");
            }
            if (isValidRoleAppoint(user.role(), updatedRole)) {
                throw new IncorrectUpdateRoleException("Нельзя назначить на роль" + updatedRole.name() +
                        " пользователя с ролью: " + user.role());
            }

            RoleRequest roleRequest = RoleRequest.builder()//TODO вынести
                    .userId(userId)
                    .requestedRole(updatedRole)
                    .typeAction(typeAction)
                    .message(message)
                    .status(RoleRequest.Status.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            return roleRequestMapper.convertEntityToDto(roleRequestRepository.save(roleRequest));
        }catch (Exception ex){
            log.error("Не удалось создать заявку, ex={} ", ex.getMessage());
            throw new IllegalArgumentException("Ошибка в заявке на создания role ex=" + ex.getMessage(),ex);
        }
    }


    @Transactional
    public RoleRequestResponse downgradeRole(Long roleRequestId) {
        try {
            RoleRequest roleRequest = roleRequestRepository.findById(roleRequestId)
                    .orElseThrow(() -> new EntityNotFoundException("Запрос не найден"));
            UserRestResponse user = findUserDTO(roleRequest.getUserId());

            adminService.downgrade(roleRequest.getUserId(),user.role(), roleRequest.getRequestedRole());

            roleRequest.setStatus(RoleRequest.Status.APPROVED);
            RoleRequest savedRoleRequest = roleRequestRepository.save(roleRequest);

            notify(user.email(),
                    Map.of("userName", "Удалить"),
                    NotifyType.DOWNGRADE_ROLE);
            return roleRequestMapper.convertEntityToDto(savedRoleRequest);
        }
        catch (Exception ex){
            log.error("Ошибка понижения пользователя, ex={} ", ex.getMessage());
            throw new RuntimeException("Не удалось понижить пользователя: " + ex.getMessage());
        }
    }

    @Transactional
    public RoleRequestResponse approveRole(Long roleRequestId) {
        try {
            RoleRequest roleRequest = roleRequestRepository.findById(roleRequestId)
                    .orElseThrow(() -> new EntityNotFoundException("Запрос не найден"));
            UserRestResponse user = findUserDTO(roleRequest.getUserId());

            adminService.appoint(roleRequest.getUserId(),user.role(), roleRequest.getRequestedRole());

            roleRequest.setStatus(RoleRequest.Status.APPROVED);
            RoleRequest savedRoleRequest = roleRequestRepository.save(roleRequest);

            notify(user.email(), Map.of(
                    "userName", "Удалить",
                    "newRole", savedRoleRequest.getRequestedRole().name()),
                    NotifyType.APPROVE_ROLE);

            return roleRequestMapper.convertEntityToDto(savedRoleRequest);
        }catch (Exception ex){
            log.error("Ошибка повышение пользователя, ex={} ", ex.getMessage());
            throw new RuntimeException("Не повысить пользователя: " + ex.getMessage());
        }
    }

    @Transactional
    public RoleRequestResponse rejectRole(Long roleRequestId) {
        try {
            RoleRequest roleRequest = roleRequestRepository.findById(roleRequestId)
                    .orElseThrow(() -> new EntityNotFoundException("Запрос не найден"));
            UserRestResponse user = findUserDTO(roleRequest.getUserId());

            roleRequest.setStatus(RoleRequest.Status.REJECTED);
            RoleRequest savedRoleRequest = roleRequestRepository.save(roleRequest);

            notify(user.email(),Map.of("userName", "Удалить"),NotifyType.REJECT_ROLE);
            return roleRequestMapper.convertEntityToDto(savedRoleRequest);
        }catch (Exception ex){
            log.error("Ошибка отмены заявки пользователя, ex={} ", ex.getMessage());
            throw new RuntimeException("Не удалось отклонить заявку: " + ex.getMessage());
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


    private boolean hasPendingRequestForSameAction(Long userId) {
        return roleRequestRepository.existsByUserIdAndStatus(userId, RoleRequest.Status.PENDING);
    }

    private void notify(String email, Map<String,Object> message, NotifyType type) {
        NotifyEvent notifyEvent = new NotifyEvent(
                email,
                message,
                type
        );
        kafkaProducer.sendMessageToKafka(notifyEvent);
    }
}
