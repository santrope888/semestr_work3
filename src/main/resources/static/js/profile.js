(function () {
    'use strict';

    function switchBlock(hideId, showId) {
        const hideEl = document.getElementById(hideId + 'Block');
        const showEl = document.getElementById(showId + 'Block');
        if (hideEl) hideEl.style.display = 'none';
        if (showEl) showEl.style.display = 'block';
    }

    function submitAvatarForm() {
        const input = document.getElementById('avatarUploadTrigger');
        if (!input || !input.files || !input.files[0]) return;
        const file = input.files[0];
        const allowed = ['image/jpeg', 'image/png', 'image/webp'];
        if (!allowed.includes(file.type)) {
            alert('Разрешены только JPG, PNG, WEBP');
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            alert('Файл слишком большой. Максимум 5 МБ');
            return;
        }
        const reader = new FileReader();
        reader.onload = e => {
            const preview = document.getElementById('avatarPreview');
            if (preview) preview.src = e.target.result;
        };
        reader.readAsDataURL(file);
        const form = document.getElementById('avatarForm');
        if (form) form.submit();
    }

    function confirmDelete() {
        const modal = document.getElementById('deleteModal');
        if (modal) modal.style.display = 'flex';
    }

    function closeDelete() {
        const modal = document.getElementById('deleteModal');
        if (modal) modal.style.display = 'none';
    }

    document.addEventListener('DOMContentLoaded', function () {
        const modal = document.getElementById('deleteModal');
        if (modal) {
            modal.addEventListener('click', function (e) {
                if (e.target === this) closeDelete();
            });
        }

        let total = 0;
        document.querySelectorAll('.profile-step').forEach(step => {
            if (step.dataset.done === 'true') {
                const t = step.querySelector('.step-percent');
                if (t) total += parseInt(t.textContent);
            }
        });
        total = Math.min(total, 100);
        const el = document.getElementById('progressText');
        const circle = document.getElementById('progressCircle');
        if (el) el.textContent = total + '%';
        if (circle) {
            circle.style.transition = 'stroke-dashoffset 1s ease';
            setTimeout(() => {
                circle.style.strokeDashoffset = 276.5 - (total / 100) * 276.5;
            }, 100);
        }
    });

    window.switchBlock = switchBlock;
    window.submitAvatarForm = submitAvatarForm;
    window.confirmDelete = confirmDelete;
    window.closeDelete = closeDelete;
})();