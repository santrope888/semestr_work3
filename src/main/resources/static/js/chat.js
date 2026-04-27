(function () {
    'use strict';

    const root = document.getElementById('chat-root');
    const chatSessionId = root ? Number(root.dataset.sessionId) || null : null;

    function getCsrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        return token && header ? { [header]: token } : {};
    }

    function renderMarkdown(text) {
        return text
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
            .replace(/\*(.+?)\*/g, '<em>$1</em>')
            .replace(/\n/g, '<br>');
    }

    function enhanceAssistantMessages() {
        document.querySelectorAll('.chat-message.assistant').forEach((node) => {
            const raw = node.textContent || '';
            node.innerHTML = renderMarkdown(raw);
        });
    }

    function appendMessage(role, text) {
        const messages = document.getElementById('chat-messages');
        const div = document.createElement('div');
        div.className = 'chat-message ' + role;

        if (role === 'assistant') {
            div.innerHTML = renderMarkdown(text);
        } else {
            div.textContent = text;
        }

        messages.appendChild(div);
        messages.scrollTop = messages.scrollHeight;
    }

    async function readErrorMessage(res) {
        try {
            const data = await res.json();
            if (data?.message) return data.message;
            if (data?.error) return data.error;
        } catch (e) {
            console.error('Не удалось разобрать JSON ошибки', e);
        }

        try {
            const text = await res.text();
            if (text && text.trim()) return text;
        } catch (e) {
            console.error('Не удалось прочитать текст ошибки', e);
        }

        return `Ошибка ${res.status}. AuraBot сейчас недоступен.`;
    }

    function toggleChatsMenu() {
        const menu = document.getElementById('chat-sessions-menu');
        menu?.classList.toggle('open');
    }

    document.addEventListener('click', function (e) {
        const menu = document.getElementById('chat-sessions-menu');
        if (!menu) return;
        const clickedInsideMenu = e.target.closest('#chat-sessions-menu');
        const clickedToggle = e.target.closest('#toggle-chats-btn');
        if (!clickedInsideMenu && !clickedToggle) {
            menu.classList.remove('open');
        }
    });

    async function sendChat() {
        if (!chatSessionId) return;

        const input = document.getElementById('chat-input');
        const btn = document.getElementById('chat-send-btn');
        const msg = input.value.trim();

        if (!msg) return;

        appendMessage('user', msg);
        input.value = '';
        btn.disabled = true;

        const messages = document.getElementById('chat-messages');
        const typing = document.createElement('div');
        typing.className = 'chat-typing';
        typing.id = 'typing';
        typing.textContent = 'AuraBot печатает...';
        messages.appendChild(typing);
        messages.scrollTop = messages.scrollHeight;

        try {
            const res = await fetch(`/api/chat/sessions/${chatSessionId}/send`, {
                method: 'POST',
                credentials: 'same-origin',
                headers: {
                    'Content-Type': 'application/json',
                    ...getCsrfHeaders()
                },
                body: JSON.stringify({ message: msg })
            });

            document.getElementById('typing')?.remove();

            if (!res.ok) {
                const errorMessage = await readErrorMessage(res);
                appendMessage('assistant', errorMessage);
                console.error('Chat API returned error', res.status, errorMessage);
                return;
            }

            const data = await res.json();

            if (data?.response) {
                appendMessage('assistant', data.response);
            } else {
                appendMessage('assistant', 'Пустой ответ от AuraBot.');
            }
        } catch (e) {
            document.getElementById('typing')?.remove();
            appendMessage('assistant', 'Не удалось связаться с сервером. Попробуйте позже.');
            console.error('Network error while sending chat message', e);
        } finally {
            btn.disabled = false;
            input.focus();
        }
    }

    window.addEventListener('DOMContentLoaded', () => {
        enhanceAssistantMessages();

        const messages = document.getElementById('chat-messages');
        if (messages) messages.scrollTop = messages.scrollHeight;

        document.getElementById('chat-input')?.focus();
    });

    window.toggleChatsMenu = toggleChatsMenu;
    window.sendChat = sendChat;
})();