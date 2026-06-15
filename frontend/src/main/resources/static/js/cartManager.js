// cartManager.js – работа с корзиной через API (добавлен X-User-Id)

class CartManager {
    constructor() {
        this.cart = { items: [], total: 0 };
        this._ready = this.refreshFromServer().catch(e => console.error(e));
    }

    // Получить userId из localStorage
    getUserId() {
        return localStorage.getItem('userId') || null;
    }

    async ready() {
        return this._ready;
    }

    async refreshFromServer() {
        try {
            const headers = { 'Accept': 'application/json' };
            const userId = this.getUserId();
            if (userId) headers['X-User-Id'] = userId;

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
            this.cart = {
                items: (data.items || []).map(item => ({
                    id: item.id,
                    productId: item.productId,
                    productName: item.productName,
                    price: item.price,
                    quantity: item.quantity,
                    description: item.description,
                    image: item.image
                })),
                total: data.totalPrice || 0
            };
            $(window).trigger('cartUpdated', [this.cart]);
            return this.cart;
        } catch (error) {
            console.error('Ошибка загрузки корзины:', error);
            this.cart = { items: [], total: 0 };
            $(window).trigger('cartUpdated', [this.cart]);
            return this.cart;
        }
    }

    async addItem(productId) {
        try {
            const headers = { 'Accept': 'application/json' };
            const userId = this.getUserId();
            if (userId) headers['X-User-Id'] = userId;

            const response = await fetch(`/api/carts/${productId}`, {
                method: 'POST',
                headers: headers,
                credentials: 'include'
            });
            if (!response.ok) {
                const err = await response.json().catch(() => ({}));
                throw new Error(err.error || `Ошибка ${response.status}`);
            }
            return this.refreshFromServer();
        } catch (error) {
            console.error('Ошибка добавления товара:', error);
            throw error;
        }
    }

    async increase(itemId) {
        try {
            const headers = { 'Accept': 'application/json' };
            const userId = this.getUserId();
            if (userId) headers['X-User-Id'] = userId;

            const response = await fetch(`/api/carts/items/${itemId}/increase`, {
                method: 'PATCH',
                headers: headers,
                credentials: 'include'
            });
            if (!response.ok) throw new Error(`Ошибка ${response.status}`);
            return this.refreshFromServer();
        } catch (error) {
            console.error('Ошибка увеличения количества:', error);
            throw error;
        }
    }

    async decrease(itemId) {
        try {
            const headers = { 'Accept': 'application/json' };
            const userId = this.getUserId();
            if (userId) headers['X-User-Id'] = userId;

            const response = await fetch(`/api/carts/items/${itemId}/decrease`, {
                method: 'PATCH',
                headers: headers,
                credentials: 'include'
            });
            if (!response.ok) throw new Error(`Ошибка ${response.status}`);
            return this.refreshFromServer();
        } catch (error) {
            console.error('Ошибка уменьшения количества:', error);
            throw error;
        }
    }

    async remove(itemId) {
        try {
            const headers = { 'Accept': 'application/json' };
            const userId = this.getUserId();
            if (userId) headers['X-User-Id'] = userId;

            const response = await fetch(`/api/carts/items/${itemId}`, {
                method: 'DELETE',
                headers: headers,
                credentials: 'include'
            });
            if (!response.ok) throw new Error(`Ошибка ${response.status}`);
            return this.refreshFromServer();
        } catch (error) {
            console.error('Ошибка удаления товара:', error);
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