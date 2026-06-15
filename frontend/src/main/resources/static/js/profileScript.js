// profileScript.js — с модальным окном адреса, DaData, Leaflet, заявками на роли

document.addEventListener('DOMContentLoaded', async function() {
    try {
        initTabs();
        initModals();
        await loadUserProfile();
        await loadUserRoleRequests();
        hideOrdersSection();
        initAddressModal();
    } catch (error) {
        console.error('Ошибка инициализации:', error);
        showNotification('Не удалось загрузить профиль', 'error');
    }
});

// ============ Константы ============
const API = {
    profile: '/api/users',
    roleRequests: '/api/users/role-request'
};
const DADATA_TOKEN = 'be1da374113295d2e5a7f71025adb4986c7de957';

const ROLE_MAP = {
    'USER': { text: 'Покупатель', icon: '👤' },
    'SELLER': { text: 'Продавец', icon: '🏪' },
    'COURIER': { text: 'Курьер', icon: '🚚' },
    'ADMIN': { text: 'Админ', icon: '⚙️' }
};

const REQUEST_STATUS = {
    'PENDING': { text: '⏳ Ожидает', class: 'pending' },
    'APPROVED': { text: '✅ Одобрено', class: 'approved' },
    'REJECTED': { text: '❌ Отклонено', class: 'rejected' }
};

// ============ Вспомогательная функция очистки названия региона ============
function cleanRegionName(regionWithType) {
    if (!regionWithType) return '';
    // Убираем типовые слова: республика, респ, область, обл, край, г., город и т.д.
    const cleaned = regionWithType
        .replace(/^(республика|респ)\s+/i, '')
        .replace(/^(область|обл)\s+/i, '')
        .replace(/^(край)\s+/i, '')
        .replace(/^(город|г\.)\s+/i, '');
    return cleaned.trim();
}

// ============ Загрузка и отображение профиля ============
async function loadUserProfile() {
    try {
        const response = await fetch(API.profile, {
            method: 'GET',
            headers: { 'Accept': 'application/json' },
            credentials: 'include'
        });
        if (response.status === 401) {
            window.location.href = '/login?redirect=/profile';
            return;
        }
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const user = await response.json();
        renderProfile(user);
    } catch (err) {
        console.error(err);
        showNotification('Ошибка загрузки профиля', 'error');
        renderDefaultProfile();
    }
}

function renderProfile(user) {
    document.getElementById('profileName').value = user.name || '';
    document.getElementById('profileEmail').value = user.email || '';
    document.querySelector('.profile-name').textContent = user.name || 'Гость';
    document.querySelector('.profile-email').textContent = user.email || '';

    const addr = user.address || {};
    const regionClean = cleanRegionName(addr.region);
    const addressString = [regionClean, addr.city, addr.street, addr.house].filter(Boolean).join(', ') || 'Не указан';
    document.getElementById('currentAddressDisplay').textContent = addressString;

    // Сохраняем в скрытые поля
    document.getElementById('profileRegion').value = addr.region || '';
    document.getElementById('profileCity').value = addr.city || '';
    document.getElementById('profileStreet').value = addr.street || '';
    document.getElementById('profileHouse').value = addr.house || '';
    document.getElementById('profileApartment').value = addr.apartment || '';
    if (user.latitude) document.getElementById('profileLat').value = user.latitude;
    if (user.longitude) document.getElementById('profileLon').value = user.longitude;

    const role = user.role || 'USER';
    document.body.dataset.userRole = role;
    updateRoleDisplay(role);
    updateRoleButtons(role);
}

function renderDefaultProfile() {
    document.getElementById('profileName').value = 'Гость';
    document.getElementById('profileEmail').value = 'Не авторизован';
    document.querySelector('.profile-name').textContent = 'Гость';
    document.querySelector('.profile-email').textContent = 'Войдите в аккаунт';
    document.getElementById('currentAddressDisplay').textContent = 'Не указан';
    const submitBtn = document.querySelector('#personalForm .btn-primary');
    if (submitBtn) submitBtn.disabled = true;
}

