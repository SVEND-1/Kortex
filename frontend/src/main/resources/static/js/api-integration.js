// ========== КОНСТАНТЫ ==========
const API = {
    REGISTER_SEND_CODE: '/api/auth/register/send-code',
    REGISTER_VERIFY: '/api/auth/register/verify',
    REGISTER_RESEND: '/api/auth/register/resend-code',
    LOGIN: '/api/auth/login',
    LOGOUT: '/api/auth/logout',
    PASSWORD_FORGOT: '/api/auth/password/forgot',
    PASSWORD_VERIFY: '/api/auth/password/verify',
    PASSWORD_RESET: '/api/auth/password/reset',
};

// ========== ИНИЦИАЛИЗАЦИЯ ==========
document.addEventListener('DOMContentLoaded', function () {
    console.log('=== API интеграция загружена ===');

    // Регистрация
    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        registerForm.addEventListener('submit', async function (e) {
            e.preventDefault();
            await handleRegistration(e);
        });
    }

    // Логин
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', async function (e) {
            e.preventDefault();
            await handleLogin(e);
        });
    }

    // Восстановление пароля (отправка email)
    const forgotPasswordBtn = document.getElementById('continueBtn');
    if (forgotPasswordBtn) {
        forgotPasswordBtn.addEventListener('click', async function () {
            await handleForgotPassword();
        });
    }

    // Подтверждение кода
    const codeConfirmBtn = document.getElementById('continueBtn1');
    if (codeConfirmBtn) {
        codeConfirmBtn.addEventListener('click', async function () {
            await handleCodeConfirmation();
        });
    }

    // Повторная отправка кода
    const resendCodeBtn = document.getElementById('resendCodeBtn');
    if (resendCodeBtn) {
        resendCodeBtn.addEventListener('click', async function () {
            if (!this.disabled) {
                await handleResendCode();
            }
        });
    }

    // Сброс пароля
    const resetPasswordForm = document.getElementById('resetPasswordForm');
    if (resetPasswordForm) {
        resetPasswordForm.addEventListener('submit', async function (e) {
            e.preventDefault();
            await handlePasswordReset();
        });
    }

    // Выход
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', async function (e) {
            e.preventDefault();
            await handleLogout();
        });
    }

    checkAuth();
});

// ========== ВСПОМОГАТЕЛЬНАЯ: получить ID из URL ==========

function getRegistrationId() {
    const urlParams = new URLSearchParams(window.location.search);
    const urlId = urlParams.get('id');

    console.log('[getRegistrationId] URL search:', window.location.search);
    console.log('[getRegistrationId] urlId из URL:', urlId);

    if (urlId && urlId.trim() !== '') {
        console.log('[getRegistrationId] Используем ID из URL:', urlId);
        return urlId.trim();
    }

    console.error('[getRegistrationId] ID не найден в URL!');
    return null;
}

function getResetId() {
    const urlParams = new URLSearchParams(window.location.search);
    const urlId = urlParams.get('id');

    console.log('[getResetId] URL search:', window.location.search);
    console.log('[getResetId] urlId из URL:', urlId);

    if (urlId && urlId.trim() !== '') {
        return urlId.trim();
    }

    return null;
}

// ========== ОСНОВНЫЕ ФУНКЦИИ ==========

// 1. РЕГИСТРАЦИЯ
async function handleRegistration(e) {
    try {
        console.log('=== РЕГИСТРАЦИЯ: НАЧАЛО ===');

        if (!validateRegistrationForm()) return;

        const form = e.target;
        const formData = new FormData(form);
        const userData = {
            email: formData.get('email'),
            password: formData.get('password'),
            name: formData.get('name') || '',
        };

        showLoading(true);

        const response = await fetch(API.REGISTER_SEND_CODE, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(userData),
        });

        const result = await response.json();
        console.log('Ответ сервера:', result);

        if (!response.ok) {
            showErrorMessage(result.message || `Ошибка сервера: ${response.status}`);
            return;
        }

        if (result.success) {
            if (!result.registrationId) {
                showErrorMessage('Ошибка сервера: не получен ID регистрации');
                return;
            }

            console.log('registrationId получен:', result.registrationId);
            showSuccessMessage(result.message || 'Код подтверждения отправлен на email');

            setTimeout(() => {
                window.location.href = `/codeEmail?type=registration&id=${result.registrationId}`;
            }, 1500);
        } else {
            showErrorMessage(result.message || 'Не удалось отправить код');
        }

    } catch (error) {
        console.error('Ошибка при регистрации:', error);
        showErrorMessage('Произошла ошибка: ' + error.message);
    } finally {
        showLoading(false);
    }
}

