// Конфигурация API
const API_BASE = '/api/delivery';
let currentUser = null;
let currentFilter = 'assigned';
let currentPage = 0;
const pageSize = 3;
let totalPages = 0;
let totalElements = 0;

// Кэш текущих отображённых заказов: id -> OrderResponse (для модалки деталей)
let ordersById = {};

// Получение данных пользователя из localStorage
function getUserData() {
    const userId = localStorage.getItem('userId');
    const userRole = localStorage.getItem('userRole');
    const userName = localStorage.getItem('userName') || 'Курьер';
    if (userId && userRole) {
        return { id: parseInt(userId), role: userRole, name: userName };
    }
    // Для теста (замените на свои значения)
    return { id: 1, role: 'COURIER', name: 'Тестовый курьер' };
}

document.addEventListener('DOMContentLoaded', async function() {
    console.log('Панель курьера загружена');
    try {
        currentUser = getUserData();
        if (currentUser.role !== 'COURIER' && currentUser.role !== 'ADMIN') {
            alert('Доступ только для курьеров и администраторов!');
            window.location.href = '/';
            return;
        }
        updateUserInfo();
        await loadData();
    } catch (error) {
        console.error('Ошибка инициализации:', error);
        showError(`Ошибка загрузки данных: ${error.message}`);
    }
});

function updateUserInfo() {
    const courierNameEl = document.getElementById('courierName');
    const courierRoleEl = document.querySelector('.courier-role');
    const headerTitle = document.querySelector('.courier-header h1');
    if (courierNameEl) courierNameEl.textContent = currentUser.name || 'Курьер';
    if (courierRoleEl) courierRoleEl.textContent = currentUser.role === 'ADMIN' ? 'Администратор' : 'Курьер';
    if (currentUser.role === 'ADMIN' && headerTitle) {
        headerTitle.textContent = 'Панель управления заказами';
        if (courierRoleEl) courierRoleEl.textContent = 'Администратор';
    }
}

async function loadData() {
    try {
        await loadStats();
        await loadOrdersData('assigned', 0);
        setActiveFilter('assigned');
    } catch (error) {
        console.error('Ошибка загрузки данных:', error);
        showError(`Ошибка загрузки данных: ${error.message}`);
    }
}

async function loadStats() {
    try {
        updateStatsUI(0, 0, 0);
        let myOrders = [];
        let availableOrders = [];

        if (currentUser.role === 'COURIER') {
            try {
                const assignedResponse = await fetch(
                    `${API_BASE}/assigned?pageSize=100&pageNumber=0`,
                    {
                        method: 'GET',
                        headers: {
                            'Content-Type': 'application/json',
                            'X-User-Id': currentUser.id,
                            'X-User-Role': currentUser.role
                        }
                    }
                );
                if (assignedResponse.ok) {
                    const result = await assignedResponse.json();
                    myOrders = extractOrdersFromResponse(result);
                }
            } catch (error) {
                console.warn('Ошибка загрузки назначенных заказов для статистики:', error);
            }
        }

        try {
            const availableResponse = await fetch(`${API_BASE}/available?pageSize=100&pageNumber=0`);
            if (availableResponse.ok) {
                const result = await availableResponse.json();
                availableOrders = extractOrdersFromResponse(result);
            }
        } catch (error) {
            console.warn('Ошибка загрузки доступных заказов для статистики:', error);
        }

        let activeOrders = 0;
        if (currentUser.role === 'COURIER') {
            activeOrders = myOrders.filter(order => {
                const status = order.status;
                return status === 'PENDING' || status === 'DISPATCHED';
            }).length;
        }

        updateStatsUI(activeOrders, availableOrders.length, myOrders.length);
    } catch (error) {
        console.error('Ошибка загрузки статистики:', error);
    }
}

function extractOrdersFromResponse(response) {
    if (!response) return [];
    if (response.content && Array.isArray(response.content)) return response.content;
    if (Array.isArray(response)) return response;
    if (response.id && response.status) return [response];
    for (const key in response) {
        if (Array.isArray(response[key]) && response[key].length > 0 && response[key][0].id) {
            return response[key];
        }
    }
    return [];
}

