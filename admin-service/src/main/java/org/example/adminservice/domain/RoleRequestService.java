package org.example.adminservice.domain;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.adminservice.api.dto.request.RoleCreateRequest;
import org.example.adminservice.api.dto.response.RolePageResponse;
import org.example.adminservice.api.dto.request.RoleRequestFilter;
import org.example.adminservice.api.dto.response.RoleRequestResponse;
import org.example.adminservice.db.RoleRequestEntity;
import org.example.adminservice.db.RoleRequestRepository;
import org.example.adminservice.domain.exception.PendingRequestException;
import org.example.adminservice.domain.http.UserFeignService;
import org.example.adminservice.domain.mapper.RoleRequestMapper;
import org.example.adminservice.kafka.KafkaProducer;
import org.example.kafkaEvent.NotifyEvent;
import org.example.kafkaEvent.NotifyType;
import org.example.rest.UserRestResponse;
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
    private final UserFeignService userFeignService;

    public RoleRequestEntity findByIdEntity(Long id){
        return roleRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Запрос не найден"));
    }

    public List<RoleRequestResponse> getAllRoleRequestsByUserId(Long userId) {
        return roleRequestMapper.convertListEntityToDto(roleRequestRepository.getAllByUserId(userId));
    }

    public RoleRequestResponse getRoleRequest(Long roleRequestId) {
        return roleRequestMapper.convertEntityToDto(roleRequestRepository.findById(roleRequestId)
                .orElseThrow(() -> new EntityNotFoundException("Заявка не найдена")));
    }

    @Async("asyncExecutor")
    public CompletableFuture<RolePageResponse> getRoleRequestsPage(RoleRequestFilter filter) {
        return CompletableFuture.supplyAsync(() -> {
            Pageable pageable = buildPageable(filter);

            Page<RoleRequestEntity> roleRequests = roleRequestRepository.findSearchFilter(
                    filter.role(), filter.status(), filter.actionType(),
                    pageable
            );

            return roleRequestMapper.convertPageEntityToDto(roleRequests);
        });
    }

    private Pageable buildPageable(RoleRequestFilter filter){
        int pageSize = filter.pageSize() != null ? filter.pageSize() : 10;
        int pageNumber = filter.pageNumber() != null ? filter.pageNumber() : 0;
        return Pageable.ofSize(pageSize).withPage(pageNumber);
    }

    public RoleRequestResponse create(RoleCreateRequest request, Long userId) {
        try {
            if (hasPendingRequestForSameAction(userId)) {
                log.warn("У вас уже есть активная заявка на это действие");
                throw new PendingRequestException("У вас уже есть активная заявка на это действие");
            }

            RoleRequestEntity roleRequestEntity = buildRequest(request,userId);
            return roleRequestMapper.convertEntityToDto(roleRequestRepository.save(roleRequestEntity));
        }catch (Exception ex){
            log.error("Не удалось создать заявку, ex={} ", ex.getMessage());
            throw new IllegalArgumentException("Ошибка в заявке на создания role ex=" + ex.getMessage(),ex);
        }
    }

    private RoleRequestEntity buildRequest(RoleCreateRequest request, Long userId){
        return RoleRequestEntity.builder()
                .userId(userId)
                .requestedRole(request.requestedRole())
                .typeAction(request.typeAction())
                .message(request.message())
                .status(RoleRequestEntity.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Transactional
    public RoleRequestResponse downgradeRole(Long roleRequestId) {
        try {
            RoleRequestEntity roleRequestEntity = findByIdEntity(roleRequestId);
            UserRestResponse user = userFeignService.findUserDTO(roleRequestEntity.getUserId());

            adminService.downgrade(roleRequestEntity.getUserId(),user.role(), roleRequestEntity.getRequestedRole());

            roleRequestEntity.setStatus(RoleRequestEntity.Status.APPROVED);
            RoleRequestEntity savedRoleRequestEntity = roleRequestRepository.save(roleRequestEntity);

            notify(user.email(),
                    Map.of("userName", user.name()),
                    NotifyType.DOWNGRADE_ROLE);

            return roleRequestMapper.convertEntityToDto(savedRoleRequestEntity);
        }
        catch (Exception ex){
            log.error("Ошибка понижения пользователя, ex={} ", ex.getMessage());
            throw new RuntimeException("Не удалось понизить пользователя: " + ex.getMessage());
        }
    }

    @Transactional
    public RoleRequestResponse approveRole(Long roleRequestId) {
        try {
            RoleRequestEntity roleRequestEntity = findByIdEntity(roleRequestId);
            UserRestResponse user = userFeignService.findUserDTO(roleRequestEntity.getUserId());

            adminService.appoint(roleRequestEntity.getUserId(),user.role(), roleRequestEntity.getRequestedRole());

            roleRequestEntity.setStatus(RoleRequestEntity.Status.APPROVED);
            RoleRequestEntity savedRoleRequestEntity = roleRequestRepository.save(roleRequestEntity);

            notify(user.email(), Map.of(
                    "userName", user.name(),
                    "newRole", savedRoleRequestEntity.getRequestedRole().name()),
                    NotifyType.APPROVE_ROLE);

            return roleRequestMapper.convertEntityToDto(savedRoleRequestEntity);
        }catch (Exception ex){
            log.error("Ошибка повышение пользователя, ex={} ", ex.getMessage());
            throw new RuntimeException("Не повысить пользователя: " + ex.getMessage());
        }
    }

    @Transactional
    public RoleRequestResponse rejectRole(Long roleRequestId) {
        try {
            RoleRequestEntity roleRequestEntity = findByIdEntity(roleRequestId);
            UserRestResponse user = userFeignService.findUserDTO(roleRequestEntity.getUserId());

            roleRequestEntity.setStatus(RoleRequestEntity.Status.REJECTED);
            RoleRequestEntity savedRoleRequestEntity = roleRequestRepository.save(roleRequestEntity);

            notify(user.email(),Map.of("userName", user.name()),NotifyType.REJECT_ROLE);
            return roleRequestMapper.convertEntityToDto(savedRoleRequestEntity);
        }catch (Exception ex){
            log.error("Ошибка отмены заявки пользователя, ex={} ", ex.getMessage());
            throw new RuntimeException("Не удалось отклонить заявку: " + ex.getMessage());
        }
    }

    private boolean hasPendingRequestForSameAction(Long userId) {
        return roleRequestRepository.existsByUserIdAndStatus(userId, RoleRequestEntity.Status.PENDING);
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
