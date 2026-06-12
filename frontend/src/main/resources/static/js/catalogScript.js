// catalogScript.js - для работы с Spring бекендом (jQuery версия)
// Поддерживает массив images (в том числе полные URL)

let currentPage = 0;
let totalPages = 1;
let totalElements = 0;
let pageSize = 12;

$(document).ready(function() {
    console.log('Каталог товаров загружен');
    initializeCatalog();
    loadProducts(0);
    // Игнорируем ошибку 404 корзины, просто логируем
    updateCartCounter().catch(e => console.warn('Корзина не доступна', e));
});

function initializeCatalog() {
    setupFilters();
    setupSearch();
    setupPaginationListeners();
}

// Загрузка товаров с сервера с пагинацией
async function loadProducts(page = 0) {
    try {
        const $productsGrid = $('#productsGrid');
        $productsGrid.html(`
            <div class="loading-container">
                <div class="loading-spinner"></div>
                <p class="loading-text">Загрузка товаров...</p>
            </div>
        `);

        const category = $('#categoryFilter').val() || '';
        const searchTerm = $('#search-input').val() || '';

        const params = new URLSearchParams();
        params.append('page', page);
        params.append('size', pageSize);
        if (category) params.append('category', category);
        if (searchTerm) params.append('query', searchTerm);

        const url = `/api/products?${params.toString()}`;
        console.log('Запрашиваем URL:', url);

        const response = await fetch(url, {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        });

        if (!response.ok) {
            const errorText = await response.text();
            console.error('Ошибка ответа:', errorText);
            throw new Error(`Ошибка HTTP: ${response.status}`);
        }

        const data = await response.json();
        console.log('Получены данные:', data);

        if (data.content && Array.isArray(data.content)) {
            const products = data.content;
            currentPage = data.number || 0;
            totalPages = data.totalPages || 1;
            totalElements = data.totalElements || 0;
            displayProducts(products);
            updatePageInfo();
            updatePagination();
        } else if (Array.isArray(data)) {
            displayProducts(data);
            $('.pagination-container').hide();
        } else {
            console.warn('Неизвестная структура данных:', data);
            showError('Неверный формат данных от сервера');
        }
    } catch (error) {
        console.error('Ошибка при загрузке товаров:', error);
        showError('Не удалось загрузить товары. Попробуйте позже.');
    }
}

