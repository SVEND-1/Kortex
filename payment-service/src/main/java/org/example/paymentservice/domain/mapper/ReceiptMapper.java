package org.example.paymentservice.domain.mapper;

import org.example.paymentservice.api.dto.response.receipt.ReceiptItem;
import org.example.paymentservice.api.dto.response.receipt.ReceiptResponse;
import org.example.paymentservice.api.dto.response.receipt.SettlementReceipt;
import org.mapstruct.Mapper;
import ru.loolzaaa.youkassa.model.Receipt;
import ru.loolzaaa.youkassa.pojo.Item;
import ru.loolzaaa.youkassa.pojo.Settlement;

@Mapper(componentModel = "spring")
public interface ReceiptMapper {

    default ReceiptResponse convertReceiptToReceiptResponse(Receipt receipt,String amount) {//TODO поменять
        return new ReceiptResponse(
                receipt.getId(),
                receipt.getType(),
                receipt.getPaymentId(),
                receipt.getStatus(),
                amount,

                receipt.getFiscalDocumentNumber(),
                receipt.getFiscalStorageNumber(),
                receipt.getFiscalAttribute(),
                receipt.getRegisteredAt(),
                receipt.getFiscalProviderId(),

                receipt.getItems().stream()
                        .map(this::convertToReceiptItem)
                        .toList(),

                receipt.getSettlements().stream()
                        .map(this::convertToSettlementReceipt)
                        .toList(),

                "Kortex"
        );
    }

    default ReceiptItem convertToReceiptItem(Item item) {
        return new ReceiptItem(
                item.getDescription(),
                item.getQuantity().toString(),
                item.getAmount().getValue(),
                item.getAmount().getCurrency(),
                item.getVatCode()
        );
    }

    default SettlementReceipt convertToSettlementReceipt(Settlement settlement) {
        return new SettlementReceipt(
                settlement.getType(),
                settlement.getAmount().getValue(),
                settlement.getAmount().getCurrency()
        );
    }

}
