(function () {
    'use strict';

    function switchBlock(hideId, showId) {
        const hideEl = document.getElementById(hideId + 'Block');
        const showEl = document.getElementById(showId + 'Block');

        if (hideEl) {
            hideEl.style.display = 'none';
        }

        if (showEl) {
            showEl.style.display = 'block';
        }
    }

    function submitAvatarForm() {
        const input = document.getElementById('avatarUploadTrigger');

        if (!input || !input.files || !input.files[0]) {
            return;
        }

        const file = input.files[0];
        const allowed = ['image/jpeg', 'image/png', 'image/webp'];

        if (!allowed.includes(file.type)) {
            showProfileToast('Разрешены только JPG, PNG, WEBP', 'error');
            input.value = '';
            return;
        }

        if (file.size > 5 * 1024 * 1024) {
            showProfileToast('Файл слишком большой. Максимум 5 МБ', 'error');
            input.value = '';
            return;
        }

        const reader = new FileReader();

        reader.onload = event => {
            const preview = document.getElementById('avatarPreview');

            if (preview) {
                preview.src = event.target.result;
            }
        };

        reader.readAsDataURL(file);

        const form = document.getElementById('avatarForm');

        if (form) {
            form.submit();
        }
    }

    function showProfileToast(message, type) {
        let toast = document.getElementById('profileToast');

        if (!toast) {
            toast = document.createElement('div');
            toast.id = 'profileToast';
            toast.className = 'profile-toast';
            document.body.appendChild(toast);
        }

        toast.textContent = message;
        toast.className = 'profile-toast';

        if (type === 'success') {
            toast.classList.add('profile-toast-success');
        } else {
            toast.classList.add('profile-toast-error');
        }

        requestAnimationFrame(function () {
            toast.classList.add('show');
        });

        window.clearTimeout(toast.hideTimer);
        toast.hideTimer = window.setTimeout(function () {
            toast.classList.remove('show');
        }, 3500);
    }

    function confirmDelete() {
        const modal = document.getElementById('deleteModal');

        if (modal) {
            modal.style.display = 'flex';
        }
    }

    function closeDelete() {
        const modal = document.getElementById('deleteModal');

        if (modal) {
            modal.style.display = 'none';
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        const modal = document.getElementById('deleteModal');

        if (modal) {
            modal.addEventListener('click', function (event) {
                if (event.target === this) {
                    closeDelete();
                }
            });
        }

        document.querySelector('[data-avatar-upload]')?.addEventListener('change', submitAvatarForm);
        document.querySelector('[data-confirm-delete-account]')?.addEventListener('click', confirmDelete);
        document.querySelector('[data-close-delete-account]')?.addEventListener('click', closeDelete);
        document.querySelectorAll('[data-switch-hide][data-switch-show]').forEach(btn => {
            btn.addEventListener('click', () => switchBlock(btn.dataset.switchHide, btn.dataset.switchShow));
        });
        document.querySelectorAll('[data-submit-parent-form]').forEach(input => {
            input.addEventListener('change', function () {
                this.closest('form')?.submit();
            });
        });

        let total = 0;

        document.querySelectorAll('.profile-step').forEach(step => {
            if (step.dataset.done === 'true') {
                const percent = step.querySelector('.step-percent');

                if (percent) {
                    total += parseInt(percent.textContent, 10);
                }
            }
        });

        total = Math.min(total, 100);

        const progressText = document.getElementById('progressText');
        const progressCircle = document.getElementById('progressCircle');

        if (progressText) {
            progressText.textContent = total + '%';
        }

        if (progressCircle) {
            progressCircle.style.transition = 'stroke-dashoffset 1s ease';

            setTimeout(() => {
                progressCircle.style.strokeDashoffset = 276.5 - (total / 100) * 276.5;
            }, 100);
        }
    });

})();
