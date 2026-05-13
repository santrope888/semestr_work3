document.addEventListener('DOMContentLoaded', function () {
    let pendingDeleteForm = null;

    createDeleteModal();

    document.addEventListener('click', function (event) {
        const deleteButton = event.target.closest('.admin-car-delete-open');

        if (!deleteButton) {
            return;
        }

        pendingDeleteForm = deleteButton.closest('form');

        const carInfo = document.querySelector('[data-admin-car-delete-info]');
        if (carInfo) {
            carInfo.textContent = deleteButton.dataset.carInfo || 'Выбранный автомобиль';
        }

        openDeleteModal();
    });

    function createDeleteModal() {
        const modal = document.createElement('div');
        modal.className = 'admin-car-delete-modal-overlay';
        modal.id = 'adminCarDeleteModal';

        modal.innerHTML = `
            <div class="admin-car-delete-modal">
                <button type="button" class="admin-car-delete-modal-close" data-admin-car-delete-close>&times;</button>

                <div class="admin-car-delete-modal-icon">
                    <i class='bx bx-error-circle'></i>
                </div>

                <h3 class="admin-car-delete-modal-title">Удалить автомобиль?</h3>

                <p class="admin-car-delete-modal-text">
                    Автомобиль будет удалён из списка. Это действие нельзя отменить.
                </p>

                <p class="admin-car-delete-modal-info" data-admin-car-delete-info>—</p>

                <div class="admin-car-delete-modal-actions">
                    <button type="button" class="admin-car-delete-no" data-admin-car-delete-close>
                        Нет, оставить
                    </button>

                    <button type="button" class="admin-car-delete-yes" data-admin-car-delete-confirm>
                        <i class='bx bx-trash'></i>
                        Да, удалить
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(modal);

        modal.addEventListener('click', function (event) {
            if (event.target === modal || event.target.closest('[data-admin-car-delete-close]')) {
                closeDeleteModal();
            }
        });

        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape' && modal.classList.contains('open')) {
                closeDeleteModal();
            }
        });

        modal.querySelector('[data-admin-car-delete-confirm]').addEventListener('click', function () {
            if (!pendingDeleteForm) {
                return;
            }

            const confirmButton = this;
            confirmButton.disabled = true;

            pendingDeleteForm.submit();
        });
    }

    function openDeleteModal() {
        const modal = document.getElementById('adminCarDeleteModal');

        if (modal) {
            modal.classList.add('open');
        }
    }

    function closeDeleteModal() {
        const modal = document.getElementById('adminCarDeleteModal');

        if (modal) {
            modal.classList.remove('open');
        }

        pendingDeleteForm = null;
    }
});