// sellerScript.js – окончательная версия с защитой от битых ответов сервера

document.addEventListener('DOMContentLoaded', function() {
    const addTabContent = document.getElementById('add-tab');
    const editTabContent = document.getElementById('edit-tab');
    const productsContainer = document.getElementById('products-container');
    const tabButtons = document.querySelectorAll('.tab-button');
    const editModal = document.getElementById('edit-modal');

    const addProductForm = document.getElementById('add-product-form');
    const addSubmitBtn = document.getElementById('add-submit-btn');
    const addImageFiles = document.getElementById('add-imageFiles');
    const addFileNamesSpan = document.getElementById('add-fileNames');
    const addPreviews = document.getElementById('add-previews');

    const editId = document.getElementById('edit-id');
    const editName = document.getElementById('edit-name');
    const editPrice = document.getElementById('edit-price');
    const editCount = document.getElementById('edit-count');
    const editCategory = document.getElementById('edit-category');
    const editDescription = document.getElementById('edit-description');
    const updateDataBtn = document.getElementById('update-data-btn');
    const updateImagesBtn = document.getElementById('update-images-btn');
    const cancelEditBtn = document.getElementById('cancel-edit');
    const deleteProductBtn = document.getElementById('delete-product');
    const editImageFiles = document.getElementById('edit-imageFiles');
    const editFileNamesSpan = document.getElementById('edit-fileNames');
    const currentImagesContainer = document.getElementById('current-images-container');

    let currentEditingProductId = null;
    let currentProductImages = [];

    const API_BASE_URL = '/api/sellers';
    const DEFAULT_IMAGE = '/images/product-img.png';

    function init() {
        initTabs();
        initEventHandlers();
        loadSellerProducts();
    }

    function initTabs() {
        tabButtons.forEach(btn => {
            btn.addEventListener('click', () => {
                const tabId = btn.getAttribute('data-tab');
                tabButtons.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                if (tabId === 'add') {
                    addTabContent.classList.add('active');
                    editTabContent.classList.remove('active');
                    closeEditModal();
                } else {
                    addTabContent.classList.remove('active');
                    editTabContent.classList.add('active');
                    closeEditModal();
                    loadSellerProducts();
                }
            });
        });
    }

    function initEventHandlers() {
        addImageFiles.addEventListener('change', function() {
            const files = Array.from(this.files);
            if (files.length) {
                addFileNamesSpan.textContent = files.map(f => f.name).join(', ');
                addPreviews.innerHTML = '';
                files.forEach(file => {
                    const reader = new FileReader();
                    reader.onload = (e) => {
                        const img = document.createElement('img');
                        img.src = e.target.result;
                        img.classList.add('image-preview');
                        addPreviews.appendChild(img);
                    };
                    reader.readAsDataURL(file);
                });
            } else {
                addFileNamesSpan.textContent = 'Файлы не выбраны';
                addPreviews.innerHTML = '';
            }
        });

        editImageFiles.addEventListener('change', function() {
            const files = Array.from(this.files);
            editFileNamesSpan.textContent = files.length ? files.map(f => f.name).join(', ') : 'Файлы не выбраны';
        });

        addSubmitBtn.addEventListener('click', addProduct);
        updateDataBtn.addEventListener('click', updateProductData);
        updateImagesBtn.addEventListener('click', updateProductImages);
        cancelEditBtn.addEventListener('click', closeEditModal);
        deleteProductBtn.addEventListener('click', deleteProduct);

        const resetBtn = addProductForm.querySelector('.reset-btn');
        if (resetBtn) {
            resetBtn.addEventListener('click', () => {
                addFileNamesSpan.textContent = 'Файлы не выбраны';
                addPreviews.innerHTML = '';
                addImageFiles.value = '';
            });
        }

        document.querySelector('.modal-close').addEventListener('click', closeEditModal);
        editModal.addEventListener('click', (e) => {
            if (e.target === editModal) closeEditModal();
        });
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') closeEditModal();
        });
    }

    function openEditModal() {
        editModal.classList.add('active');
        document.body.style.overflow = 'hidden';
    }

    function closeEditModal() {
        editModal.classList.remove('active');
        document.body.style.overflow = '';
        currentEditingProductId = null;
        editId.value = '';
        editName.value = '';
        editPrice.value = '';
        editCount.value = '';
        editCategory.value = '';
        editDescription.value = '';
        editImageFiles.value = '';
        editFileNamesSpan.textContent = 'Файлы не выбраны';
        currentImagesContainer.innerHTML = '';
        currentProductImages = [];
    }

    async function loadSellerProducts() {
        try {
            productsContainer.innerHTML = '<p class="loading-text">Загрузка товаров...</p>';
            const response = await fetch(`${API_BASE_URL}/products`, {
                credentials: 'include'
            });
            if (response.status === 401) {
                showNotification('Требуется авторизация', 'error');
                setTimeout(() => window.location.href = '/login?redirect=/seller', 2000);
                return;
            }
            if (response.status === 403) {
                showNotification('У вас нет прав', 'error');
                setTimeout(() => window.location.href = '/profile', 2000);
                return;
            }
            if (!response.ok) throw new Error(`HTTP ${response.status}`);

            let products = await response.json();
            console.log('Ответ сервера /products:', products);

            // КОСТЫЛЬ: если ответ – массив вида ["java...", [...]]
            if (Array.isArray(products) && products.length === 2 && Array.isArray(products[1])) {
                products = products[1];
                console.log('Извлечён массив товаров:', products);
            }
            // Если ответ – объект Page с полем content
            else if (products && typeof products === 'object' && Array.isArray(products.content)) {
                products = products.content;
            }

            if (!Array.isArray(products) || products.length === 0) {
                productsContainer.innerHTML = '<div class="no-products"><p>У вас пока нет товаров</p></div>';
                return;
            }

            const validProducts = products.filter(p => p && typeof p === 'object' && (p.id !== undefined && p.id !== null));
            if (validProducts.length === 0) {
                productsContainer.innerHTML = '<div class="no-products"><p>Нет корректных товаров для отображения</p></div>';
                return;
            }

            renderProducts(validProducts);
        } catch (err) {
            console.error(err);
            productsContainer.innerHTML = `<div class="error-message"><p>Ошибка загрузки: ${err.message}</p><button onclick="loadSellerProducts()" class="retry-btn">Повторить</button></div>`;
        }
    }

    function renderProducts(products) {
        productsContainer.innerHTML = '';
        productsContainer.classList.add('products-grid');
        products.forEach(product => {
            const card = createProductCard(product);
            if (card) productsContainer.appendChild(card);
        });
    }

    function createProductCard(product) {
        if (!product || product.id === undefined || product.id === null) {
            console.warn('Пропущен товар без id', product);
            return null;
        }

        const card = document.createElement('div');
        card.className = 'product-card';
        card.dataset.id = product.id;

        const name = product.name && typeof product.name === 'string' ? product.name : 'Без названия';
        let priceDisplay = 'Цена не указана';
        if (product.price !== undefined && product.price !== null && !isNaN(parseFloat(product.price))) {
            priceDisplay = `${Number(product.price).toFixed(2)} ₽`;
        }
        let countDisplay = '? шт.';
        let stockClass = '';
        if (product.count !== undefined && product.count !== null && !isNaN(parseInt(product.count))) {
            const cnt = parseInt(product.count);
            countDisplay = `${cnt} шт.`;
            if (cnt < 10) stockClass = 'low';
        }

        const categoryMap = {
            'ELECTRONICS': 'Электроника', 'CLOTHING': 'Одежда', 'BOOKS': 'Книги',
            'FOOD': 'Еда', 'SPORTS': 'Спорт товары', 'HOME': 'Товары для дома',
            'BEAUTY': 'Красота', 'OTHER': 'Другое'
        };
        let categoryName = 'Другое';
        if (product.category && typeof product.category === 'string') {
            categoryName = categoryMap[product.category] || product.category;
        }

        const description = (product.description && typeof product.description === 'string') ? product.description : 'Описание отсутствует';
        let imageUrl = DEFAULT_IMAGE;
        if (product.images && Array.isArray(product.images) && product.images.length > 0 && product.images[0]) {
            imageUrl = product.images[0];
        }

        card.innerHTML = `
            <div class="product-image">
                <img src="${escapeHtml(imageUrl)}" alt="${escapeHtml(name)}" onerror="this.src='${DEFAULT_IMAGE}'">
            </div>
            <div class="product-info">
                <h3 class="product-name">${escapeHtml(name)}</h3>
                <div class="product-meta">
                    <p class="product-price">${escapeHtml(priceDisplay)}</p>
                    <p class="product-stock ${stockClass}">${escapeHtml(countDisplay)}</p>
                </div>
                <p class="product-category">${escapeHtml(categoryName)}</p>
                <p class="product-description">${escapeHtml(description)}</p>
            </div>
        `;
        card.addEventListener('click', (e) => {
            e.stopPropagation();
            editProduct(product.id);
        });
        return card;
    }

    async function addProduct() {
        const name = document.getElementById('add-name').value.trim();
        const price = parseFloat(document.getElementById('add-price').value);
        const count = parseInt(document.getElementById('add-count').value);
        const category = document.getElementById('add-category').value;
        const description = document.getElementById('add-description').value.trim();
        const files = addImageFiles.files;

        if (!validateProductData({ name, price, count, category, description })) return;
        if (files.length === 0) {
            showNotification('Выберите хотя бы одно изображение', 'error');
            return;
        }

        const formData = new FormData();
        formData.append('name', name);
        formData.append('price', price);
        formData.append('count', count);
        formData.append('category', category);
        formData.append('description', description);
        for (let i = 0; i < files.length; i++) {
            formData.append('imageFiles', files[i]);
        }

        try {
            addSubmitBtn.disabled = true;
            addSubmitBtn.textContent = 'Добавление...';
            const response = await fetch(`${API_BASE_URL}/products`, {
                method: 'POST',
                body: formData,
                credentials: 'include'
            });
            if (response.status === 401) {
                showNotification('Сессия истекла', 'error');
                setTimeout(() => window.location.href = '/login?redirect=/seller', 2000);
                return;
            }
            if (!response.ok) {
                const err = await response.json();
                throw new Error(err.error || 'Ошибка при создании товара');
            }
            showNotification('Товар успешно добавлен!', 'success');
            addProductForm.reset();
            addFileNamesSpan.textContent = 'Файлы не выбраны';
            addPreviews.innerHTML = '';
            addImageFiles.value = '';
            document.querySelector('[data-tab="edit"]').click();
        } catch (err) {
            showNotification(err.message, 'error');
        } finally {
            addSubmitBtn.disabled = false;
            addSubmitBtn.textContent = 'Добавить товар';
        }
    }

    async function editProduct(productId) {
        if (!productId || productId === 'undefined') {
            showNotification('Ошибка: идентификатор товара не указан', 'error');
            return;
        }
        try {
            const response = await fetch(`${API_BASE_URL}/products/${productId}`, {
                credentials: 'include'
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const text = await response.text();
            if (!text.trim()) throw new Error('Сервер вернул пустой ответ');
            let product = JSON.parse(text);
            console.log('Детали товара (сырой ответ):', product);

            // Если ответ тоже в странном формате (массив из двух элементов)
            if (Array.isArray(product) && product.length === 2 && product[1]) {
                product = product[1];
            }
            // Если ответ – объект с полем content (Page)
            else if (product && typeof product === 'object' && product.content && Array.isArray(product.content)) {
                product = product.content[0];
            }

            if (!product || !product.id) {
                throw new Error('Некорректные данные товара: ' + JSON.stringify(product));
            }

            currentEditingProductId = productId;
            currentProductImages = Array.isArray(product.images) ? product.images : [];

            editId.value = product.id;
            editName.value = product.name || '';
            editPrice.value = product.price !== undefined ? product.price : '';
            editCount.value = product.count !== undefined ? product.count : '';
            editCategory.value = product.category || 'OTHER';
            editDescription.value = product.description || '';

            renderCurrentImages(currentProductImages);
            editImageFiles.value = '';
            editFileNamesSpan.textContent = 'Файлы не выбраны';
            openEditModal();
        } catch (err) {
            showNotification(`Ошибка загрузки товара: ${err.message}`, 'error');
            console.error(err);
        }
    }

    function renderCurrentImages(imageUrls) {
        currentImagesContainer.innerHTML = '';
        if (!imageUrls.length) {
            currentImagesContainer.innerHTML = '<p>Нет изображений</p>';
            return;
        }
        imageUrls.forEach(url => {
            if (!url) return;
            const img = document.createElement('img');
            img.src = url;
            img.classList.add('current-image-item');
            img.onerror = () => img.src = DEFAULT_IMAGE;
            currentImagesContainer.appendChild(img);
        });
    }

    async function updateProductData() {
        const id = editId.value;
        if (!id || id === 'undefined') {
            showNotification('Ошибка: ID товара не определён', 'error');
            return;
        }
        const name = editName.value.trim();
        const price = parseFloat(editPrice.value);
        const count = parseInt(editCount.value);
        const category = editCategory.value;
        const description = editDescription.value.trim();

        if (!validateProductData({ name, price, count, category, description })) return;

        const payload = { name, price, count, category, description };
        try {
            updateDataBtn.disabled = true;
            updateDataBtn.textContent = 'Сохранение...';
            const response = await fetch(`${API_BASE_URL}/products/${id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
                credentials: 'include'
            });
            if (!response.ok) {
                const err = await response.json();
                throw new Error(err.error || 'Ошибка обновления');
            }
            showNotification('Данные обновлены', 'success');
            closeEditModal();
            loadSellerProducts();
        } catch (err) {
            showNotification(err.message, 'error');
        } finally {
            updateDataBtn.disabled = false;
            updateDataBtn.textContent = 'Сохранить данные';
        }
    }

    async function updateProductImages() {
        const productId = currentEditingProductId;
        if (!productId) {
            showNotification('Ошибка: товар не выбран', 'error');
            return;
        }
        const files = editImageFiles.files;
        if (!files.length) {
            showNotification('Выберите хотя бы одно изображение', 'error');
            return;
        }
        const formData = new FormData();
        for (let i = 0; i < files.length; i++) {
            formData.append('imageFiles', files[i]);
        }
        try {
            updateImagesBtn.disabled = true;
            updateImagesBtn.textContent = 'Загрузка...';
            const response = await fetch(`${API_BASE_URL}/images/${productId}`, {
                method: 'PUT',
                body: formData,
                credentials: 'include'
            });
            if (!response.ok) {
                const err = await response.json();
                throw new Error(err.error || 'Ошибка обновления изображений');
            }
            showNotification('Изображения обновлены', 'success');
            closeEditModal();
            loadSellerProducts();
        } catch (err) {
            showNotification(err.message, 'error');
        } finally {
            updateImagesBtn.disabled = false;
            updateImagesBtn.textContent = 'Обновить изображения';
        }
    }

    async function deleteProduct() {
        const productId = currentEditingProductId;
        if (!productId || !confirm('Вы уверены, что хотите удалить этот товар?')) return;
        try {
            deleteProductBtn.disabled = true;
            deleteProductBtn.textContent = 'Удаление...';
            const response = await fetch(`${API_BASE_URL}/products/${productId}`, {
                method: 'DELETE',
                credentials: 'include'
            });
            if (!response.ok) {
                const err = await response.json();
                throw new Error(err.error || 'Ошибка удаления');
            }
            showNotification('Товар удалён', 'success');
            closeEditModal();
            loadSellerProducts();
        } catch (err) {
            showNotification(err.message, 'error');
        } finally {
            deleteProductBtn.disabled = false;
            deleteProductBtn.textContent = 'Удалить товар';
        }
    }

    function validateProductData({ name, price, count, category }) {
        if (!name || name.length < 2) {
            showNotification('Название минимум 2 символа', 'error');
            return false;
        }
        if (isNaN(price) || price <= 0) {
            showNotification('Цена должна быть > 0', 'error');
            return false;
        }
        if (isNaN(count) || count < 0) {
            showNotification('Количество не может быть отрицательным', 'error');
            return false;
        }
        if (!category) {
            showNotification('Выберите категорию', 'error');
            return false;
        }
        return true;
    }

    function showNotification(message, type = 'info') {
        const existing = document.querySelector('.notification');
        if (existing) existing.remove();
        const notification = document.createElement('div');
        notification.className = `notification ${type}`;
        notification.textContent = message;
        notification.style.cssText = `
            position: fixed; top: 20px; right: 20px; padding: 12px 24px;
            border-radius: 8px; color: white; font-weight: bold;
            z-index: 2000; box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            background: ${type === 'success' ? '#4CAF50' : (type === 'error' ? '#f44336' : '#2196F3')};
        `;
        document.body.appendChild(notification);
        setTimeout(() => notification.remove(), 3000);
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str).replace(/[&<>]/g, function(m) {
            if (m === '&') return '&amp;';
            if (m === '<') return '&lt;';
            if (m === '>') return '&gt;';
            return m;
        });
    }

    window.loadSellerProducts = loadSellerProducts;
    window.editProduct = editProduct;
    window.closeEditModal = closeEditModal;

    init();
});