// Поля пагинации строго по OrderPageResponse: pageNumber, pageSize, totalElements, totalPages, first, last, empty
function updatePaginationInfo(response) {
    if (!response || typeof response !== 'object') {
        totalPages = 0;
        totalElements = 0;
        currentPage = 0;
        return;
    }
    totalPages = response.totalPages || 0;
    totalElements = response.totalElements || 0;
    currentPage = response.pageNumber !== undefined ? response.pageNumber : 0;
}

function updateStatsUI(active, available, total) {
    const activeEl = document.getElementById('activeOrders');
    const availableEl = document.getElementById('availableOrders');
    const totalEl = document.getElementById('totalOrders');
    if (activeEl) activeEl.textContent = active;
    if (availableEl) availableEl.textContent = available;
    if (totalEl) totalEl.textContent = total;
}

// Основная функция загрузки заказов
async function loadOrdersData(filter = 'assigned', page = 0) {
    currentFilter = filter;
    currentPage = page;

    const ordersContainer = document.getElementById('ordersContainer');
    const ordersTitle = document.getElementById('ordersTitle');
    const paginationContainer = document.getElementById('paginationContainer');

    if (!ordersContainer) {
        console.error('Элемент ordersContainer не найден');
        return;
    }

    ordersContainer.innerHTML = '<div class="loading">Загрузка заказов...</div>';
    if (paginationContainer) paginationContainer.style.display = 'none';

    try {
        let apiUrl = '';
        let title = '';
        let options = {};

        if (filter === 'assigned') {
            apiUrl = `${API_BASE}/assigned?pageSize=${pageSize}&pageNumber=${page}`;
            title = 'Мои заказы';
            options = {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'X-User-Id': currentUser.id,
                    'X-User-Role': currentUser.role
                }
            };
        } else if (filter === 'available') {
            apiUrl = `${API_BASE}/available?pageSize=${pageSize}&pageNumber=${page}`;
            title = currentUser.role === 'ADMIN' ? 'Заказы без курьера' : 'Доступные заказы';
            options = {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            };
        }

        console.log('Запрос к API:', apiUrl, options);

        const response = await fetch(apiUrl, options);
        if (!response.ok) {
            const errorText = await response.text();
            console.error('Текст ошибки:', errorText);
            throw new Error(`Ошибка сервера: ${response.status}`);
        }

        const result = await response.json();
        console.log('Ответ API:', result);

        const orders = extractOrdersFromResponse(result);
        updatePaginationInfo(result);

        if (ordersTitle) ordersTitle.textContent = title;

        displayOrders(orders, filter);
    } catch (error) {
        console.error('Ошибка загрузки заказов:', error);
        ordersContainer.innerHTML = `
            <div class="error-message">
                Ошибка загрузки заказов: ${getErrorMessage(error)}
                <br><br>
                <button onclick="loadOrdersData('${filter}', ${currentPage})" class="btn btn-outline btn-small">
                    Повторить
                </button>
            </div>
        `;
        if (paginationContainer) paginationContainer.style.display = 'none';
    }
}

