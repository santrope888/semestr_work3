(function () {
    'use strict';

    function getCsrfHeaders() {
        const token  = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        return token && header ? { [header]: token } : {};
    }

    function toggleUserMenu(e) {
        e.stopPropagation();
        document.getElementById('notifDropdown')?.classList.remove('open');
        document.getElementById('userDropdown')?.classList.toggle('open');
    }

    function toggleNotif(e) {
        e.stopPropagation();
        document.getElementById('userDropdown')?.classList.remove('open');
        const dropdown = document.getElementById('notifDropdown');
        if (!dropdown) return;
        dropdown.classList.toggle('open');
        if (dropdown.classList.contains('open')) {
            loadNotifications();
        }
    }

    document.addEventListener('click', function () {
        document.getElementById('userDropdown')?.classList.remove('open');
        document.getElementById('notifDropdown')?.classList.remove('open');
    });

    function getNotifIcon(type) {
        if (type?.includes('CONFIRM'))                           return { cls: 'booking', icon: 'bx-check-circle' };
        if (type?.includes('CANCEL'))                            return { cls: 'cancel',  icon: 'bx-x-circle' };
        if (type?.includes('PAYMENT') || type?.includes('PAID')) return { cls: 'payment', icon: 'bx-credit-card' };
        if (type?.includes('COMPLETE'))                          return { cls: 'payment', icon: 'bx-check-double' };
        if (type?.includes('BOOKING'))                           return { cls: 'booking', icon: 'bx-calendar-plus' };
        return { cls: 'info', icon: 'bx-info-circle' };
    }

    function timeAgo(dateStr) {
        const diff = Date.now() - new Date(dateStr).getTime();
        const mins = Math.floor(diff / 60000);
        if (mins < 1)  return 'только что';
        if (mins < 60) return mins + ' мин. назад';
        const hours = Math.floor(mins / 60);
        if (hours < 24) return hours + ' ч. назад';
        return Math.floor(hours / 24) + ' дн. назад';
    }

    async function loadNotifications() {
        try {
            const res = await fetch('/api/notifications/my');
            if (!res.ok) return;
            const notifications = await res.json();

            const list  = document.getElementById('notifList');
            const empty = document.getElementById('notifEmpty');
            if (!list || !empty) return;

            if (notifications.length === 0) {
                empty.style.display = 'block';
                return;
            }
            empty.style.display = 'none';

            list.innerHTML = '';
            notifications.slice(0, 20).forEach(n => {
                const icon = getNotifIcon(n.type);
                const item = document.createElement('div');
                item.className = 'notif-item' + (n.isRead ? '' : ' unread');
                item.onclick = (e) => {
                    e.stopPropagation();
                    markRead(n.id, item);
                };
                item.innerHTML = `
                    <div class="notif-item-icon ${icon.cls}"><i class='bx ${icon.icon}'></i></div>
                    <div class="notif-item-text">
                        <span></span>
                        <div class="notif-item-time">${timeAgo(n.createdAt)}</div>
                    </div>
                    ${n.isRead ? '' : '<div class="notif-item-dot"></div>'}
                `;
                item.querySelector('.notif-item-text span').textContent = n.message;
                list.appendChild(item);
            });
        } catch (e) {
            console.error('Failed to load notifications', e);
        }
    }

    async function markRead(id, el) {
        try {
            const res = await fetch(`/api/notifications/${id}/read`, {
                method: 'PATCH',
                credentials: 'same-origin',
                headers: getCsrfHeaders()
            });
            if (!res.ok) {
                console.error('markRead failed:', res.status);
                return;
            }
            el.classList.remove('unread');
            el.querySelector('.notif-item-dot')?.remove();
            updateBadge();
        } catch (e) {
            console.error('markRead network error', e);
        }
    }

    async function markAllRead() {
        try {
            const res = await fetch('/api/notifications/read-all', {
                method: 'PATCH',
                credentials: 'same-origin',
                headers: getCsrfHeaders()
            });
            if (!res.ok) {
                console.error('markAllRead failed:', res.status);
                return;
            }
            document.querySelectorAll('.notif-item.unread').forEach(el => {
                el.classList.remove('unread');
                el.querySelector('.notif-item-dot')?.remove();
            });
            updateBadge();
        } catch (e) {
            console.error('markAllRead network error', e);
        }
    }

    async function updateBadge() {
        try {
            const res = await fetch('/api/notifications/count');
            if (!res.ok) return;
            const data = await res.json();
            const badge = document.getElementById('notifBadge');
            if (!badge) return;
            if (data.count > 0) {
                badge.textContent = data.count > 99 ? '99+' : data.count;
                badge.style.display = 'flex';
            } else {
                badge.style.display = 'none';
            }
        } catch (e) { /* silent */ }
    }

    window.toggleUserMenu = toggleUserMenu;
    window.toggleNotif    = toggleNotif;
    window.markAllRead    = markAllRead;
    window.loadNotifications = loadNotifications;
    window.updateBadge    = updateBadge;

    document.addEventListener('DOMContentLoaded', updateBadge);
})();