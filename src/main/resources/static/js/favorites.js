(function () {
    'use strict';

    function getCsrfHeaders() {
        const token  = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        return token && header ? { [header]: token } : {};
    }

    async function onFavoritesPageClick(event, btn) {
        event.preventDefault();
        event.stopPropagation();

        if (!btn || btn.disabled) return;
        const carId = btn.dataset.carId;
        if (!carId) return;

        const card = document.querySelector(`.fav-card[data-card-car-id="${carId}"]`);
        if (card) card.classList.add('removing');
        btn.disabled = true;

        try {
            const res = await fetch(`/api/favorites?carId=${encodeURIComponent(carId)}`, {
                method: 'DELETE',
                credentials: 'same-origin',
                headers: { ...getCsrfHeaders() }
            });

            if (!res.ok) {
                if (card) card.classList.remove('removing');
                btn.disabled = false;
                if (res.status === 401 || res.status === 403) {
                    window.location.href = '/login';
                    return;
                }
                console.error('Favorite remove failed', res.status);
                return;
            }

            if (card) {
                setTimeout(() => {
                    card.remove();
                    const grid = document.querySelector('.fav-grid');
                    if (grid && grid.children.length === 0) {
                        window.location.reload();
                    }
                }, 300);
            }
        } catch (e) {
            if (card) card.classList.remove('removing');
            btn.disabled = false;
            console.error('Favorite remove network error', e);
        }
    }

    window.onFavoritesPageClick = onFavoritesPageClick;
})();