function displayOrders(orders, filter) {
    const ordersContainer = document.getElementById('ordersContainer');
    const paginationContainer = document.getElementById('paginationContainer');
    if (!ordersContainer) return;

    // Обновляем кэш заказов для модалки деталей
    ordersById = {};
    (Array.isArray(orders) ? orders : []).forEach(order => {
        if (order && order.id !== undefined && order.id !== null) {
            ordersById[order.id] = order;
        }
    });

    if (!Array.isArray(orders) || orders.length === 0) {
        let message = currentUser.role === 'ADMIN'
            ? (filter === 'assigned' ? 'Нет заказов для отображения' : 'Нет заказов без курьера')
            : (filter === 'assigned' ? 'У вас нет назначенных заказов' : 'В данный момент нет доступных заказов');
        ordersContainer.innerHTML = `
            <div class="no-orders">
                <div class="no-orders-icon">📦</div>
                <h4>Заказов нет</h4>
                <p>${message}</p>
            </div>
        `;
        if (paginationContainer) paginationContainer.style.display = 'none';
        return;
    }

    ordersContainer.innerHTML = '';
    orders.forEach(order => {
        try {
            const orderElement = createOrderElement(order, filter);
            ordersContainer.appendChild(orderElement);
        } catch (error) {
            console.error('Ошибка создания элемента заказа:', error, order);
        }
    });

    if (totalPages > 1 && paginationContainer) {
        addPaginationControls(paginationContainer, filter);
    } else if (paginationContainer) {
        paginationContainer.style.display = 'none';
    }
}

// Создание элемента заказа строго по полям OrderResponse: id, status, address, message, orderItems
function createOrderElement(order, filter) {
    const template = document.getElementById('orderTemplate');
    if (!template) throw new Error('Шаблон orderTemplate не найден');

    const clone = template.content.cloneNode(true);
    const orderCard = clone.querySelector('.order-card');
    if (!orderCard) throw new Error('Элемент .order-card не найден в шаблоне');

    const orderId = order.id || 'N/A';
    orderCard.setAttribute('data-order-id', orderId);

    const elements = {
        orderId: orderCard.querySelector('.order-id'),
        customerAddress: orderCard.querySelector('.customer-address'),
        orderMessage: orderCard.querySelector('.order-message'),
        itemsCount: orderCard.querySelector('.order-items-count'),
        priceValue: orderCard.querySelector('.price-value'),
        orderStatus: orderCard.querySelector('.order-status'),
        acceptBtn: orderCard.querySelector('.accept-btn'),
        startBtn: orderCard.querySelector('.start-btn'),
        completeBtn: orderCard.querySelector('.complete-btn'),
        detailsBtn: orderCard.querySelector('.details-btn'),
        cancelBtn: orderCard.querySelector('.cancel-btn'),
        returnBtn: orderCard.querySelector('.return-btn')
    };

    if (elements.orderId) elements.orderId.textContent = `Заказ #${orderId}`;

    // Адрес — AddressRestResponse: region, city, street, house, apartment
    if (elements.customerAddress) {
        const addr = order.address || {};
        const parts = [addr.region, addr.city, addr.street, addr.house, addr.apartment].filter(Boolean);
        elements.customerAddress.textContent = parts.length ? parts.join(', ') : 'Не указано';
    }

    // Комментарий к заказу — поле message
    if (elements.orderMessage) {
        elements.orderMessage.textContent = order.message ? order.message : '—';
    }

    const items = Array.isArray(order.orderItems) ? order.orderItems : [];

    // Кол-во позиций в заказе
    if (elements.itemsCount) {
        elements.itemsCount.textContent = items.length;
    }

    // Сумма — считаем из orderItems (price * quantity)
    if (elements.priceValue) {
        const total = items.reduce((sum, item) => {
            const price = item.price ? parseFloat(item.price) : 0;
            const quantity = item.quantity || 1;
            return sum + price * quantity;
        }, 0);
        elements.priceValue.textContent = `${formatPrice(total)} ₽`;
    }

    // Статус
    const status = order.status || 'PENDING';
    if (elements.orderStatus) {
        elements.orderStatus.textContent = getStatusText(status);
        elements.orderStatus.className = `order-status status-${status.toLowerCase()}`;
    }

    setupOrderButtons(elements, status, orderId, filter);

    return orderCard;
}

