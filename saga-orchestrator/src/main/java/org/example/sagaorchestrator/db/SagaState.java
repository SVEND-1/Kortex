package org.example.sagaorchestrator.db;

public enum SagaState {
    STARTED,
    RESERVE_STOCK,           // Резервирование товара на складе
    CLEAR_CART,              // Очистка корзины покупателя
    CREATE_PAYMENT,          // Формирование платёжной сессии
    AWAIT_PAYMENT,           // Ожидание подтверждения оплаты
    UPDATE_ORDER_STATUS,     // Обновление статуса заказа (например, на «оплачен»)
    COMPLETED,               // Сага успешно завершена

    COMPENSATING_PAYMENT,    // Возврат платежа
    COMPENSATING_CART,       // Восстановить корзину (если надо)
    COMPENSATING_STOCK,      // Освободить резерв товара
    COMPENSATING_ORDER,      // Отменить заказ
    FAILED                   // Сага завершилась с ошибкой
}
