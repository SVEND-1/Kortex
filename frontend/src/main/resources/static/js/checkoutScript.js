document.addEventListener('DOMContentLoaded', function() {
    initCheckout();
});

let selectedCartIds = [];
let cartItems = [];
let userData = null;

function initCheckout() {
    const urlParams = new URLSearchParams(window.location.search);
    const idsParam = urlParams.get('ids');
    if (idsParam) {
        selectedCartIds = idsParam.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
    }
    console.log('Выбранные ID корзины:', selectedCartIds);

    loadUserAndCart();
    setupEventHandlers();
}

async function loadUserAndCart() {
    try {
        showLoading();

        // Получаем данные пользователя
        const userResponse = await fetch('/api/users', {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include'
        });

        if (!userResponse.ok) {
            if (userResponse.status === 401 || userResponse.status === 403) {
                showError('Для оформления заказа требуется авторизация');
                setTimeout(() => {
                    window.location.href = '/login?redirect=/checkout';
                }, 2000);
                return;
            }
            throw new Error(`Ошибка загрузки пользователя: ${userResponse.status}`);
        }

        userData = await userResponse.json();
        console.log('Данные пользователя:', userData);

        // Проверяем адрес
        const hasAddress = userData.address && userData.address.city && userData.address.street;
        const addressWarning = document.getElementById('addressWarning');
        const userInfoBlock = document.getElementById('userInfoBlock');
        const submitBtn = document.getElementById('submit-order-btn');

        if (!hasAddress) {
            addressWarning.style.display = 'block';
            userInfoBlock.style.display = 'none';
            submitBtn.disabled = true;
        } else {
            addressWarning.style.display = 'none';
            userInfoBlock.style.display = 'block';
            // Заполняем информацию о пользователе
            document.getElementById('displayName').textContent = userData.name || 'Не указано';
            document.getElementById('displayEmail').textContent = userData.email || 'Не указано';
            const fullAddress = `${userData.address.city}, ${userData.address.street}, ${userData.address.house || ''} ${userData.address.apartment || ''}`.trim();
            document.getElementById('displayAddress').textContent = fullAddress || 'Не указан';
            submitBtn.disabled = false;
        }

        // Получаем корзину
        const cartResponse = await fetch('/api/carts/me', {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include'
        });

        if (!cartResponse.ok) {
            throw new Error(`Ошибка загрузки корзины: ${cartResponse.status}`);
        }

        const cartData = await cartResponse.json();
        console.log('Данные корзины:', cartData);

        const allItems = cartData.items || [];
        if (selectedCartIds.length > 0) {
            cartItems = allItems.filter(item => selectedCartIds.includes(item.id));
        } else {
            cartItems = allItems;
        }

        if (cartItems.length === 0) {
            document.getElementById('order-items-container').innerHTML = '<p class="empty-cart">Нет выбранных товаров</p>';
            submitBtn.disabled = true;
            return;
        }

        await enrichItemsWithProductDetails();
        renderOrderItems(cartItems);
        updateOrderSummary(cartItems);

        // Если адрес есть, кнопка уже активна, иначе она заблокирована
        if (!hasAddress) {
            submitBtn.disabled = true;
        }

    } catch (error) {
        console.error('Ошибка загрузки данных:', error);
        showError(`Ошибка загрузки: ${error.message}`);
    } finally {
        hideLoading();
    }
}

// Функция enrichItemsWithProductDetails (копия из cartManager)
async function enrichItemsWithProductDetails() {
    const uniqueProductIds = [...new Set(cartItems.map(item => item.productId).filter(id => id))];
    const productPromises = uniqueProductIds.map(async (productId) => {
        try {
            const response = await fetch(`/api/products/${productId}`, {
                credentials: 'include'
            });
            if (!response.ok) return null;
            const productData = await response.json();
            return { productId, productData };
        } catch (err) {
            console.error(`Ошибка загрузки товара ${productId}:`, err);
            return null;
        }
    });

    const results = await Promise.all(productPromises);
    const productMap = {};
    results.forEach(result => {
        if (result) {
            productMap[result.productId] = result.productData;
        }
    });

    cartItems = cartItems.map(item => {
        const product = productMap[item.productId];
        if (product) {
            const firstImage = product.images && product.images.length > 0 ? product.images[0] : null;
            return {
                ...item,
                productName: product.name || 'Товар',
                image: firstImage,
                unitPrice: Number(product.price) || 0,
                description: product.description || ''
            };
        }
        return {
            ...item,
            productName: 'Товар (данные не загружены)',
            image: null,
            unitPrice: 0,
            description: ''
        };
    });
}