// 2. ЛОГИН
async function handleLogin(e) {
    try {
        const form = e.target;
        const formData = new FormData(form);
        const loginData = {
            email: formData.get('email'),
            password: formData.get('password'),
        };

        if (!loginData.email || !loginData.password) {
            showErrorMessage('Заполните все поля');
            return;
        }

        if (!validateEmail(loginData.email)) {
            showErrorMessage('Введите корректный email');
            return;
        }

        showLoading(true);

        const response = await fetch(API.LOGIN, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(loginData),
        });

        const result = await response.json();

        if (result.success) {
            showSuccessMessage(result.message || 'Вход выполнен успешно');
            setTimeout(() => {
                window.location.href = result.redirectUrl || '/';
            }, 1000);
        } else {
            showErrorMessage(result.message || 'Неверный email или пароль');
        }

    } catch (error) {
        console.error('Ошибка при входе:', error);
        showErrorMessage('Произошла ошибка при входе');
    } finally {
        showLoading(false);
    }
}

// 3. ВОССТАНОВЛЕНИЕ ПАРОЛЯ (ОТПРАВКА EMAIL)
async function handleForgotPassword() {
    try {
        const emailInput = document.getElementById('forgotPasswordEmail') ||
            document.querySelector('input[type="email"]');
        const email = emailInput ? emailInput.value.trim() : '';

        if (!email || !validateEmail(email)) {
            showErrorMessage('Введите корректный email');
            if (emailInput) emailInput.focus();
            return;
        }

        showLoading(true);

        const response = await fetch(`${API.PASSWORD_FORGOT}?email=${encodeURIComponent(email)}`, {
            method: 'POST',
        });

        const result = await response.json();

        if (!response.ok) {
            showErrorMessage(result.message || `Ошибка сервера: ${response.status}`);
            return;
        }

        if (result.success) {
            showSuccessMessage(result.message || 'Код отправлен на email');
            setTimeout(() => {
                window.location.href = `/codeEmail?type=reset&id=${result.resetId}`;
            }, 1500);
        } else {
            showErrorMessage(result.message || 'Пользователь не найден');
        }

    } catch (error) {
        console.error('Ошибка при восстановлении пароля:', error);
        showErrorMessage('Произошла ошибка');
    } finally {
        showLoading(false);
    }
}

// 4. ПОДТВЕРЖДЕНИЕ КОДА
async function handleCodeConfirmation() {
    try {
        console.log('=== ПОДТВЕРЖДЕНИЕ КОДА ===');

        const code = getCodeFromInputs();
        if (!code) return;

        showLoading(true);

        const urlParams = new URLSearchParams(window.location.search);
        const type = urlParams.get('type');

        console.log('Тип операции:', type);

        if (type === 'registration') {
            const registrationId = getRegistrationId();

            if (!registrationId) {
                showErrorMessage('ID регистрации не найден. Начните регистрацию заново.');
                setTimeout(() => { window.location.href = '/register'; }, 3000);
                return;
            }

            console.log('Верификация регистрации:', { registrationId, code });

            const response = await fetch(API.REGISTER_VERIFY, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ registrationId, code }),
            });

            const result = await response.json();
            console.log('Ответ сервера:', result);

            if (result.success) {
                showSuccessMessage(result.message || 'Регистрация завершена!');
                setTimeout(() => {
                    window.location.href = result.redirectUrl || '/profile';
                }, 1000);
            } else {
                showErrorMessage(result.message || 'Неверный код');
            }

        } else if (type === 'reset') {
            const resetId = getResetId();

            if (!resetId) {
                showErrorMessage('ID сброса не найден. Начните восстановление заново.');
                setTimeout(() => { window.location.href = '/forgotPassword'; }, 3000);
                return;
            }

            const response = await fetch(
                `${API.PASSWORD_VERIFY}?resetId=${encodeURIComponent(resetId)}&code=${encodeURIComponent(code)}`,
                { method: 'POST', credentials: 'include' }
            );

            const result = await response.json();
            console.log('Ответ сервера:', result);

            if (result.success) {
                showSuccessMessage(result.message || 'Код подтверждён!');
                setTimeout(() => {
                    window.location.href = `/recoveryPassword?type=reset&id=${resetId}`;
                }, 1000);
            } else {
                showErrorMessage(result.message || 'Неверный код');
            }

        } else {
            showErrorMessage('Неизвестный тип операции. Начните заново.');
        }

    } catch (error) {
        console.error('Ошибка при подтверждении кода:', error);
        showErrorMessage('Произошла ошибка: ' + error.message);
    } finally {
        showLoading(false);
    }
}

