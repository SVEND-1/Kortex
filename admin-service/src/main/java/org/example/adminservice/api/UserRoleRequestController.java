package org.example.adminservice.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.adminservice.api.dto.request.RoleCreateRequest;
import org.example.adminservice.api.dto.response.RoleRequestResponse;
import org.example.adminservice.domain.RoleRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users/role-request")
@Tag(name = "UserRole",description = "Работа с заявки у пользователя")
public class UserRoleRequestController {
    private final RoleRequestService roleRequestService;

    @Operation(summary = "Получение истории заявок")
    @GetMapping
    public ResponseEntity<List<RoleRequestResponse>> getUserRoleRequests(
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ){
        return ResponseEntity.ok().body(roleRequestService.getAllRoleRequestsByUserId(userId));
    }

    @Operation(summary = "Создание заявки")
    @PostMapping
    public ResponseEntity<RoleRequestResponse> create(
            @RequestBody @Valid RoleCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return ResponseEntity.ok(
                roleRequestService.create(request,userId)
        );
    }

}