// ============ Роли и кнопки ============
function updateRoleDisplay(role) {
    const info = ROLE_MAP[role] || ROLE_MAP.USER;
    const roleElements = document.querySelectorAll('.user-role-display');
    roleElements.forEach(el => el.innerHTML = `${info.icon} ${info.text}`);
    updateSidebarButtons(role);
}

function updateSidebarButtons(role) {
    const container = document.querySelector('.role-buttons');
    if (!container) return;
    container.innerHTML = '';
    if (role === 'USER') {
        container.innerHTML = `<a href="/seller" class="btn btn-seller">🏪 Для продавца</a>
                               <a href="/courier" class="btn btn-courier">🚚 Для курьера</a>`;
    } else if (role === 'SELLER') {
        container.innerHTML = `<a href="/seller" class="btn btn-seller">🏪 Панель продавца</a>
                               <a href="/courier" class="btn btn-courier" style="opacity:0.6; pointer-events:none;">🚚 Для курьера</a>`;
    } else if (role === 'COURIER') {
        container.innerHTML = `<a href="/seller" class="btn btn-seller" style="opacity:0.6; pointer-events:none;">🏪 Для продавца</a>
                               <a href="/courier" class="btn btn-courier">🚚 Панель курьера</a>`;
    } else if (role === 'ADMIN') {
        container.innerHTML = `<a href="/seller" class="btn btn-seller">🏪 Панель продавца</a>
                               <a href="/courier" class="btn btn-courier">🚚 Панель курьера</a>
                               <a href="/admin" class="btn btn-admin">⚙️ Админка</a>`;
    }
}

function updateRoleButtons(role) {
    const cards = document.querySelectorAll('.request-card');
    if (cards.length < 3) return;
    const [sellerCard, courierCard, downgradeCard] = cards;
    const sellerBtn = sellerCard.querySelector('button');
    const courierBtn = courierCard.querySelector('button');
    const downgradeBtn = downgradeCard.querySelector('button');

    if (role === 'USER') {
        sellerBtn.textContent = '📝 Стать продавцом'; sellerBtn.disabled = false;
        courierBtn.textContent = '📝 Стать курьером'; courierBtn.disabled = false;
        downgradeBtn.disabled = true; downgradeCard.style.opacity = '0.6';
    } else if (role === 'SELLER') {
        sellerBtn.textContent = '✅ Вы уже продавец'; sellerBtn.disabled = true;
        courierBtn.textContent = '📝 Стать курьером'; courierBtn.disabled = false;
        downgradeBtn.disabled = false; downgradeCard.style.opacity = '1';
        downgradeBtn.textContent = '📝 Сняться с роли продавца';
    } else if (role === 'COURIER') {
        sellerBtn.textContent = '📝 Стать продавцом'; sellerBtn.disabled = false;
        courierBtn.textContent = '✅ Вы уже курьер'; courierBtn.disabled = true;
        downgradeBtn.disabled = false; downgradeCard.style.opacity = '1';
        downgradeBtn.textContent = '📝 Сняться с роли курьера';
    } else if (role === 'ADMIN') {
        sellerBtn.disabled = true; courierBtn.disabled = true; downgradeBtn.disabled = true;
        sellerBtn.textContent = '🚫 Недоступно'; courierBtn.textContent = '🚫 Недоступно'; downgradeBtn.textContent = '🚫 Недоступно';
    }
}

