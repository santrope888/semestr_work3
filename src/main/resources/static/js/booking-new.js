(function () {
    'use strict';

    const root = document.getElementById('legacy-booking-root');

    if (!root) {
        return;
    }

    const carPricePerDayRub = Number(root.dataset.carPrice);
    const carId = Number(root.dataset.carId);
    let currentCurrency = 'RUB';
    let bookedPeriods = [];

    const startDateInput = document.querySelector('input[name="startDate"]');
    const endDateInput = document.querySelector('input[name="endDate"]');
    const insuranceCheckboxes = document.querySelectorAll('input[name="insuranceIds"]');
    const pricePerDayElement = document.getElementById('price-per-day');
    const daysCountElement = document.getElementById('days-count');
    const insuranceTotalElement = document.getElementById('insurance-total');
    const totalPriceElement = document.getElementById('total-price');
    const currencyRateCache = { RUB: 1 };

    async function loadBookedPeriods() {
        try {
            const res = await fetch(`/api/bookings/car/${carId}/booked-periods`);
            if (res.ok) {
                bookedPeriods = await res.json();
            }
        } catch (e) {
            console.error('Failed to load booked periods', e);
        }

        const today = new Date().toISOString().split('T')[0];
        startDateInput?.setAttribute('min', today);
        endDateInput?.setAttribute('min', today);
    }

    function setWarningVisible(warningEl, visible) {
        warningEl?.classList.toggle('is-hidden', !visible);
    }

    function checkOverlap() {
        const startVal = startDateInput?.value;
        const endVal = endDateInput?.value;
        const warningEl = document.getElementById('date-warning');

        if (!startVal || !endVal || !warningEl) {
            setWarningVisible(warningEl, false);
            return false;
        }

        const start = new Date(startVal);
        const end = new Date(endVal);

        for (const period of bookedPeriods) {
            const bStart = new Date(period.start);
            const bEnd = new Date(period.end);

            if (start <= bEnd && end >= bStart) {
                warningEl.textContent = `Даты пересекаются с бронированием ${period.start} — ${period.end}`;
                setWarningVisible(warningEl, true);
                return true;
            }
        }

        setWarningVisible(warningEl, false);
        return false;
    }

    function calculateDays() {
        const startValue = startDateInput?.value;
        const endValue = endDateInput?.value;

        if (!startValue || !endValue) {
            return 0;
        }

        const start = new Date(startValue);
        const end = new Date(endValue);

        if (end < start) {
            return 0;
        }

        return Math.floor((end - start) / 86400000) + 1;
    }

    function calculateInsuranceTotalRub(days) {
        if (days <= 0) {
            return 0;
        }

        let insuranceTotal = 0;
        insuranceCheckboxes.forEach(cb => {
            if (cb.checked) {
                insuranceTotal += Number(cb.dataset.price) * days;
            }
        });
        return insuranceTotal;
    }

    function calculateSummaryRub() {
        const days = calculateDays();
        const insuranceTotalRub = calculateInsuranceTotalRub(days);
        const carTotalRub = days > 0 ? carPricePerDayRub * days : 0;

        return {
            days,
            insuranceTotalRub,
            totalRub: carTotalRub + insuranceTotalRub
        };
    }

    async function getCurrencyRate(currency) {
        if (currencyRateCache[currency] !== undefined) {
            return currencyRateCache[currency];
        }

        const probeAmountRub = 1000;
        const res = await fetch(`/api/payments/convert?amount=${probeAmountRub}&currency=${currency}`);

        if (!res.ok) {
            throw new Error('Currency rate fetch failed');
        }

        const data = await res.json();
        const rate = Number(data.converted) / probeAmountRub;
        currencyRateCache[currency] = rate;
        return rate;
    }

    function formatCurrency(amount, currency) {
        if (currency === 'RUB') {
            return `${Math.round(amount)} ₽`;
        }

        if (currency === 'USD') {
            return `${amount.toFixed(2)} $`;
        }

        return `${amount.toFixed(2)} €`;
    }

    async function renderTotal() {
        const { days, insuranceTotalRub, totalRub } = calculateSummaryRub();
        daysCountElement.textContent = days > 0 ? days : '—';

        if (currentCurrency === 'RUB') {
            pricePerDayElement.textContent = formatCurrency(carPricePerDayRub, 'RUB');
            insuranceTotalElement.textContent = days > 0 ? formatCurrency(insuranceTotalRub, 'RUB') : '—';
            totalPriceElement.textContent = days > 0 ? formatCurrency(totalRub, 'RUB') : '—';
            return;
        }

        try {
            const rate = await getCurrencyRate(currentCurrency);
            pricePerDayElement.textContent = formatCurrency(carPricePerDayRub * rate, currentCurrency);

            if (days <= 0) {
                insuranceTotalElement.textContent = '—';
                totalPriceElement.textContent = '—';
                return;
            }

            insuranceTotalElement.textContent = formatCurrency(insuranceTotalRub * rate, currentCurrency);
            totalPriceElement.textContent = formatCurrency(totalRub * rate, currentCurrency);
        } catch (e) {
            pricePerDayElement.textContent = 'Ошибка';
            insuranceTotalElement.textContent = 'Ошибка';
            totalPriceElement.textContent = 'Ошибка';
        }
    }

    async function setCurrency(cur) {
        currentCurrency = cur;
        document.querySelectorAll('.currency-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.currency === cur);
        });
        await renderTotal();
    }

    function attachListeners() {
        document.getElementById('legacyBookingForm')?.addEventListener('submit', function (event) {
            if (!checkOverlap()) {
                return;
            }

            event.preventDefault();
            const warningEl = document.getElementById('date-warning');

            if (warningEl) {
                warningEl.textContent = 'Выбранные даты заняты. Выберите другой период.';
                setWarningVisible(warningEl, true);
                warningEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        });

        startDateInput?.addEventListener('change', () => {
            if (startDateInput.value) {
                endDateInput?.setAttribute('min', startDateInput.value);
            }
            checkOverlap();
            renderTotal();
        });
        endDateInput?.addEventListener('change', () => {
            checkOverlap();
            renderTotal();
        });

        insuranceCheckboxes.forEach(cb => {
            const label = cb.closest('.insurance-item');
            label?.classList.toggle('checked', cb.checked);

            cb.addEventListener('change', () => {
                label?.classList.toggle('checked', cb.checked);
                renderTotal();
            });
        });

        document.querySelectorAll('.currency-btn[data-currency]').forEach(btn => {
            btn.addEventListener('click', () => setCurrency(btn.dataset.currency));
        });
    }

    document.addEventListener('DOMContentLoaded', async function () {
        attachListeners();
        await loadBookedPeriods();
        renderTotal();
    });
})();
