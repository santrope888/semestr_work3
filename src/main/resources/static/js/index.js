(function () {
    'use strict';

    function getCsrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        return token && header ? { [header]: token } : {};
    }

    function setFavoriteState(btn, isOn) {
        const icon = btn.querySelector('i');

        btn.classList.toggle('on', isOn);

        if (icon) {
            icon.classList.toggle('bx-heart', !isOn);
            icon.classList.toggle('bxs-heart', isOn);
        }
    }

    async function onHomeFavClick(event, btn) {
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
        setFavoriteState(btn, !wasOn);
        btn.disabled = true;

        try {
            const res = await fetch(`/api/favorites?carId=${encodeURIComponent(carId)}`, {
                method: wasOn ? 'DELETE' : 'POST',
                credentials: 'same-origin',
                headers: getCsrfHeaders()
            });

            if (!res.ok) {
                setFavoriteState(btn, wasOn);

                if (res.status === 401 || res.status === 403) {
                    window.location.href = '/login';
                    return;
                }

                console.error('Favorite toggle failed', res.status);
            }
        } catch (e) {
            setFavoriteState(btn, wasOn);
            console.error('Favorite toggle network error', e);
        } finally {
            btn.disabled = false;
        }
    }

    window.onHomeFavClick = onHomeFavClick;

    if (typeof ScrollReveal !== 'function') return;

    const sr = ScrollReveal({
        origin: 'top',
        distance: '60px',
        duration: 2500,
        delay: 400,
        reset: false
    });

    sr.reveal('.nav');
    sr.reveal('.home-img', { delay: 600, origin: 'right' });
    sr.reveal('.heading', { delay: 500 });
    sr.reveal('.trend-box, .rental-box', { interval: 100, origin: 'bottom' });
})();
