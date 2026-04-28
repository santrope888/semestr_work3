(function () {
    'use strict';

    const root = document.getElementById('payment-root');
    const amount = root ? Number(root.dataset.amount) || 0 : 0;

    async function setCurrency(cur, btn) {
        document.querySelectorAll('.currency-btn').forEach(b => b.classList.remove('active'));
        if (btn) btn.classList.add('active');

        const target = document.getElementById('total-converted');
        if (!target) return;

        if (cur === 'RUB') {
            target.textContent = amount + ' ₽';
            return;
        }

        try {
            const res = await fetch(`/api/payments/convert?amount=${amount}&currency=${cur}`);
            if (!res.ok) {
                console.error('Currency convert failed', res.status);
                return;
            }
            const data = await res.json();
            const symbol = cur === 'USD' ? '$' : '€';
            target.textContent = data.converted.toFixed(2) + ' ' + symbol;
        } catch (e) {
            console.error('Currency convert error', e);
        }
    }

    window.setCurrency = setCurrency;
})();