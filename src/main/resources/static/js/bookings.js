(function () {
    'use strict';

    let pendingCancelId = null;

    function getCsrfHeaders() {
        const token  = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        return token && header ? { [header]: token } : {};
    }

    function openReviewModal(carId, carName) {
        document.getElementById('reviewCarName').textContent = carName;
        document.getElementById('reviewForm').action = '/cars/' + carId + '/reviews';
        document.getElementById('reviewModal').classList.add('open');
        setRating(5);
    }

    function closeReviewModal() {
        document.getElementById('reviewModal').classList.remove('open');
    }

    function openCancelModal(bookingId, info) {
        pendingCancelId = bookingId;
        document.getElementById('cancelInfo').textContent = info;
        document.getElementById('cancelConfirmBtn').disabled = false;
        document.getElementById('cancelModal').classList.add('open');
    }

    function closeCancelModal() {
        pendingCancelId = null;
        document.getElementById('cancelModal').classList.remove('open');
    }

    function setRating(value) {
        document.getElementById('ratingInput').value = value;
        const stars = document.querySelectorAll('#starSelect i');
        stars.forEach((star, index) => {
            if (index < value) {
                star.className = 'bx bxs-star active';
            } else {
                star.className = 'bx bx-star';
            }
        });
    }

    async function confirmCancel() {
        if (!pendingCancelId) return;
        const id = pendingCancelId;
        const card = document.querySelector(`.booking-card[data-booking-card-id="${id}"]`);
        const confirmBtn = document.getElementById('cancelConfirmBtn');

        confirmBtn.disabled = true;
        if (card) card.classList.add('cancelling');

        try {
            const res = await fetch(`/bookings/${id}/cancel`, {
                method: 'POST',
                credentials: 'same-origin',
                headers: { ...getCsrfHeaders() }
            });

            if (!res.ok && res.status !== 302) {
                console.error('Cancel failed:', res.status);
                if (card) card.classList.remove('cancelling');
                confirmBtn.disabled = false;
                if (res.status === 401 || res.status === 403) {
                    window.location.href = '/login';
                }
                return;
            }

            closeCancelModal();
            if (card) {
                card.classList.add('removing');
                setTimeout(() => {
                    card.remove();
                    const remaining = document.querySelectorAll('.booking-card').length;
                    if (remaining === 0) window.location.reload();
                }, 400);
            }

            if (typeof window.updateBadge === 'function') window.updateBadge();
            if (typeof window.loadNotifications === 'function') window.loadNotifications();
        } catch (e) {
            console.error('Cancel network error', e);
            if (card) card.classList.remove('cancelling');
            confirmBtn.disabled = false;
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        const cancelModal = document.getElementById('cancelModal');
        if (cancelModal) {
            cancelModal.addEventListener('click', function (e) {
                if (e.target === this) closeCancelModal();
            });
        }

        const reviewModal = document.getElementById('reviewModal');
        if (reviewModal) {
            reviewModal.addEventListener('click', function (e) {
                if (e.target === this) closeReviewModal();
            });
        }

        document.addEventListener('click', function (e) {
            const reviewClose = e.target.closest('[data-review-modal-close]');
            if (reviewClose) {
                closeReviewModal();
                return;
            }

            const cancelClose = e.target.closest('[data-cancel-modal-close]');
            if (cancelClose) {
                closeCancelModal();
                return;
            }

            const cancelConfirm = e.target.closest('[data-cancel-confirm]');
            if (cancelConfirm) {
                confirmCancel();
                return;
            }

            const ratingStar = e.target.closest('#starSelect i[data-value]');
            if (ratingStar) {
                setRating(Number(ratingStar.dataset.value));
                return;
            }

            const cancelBtn = e.target.closest('.cancel-btn');
            if (cancelBtn) {
                openCancelModal(cancelBtn.dataset.bookingId, cancelBtn.dataset.bookingInfo);
                return;
            }

            const reviewBtn = e.target.closest('.review-btn');
            if (reviewBtn) {
                openReviewModal(reviewBtn.dataset.carId, reviewBtn.dataset.carName);
                return;
            }

            const card = e.target.closest('.booking-card');
            if (card && card.dataset.paymentUrl) {
                window.location.href = card.dataset.paymentUrl;
            }
        });

        document.addEventListener('error', function (event) {
            const img = event.target;

            if (!(img instanceof HTMLImageElement) || !img.dataset.fallbackSrc) {
                return;
            }

            img.src = img.dataset.fallbackSrc;
        }, true);
    });

    window.openReviewModal = openReviewModal;
    window.closeReviewModal = closeReviewModal;
    window.openCancelModal = openCancelModal;
    window.closeCancelModal = closeCancelModal;
    window.confirmCancel = confirmCancel;
    window.setRating = setRating;
})();
