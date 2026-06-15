class ProductManager {
    // Получить товар по ID
    async getProductById(id) {
        try {
            const response = await fetch(`/api/products/${id}`);
            if (response.ok) {
                return await response.json();
            }
            throw new Error(`Ошибка HTTP: ${response.status}`);
        } catch (error) {
            console.error('Ошибка при запросе к серверу:', error);
            return null;
        }
    }

    // Получить все товары
    async getAllProducts() {
        try {
            const response = await fetch('/api/products');
            if (response.ok) {
                const data = await response.json();
                return data.content || data;
            }
            throw new Error(`Ошибка HTTP: ${response.status}`);
        } catch (error) {
            console.error('Ошибка при запросе к серверу:', error);
            return [];
        }
    }

    // Получить товары продавца
    async getSellerProducts() {
        try {
            const response = await fetch('/api/sellers/products', {
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`Ошибка HTTP: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Ошибка при получении товаров продавца:', error);
            return [];
        }
    }

    // Добавить товар
    async addProduct(productData, imageFile) {
        try {
            const formData = new FormData();
            formData.append('name', productData.name);
            formData.append('price', productData.price);
            formData.append('count', productData.count);
            formData.append('category', productData.category);
            formData.append('description', productData.description);
            formData.append('brand', productData.brand || '');

            if (imageFile) {
                formData.append('imageFile', imageFile);
            }

            const response = await fetch('/api/sellers/products', {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.error || `Ошибка HTTP: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            throw new Error(error.message);
        }
    }

    // Обновить товар
    async updateProduct(productId, productData, imageFile = null) {
        try {
            const formData = new FormData();
            formData.append('name', productData.name);
            formData.append('price', productData.price);
            formData.append('count', productData.count);
            formData.append('category', productData.category);
            formData.append('description', productData.description);
            formData.append('brand', productData.brand || '');

            if (imageFile) {
                formData.append('imageFile', imageFile);
            }

            const response = await fetch(`/api/sellers/products/${productId}`, {
                method: 'PUT',
                body: formData
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.error || `Ошибка HTTP: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            throw new Error(error.message);
        }
    }

    // Удалить товар
    async deleteProduct(productId) {
        try {
            const response = await fetch(`/api/sellers/products/${productId}`, {
                method: 'DELETE'
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.error || `Ошибка HTTP: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            throw new Error(error.message);
        }
    }

    // Получить название категории
    getCategoryName(categoryKey) {
        const categories = {
            ELECTRONICS: 'Электроника',
            CLOTHING: 'Одежда',
            BOOKS: 'Книги',
            FOOD: 'Еда',
            SPORTS: 'Спорт товары',
            HOME: 'Товары для дома',
            BEAUTY: 'Красота',
            OTHER: 'Другое'
        };
        return categories[categoryKey] || 'Другое';
    }
}

window.productManager = new ProductManager();