// Настройка кнопок заказа (с поддержкой CREATED)
function setupOrderButtons(elements, status, orderId, filter) {
    Object.values(elements).forEach(el => {
        if (el && el.classList && el.classList.contains('btn')) {
            el.style.display = 'none';
        }
    });

    if (elements.detailsBtn) {
        elements.detailsBtn.style.display = 'inline-block';
        elements.detailsBtn.onclick = () => showOrderDetails(orderId);
    }

    if (filter === 'available') {
        if (elements.acceptBtn) {
            elements.acceptBtn.style.display = 'inline-block';
            elements.acceptBtn.textContent = currentUser.role === 'ADMIN' ? 'Назначить курьера' : 'Принять заказ';
            elements.acceptBtn.onclick = () => {
                if (currentUser.role === 'ADMIN') {
                    assignOrderAsAdmin(orderId);
                } else {
                    assignOrder(orderId);
                }
            };
        }
        return;
    }

    const upperStatus = status.toUpperCase();

    switch(upperStatus) {
        case 'CREATED':
        case 'PENDING':
            if (elements.startBtn) {
                elements.startBtn.style.display = 'inline-block';
                elements.startBtn.textContent = upperStatus === 'CREATED' ? 'Принять в работу' : 'Начать доставку';
                elements.startBtn.onclick = () => updateOrderStatus(orderId, 'DISPATCHED');
            }
            if (elements.cancelBtn) {
                elements.cancelBtn.style.display = 'inline-block';
                elements.cancelBtn.textContent = 'Отменить';
                elements.cancelBtn.onclick = () => updateOrderStatus(orderId, 'CANCELLED');
            }
            break;
        case 'DISPATCHED':
            if (elements.completeBtn) {
                elements.completeBtn.style.display = 'inline-block';
                elements.completeBtn.textContent = 'Доставлен';
                elements.completeBtn.onclick = () => updateOrderStatus(orderId, 'DELIVERED_TO_DESTINATION');
            }
            break;
        case 'DELIVERED_TO_DESTINATION':
            if (elements.completeBtn) {
                elements.completeBtn.style.display = 'inline-block';
                elements.completeBtn.textContent = 'Завершить';
                elements.completeBtn.className = 'btn btn-success btn-small complete-btn';
                elements.completeBtn.onclick = () => updateOrderStatus(orderId, 'COMPLETED');
            }
            if (elements.returnBtn) {
                elements.returnBtn.style.display = 'inline-block';
                elements.returnBtn.textContent = 'Вернуть';
                elements.returnBtn.onclick = () => updateOrderStatus(orderId, 'RETURNED');
            }
            break;
        case 'COMPLETED':
        case 'CANCELLED':
        case 'RETURNED':
            // Только кнопка "Подробнее"
            break;
    }
}

// Принять заказ (курьер)
async function assignOrder(orderId) {
    if (!confirm(`Принять заказ #${orderId}?`)) return;
    try {
        const response = await fetch(`${API_BASE}/${orderId}/take`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-User-Id': currentUser.id,
                'X-User-Role': currentUser.role
            }
        });
        const result = await handleApiResponse(response, `Принятие заказа #${orderId}`);
        if (result.success) {
            alert(`Заказ #${orderId} успешно принят!`);
            await refreshData();
        }
    } catch (error) {
        console.error('Ошибка при принятии заказа:', error);
        alert(`Ошибка: ${error.message}`);
    }
}

// Назначить заказ (админ)
async function assignOrderAsAdmin(orderId) {
    if (!confirm(`Назначить себя курьером для заказа #${orderId}?`)) return;
    try {
        const response = await fetch(`${API_BASE}/${orderId}/take`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-User-Id': currentUser.id,
                'X-User-Role': currentUser.role
            }
        });
        const result = await handleApiResponse(response, `Назначение заказа #${orderId}`);
        if (result.success) {
            alert(`Вы назначены курьером для заказа #${orderId}!`);
            await refreshData();
        }
    } catch (error) {
        console.error('Ошибка при назначении заказа:', error);
        alert(`Ошибка: ${error.message}`);
    }
}

