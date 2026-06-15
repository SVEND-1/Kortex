let cartManager = null;

async function initCartPage() {
    if (!window.cartManager) {
        console.error('cartManager не найден');
        return;
    }
    cartManager = window.cartManager;
    await cartManager.ready();
    renderCart();
    $(window).on('cartUpdated', function() {
        renderCart();
        if (typeof updateCartCounter === 'function') {
            updateCartCounter(cartManager.getUniqueCount());
        }
    });
}

function renderCart() {
    if (!cartManager) return;
    const items = cartManager.getItems();
    const total = cartManager.getTotal();

    const $cartItemsContainer = $('#cartItems');
    const $emptyCart = $('#emptyCart');
    const $cartWithItems = $('#cartWithItems');
    const $totalItemsText = $('#totalItemsText');
    const $totalPrice = $('#totalPrice');
    const $finalTotal = $('#finalTotal');
    const $checkoutBtn = $('#checkoutBtn');
    const $clearCartBtn = $('#clearCartBtn');

    if (!items || items.length === 0) {
        $emptyCart.show();
        $cartWithItems.hide();
        if ($totalItemsText.length) $totalItemsText.text('Товары (0)');
        if ($totalPrice.length) $totalPrice.text(formatPrice(0));
        if ($finalTotal.length) $finalTotal.text(formatPrice(0));
        if ($cartItemsContainer.length) $cartItemsContainer.empty();
        if ($checkoutBtn.length) $checkoutBtn.prop('disabled', true).css('opacity', '0.6');
        if ($clearCartBtn.length) $clearCartBtn.hide();
        return;
    }

    $emptyCart.hide();
    $cartWithItems.show();
    if ($checkoutBtn.length) $checkoutBtn.prop('disabled', false).css('opacity', '1');
    if ($clearCartBtn.length) $clearCartBtn.show();

    if ($cartItemsContainer.length) {
        const itemsHtml = items.map(item => {
            let imageUrl = '/images/product-img.png';
            if (item.image) {
                if (item.image.startsWith('http') || item.image.startsWith('/')) {
                    imageUrl = item.image;
                } else {
                    imageUrl = `/uploads/images/${item.image}`;
                }
            }

            const name = item.productName || 'Товар';
            const unitPrice = item.unitPrice;
            const quantity = item.quantity;
            const lineTotal = item.totalPrice;
            const displayUnitPrice = (unitPrice > 0) ? unitPrice : (lineTotal / quantity);
            const displayLineTotal = lineTotal;

            return `
            <div class="cart-item" data-item-id="${item.id}">
                <div class="item-image">
                    <img src="${imageUrl}" alt="${escapeHtml(name)}" 
                         onerror="this.onerror=null;this.src='/images/product-img.png'">
                </div>
                <div class="item-details">
                    <h3 class="item-name">${escapeHtml(name)}</h3>
                    <p class="item-description">${escapeHtml(item.description || '').substring(0, 120)}</p>
                    <div class="item-price">${formatPrice(displayUnitPrice)} <span class="per-piece">за шт.</span></div>
                    <div class="item-line-total">Итого: ${formatPrice(displayLineTotal)}</div>
                </div>
                <div class="item-controls">
                    <div class="quantity-controls">
                        <button type="button" class="quantity-btn minus-btn" data-action="decrease" data-id="${item.id}" ${quantity <= 1 ? 'disabled' : ''}>-</button>
                        <span class="quantity">${quantity}</span>
                        <button type="button" class="quantity-btn plus-btn" data-action="increase" data-id="${item.id}">+</button>
                    </div>
                    <button type="button" class="remove-btn" data-action="remove" data-id="${item.id}">Удалить</button>
                </div>
            </div>`;
        }).join('');
        $cartItemsContainer.html(itemsHtml);
    }

    const uniqueCount = cartManager.getUniqueCount();
    if ($totalItemsText.length) $totalItemsText.text(`Товары (${uniqueCount})`);
    if ($totalPrice.length) $totalPrice.text(formatPrice(total));
    if ($finalTotal.length) $finalTotal.text(formatPrice(total));

    attachCartEvents();
}

function attachCartEvents() {
    const $container = $('#cartItems');
    if (!$container.length) return;
    $container.off('click', handleCartClick);
    $container.on('click', handleCartClick);
}

async function handleCartClick(event) {
    const $button = $(event.target).closest('[data-action]');
    if (!$button.length) return;
    event.preventDefault();
    const action = $button.data('action');
    const itemId = $button.data('id');
    if (!action || !itemId) return;

    $button.prop('disabled', true);
    try {
        if (action === 'increase') await cartManager.increase(itemId);
        else if (action === 'decrease') await cartManager.decrease(itemId);
        else if (action === 'remove') {
            if (!confirm('Удалить товар из корзины?')) {
                $button.prop('disabled', false);
                return;
            }
            await cartManager.remove(itemId);
        }
    } catch (error) {
        console.error(error);
        showNotification('Ошибка: ' + error.message, 'error');
        $button.prop('disabled', false);
    }
}

function formatPrice(price) {
    return new Intl.NumberFormat('ru-RU', {
        style: 'currency',
        currency: 'RUB',
        minimumFractionDigits: 0
    }).format(price || 0);
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

$(document).on('click', '#clearCartBtn', async function() {
    if (!cartManager) return;
    if (confirm('Вы уверены, что хотите очистить корзину?')) {
        try {
            const loadingOverlay = document.getElementById('loadingOverlay');
            if (loadingOverlay) loadingOverlay.style.display = 'flex';
            await cartManager.clearCart();
            showNotification('Корзина очищена', 'success');
        } catch (error) {
            showNotification('Ошибка очистки: ' + error.message, 'error');
        } finally {
            const loadingOverlay = document.getElementById('loadingOverlay');
            if (loadingOverlay) loadingOverlay.style.display = 'none';
        }
    }
});

function showNotification(message, type = 'success') {
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    notification.style.cssText = `
        position: fixed; top: 20px; right: 20px; padding: 12px 24px;
        border-radius: 8px; background: ${type === 'success' ? '#4CAF50' : '#f44336'};
        color: white; z-index: 2000; box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    `;
    document.body.appendChild(notification);
    setTimeout(() => notification.remove(), 3000);
}

$(document).ready(initCartPage);