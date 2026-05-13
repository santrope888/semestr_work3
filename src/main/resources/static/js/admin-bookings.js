document.addEventListener('DOMContentLoaded', function () {
    let pendingCancelForm = null;

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || '';
    const csrfParameter = document.querySelector('meta[name="_csrf_parameter"]')?.content || '_csrf';

    createCancelModal();
    createToast();

    document.addEventListener('submit', function (event) {
        const form = event.target.closest('.admin-booking-action-form');

        if (!form) {
            return;
        }

        event.preventDefault();
        submitBookingAction(form);
    });

    document.addEventListener('click', function (event) {
        const cancelButton = event.target.closest('.admin-cancel-open');

        if (!cancelButton) {
            return;
        }

        pendingCancelForm = cancelButton.closest('form');

        const bookingInfo = document.querySelector('[data-booking-info-modal]');
        bookingInfo.textContent = cancelButton.dataset.bookingInfo || 'Выбранное бронирование';

        openCancelModal();
    });

    function submitBookingAction(form) {
        const submitButton = form.querySelector('button');

        if (submitButton) {
            submitButton.disabled = true;
            submitButton.classList.add('admin-action-loading');
        }

        const headers = {
            'X-Requested-With': 'XMLHttpRequest',
            'Accept': 'application/json'
        };

        if (csrfHeader && csrfToken) {
            headers[csrfHeader] = csrfToken;
        }

        fetch(form.action, {
            method: 'POST',
            headers: headers,
            credentials: 'same-origin'
        })
            .then(async function (response) {
                const contentType = response.headers.get('content-type') || '';

                if (!contentType.includes('application/json')) {
                    throw new Error('Сервер вернул не JSON-ответ');
                }

                const data = await response.json();

                if (!response.ok || !data.success) {
                    throw new Error(data.message || 'Не удалось выполнить действие');
                }

                updateBookingRow(data);
                showMessage(data.message, 'success');
            })
            .catch(function (error) {
                showMessage(error.message || 'Произошла ошибка', 'error');
            })
            .finally(function () {
                if (submitButton) {
                    submitButton.disabled = false;
                    submitButton.classList.remove('admin-action-loading');
                }
            });
    }

    function updateBookingRow(booking) {
        const row = document.getElementById(`booking-row-${booking.id}`);

        if (!row) {
            return;
        }

        const statusBadge = row.querySelector('[data-booking-status]');
        if (statusBadge) {
            statusBadge.textContent = booking.status;
            statusBadge.className = `badge badge-${booking.status.toLowerCase()}`;
        }

        const paymentCell = row.querySelector('[data-booking-payment-cell]');
        if (paymentCell) {
            paymentCell.innerHTML = renderPayment(booking.paymentStatus);
        }

        const actionsCell = row.querySelector('[data-booking-actions]');
        if (actionsCell) {
            actionsCell.innerHTML = renderActions(booking);
        }
    }

    function renderPayment(paymentStatus) {
        if (!paymentStatus) {
            return '<span>—</span>';
        }

        return `
            <span class="badge badge-${escapeAttr(paymentStatus.toLowerCase())}">
                ${escapeHtml(paymentStatus)}
            </span>
        `;
    }

    function renderActions(booking) {
        const id = booking.id;
        const bookingInfo = `${booking.carBrand} ${booking.carModel} · ${booking.startDate} — ${booking.endDate}`;

        let html = '';

        if (booking.canConfirm) {
            html += `
                <form action="/admin/bookings/${id}/confirm"
                      method="post"
                      class="admin-booking-action-form"
                      style="display:inline;">
                    ${csrfHiddenInput()}
                    <button type="submit" class="btn btn-sm btn-primary" title="Подтвердить">
                        <i class='bx bx-check'></i>
                    </button>
                </form>
            `;
        }

        if (booking.canComplete) {
            html += `
                <form action="/admin/bookings/${id}/complete"
                      method="post"
                      class="admin-booking-action-form"
                      style="display:inline;">
                    ${csrfHiddenInput()}
                    <button type="submit" class="btn btn-sm btn-primary" title="Завершить">
                        <i class='bx bx-check-double'></i>
                    </button>
                </form>
            `;
        } else if (booking.status === 'CONFIRMED') {
            html += `
                <button type="button"
                        class="btn btn-sm btn-secondary"
                        disabled
                        title="Завершить можно только в день окончания аренды или позже"
                        style="opacity:0.55;cursor:not-allowed;">
                    <i class='bx bx-check-double'></i>
                </button>
            `;
        }

        if (booking.canCancel) {
            html += `
                <form action="/admin/bookings/${id}/cancel"
                      method="post"
                      class="admin-booking-cancel-form"
                      style="display:inline;">
                    ${csrfHiddenInput()}
                    <button type="button"
                            class="btn btn-sm btn-danger admin-cancel-open"
                            title="Отменить"
                            data-booking-info="${escapeAttr(bookingInfo)}">
                        <i class='bx bx-x'></i>
                    </button>
                </form>
            `;
        }

        return html;
    }

    function csrfHiddenInput() {
        if (!csrfToken) {
            return '';
        }

        return `<input type="hidden" name="${escapeAttr(csrfParameter)}" value="${escapeAttr(csrfToken)}">`;
    }

    function createCancelModal() {
        const modal = document.createElement('div');
        modal.className = 'admin-confirm-modal-overlay';
        modal.id = 'adminCancelModal';

        modal.innerHTML = `
            <div class="admin-confirm-modal">
                <button type="button" class="admin-confirm-modal-close" data-modal-close>&times;</button>

                <div class="admin-confirm-modal-icon">
                    <i class='bx bx-error-circle'></i>
                </div>

                <h3 class="admin-confirm-modal-title">Отменить бронирование?</h3>

                <p class="admin-confirm-modal-text">
                    Это действие изменит статус бронирования и связанного платежа.
                </p>

                <p class="admin-confirm-modal-info" data-booking-info-modal>—</p>

                <div class="admin-confirm-modal-actions">
                    <button type="button" class="admin-confirm-no" data-modal-close>
                        Нет, оставить
                    </button>

                    <button type="button" class="admin-confirm-yes" data-confirm-cancel>
                        <i class='bx bx-x'></i>
                        Да, отменить
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(modal);

        modal.addEventListener('click', function (event) {
            if (event.target === modal || event.target.closest('[data-modal-close]')) {
                closeCancelModal();
            }
        });

        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape' && modal.classList.contains('open')) {
                closeCancelModal();
            }
        });

        modal.querySelector('[data-confirm-cancel]').addEventListener('click', function () {
            if (!pendingCancelForm) {
                return;
            }

            const form = pendingCancelForm;
            closeCancelModal();
            submitBookingAction(form);
        });
    }

    function openCancelModal() {
        const modal = document.getElementById('adminCancelModal');

        if (modal) {
            modal.classList.add('open');
        }
    }

    function closeCancelModal() {
        const modal = document.getElementById('adminCancelModal');

        if (modal) {
            modal.classList.remove('open');
        }

        pendingCancelForm = null;
    }

    function showAlert(message, type) {
        const alert = document.getElementById('adminBookingAlert');

        if (!alert) {
            return;
        }

        alert.innerHTML = `<span>${escapeHtml(message)}</span>`;
        alert.className = 'admin-booking-alert';

        if (type === 'success') {
            alert.classList.add('admin-booking-alert-success');
        } else {
            alert.classList.add('admin-booking-alert-error');
        }

        alert.classList.remove('admin-booking-alert-hidden');

        window.setTimeout(function () {
            alert.classList.add('admin-booking-alert-hidden');
        }, 3500);
    }

    function createToast() {
        if (document.getElementById('adminToast')) {
            return;
        }

        const toast = document.createElement('div');
        toast.id = 'adminToast';
        toast.className = 'admin-toast';
        document.body.appendChild(toast);
    }

    function showMessage(message, type) {
        showAlert(message, type);
        showToast(message, type);
    }

    function showToast(message, type) {
        const toast = document.getElementById('adminToast');

        if (!toast) {
            return;
        }

        toast.textContent = message;
        toast.className = 'admin-toast';

        if (type === 'success') {
            toast.classList.add('admin-toast-success');
        } else {
            toast.classList.add('admin-toast-error');
        }

        requestAnimationFrame(function () {
            toast.classList.add('show');
        });

        window.clearTimeout(toast.hideTimer);
        toast.hideTimer = window.setTimeout(function () {
            toast.classList.remove('show');
        }, 3500);
    }

    function escapeHtml(value) {
        return String(value)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    function escapeAttr(value) {
        return escapeHtml(value);
    }
});