// Обновление статуса (передаём userId)
async function updateOrderStatus(orderId, status) {
    const statusText = getStatusText(status);
    const actionText = getActionText(status, currentUser.role);
    if (!confirm(`Вы уверены, что хотите ${actionText} заказа #${orderId}?`)) return;
    try {
        const response = await fetch(`${API_BASE}/${orderId}/status?status=${status}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-User-Id': currentUser.id,
                'X-User-Role': currentUser.role
            }
        });
        const result = await handleApiResponse(response, `Обновление статуса заказа #${orderId}`);
        if (result.success) {
            alert(`Статус заказа #${orderId} обновлен на "${statusText}"!`);
            await refreshData();
        }
    } catch (error) {
        console.error('Ошибка при обновлении статуса:', error);
        alert(`Ошибка: ${error.message}`);
    }
}

// Обработка ответа API (исправлена для пустого тела)
async function handleApiResponse(response, action) {
    if (!response.ok) {
        const errorText = await response.text();
        let errorMessage = `Ошибка при ${action}`;
        try {
            const errorData = JSON.parse(errorText);
            errorMessage = errorData.message || errorData.error || errorMessage;
        } catch (e) {
            errorMessage = errorText || errorMessage;
        }
        throw new Error(errorMessage);
    }
    const text = await response.text();
    if (!text) {
        return { success: true };
    }
    try {
        return JSON.parse(text);
    } catch (e) {
        return { success: true };
    }
}

// Обновление данных
async function refreshData() {
    try {
        await Promise.all([
            loadStats(),
            loadOrdersData(currentFilter, currentPage)
        ]);
    } catch (error) {
        console.error('Ошибка при обновлении данных:', error);
    }
}

// Детали заказа (модальное окно) — строим из полного OrderResponse, включая orderItems
function showOrderDetails(orderId) {
    const order = ordersById[orderId];
    if (!order) {
        alert('Заказ не найден в списке. Попробуйте обновить.');
        return;
    }

    const modal = document.getElementById('orderDetailModal');
    const title = document.getElementById('detailOrderTitle');
    const content = document.getElementById('detailOrderContent');

    title.textContent = `Детали заказа #${order.id}`;

    // Адрес — полностью, включая region
    const addr = order.address || {};
    const addressParts = [addr.region, addr.city, addr.street, addr.house, addr.apartment].filter(Boolean);
    const addressText = addressParts.length ? addressParts.join(', ') : 'Не указано';

    const statusText = getStatusText(order.status || 'PENDING');
    const messageText = order.message ? order.message : '—';

    const items = Array.isArray(order.orderItems) ? order.orderItems : [];
    let total = 0;
    const itemsHtml = items.length
        ? items.map(item => {
            const price = item.price ? parseFloat(item.price) : 0;
            const quantity = item.quantity || 1;
            const lineTotal = price * quantity;
            total += lineTotal;
            const product = item.product || {};
            const productName = product.name || 'Товар';
            const productCategory = product.category ? ` (${product.category})` : '';
            return `
                <div class="detail-item-row">
                    <span class="detail-item-name">${escapeHtml(productName)}${escapeHtml(productCategory)}</span>
                    <span class="detail-item-qty">${quantity} шт.</span>
                    <span class="detail-item-price">${formatPrice(lineTotal)} ₽</span>
                </div>
            `;
        }).join('')
        : '<p style="color: #999;">Информация о товарах отсутствует.</p>';

    content.innerHTML = `
        <div class="detail-item">
            <span class="detail-label">Статус:</span>
            <span class="detail-value">${escapeHtml(statusText)}</span>
        </div>
        <div class="detail-item">
            <span class="detail-label">Адрес:</span>
            <span class="detail-value">${escapeHtml(addressText)}</span>
        </div>
        <div class="detail-item">
            <span class="detail-label">Комментарий:</span>
            <span class="detail-value">${escapeHtml(messageText)}</span>
        </div>
        <div style="margin-top: 15px;">
            <h4>Товары:</h4>
            <div class="detail-items-list">
                ${itemsHtml}
            </div>
        </div>
        <div class="detail-total">Итого: ${formatPrice(total)} ₽</div>
    `;

    modal.classList.add('active');
}

function closeOrderDetailModal() {
    const modal = document.getElementById('orderDetailModal');
    if (modal) modal.classList.remove('active');
}

// Закрытие модалки по клику вне окна
document.addEventListener('click', function(e) {
    const modal = document.getElementById('orderDetailModal');
    if (modal && e.target === modal) {
        closeOrderDetailModal();
    }
});

// Закрытие по ESC
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        closeOrderDetailModal();
    }
});