// ============ Сохранение имени и email ============
document.getElementById('personalForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const name = document.getElementById('profileName').value.trim();
    const email = document.getElementById('profileEmail').value.trim();
    if (!name) return showNotification('Имя не может быть пустым', 'error');

    const btn = e.target.querySelector('button[type="submit"]');
    const originalText = btn.textContent;
    btn.disabled = true;
    btn.textContent = 'Сохранение...';
    try {
        const response = await fetch(API.profile, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email }), // только имя и email
            credentials: 'include'
        });
        if (response.ok) {
            showNotification('✅ Имя и email обновлены', 'success');
            document.querySelector('.profile-name').textContent = name;
        } else {
            const err = await response.text();
            throw new Error(err || 'Ошибка сервера');
        }
    } catch (err) {
        showNotification(`❌ ${err.message}`, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = originalText;
    }
});

// ============ Заявки на роли ============
async function loadUserRoleRequests() {
    try {
        const response = await fetch(API.roleRequests, { credentials: 'include' });
        if (response.status === 404) {
            renderRoleRequests([]);
            return;
        }
        if (!response.ok) throw new Error();
        const requests = await response.json();
        renderRoleRequests(requests);
    } catch {
        renderRoleRequests([]);
    }
}

function renderRoleRequests(requests) {
    const container = document.getElementById('requestsList');
    if (!container) return;
    if (!requests || requests.length === 0) {
        container.innerHTML = `<div class="no-requests">📭 У вас ещё нет заявок</div>`;
        return;
    }
    container.innerHTML = requests.map(req => {
        const status = REQUEST_STATUS[req.status] || REQUEST_STATUS.PENDING;
        const action = req.typeAction === 'ENHANCE' ? 'Повышение до' : 'Снятие роли';
        const roleName = ROLE_MAP[req.requestedRole]?.text || req.requestedRole;
        const date = formatDate(req.createdAt);
        return `
            <div class="request-item status-${status.class}">
                <div class="request-header">
                    <span>${action}</span>
                    <span class="request-role">${roleName}</span>
                    <span class="request-date">${date}</span>
                </div>
                <div class="request-body"><p>${escapeHtml(req.message || 'Без описания')}</p></div>
                <div class="request-footer">
                    <span class="request-status ${status.class}">${status.text}</span>
                    <span>ID: ${req.id}</span>
                </div>
            </div>
        `;
    }).join('');
}

async function submitRoleRequest(type, requestedRole, message) {
    const response = await fetch(API.roleRequests, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ requestedRole, typeAction: type, message }),
        credentials: 'include'
    });
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Ошибка при отправке');
    }
    return response.json();
}

// ============ Модальные окна заявок ============
function initModals() {
    const msgField = document.getElementById('requestMessage');
    if (msgField) msgField.addEventListener('input', () => {
        document.getElementById('charCount').innerText = msgField.value.length;
    });
    const downgradeMsg = document.getElementById('downgradeMessage');
    if (downgradeMsg) downgradeMsg.addEventListener('input', () => {
        document.getElementById('downgradeCharCount').innerText = downgradeMsg.value.length;
    });

    const requestForm = document.getElementById('requestForm');
    if (requestForm) {
        requestForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const role = document.getElementById('requestRole').value;
            const message = document.getElementById('requestMessage').value.trim();
            if (!role) return showNotification('Выберите роль', 'error');
            if (message.length < 20) return showNotification('Опишите причину подробнее (мин. 20 символов)', 'error');
            const btn = requestForm.querySelector('button[type="submit"]');
            btn.disabled = true;
            try {
                await submitRoleRequest('ENHANCE', role, message);
                closeRequestModal();
                await loadUserRoleRequests();
                showNotification('✅ Заявка отправлена', 'success');
            } catch (err) {
                showNotification(`❌ ${err.message}`, 'error');
            } finally {
                btn.disabled = false;
            }
        });
    }

    const downgradeForm = document.getElementById('downgradeForm');
    if (downgradeForm) {
        downgradeForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const currentRole = document.getElementById('currentRole').value;
            const message = document.getElementById('downgradeMessage').value.trim();
            if (!currentRole) return showNotification('Выберите текущую роль', 'error');
            if (message.length < 20) return showNotification('Опишите причину (мин. 20 символов)', 'error');
            const btn = downgradeForm.querySelector('button[type="submit"]');
            btn.disabled = true;
            try {
                await submitRoleRequest('REMOVE', 'USER', message);
                closeDowngradeModal();
                await loadUserRoleRequests();
                showNotification('✅ Заявка на снятие роли отправлена', 'success');
            } catch (err) {
                showNotification(`❌ ${err.message}`, 'error');
            } finally {
                btn.disabled = false;
            }
        });
    }

    document.querySelectorAll('.modal').forEach(modal => {
        modal.addEventListener('click', (e) => {
            if (e.target === modal || e.target.classList.contains('modal-close')) {
                modal.style.display = 'none';
                const form = modal.querySelector('form');
                if (form) form.reset();
            }
        });
    });
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') document.querySelectorAll('.modal').forEach(m => m.style.display = 'none');
    });
}

