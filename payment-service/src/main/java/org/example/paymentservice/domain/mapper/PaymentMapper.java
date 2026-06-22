package org.example.paymentservice.domain.mapper;

import org.example.paymentservice.api.dto.response.payment.PaymentPageResponse;
import org.example.paymentservice.api.dto.response.payment.PaymentResponse;
import org.example.paymentservice.db.PaymentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;
import ru.loolzaaa.youkassa.model.Payment;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentMapper {


    @Mapping(target = "id", source = "paymentId")
    @Mapping(target = "value", source = "amount")
    @Mapping(target = "description", constant = "Оплата заказа")
    @Mapping(target = "status", source = "paid", qualifiedByName = "paidToStatus")
    PaymentResponse toResponse(PaymentEntity entity);

    List<PaymentResponse> toResponseList(List<PaymentEntity> entities);

    default PaymentPageResponse toPageResponse(Page<PaymentEntity> page) {
        if (page == null) {
            return new PaymentPageResponse(
                    List.of(),
                    0, 0, 0, 0,
                    true, true, true
            );
        }
        List<PaymentResponse> content = toResponseList(page.getContent());
        return new PaymentPageResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }

    @Named("paidToStatus")
    default String paidToStatus(Boolean paid) {
        return paid != null && paid ? "succeeded" : "pending";
    }

}
