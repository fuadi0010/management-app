
document.addEventListener('DOMContentLoaded', () => {

    const form = document.querySelector('form');
    if (form) {
        form.addEventListener('submit', () => {
            showLoading();
        });
    }

    // Auto dismiss alert messages
    document.querySelectorAll('.alert, .error-message, .success-message')
        .forEach(msg => {
            setTimeout(() => {
                msg.style.opacity = '0';
                setTimeout(() => msg.remove(), 400);
            }, 5000);
        });

    // Toggle password visibility (UX only)
    document.querySelectorAll('.toggle-password')
        .forEach(btn => {
            btn.addEventListener('click', () => {
                const input = document.getElementById(btn.dataset.target);
                if (!input) return;

                input.type = input.type === 'password' ? 'text' : 'password';
                btn.textContent = input.type === 'password' ? '👁️' : '🙈';
            });
        });

});

/* ========== UX HELPERS ========== */

function showLoading() {
    const btn = document.querySelector('button[type="submit"]');
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '⏳ Memproses...';
    }
}
