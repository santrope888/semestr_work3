(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('.cd-tab').forEach(btn => {
            btn.addEventListener('click', () => {
                const t = btn.dataset.tab;
                document.querySelectorAll('.cd-tab').forEach(b => b.classList.toggle('active', b === btn));
                document.querySelectorAll('.cd-pane').forEach(p =>
                    p.classList.toggle('active', p.id === 'cd-pane-' + t));
            });
        });
    });

    function getCsrfHeaders() {
        const token  = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        return token && header ? { [header]: token } : {};
    }

    async function toggleFavorite(btn) {
        if (!btn || btn.disabled) return;
        const carId = btn.dataset.carId;
        if (!carId) return;

        const wasOn = btn.classList.contains('on');
        const icon = btn.querySelector('i');

        btn.disabled = true;
        btn.classList.toggle('on');
        if (icon) {
            icon.classList.toggle('bx-heart');
            icon.classList.toggle('bxs-heart');
        }

        const url = `/api/favorites?carId=${encodeURIComponent(carId)}`;
        const method = wasOn ? 'DELETE' : 'POST';

        try {
            const res = await fetch(url, {
                method,
                credentials: 'same-origin',
                headers: { ...getCsrfHeaders() }
            });

            if (!res.ok) {
                btn.classList.toggle('on');
                if (icon) {
                    icon.classList.toggle('bx-heart');
                    icon.classList.toggle('bxs-heart');
                }
                if (res.status === 401 || res.status === 403) {
                    window.location.href = '/login';
                    return;
                }
                console.error('Favorite toggle failed', res.status);
            }
        } catch (e) {
            btn.classList.toggle('on');
            if (icon) {
                icon.classList.toggle('bx-heart');
                icon.classList.toggle('bxs-heart');
            }
            console.error('Favorite toggle network error', e);
        } finally {
            btn.disabled = false;
        }
    }

    document.addEventListener('click', function (event) {
        const btn = event.target.closest('[data-car-detail-fav-btn]');

        if (btn) {
            toggleFavorite(btn);
        }
    });
})();
