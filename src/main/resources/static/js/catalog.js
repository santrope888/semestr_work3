(function () {
    'use strict';

    function getCsrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        return token && header ? { [header]: token } : {};
    }

    function removeFilterFromUrl(filterName, filterValue) {
        const url = new URL(window.location.href);
        const params = url.searchParams;

        if (filterValue !== null && filterValue !== undefined && filterValue !== '') {
            const remainingValues = params
                .getAll(filterName)
                .filter(value => value !== filterValue);

            params.delete(filterName);

            remainingValues.forEach(value => {
                params.append(filterName, value);
            });
        } else {
            params.delete(filterName);
        }

        params.delete('page');

        const queryString = params.toString();
        window.location.href = url.pathname + (queryString ? '?' + queryString : '');
    }

    document.addEventListener('click', function (event) {
        const chip = event.target.closest('.removable-chip');

        if (!chip) {
            return;
        }

        const filterName = chip.dataset.filterName;
        const filterValue = chip.dataset.filterValue;

        if (!filterName) {
            return;
        }

        removeFilterFromUrl(filterName, filterValue);
    });

    async function onCatalogFavClick(event, btn) {
        event.preventDefault();
        event.stopPropagation();

        if (!btn || btn.disabled) {
            return;
        }

        const carId = btn.dataset.carId;

        if (!carId) {
            return;
        }

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
                headers: {
                    ...getCsrfHeaders()
                }
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

    window.onCatalogFavClick = onCatalogFavClick;
})();