function renderOrderItems(items) {
    const container = document.getElementById('order-items-container');
    if (!items || items.length === 0) {
        container.innerHTML = '<p class="empty-cart">Нет товаров для отображения</p>';
        return;
    }

    container.innerHTML = items.map(item => {
        const imageUrl = item.image ? `/uploads/images/${item.image}` : '/images/no-image.png';
        const productName = item.productName || 'Товар';
        const price = item.unitPrice || 0;
        const quantity = item.quantity || 1;
        const total = price * quantity;

        return `
            <div class="order-item">
                <div class="item-image">
                    <img src="${imageUrl}" alt="${productName}" 
                         onerror="this.onerror=null; this.src='/images/no-image.png'">
                </div>
                <div class="item-details">
                    <div class="item-name">${productName}</div>
                    <div class="item-meta">
                        <div class="item-quantity">${quantity} шт.</div>
                        <div class="item-price">${formatPrice(total)}</div>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function updateOrderSummary(items) {
    let total = 0;
    let count = 0;
    items.forEach(item => {
        const price = item.unitPrice || 0;
        const quantity = item.quantity || 1;
        total += price * quantity;
        count += quantity;
    });

    document.getElementById('items-count').textContent = `${count} шт.`;
    document.getElementById('total-price').textContent = formatPrice(total);
    updateCheckoutButton(total);
}

function setupEventHandlers() {
    const submitBtn = document.getElementById('submit-order-btn');
    submitBtn.addEventListener('click', handleSubmitOrder);

    // Валидация только согласия (других полей нет)
    document.getElementById('agree-terms').addEventListener('change', function() {
        // Можно дополнительно проверять, но основная валидация в validateForm
    });
}

function validateForm() {
    let isValid = true;

    // Проверяем согласие
    const agree = document.getElementById('agree-terms');
    if (!agree || !agree.checked) {
        showError('Необходимо согласиться с условиями использования');
        isValid = false;
    }

    // Проверяем наличие адреса (дополнительная защита)
    if (userData && (!userData.address || !userData.address.city)) {
        showError('Добавьте адрес доставки в профиле');
        isValid = false;
    }

    return isValid;
}

async function handleSubmitOrder(e) {
    e.preventDefault();

    if (!validateForm()) return;

    const requestItems = cartItems.map(item => ({
        productId: item.productId,
        quantity: item.quantity
    }));

    const comment = document.getElementById('comment').value.trim();

    const submitBtn = document.getElementById('submit-order-btn');
    const originalText = submitBtn.innerHTML;
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span>Оформление...</span>';

    try {
        const response = await fetch('/api/orders', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({
                comment: comment,
                request: requestItems
            })
        });

        if (!response.ok) {
            const err = await response.json().catch(() => ({}));
            throw new Error(err.error || `Ошибка ${response.status}`);
        }

        alert('✅ Заказ успешно оформлен!');
        window.location.href = '/';
    } catch (error) {
        console.error('Ошибка оформления:', error);
        showError(error.message);
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalText;
    }
}

function showLoading() {
    const container = document.getElementById('order-items-container');
    if (container) container.innerHTML = '<div class="loading-spinner">Загрузка...</div>';
}

function hideLoading() {
    // можно оставить пустым
}

function showError(message) {
    const modal = document.getElementById('error-modal');
    const messageElement = document.getElementById('error-message');
    if (messageElement) messageElement.textContent = message;
    if (modal) {
        modal.classList.add('active');
        document.body.style.overflow = 'hidden';
    }
}

function closeModal() {
    document.querySelectorAll('.modal').forEach(modal => modal.classList.remove('active'));
    document.body.style.overflow = '';
}

function updateCheckoutButton(total) {
    const button = document.getElementById('submit-order-btn');
    if (button) {
        const priceSpan = button.querySelector('.button-price') || (() => {
            const span = document.createElement('span');
            span.className = 'button-price';
            button.querySelector('.button-text')?.after(span);
            return span;
        })();
        priceSpan.textContent = ` ${formatPrice(total)}`;
    }
}

function formatPrice(price) {
    return new Intl.NumberFormat('ru-RU', {
        style: 'currency',
        currency: 'RUB',
        minimumFractionDigits: 0
    }).format(price || 0);
}

// Глобальные функции для модалок
window.closeModal = closeModal;

document.addEventListener('click', function(e) {
    if (e.target.classList.contains('modal')) closeModal();
});

document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') closeModal();
});