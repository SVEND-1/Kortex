package org.example.userservice.api;

import lombok.RequiredArgsConstructor;
import org.example.rest.AddressRestResponse;
import org.example.rest.UserRestResponse;
import org.example.userservice.api.dto.request.AddressUpdatedRequest;
import org.example.userservice.api.dto.response.UserResponse;
import org.example.userservice.db.Address;
import org.example.userservice.domain.UserService;
import org.example.userservice.domain.expetions.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @GetMapping("/{id}")
    public UserRestResponse getUser(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String currentUserRole
    ) {
        if (currentUserRole == null || !"ADMIN".equals(currentUserRole)) {
            throw new AccessDeniedException("Нет прав доступа к данному профилю");
        }
        return userService.getByIdRest(id);
    }

    @GetMapping("/{id}/address")
    public AddressRestResponse getUserAddress(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        if(!userId.equals(id)){
            throw new RuntimeException("Пользователь не может получать адрес который не привязан к нему");
        }
        return userService.getAddressRest(id);
    }

    @GetMapping()
    public ResponseEntity<UserResponse> getUserById(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(userService.getByIdDto(userId));
    }

    @PatchMapping()
    public ResponseEntity<Void> changedAddress(
            @RequestBody AddressUpdatedRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        userService.changeAddress(request,userId);
        return ResponseEntity.ok().build();
    }
}