window.openRequestModal = (type) => {
    const modal = document.getElementById('requestModal');
    if (!modal) return;
    const select = document.getElementById('requestRole');
    const title = document.getElementById('requestModalTitle');
    const roleSpan = document.getElementById('roleName');
    if (type === 'seller') {
        select.value = 'SELLER';
        title.innerText = 'Заявка на роль продавца';
        roleSpan.innerText = 'продавцом';
    } else if (type === 'courier') {
        select.value = 'COURIER';
        title.innerText = 'Заявка на роль курьера';
        roleSpan.innerText = 'курьером';
    }
    modal.style.display = 'flex';
};
window.closeRequestModal = () => {
    const modal = document.getElementById('requestModal');
    if (modal) modal.style.display = 'none';
};
window.openDowngradeModal = () => {
    const modal = document.getElementById('downgradeModal');
    if (modal) {
        const currentRole = document.body.dataset.userRole;
        const select = document.getElementById('currentRole');
        if (currentRole && currentRole !== 'USER') {
            select.value = currentRole;
            select.disabled = true;
        } else {
            select.disabled = false;
            select.value = '';
        }
        modal.style.display = 'flex';
    }
};
window.closeDowngradeModal = () => {
    const modal = document.getElementById('downgradeModal');
    if (modal) modal.style.display = 'none';
};

// ============ Модальное окно для адреса ============
let addressModal, map, marker;

