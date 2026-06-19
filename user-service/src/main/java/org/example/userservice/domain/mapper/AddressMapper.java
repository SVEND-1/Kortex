package org.example.userservice.domain.mapper;

import org.example.rest.AddressRestResponse;
import org.example.userservice.api.dto.request.AddressUpdatedRequest;
import org.example.userservice.db.Address;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AddressMapper {
    Address convertDtoToEntity(AddressUpdatedRequest request);

    AddressRestResponse convertEntityToDto(Address address);
}
