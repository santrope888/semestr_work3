(function () {
    'use strict';

    const root = document.getElementById('wizard-root');
    const CAR_PRICE_PER_DAY = root ? Number(root.dataset.carPrice) || 0 : 0;
    const CAR_ID = root ? Number(root.dataset.carId) || null : null;

    let currentStep = 1;
    let currentCurrency = 'RUB';
    const rateCache = { RUB: 1 };
    let bookedPeriods = [];
    let startPicker, endPicker;

    document.addEventListener('DOMContentLoaded', async () => {
        await loadBookedPeriods();
        initDatePickers();
        attachListeners();
        recalcAndRender();
    });

    function initDatePickers() {
        const disabled = bookedPeriods.map(p => ({ from: p.start, to: p.end }));

        const today = new Date();
        today.setHours(0, 0, 0, 0);

        const common = {
            dateFormat: 'Y-m-d',
            altInput: true,
            altFormat: 'd.m.Y',
            minDate: 'today',
            disable: disabled,
            locale: 'ru',
            allowInput: false
        };

        startPicker = flatpickr('#startDate', {
            ...common,
            onChange: (sel) => {
                if (sel[0]) endPicker.set('minDate', sel[0]);
                clearFieldError('rowStartDate');
                recalcAndRender();
            }
        });

        endPicker = flatpickr('#endDate', {
            ...common,
            onChange: () => {
                clearFieldError('rowEndDate');
                recalcAndRender();
            }
        });
    }

    function attachListeners() {
        document.getElementById('pickupLocation')?.addEventListener('change', () => clearFieldError('rowPickup'));
        document.getElementById('returnLocation')?.addEventListener('change', () => clearFieldError('rowReturn'));

        document.querySelectorAll('.option-card input[type="checkbox"]').forEach(cb => {
            cb.addEventListener('change', () => {
                cb.closest('.option-card').classList.toggle('selected', cb.checked);
                recalcAndRender();
            });
        });

        const cardNumber = document.getElementById('cardNumber');
        cardNumber?.addEventListener('input', (e) => {
            const digits = e.target.value.replace(/\D/g, '').slice(0, 16);
            e.target.value = digits.match(/.{1,4}/g)?.join(' ') || '';
            clearFieldError('rowCardNumber');
        });

        document.getElementById('cardHolder')?.addEventListener('input', () => clearFieldError('rowCardHolder'));

        document.getElementById('cardExpiry')?.addEventListener('input', (e) => {
            let v = e.target.value.replace(/\D/g, '').slice(0, 4);
            if (v.length >= 3) v = v.slice(0, 2) + '/' + v.slice(2);
            e.target.value = v;
            clearFieldError('rowCardExpiry');
        });

        document.getElementById('cardCvv')?.addEventListener('input', (e) => {
            e.target.value = e.target.value.replace(/\D/g, '').slice(0, 3);
            clearFieldError('rowCardCvv');
        });
    }

    async function loadBookedPeriods() {
        if (!CAR_ID) return;
        try {
            const res = await fetch(`/api/bookings/car/${CAR_ID}/booked-periods`);
            if (res.ok) bookedPeriods = await res.json();
        } catch (e) {
            console.warn('Не удалось загрузить занятые даты');
        }
    }

    function goToStep(target) {
        if (target < currentStep) { switchStep(target); return; }
        if (target === currentStep) return;
        for (let s = currentStep; s < target; s++) {
            if (!validateStep(s)) return;
        }
        switchStep(target);
    }

    function nextFromStep(from) {
        if (!validateStep(from)) return;
        switchStep(from + 1);
    }

    function switchStep(n) {
        currentStep = n;
        document.querySelectorAll('.step').forEach(el => {
            el.classList.toggle('active', Number(el.dataset.step) === n);
        });
        document.querySelectorAll('.stepper-item').forEach(el => {
            const stepNum = Number(el.dataset.step);
            el.classList.toggle('active', stepNum === n);
            el.classList.toggle('completed', stepNum < n);
        });
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    function validateStep(step) {
        clearStepError(step);
        if (step === 1) return validateStep1();
        if (step === 2) return true;
        if (step === 3) return validateStep3();
        return true;
    }

    function validateStep1() {
        let ok = true;
        const sd = document.getElementById('startDate').value;
        const ed = document.getElementById('endDate').value;
        const pickup = document.getElementById('pickupLocation').value;
        const returnLoc = document.getElementById('returnLocation').value;

        if (!sd) { markFieldError('rowStartDate'); ok = false; }
        if (!ed) { markFieldError('rowEndDate'); ok = false; }
        if (!pickup) { markFieldError('rowPickup'); ok = false; }
        if (!returnLoc) { markFieldError('rowReturn'); ok = false; }

        if (!ok) return showStepError(1, 'Заполните все поля');

        if (new Date(ed) < new Date(sd)) {
            markFieldError('rowEndDate');
            return showStepError(1, 'Дата окончания не может быть раньше даты начала');
        }
        return true;
    }

    function validateStep3() {
        const method = document.getElementById('paymentMethod').value;
        if (method === 'CASH') return true;

        let ok = true;
        const num = document.getElementById('cardNumber').value.replace(/\s/g, '');
        const holder = document.getElementById('cardHolder').value.trim();
        const expiry = document.getElementById('cardExpiry').value;
        const cvv = document.getElementById('cardCvv').value;

        if (num.length !== 16 || !/^\d{16}$/.test(num)) { markFieldError('rowCardNumber'); ok = false; }
        if (!holder) { markFieldError('rowCardHolder'); ok = false; }
        if (!/^\d{2}\/\d{2}$/.test(expiry)) {
            markFieldError('rowCardExpiry'); ok = false;
        } else {
            const [mm, yy] = expiry.split('/').map(Number);
            const now = new Date();
            const currentYY = now.getFullYear() % 100;
            const currentMM = now.getMonth() + 1;
            if (mm < 1 || mm > 12 || yy < currentYY || (yy === currentYY && mm < currentMM)) {
                markFieldError('rowCardExpiry'); ok = false;
            }
        }
        if (cvv.length !== 3 || !/^\d{3}$/.test(cvv)) { markFieldError('rowCardCvv'); ok = false; }

        if (!ok) {
            const firstInvalid = document.querySelector('#payCardForm .field-row.invalid input');
            if (firstInvalid) firstInvalid.focus();
            return showStepError(3, 'Заполните данные карты корректно');
        }
        return true;
    }

    function validateBeforeSubmit(event) {
        if (!validateStep1()) {
            event.preventDefault();
            switchStep(1);
            return false;
        }
        if (!validateStep3()) {
            event.preventDefault();
            switchStep(3);
            return false;
        }
        return true;
    }

    function markFieldError(rowId) { document.getElementById(rowId)?.classList.add('invalid'); }
    function clearFieldError(rowId) { document.getElementById(rowId)?.classList.remove('invalid'); }

    function showStepError(step, msg) {
        const el = document.getElementById(`step${step}Error`);
        if (el) {
            el.textContent = msg;
            el.classList.add('active');
        }
        return false;
    }

    function clearStepError(step) {
        const el = document.getElementById(`step${step}Error`);
        if (el) {
            el.classList.remove('active');
            el.textContent = '';
        }
    }

    function selectPayment(method) {
        document.getElementById('paymentMethod').value = method;
        document.querySelectorAll('.pay-method-tab').forEach(t => {
            t.classList.toggle('active', t.dataset.method === method);
        });
        document.getElementById('payCardForm').style.display = method === 'CARD' ? 'block' : 'none';
        document.getElementById('payCashInfo').classList.toggle('active', method === 'CASH');

        if (method === 'CASH') {
            ['rowCardNumber', 'rowCardHolder', 'rowCardExpiry', 'rowCardCvv'].forEach(clearFieldError);
            clearStepError(3);
        }
    }

    function calcDays() {
        const sd = document.getElementById('startDate').value;
        const ed = document.getElementById('endDate').value;
        if (!sd || !ed) return 0;
        const start = new Date(sd), end = new Date(ed);
        if (end < start) return 0;
        return Math.floor((end - start) / 86400000) + 1;
    }

    function calcInsuranceTotal(days) {
        if (days <= 0) return 0;
        let sum = 0;
        document.querySelectorAll('.js-insurance:checked').forEach(cb => {
            sum += Number(cb.dataset.price) * days;
        });
        return sum;
    }

    function calcExtrasTotal(days) {
        if (days <= 0) return 0;
        let sum = 0;
        document.querySelectorAll('.js-extra:checked').forEach(cb => {
            sum += Number(cb.dataset.price) * days;
        });
        return sum;
    }

    async function recalcAndRender() {
        const days = calcDays();
        const carTotal = days > 0 ? CAR_PRICE_PER_DAY * days : 0;
        const insurance = calcInsuranceTotal(days);
        const extras = calcExtrasTotal(days);
        const totalRub = carTotal + insurance + extras;

        let rate = 1;
        try { rate = await getRate(currentCurrency); } catch (e) { rate = 1; }

        document.getElementById('sumDays').textContent = days > 0 ? `${days} дн.` : '—';
        document.getElementById('sumTariff').textContent =
            formatCurrency(CAR_PRICE_PER_DAY * rate, currentCurrency) + ' /день';

        const insRow = document.getElementById('sumInsuranceRow');
        if (insurance > 0) {
            insRow.style.display = 'flex';
            document.getElementById('sumInsurance').textContent = formatCurrency(insurance * rate, currentCurrency);
        } else { insRow.style.display = 'none'; }

        const exRow = document.getElementById('sumExtrasRow');
        if (extras > 0) {
            exRow.style.display = 'flex';
            document.getElementById('sumExtras').textContent = formatCurrency(extras * rate, currentCurrency);
        } else { exRow.style.display = 'none'; }

        document.getElementById('sumTotal').textContent =
            days > 0 ? formatCurrency(totalRub * rate, currentCurrency) : '—';

        document.getElementById('submitTotal').textContent =
            days > 0 ? formatCurrency(totalRub * rate, currentCurrency) : '';
    }

    async function getRate(currency) {
        if (rateCache[currency] !== undefined) return rateCache[currency];
        const probe = 1000;
        const res = await fetch(`/api/payments/convert?amount=${probe}&currency=${currency}`);
        if (!res.ok) throw new Error('rate fetch failed');
        const data = await res.json();
        const rate = Number(data.converted) / probe;
        rateCache[currency] = rate;
        return rate;
    }

    function formatCurrency(amount, currency) {
        const rounded = Math.round(amount);
        const withSpaces = rounded.toLocaleString('ru-RU').replace(/,/g, ' ');
        if (currency === 'RUB') return `${withSpaces} ₽`;
        if (currency === 'USD') return `$${amount.toFixed(2)}`;
        return `€${amount.toFixed(2)}`;
    }

    async function setCurrency(cur) {
        currentCurrency = cur;
        document.querySelectorAll('.currency-btn').forEach(b => {
            b.classList.toggle('active', b.dataset.currency === cur);
        });
        await recalcAndRender();
    }

    window.goToStep = goToStep;
    window.nextFromStep = nextFromStep;
    window.selectPayment = selectPayment;
    window.setCurrency = setCurrency;
    window.validateBeforeSubmit = validateBeforeSubmit;
})();