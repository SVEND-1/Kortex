class CartManager {
    constructor() {
        this.cart = { items: [], total: 0 };
        this._ready = this.refreshFromServer().catch(e => console.error(e));
    }

    getUserId() {
        return localStorage.getItem('userId') || null;
    }

    async ready() {
        return this._ready;
    }

    async refreshFromServer() {
        try {
            const headers = { 'Accept': 'application/json' };
            const response = await fetch('/api/carts/me', {
                method: 'GET',
                headers: headers,
                credentials: 'include'
            });

            if (!response.ok) {
                if (response.status === 401 || response.status === 403) {
                    this.cart = { items: [], total: 0 };
                    $(window).trigger('cartUpdated', [this.cart]);
                    return this.cart;
                }
                throw new Error(`HTTP ${response.status}`);
            }

            const data = await response.json();
            // items: каждый item имеет id, productId, price (общая стоимость), quantity
            const items = (data.items || []).map(item => ({
                id: item.id,
                productId: item.productId,
                quantity: Number(item.quantity) || 1,
                totalPrice: Number(item.price) || 0,  // полная стоимость позиции
                productName: null,
                image: null,
                unitPrice: null,
                description: ''
            }));

            this.cart = {
                items: items,
                total: Number(data.total) || 0
            };

            // Обогащаем товары данными из product-service
            await this.enrichItemsWithProductDetails();

            $(window).trigger('cartUpdated', [this.cart]);
            return this.cart;
        } catch (error) {
            console.error('Ошибка загрузки корзины:', error);
            this.cart = { items: [], total: 0 };
            $(window).trigger('cartUpdated', [this.cart]);
            return this.cart;
        }
    }

    // Загружает данные о товарах для всех productId
    async enrichItemsWithProductDetails() {
        const uniqueProductIds = [...new Set(this.cart.items.map(item => item.productId).filter(id => id))];
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

        // Обновляем каждый item, извлекая первое изображение из списка images
        this.cart.items = this.cart.items.map(item => {
            const product = productMap[item.productId];
            if (product) {
                // images - это массив, берём первый элемент или null
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

        // Пересчитываем total, если нужно (на всякий случай)
        this.cart.total = this.cart.items.reduce((sum, item) => sum + item.totalPrice, 0);
    }

    async addItem(productId) {
        try {
            const response = await fetch(`/api/carts/${productId}`, {
                method: 'POST',
                headers: { 'Accept': 'application/json' },
                credentials: 'include'
            });
            if (!response.ok) {
                const err = await response.json().catch(() => ({}));
                throw new Error(err.error || `Ошибка ${response.status}`);
            }
            return this.refreshFromServer();
        } catch (error) {
            console.error('Ошибка добавления:', error);
            throw error;
        }
    }

    async increase(itemId) {
        try {
            const response = await fetch(`/api/carts/items/${itemId}/increase`, {
                method: 'PATCH',
                headers: { 'Accept': 'application/json' },
                credentials: 'include'
            });
            if (!response.ok) throw new Error(`Ошибка ${response.status}`);
            return this.refreshFromServer();
        } catch (error) {
            console.error('Ошибка увеличения:', error);
            throw error;
        }
    }

    async decrease(itemId) {
        try {
            const response = await fetch(`/api/carts/items/${itemId}/decrease`, {
                method: 'PATCH',
                headers: { 'Accept': 'application/json' },
                credentials: 'include'
            });
            if (!response.ok) throw new Error(`Ошибка ${response.status}`);
            return this.refreshFromServer();
        } catch (error) {
            console.error('Ошибка уменьшения:', error);
            throw error;
        }
    }

    async remove(itemId) {
        try {
            const response = await fetch(`/api/carts/items/${itemId}`, {
                method: 'DELETE',
                headers: { 'Accept': 'application/json' },
                credentials: 'include'
            });
            if (!response.ok) throw new Error(`Ошибка ${response.status}`);
            return this.refreshFromServer();
        } catch (error) {
            console.error('Ошибка удаления:', error);
            throw error;
        }
    }

    async clearCart() {
        try {
            const response = await fetch('/api/carts', {
                method: 'DELETE',
                headers: { 'Accept': 'application/json' },
                credentials: 'include'
            });
            if (!response.ok) throw new Error(`Ошибка ${response.status}`);
            return this.refreshFromServer();
        } catch (error) {
            console.error('Ошибка очистки:', error);
            throw error;
        }
    }

    getItems() {
        return this.cart.items;
    }

    getTotal() {
        return this.cart.total;
    }

    getUniqueCount() {
        return this.cart.items.length;
    }
}

window.cartManager = new CartManager();