function initAddressModal() {
    const modal = document.getElementById('addressModal');
    const openBtn = document.getElementById('openAddressModalBtn');
    const closeBtn = document.getElementById('closeAddressModalBtn');
    const cancelBtn = document.getElementById('cancelAddressBtn');
    const saveBtn = document.getElementById('saveAddressBtn');

    if (!modal) return;

    openBtn?.addEventListener('click', () => {
        // Загружаем текущие данные из скрытых полей
        const region = document.getElementById('profileRegion').value;
        const city = document.getElementById('profileCity').value;
        const street = document.getElementById('profileStreet').value;
        const house = document.getElementById('profileHouse').value;
        const apartment = document.getElementById('profileApartment').value;
        const lat = parseFloat(document.getElementById('profileLat').value) || 55.751574;
        const lon = parseFloat(document.getElementById('profileLon').value) || 37.573856;

        document.getElementById('modalRegion').value = region;
        document.getElementById('modalCity').value = city;
        document.getElementById('modalStreet').value = street;
        document.getElementById('modalHouse').value = house;
        document.getElementById('modalApartment').value = apartment;
        document.getElementById('modalLat').value = lat;
        document.getElementById('modalLon').value = lon;

        modal.style.display = 'flex';
        if (!map) {
            initModalMap(lat, lon);
            initModalDadata();
        } else {
            map.setView([lat, lon], 15);
            marker.setLatLng([lat, lon]);
        }
    });

    const closeModal = () => {
        modal.style.display = 'none';
    };
    closeBtn?.addEventListener('click', closeModal);
    cancelBtn?.addEventListener('click', closeModal);

    saveBtn?.addEventListener('click', async () => {
        const region = document.getElementById('modalRegion').value.trim();
        const city = document.getElementById('modalCity').value.trim();
        const street = document.getElementById('modalStreet').value.trim();
        const house = document.getElementById('modalHouse').value.trim();
        const apartment = document.getElementById('modalApartment').value.trim();
        const lat = document.getElementById('modalLat').value;
        const lon = document.getElementById('modalLon').value;

        if (!city || !street) {
            showNotification('Укажите город и улицу (выберите адрес на карте или из подсказок)', 'error');
            return;
        }

        const payload = {
            region, city, street, house, apartment,
            latitude: parseFloat(lat) || 0,
            longitude: parseFloat(lon) || 0
        };

        saveBtn.disabled = true;
        saveBtn.textContent = 'Сохранение...';
        try {
            const response = await fetch(API.profile, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
                credentials: 'include'
            });
            if (response.ok) {
                showNotification('✅ Адрес обновлён', 'success');
                // Обновляем скрытые поля на главной
                document.getElementById('profileRegion').value = region;
                document.getElementById('profileCity').value = city;
                document.getElementById('profileStreet').value = street;
                document.getElementById('profileHouse').value = house;
                document.getElementById('profileApartment').value = apartment;
                document.getElementById('profileLat').value = lat;
                document.getElementById('profileLon').value = lon;
                // Обновляем отображение адреса
                const regionClean = cleanRegionName(region);
                const addressString = [regionClean, city, street, house].filter(Boolean).join(', ') || 'Не указан';
                document.getElementById('currentAddressDisplay').textContent = addressString;
                closeModal();
            } else {
                const err = await response.text();
                throw new Error(err || 'Ошибка сервера');
            }
        } catch (err) {
            showNotification(`❌ ${err.message}`, 'error');
        } finally {
            saveBtn.disabled = false;
            saveBtn.textContent = 'Сохранить адрес';
        }
    });

    modal.addEventListener('click', (e) => {
        if (e.target === modal) closeModal();
    });
}