// 5. СБРОС ПАРОЛЯ (УСТАНОВКА НОВОГО ПАРОЛЯ)
async function handlePasswordReset() {
    try {
        const passwordInput = document.getElementById('recoveryPassword');
        const confirmPasswordInput = document.getElementById('recoveryConfirmPassword');

        if (!passwordInput || !confirmPasswordInput) {
            showErrorMessage('Элементы формы не найдены');
            return;
        }

        const password = passwordInput.value;
        const confirmPassword = confirmPasswordInput.value;

        if (password.length < 6) {
            showErrorMessage('Пароль должен содержать не менее 6 символов');
            return;
        }

        if (password !== confirmPassword) {
            showErrorMessage('Пароли не совпадают');
            return;
        }

        const resetId = getResetId();
        if (!resetId) {
            showErrorMessage('Сессия истекла. Начните восстановление заново.');
            setTimeout(() => { window.location.href = '/forgotPassword'; }, 2000);
            return;
        }

        showLoading(true);

        const response = await fetch(API.PASSWORD_RESET, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                resetId: resetId,
                newPassword: password,
                confirmPassword: confirmPassword
            }),
        });

        const result = await response.json();

        if (result.success) {
            showSuccessMessage(result.message || 'Пароль изменён!');
            setTimeout(() => {
                window.location.href = result.redirectUrl || '/login';
            }, 1000);
        } else {
            showErrorMessage(result.message || 'Не удалось изменить пароль');
        }

    } catch (error) {
        console.error('Ошибка при сбросе пароля:', error);
        showErrorMessage('Произошла ошибка: ' + error.message);
    } finally {
        showLoading(false);
    }
}

// 6. ПОВТОРНАЯ ОТПРАВКА КОДА
async function handleResendCode() {
    try {
        const urlParams = new URLSearchParams(window.location.search);
        const type = urlParams.get('type');

        showLoading(true);

        if (type === 'registration') {
            const registrationId = getRegistrationId();

            if (!registrationId) {
                showErrorMessage('ID регистрации не найден');
                return;
            }

            const response = await fetch(
                `${API.REGISTER_RESEND}?registrationId=${encodeURIComponent(registrationId)}`,
                { method: 'POST', credentials: 'include' }
            );

            const result = await response.json();

            if (result.success) {
                showSuccessMessage(result.message || 'Новый код отправлен');
                resetTimerOnPage();
            } else {
                showErrorMessage(result.message || 'Не удалось отправить код');
            }

        } else if (type === 'reset') {
            const resetId = getResetId();

            if (!resetId) {
                showErrorMessage('ID сброса не найден');
                return;
            }

            const response = await fetch(
                `${API.PASSWORD_FORGOT}?resetId=${encodeURIComponent(resetId)}`,
                { method: 'POST', credentials: 'include' }
            );

            const result = await response.json();

            if (result.success) {
                if (result.resetId) {
                    const newUrl = `/codeEmail?type=reset&id=${result.resetId}`;
                    window.history.replaceState(null, '', newUrl);
                }
                showSuccessMessage(result.message || 'Новый код отправлен');
                resetTimerOnPage();
            } else {
                showErrorMessage(result.message || 'Не удалось отправить код');
            }
        }

    } catch (error) {
        console.error('Ошибка при повторной отправке:', error);
        showErrorMessage('Произошла ошибка');
    } finally {
        showLoading(false);
    }
}

// 7. ВЫХОД
async function handleLogout() {
    try {
        showLoading(true);

        const response = await fetch(API.LOGOUT, {
            method: 'POST',
            credentials: 'include',
        });

        const result = await response.json();

        if (result.success) {
            showSuccessMessage(result.message || 'Выход выполнен');
            setTimeout(() => { window.location.href = '/login'; }, 1000);
        } else {
            showErrorMessage(result.message || 'Ошибка при выходе');
        }

    } catch (error) {
        console.error('Ошибка при выходе:', error);
        showErrorMessage('Произошла ошибка');
    } finally {
        showLoading(false);
    }
}

// ========== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ==========

