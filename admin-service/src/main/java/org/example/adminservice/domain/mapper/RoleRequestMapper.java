package org.example.adminservice.domain.mapper;

import org.example.adminservice.api.dto.response.RolePageResponse;
import org.example.adminservice.api.dto.response.RoleRequestResponse;
import org.example.adminservice.db.RoleRequest;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoleRequestMapper {

    RoleRequestResponse convertEntityToDto(RoleRequest roleRequest);


    List<RoleRequestResponse> convertListEntityToDto(List<RoleRequest> roleRequests);

    default RolePageResponse convertPageEntityToDto(Page<RoleRequest> roleRequests) {
        if (roleRequests == null) {
            return null;
        }

        return new RolePageResponse(
                convertListEntityToDto(roleRequests.getContent()),
                roleRequests.getNumber(),
                roleRequests.getSize(),
                roleRequests.getTotalElements(),
                roleRequests.getTotalPages(),
                roleRequests.isFirst(),
                roleRequests.isLast(),
                roleRequests.isEmpty()
        );
    }
}