function initModalMap(lat, lon, zoom = 15) {
    const container = document.getElementById('modalMap');
    if (!container) return;
    if (map) map.remove();

    map = L.map('modalMap').setView([lat, lon], zoom);
    L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/attributions">CARTO</a>',
        subdomains: 'abcd',
        maxZoom: 19
    }).addTo(map);

    const icon = L.icon({
        iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
        shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
        iconSize: [25, 41],
        iconAnchor: [12, 41]
    });
    marker = L.marker([lat, lon], { draggable: true, icon }).addTo(map);

    marker.on('dragend', async () => {
        const pos = marker.getLatLng();
        document.getElementById('modalLat').value = pos.lat;
        document.getElementById('modalLon').value = pos.lng;
        try {
            const resp = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${pos.lat}&lon=${pos.lng}&addressdetails=1&accept-language=ru`);
            const data = await resp.json();
            if (data && data.address) {
                const a = data.address;
                const regionRaw = a.state || a.region || '';
                document.getElementById('modalRegion').value = cleanRegionName(regionRaw);
                document.getElementById('modalCity').value = a.city || a.town || a.village || '';
                document.getElementById('modalStreet').value = a.road || '';
                document.getElementById('modalHouse').value = a.house_number || '';
            }
        } catch (err) { console.error('Reverse geocoding error:', err); }
    });
}

function initModalDadata() {
    const input = document.getElementById('modalAddressAutocomplete');
    if (!input) return;

    let timeoutId;
    input.addEventListener('input', function() {
        clearTimeout(timeoutId);
        const query = this.value.trim();
        if (query.length < 3) return;
        timeoutId = setTimeout(() => fetchSuggestions(query), 300);
    });

    async function fetchSuggestions(query) {
        try {
            const response = await fetch('https://suggestions.dadata.ru/suggestions/api/4_1/rs/suggest/address', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json',
                    'Authorization': 'Token ' + DADATA_TOKEN
                },
                body: JSON.stringify({ query, count: 8 })
            });
            if (!response.ok) {
                if (response.status === 403) showNotification('Ошибка DaData: проверьте API-ключ', 'error');
                return;
            }
            const data = await response.json();
            if (data.suggestions) showSuggestionsDropdown(data.suggestions);
        } catch (err) { console.error(err); }
    }

    function showSuggestionsDropdown(suggestions) {
        let dropdown = document.getElementById('dadata-modal-dropdown');
        if (!dropdown) {
            dropdown = document.createElement('div');
            dropdown.id = 'dadata-modal-dropdown';
            dropdown.style.cssText = `
                position: absolute;
                background: white;
                border: 1px solid #ccc;
                border-radius: 4px;
                max-height: 200px;
                overflow-y: auto;
                z-index: 1001;
                width: ${input.offsetWidth}px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            `;
            input.parentNode.style.position = 'relative';
            input.parentNode.appendChild(dropdown);
        }
        dropdown.innerHTML = '';
        suggestions.forEach(sug => {
            const item = document.createElement('div');
            item.textContent = sug.value;
            item.style.cssText = 'padding: 8px 12px; cursor: pointer; border-bottom: 1px solid #eee;';
            item.addEventListener('click', () => {
                input.value = sug.value;
                dropdown.style.display = 'none';
                const data = sug.data;
                const regionRaw = data.region_with_type || '';
                document.getElementById('modalRegion').value = cleanRegionName(regionRaw);
                document.getElementById('modalCity').value = data.city || data.settlement || '';
                document.getElementById('modalStreet').value = data.street_with_type || '';
                document.getElementById('modalHouse').value = data.house || '';
                const lat = data.geo_lat, lon = data.geo_lon;
                if (lat && lon && map && marker) {
                    const newLat = parseFloat(lat);
                    const newLon = parseFloat(lon);
                    map.setView([newLat, newLon], 16);
                    marker.setLatLng([newLat, newLon]);
                    document.getElementById('modalLat').value = newLat;
                    document.getElementById('modalLon').value = newLon;
                }
                document.getElementById('modalHouse').focus();
            });
            dropdown.appendChild(item);
        });
        dropdown.style.display = 'block';
        const closeHandler = (e) => {
            if (!dropdown.contains(e.target) && e.target !== input) {
                dropdown.style.display = 'none';
                document.removeEventListener('click', closeHandler);
            }
        };
        setTimeout(() => document.addEventListener('click', closeHandler), 100);
    }
}

// ============ Вспомогательные функции ============
function initTabs() {
    const tabs = document.querySelectorAll('.tab-button');
    tabs.forEach(btn => {
        btn.addEventListener('click', () => {
            const tabId = btn.dataset.tab;
            tabs.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            document.querySelectorAll('.tab-pane').forEach(pane => pane.classList.remove('active'));
            document.getElementById(`${tabId}-tab`).classList.add('active');
        });
    });
}

function hideOrdersSection() {
    const ordersSection = document.querySelector('.orders-history');
    if (ordersSection) ordersSection.style.display = 'none';
}

function formatDate(dateStr) {
    if (!dateStr) return 'не указана';
    try { return new Date(dateStr).toLocaleString('ru-RU', { day:'2-digit', month:'2-digit', year:'numeric', hour:'2-digit', minute:'2-digit' }); }
    catch { return dateStr; }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>]/g, (m) => m === '&' ? '&amp;' : (m === '<' ? '&lt;' : '&gt;'));
}

function showNotification(msg, type = 'success') {
    const notif = document.createElement('div');
    notif.className = `profile-notification ${type}`;
    notif.innerHTML = `<span>${type === 'success' ? '✅' : type === 'error' ? '❌' : '⚠️'} ${msg}</span>`;
    document.body.appendChild(notif);
    setTimeout(() => {
        notif.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => notif.remove(), 300);
    }, 3000);
}