// Вспомогательные функции
function getStatusText(status) {
    const map = {
        'CREATED': 'Создан',
        'PENDING': 'Ожидает',
        'DISPATCHED': 'В доставке',
        'DELIVERED_TO_DESTINATION': 'Доставлен',
        'COMPLETED': 'Завершен',
        'CANCELLED': 'Отменен',
        'RETURNED': 'Возвращен'
    };
    return map[status.toUpperCase()] || status || 'Неизвестно';
}

function getActionText(status, role) {
    const map = {
        'DISPATCHED': 'начать доставку',
        'DELIVERED_TO_DESTINATION': 'отметить как доставленный',
        'COMPLETED': 'завершить заказ',
        'CANCELLED': 'отменить заказ',
        'RETURNED': 'вернуть заказ'
    };
    return map[status.toUpperCase()] || 'выполнить действие';
}

function formatPrice(price) {
    if (!price) return '0';
    const num = typeof price === 'string' ? parseFloat(price) : Number(price);
    return isNaN(num) ? '0' : num.toLocaleString('ru-RU', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
}

function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function getErrorMessage(error) {
    if (error.message && error.message.includes('Failed to fetch')) return 'Ошибка соединения с сервером';
    if (error.message && error.message.includes('HTTP')) return 'Ошибка сервера';
    return error.message || 'Неизвестная ошибка';
}

function setActiveFilter(filter) {
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.classList.remove('active');
        if (btn.textContent.includes(filter === 'assigned' ? 'Мои' : 'Доступные')) {
            btn.classList.add('active');
        }
    });
}

function showError(message) {
    const container = document.getElementById('ordersContainer');
    if (!container) return;
    container.innerHTML = `
        <div class="error-message">
            ${message}
            <br><br>
            <button onclick="loadData()" class="btn btn-outline btn-small">Повторить</button>
        </div>
    `;
}

function addPaginationControls(container, filter) {
    container.innerHTML = '';
    container.style.display = 'block';

    const paginationDiv = document.createElement('div');
    paginationDiv.className = 'pagination';

    const pageInfo = document.createElement('span');
    pageInfo.className = 'page-info';
    pageInfo.textContent = `Страница ${currentPage + 1} из ${totalPages}`;

    const prevButton = document.createElement('button');
    prevButton.className = 'btn btn-outline pagination-btn';
    prevButton.innerHTML = '&larr; Назад';
    prevButton.disabled = currentPage === 0;
    prevButton.onclick = function() {
        if (currentPage > 0) loadOrdersData(filter, currentPage - 1);
    };

    const nextButton = document.createElement('button');
    nextButton.className = 'btn btn-outline pagination-btn';
    nextButton.innerHTML = 'Вперед &rarr;';
    nextButton.disabled = currentPage >= totalPages - 1;
    nextButton.onclick = function() {
        if (currentPage < totalPages - 1) loadOrdersData(filter, currentPage + 1);
    };

    paginationDiv.appendChild(prevButton);
    paginationDiv.appendChild(pageInfo);
    paginationDiv.appendChild(nextButton);
    container.appendChild(paginationDiv);
}

window.setFilter = function(filter) {
    setActiveFilter(filter);
    currentPage = 0;
    loadOrdersData(filter, 0).catch(error => {
        console.error('Ошибка при смене фильтра:', error);
        showError('Ошибка загрузки заказов');
    });
};

window.refreshOrders = function() {
    refreshData().catch(console.error);
};

window.closeOrderDetailModal = closeOrderDetailModal;