function getCodeFromInputs() {
    const codeInputs = document.querySelectorAll('.code-input');
    let code = '';
    codeInputs.forEach(input => { code += input.value; });

    console.log('Собранный код:', code, '| длина:', code.length);

    if (code.length !== 6) {
        showErrorMessage('Введите все 6 цифр кода');
        return null;
    }
    return code;
}

function checkAuth() {
    const isAuthenticated = document.cookie.split(';').some(c => c.trim().startsWith('jwtToken='));

    document.querySelectorAll('.auth-only').forEach(el => {
        el.style.display = isAuthenticated ? 'block' : 'none';
    });
    document.querySelectorAll('.guest-only').forEach(el => {
        el.style.display = isAuthenticated ? 'none' : 'block';
    });
}

function validateRegistrationForm() {
    const nameInput = document.querySelector('input[name="name"]');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const confirmPasswordInput = document.getElementById('confirmPassword');
    const personalDataCheckbox = document.getElementById('personalData');

    if (!nameInput?.value.trim() || nameInput.value.trim().length < 2) {
        showErrorMessage('Введите имя (минимум 2 символа)');
        nameInput?.focus();
        return false;
    }

    if (!emailInput || !validateEmail(emailInput.value)) {
        showErrorMessage('Введите корректный email');
        emailInput?.focus();
        return false;
    }

    if (!passwordInput || passwordInput.value.length < 6) {
        showErrorMessage('Пароль должен содержать не менее 6 символов');
        passwordInput?.focus();
        return false;
    }

    if (passwordInput.value !== confirmPasswordInput?.value) {
        showErrorMessage('Пароли не совпадают');
        confirmPasswordInput?.focus();
        return false;
    }

    if (!personalDataCheckbox?.checked) {
        showErrorMessage('Необходимо согласие с политикой конфиденциальности');
        return false;
    }

    return true;
}

function validateEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function resetTimerOnPage() {
    if (typeof window.resetTimer === 'function') {
        window.resetTimer();
    }
}

function showSuccessMessage(message) { showMessage(message, 'success'); }
function showErrorMessage(message) { showMessage(message, 'error'); }

function showMessage(message, type) {
    document.querySelectorAll('.alert-message').forEach(el => el.remove());

    const alertDiv = document.createElement('div');
    alertDiv.className = `alert-message alert-${type}`;
    alertDiv.textContent = message;
    alertDiv.style.cssText = `
        position: fixed; top: 20px; right: 20px;
        padding: 15px 20px; border-radius: 5px;
        color: white; font-weight: 500; z-index: 9999;
        animation: slideIn 0.3s ease-out;
        background-color: ${type === 'success' ? '#4CAF50' : '#f44336'};
        box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        z-index: 10000;
    `;
    document.body.appendChild(alertDiv);

    if (!document.getElementById('alert-animations')) {
        const style = document.createElement('style');
        style.id = 'alert-animations';
        style.textContent = `
            @keyframes slideIn { from { transform: translateX(100%); opacity: 0; } to { transform: translateX(0); opacity: 1; } }
            @keyframes slideOut { from { transform: translateX(0); opacity: 1; } to { transform: translateX(100%); opacity: 0; } }
        `;
        document.head.appendChild(style);
    }

    setTimeout(() => {
        if (alertDiv.parentNode) {
            alertDiv.style.animation = 'slideOut 0.3s ease-out';
            setTimeout(() => alertDiv.parentNode?.removeChild(alertDiv), 300);
        }
    }, 5000);
}

function showLoading(show) {
    let loader = document.getElementById('global-loader');

    if (show) {
        if (loader) return;
        loader = document.createElement('div');
        loader.id = 'global-loader';
        loader.style.cssText = `
            position: fixed; top: 0; left: 0; width: 100%; height: 100%;
            background-color: rgba(255,255,255,0.8);
            display: flex; justify-content: center; align-items: center;
            z-index: 9999;
            backdrop-filter: blur(2px);
        `;
        const spinner = document.createElement('div');
        spinner.style.cssText = `
            width: 50px; height: 50px;
            border: 5px solid #f3f3f3; border-top: 5px solid #3498db;
            border-radius: 50%; animation: spin 1s linear infinite;
        `;
        loader.appendChild(spinner);

        if (!document.getElementById('loader-animation')) {
            const style = document.createElement('style');
            style.id = 'loader-animation';
            style.textContent = `@keyframes spin { to { transform: rotate(360deg); } }`;
            document.head.appendChild(style);
        }

        document.body.appendChild(loader);
    } else {
        loader?.parentNode?.removeChild(loader);
    }
}