// Отображение товаров (поддержка полных URL изображений)
function displayProducts(products) {
    const $productsGrid = $('#productsGrid');

    if (!products || products.length === 0) {
        $productsGrid.html(`
            <div class="empty-catalog">
                <div class="empty-catalog-icon">📦</div>
                <h3>Товары не найдены</h3>
                <p>Попробуйте изменить параметры поиска или фильтрации</p>
                <button onclick="resetFilters()" class="btn-reset-filters">Сбросить фильтры</button>
            </div>
        `);
        $('.pagination-container').hide();
        return;
    }

    $('.pagination-container').show();

    const productsHtml = products.map(product => {
        // ---- ИСПРАВЛЕНИЕ: корректный URL изображения ----
        let imageUrl = '/images/product-img.png';
        if (product.images && Array.isArray(product.images) && product.images.length > 0) {
            let firstImage = product.images[0];
            if (firstImage && (firstImage.startsWith('http://') || firstImage.startsWith('https://'))) {
                imageUrl = firstImage; // уже полный URL
            } else if (firstImage) {
                imageUrl = `/uploads/images/${firstImage}`;
            }
        } else if (product.image) {
            if (product.image.startsWith('http://') || product.image.startsWith('https://')) {
                imageUrl = product.image;
            } else {
                imageUrl = `/uploads/images/${product.image}`;
            }
        }
        // -------------------------------------------------

        const categoryMap = {
            'ELECTRONICS': 'Электроника', 'CLOTHING': 'Одежда',
            'BOOKS': 'Книги', 'FOOD': 'Еда', 'SPORTS': 'Спорт',
            'HOME': 'Дом', 'BEAUTY': 'Красота', 'OTHER': 'Другое'
        };
        let categoryName = categoryMap[product.category] || product.category || 'Без категории';

        const inStock = product.count > 0;
        const stockText = inStock ? `В наличии: ${product.count}` : 'Нет в наличии';
        const stockClass = inStock ? 'in-stock' : 'out-of-stock';

        return `
        <div class="product-card" data-product-id="${product.id}">
            <div class="product-image">
                <img src="${imageUrl}" alt="${escapeHtml(product.name || 'Товар')}" 
                     onerror="this.onerror=null; this.src='/images/product-img.png'">
            </div>
            <div class="product-info">
                <h3 class="product-name">${escapeHtml(product.name || 'Без названия')}</h3>
                <div class="product-meta">
                    <span class="product-category">${categoryName}</span>
                </div>
                <p class="product-description">${escapeHtml(product.description || 'Описание отсутствует')}</p>
                <div class="product-price-row">
                    <div class="product-price">${formatPrice(product.price || 0)}</div>
                    <div class="product-stock ${stockClass}">${stockText}</div>
                </div>
                <div class="product-actions">
                    <button class="btn-add-to-cart" onclick="event.stopPropagation(); addToCartViaAPI(${product.id})" ${!inStock ? 'disabled' : ''}>
                        ${inStock ? 'В корзину' : 'Нет в наличии'}
                    </button>
                    <button class="btn-view-details" onclick="event.stopPropagation(); viewProductDetails(${product.id})">Подробнее</button>
                </div>
            </div>
        </div>
        `;
    }).join('');

    $productsGrid.html(productsHtml);
    makeProductCardsClickable();
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>]/g, function(m) {
        if (m === '&') return '&amp;';
        if (m === '<') return '&lt;';
        if (m === '>') return '&gt;';
        return m;
    });
}

function resetFilters() {
    $('#categoryFilter').val('');
    $('#search-input').val('');
    currentPage = 0;
    loadProducts(0);
}

function setupFilters() {
    const $categoryFilter = $('#categoryFilter');
    if ($categoryFilter.length) {
        $categoryFilter.on('change', function() {
            currentPage = 0;
            loadProducts(0);
        });
    }
    const $sortFilter = $('#sortFilter');
    if ($sortFilter.length) $sortFilter.hide(); // сортировка отключена
}

function setupSearch() {
    const $searchForm = $('#searchForm'); // изменили селектор
    const $searchInput = $('#search-input');

    $searchForm.on('submit', function(e) {
        e.preventDefault();
        currentPage = 0;
        loadProducts(0);
    });

    if ($searchInput.length) {
        let searchTimeout;
        $searchInput.on('input', function() {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => {
                currentPage = 0;
                loadProducts(0);
            }, 500);
        });
    }
}

function setupPaginationListeners() {
    $('#prevPage').on('click', function() {
        if (currentPage > 0) loadProducts(currentPage - 1);
    });
    $('#nextPage').on('click', function() {
        if (currentPage < totalPages - 1) loadProducts(currentPage + 1);
    });
}

function updatePageInfo() {
    const startItem = currentPage * pageSize + 1;
    const endItem = Math.min((currentPage + 1) * pageSize, totalElements);
    $('#shownItems').text(`${startItem}-${endItem}`);
    $('#totalItems').text(totalElements);
}

function updatePagination() {
    const $prevBtn = $('#prevPage');
    const $nextBtn = $('#nextPage');
    const $paginationNumbers = $('#paginationNumbers');

    $prevBtn.prop('disabled', currentPage === 0);
    $nextBtn.prop('disabled', currentPage === totalPages - 1 || totalPages === 0);

    if (totalPages <= 1) {
        $paginationNumbers.empty();
        return;
    }

    let startPage = Math.max(0, currentPage - 3);
    let endPage = Math.min(totalPages - 1, currentPage + 3);
    if (endPage - startPage + 1 < 7) {
        if (startPage === 0) endPage = Math.min(totalPages - 1, 6);
        else startPage = Math.max(0, totalPages - 7);
    }

    let pageNumbers = '';
    if (startPage > 0) {
        pageNumbers += `<button class="pagination-number" onclick="loadProducts(0)">1</button>`;
        if (startPage > 1) pageNumbers += `<span class="pagination-ellipsis">...</span>`;
    }
    for (let i = startPage; i <= endPage; i++) {
        pageNumbers += `<button class="pagination-number ${i === currentPage ? 'active' : ''}" onclick="loadProducts(${i})">${i+1}</button>`;
    }
    if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) pageNumbers += `<span class="pagination-ellipsis">...</span>`;
        pageNumbers += `<button class="pagination-number" onclick="loadProducts(${totalPages-1})">${totalPages}</button>`;
    }
    $paginationNumbers.html(pageNumbers);
}

