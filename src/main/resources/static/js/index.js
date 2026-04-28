(function () {
    'use strict';

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