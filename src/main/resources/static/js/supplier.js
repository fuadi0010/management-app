document.addEventListener('DOMContentLoaded', function () {

    // Auto hide flash messages
    document.querySelectorAll('.flash-message').forEach(msg => {
        setTimeout(() => {
            msg.style.opacity = '0';
            setTimeout(() => msg.remove(), 500);
        }, 5000);
    });

    // Button click animation
    document.querySelectorAll('.btn-primary, .btn-warning')
        .forEach(btn => {
            btn.addEventListener('mousedown', () => btn.style.transform = 'scale(0.97)');
            btn.addEventListener('mouseup', () => btn.style.transform = '');
            btn.addEventListener('mouseleave', () => btn.style.transform = '');
        });

    // Highlight table rows (UI only)
    document.querySelectorAll('.data-table tbody tr').forEach(row => {
        row.addEventListener('mouseenter', () => {
            row.style.backgroundColor = '#f9f9f9';
        });
        row.addEventListener('mouseleave', () => {
            row.style.backgroundColor = '';
        });
    });

});