function formatPrice(price) {
    let num = (typeof price === 'string') ? parseFloat(price) : price;
    if (isNaN(num)) num = 0;
    return new Intl.NumberFormat('ru-RU', {
        style: 'currency', currency: 'RUB',
        minimumFractionDigits: 0, maximumFractionDigits: 2
    }).format(num);
}

async function addToCartViaAPI(productId) {
    try {
        const response = await fetch(`/api/carts/items?productId=${productId}`, {
            method: "POST", credentials: "include",
            headers: { "Accept": "application/json" }
        });
        if (response.redirected || response.url.includes("/login")) {
            window.location.href = "/login";
            return;
        }
        const contentType = response.headers.get("content-type");
        if (contentType && contentType.includes("text/html")) {
            window.location.href = "/login";
            return;
        }
        if (!response.ok) {
            const err = await response.json().catch(() => ({}));
            throw new Error(err.error || "Ошибка при добавлении товара");
        }
        showNotification("Товар добавлен в корзину!");
        await updateCartCounter();
    } catch (e) {
        console.error(e);
        showNotification("Ошибка: " + e.message);
    }
}

async function updateCartCounter() {
    try {
        const response = await fetch('/api/carts/me', {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        });
        if (!response.ok) {
            if (response.status === 404) {
                console.warn('Эндпоинт корзины не найден (404)');
                return;
            }
            throw new Error(`HTTP ${response.status}`);
        }
        const cartData = await response.json();
        const totalItems = cartData.totalItems || 0;
        const $cartLink = $('a[href="/cart"]');
        $cartLink.find('.cart-counter').remove();
        if (totalItems > 0) {
            const $counter = $('<span>', { class: 'cart-counter', text: totalItems > 9 ? '9+' : totalItems });
            $cartLink.css('position', 'relative').append($counter);
        }
    } catch (error) {
        console.warn('Не удалось обновить счётчик корзины:', error);
    }
}

function viewProductDetails(productId) {
    window.location.href = `/productForm?id=${productId}`;
}

function makeProductCardsClickable() {
    $('.product-card').on('click', function(e) {
        if (!$(e.target).closest('button').length) {
            const productId = $(this).data('product-id');
            viewProductDetails(parseInt(productId));
        }
    });
}

function showNotification(message) {
    $('.notification').remove();
    const $notification = $('<div class="notification">' + message + '</div>');
    $('body').append($notification);
    setTimeout(() => $notification.css({ transform: 'translateX(0)', opacity: 1 }), 10);
    setTimeout(() => {
        $notification.css({ transform: 'translateX(100%)', opacity: 0 });
        setTimeout(() => $notification.remove(), 300);
    }, 3000);
}

function showError(message) {
    $('#productsGrid').html(`
        <div class="error-message">
            <div class="error-icon">⚠️</div>
            <h3>Ошибка загрузки</h3>
            <p>${message}</p>
            <button onclick="loadProducts(0)" class="btn-retry">Попробовать снова</button>
        </div>
    `);
    $('.pagination-container').hide();
}

// Глобальные функции
window.loadProducts = loadProducts;
window.resetFilters = resetFilters;
window.addToCartViaAPI = addToCartViaAPI;
window.viewProductDetails = viewProductDetails;
window.formatPrice = formatPrice;
window.updateCartCounter = updateCartCounter;