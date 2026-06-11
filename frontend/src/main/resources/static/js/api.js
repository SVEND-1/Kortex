// api.js – единый модуль для всех AJAX-запросов
const API_BASE_URL = ''; // пустая строка – запросы идут на тот же origin (Gateway)

let accessToken = localStorage.getItem('accessToken');
let refreshToken = localStorage.getItem('refreshToken');

// Флаг, чтобы не делать несколько refresh параллельно
let isRefreshing = false;
let refreshSubscribers = [];

function onRefreshed(newToken) {
    refreshSubscribers.forEach(cb => cb(newToken));
    refreshSubscribers = [];
}

export function setTokens(access, refresh) {
    accessToken = access;
    refreshToken = refresh;
    if (access) localStorage.setItem('accessToken', access);
    else localStorage.removeItem('accessToken');
    if (refresh) localStorage.setItem('refreshToken', refresh);
    else localStorage.removeItem('refreshToken');
}

export function clearTokens() {
    setTokens(null, null);
}

export async function apiRequest(url, options = {}) {
    // Копируем опции, чтобы не мутировать оригинал
    const opts = { ...options };
    opts.headers = opts.headers || {};
    opts.credentials = 'include'; // важно для кук (но токен тоже передаём)

    // Добавляем access-токен в заголовок, если он есть
    if (accessToken) {
        opts.headers['Authorization'] = `Bearer ${accessToken}`;
    }

    const makeRequest = () => fetch(url, opts);
    let response = await makeRequest();

    // Если не 401 – возвращаем ответ
    if (response.status !== 401) {
        return response;
    }

    // === 401: пробуем обновить токены ===
    if (!refreshToken) {
        // нет refresh‑токена – шлём на логин
        window.location.href = '/login';
        throw new Error('No refresh token');
    }

    if (isRefreshing) {
        // уже идёт обновление – подписываемся на новый токен
        return new Promise((resolve) => {
            refreshSubscribers.push(async (newToken) => {
                opts.headers['Authorization'] = `Bearer ${newToken}`;
                const retryResponse = await fetch(url, opts);
                resolve(retryResponse);
            });
        });
    }

    isRefreshing = true;
    try {
        const refreshResponse = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
            method: 'POST',
            headers: { 'X-Refresh-Token': refreshToken },
            credentials: 'include'
        });

        if (!refreshResponse.ok) {
            // refresh не удался – чистим токены и редирект
            clearTokens();
            window.location.href = '/login';
            throw new Error('Refresh failed');
        }

        const { accessToken: newAccess, refreshToken: newRefresh } = await refreshResponse.json();
        setTokens(newAccess, newRefresh);
        onRefreshed(newAccess);

        // Повторяем исходный запрос с новым токеном
        opts.headers['Authorization'] = `Bearer ${newAccess}`;
        response = await fetch(url, opts);
        return response;
    } catch (err) {
        console.error('Refresh error', err);
        throw err;
    } finally {
        isRefreshing = false;
    }
}

// Делаем функции глобальными (для удобного доступа из других скриптов, которые ещё не используют модули)
window.apiRequest = apiRequest;
window.setTokens = setTokens;
window.clearTokens = clearTokens;