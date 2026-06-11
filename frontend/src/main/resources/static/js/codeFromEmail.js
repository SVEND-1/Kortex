// ========== UI ДЛЯ СТРАНИЦЫ ПОДТВЕРЖДЕНИЯ КОДА ==========
// Только визуальные эффекты, вся бизнес-логика в api-integration.js

let timerIntervalGlobal = null;
let timeLeftGlobal = 60;

function startTimer() {
    const timerElement = document.getElementById('timer');
    const resendBtn = document.getElementById('resendCodeBtn');

    if (!timerElement || !resendBtn) {
        console.log('Таймер не найден на странице');
        return;
    }

    if (timerIntervalGlobal) clearInterval(timerIntervalGlobal);

    timeLeftGlobal = 60;
    resendBtn.disabled = true;
    resendBtn.style.opacity = '0.6';
    resendBtn.style.cursor = 'not-allowed';

    const updateTimer = () => {
        if (timeLeftGlobal <= 0) {
            clearInterval(timerIntervalGlobal);
            resendBtn.disabled = false;
            resendBtn.style.opacity = '1';
            resendBtn.style.cursor = 'pointer';
            timerElement.textContent = 'Код истек';
            return;
        }

        const minutes = Math.floor(timeLeftGlobal / 60);
        const seconds = timeLeftGlobal % 60;
        timerElement.textContent = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
        timeLeftGlobal--;
    };

    updateTimer();
    timerIntervalGlobal = setInterval(updateTimer, 1000);
}

function resetTimer() {
    console.log('Сброс таймера');
    if (timerIntervalGlobal) clearInterval(timerIntervalGlobal);
    startTimer();
}

function setupCodeInputs() {
    const codeInputs = document.querySelectorAll('.code-input');

    if (codeInputs.length === 0) {
        console.log('Поля ввода кода не найдены');
        return;
    }

    console.log('Найдено полей ввода кода:', codeInputs.length);

    codeInputs.forEach((input, index) => {
        input.addEventListener('input', function(e) {
            this.value = this.value.replace(/\D/g, '');

            if (this.value.length === 1 && index < codeInputs.length - 1) {
                codeInputs[index + 1].focus();
                codeInputs[index + 1].select();
            }

            this.classList.toggle('filled', this.value !== '');
        });

        input.addEventListener('keydown', function(e) {
            if (e.key === 'Backspace' && !this.value && index > 0) {
                codeInputs[index - 1].focus();
                codeInputs[index - 1].select();
            }

            if (e.key === 'ArrowLeft' && index > 0) {
                e.preventDefault();
                codeInputs[index - 1].focus();
                codeInputs[index - 1].select();
            }

            if (e.key === 'ArrowRight' && index < codeInputs.length - 1) {
                e.preventDefault();
                codeInputs[index + 1].focus();
                codeInputs[index + 1].select();
            }
        });

        input.addEventListener('paste', function(e) {
            e.preventDefault();
            const pastedData = e.clipboardData.getData('text');
            const digits = pastedData.replace(/\D/g, '').split('');

            for (let i = 0; i < Math.min(digits.length, codeInputs.length); i++) {
                codeInputs[i].value = digits[i];
                codeInputs[i].classList.add('filled');
            }

            const lastIndex = Math.min(digits.length, codeInputs.length) - 1;
            if (lastIndex >= 0) {
                codeInputs[lastIndex].focus();
                codeInputs[lastIndex].select();
            }
        });

        input.addEventListener('click', function() {
            this.select();
        });
    });
}

// Экспортируем функции в глобальный объект
window.resetTimer = resetTimer;
window.startTimer = startTimer;

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    console.log('=== codeFromEmail.js загружен ===');
    console.log('Страница codeEmail загружена');
    console.log('URL:', window.location.href);

    // Получаем тип и ID из URL для информативности
    const urlParams = new URLSearchParams(window.location.search);
    const type = urlParams.get('type');
    const id = urlParams.get('id');

    console.log('Тип операции:', type);
    console.log('ID из URL:', id);

    if (!id) {
        console.warn('ВНИМАНИЕ: ID не найден в URL!');
    }

    setupCodeInputs();
    startTimer();
});