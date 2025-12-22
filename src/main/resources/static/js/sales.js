// invoice.js - UX ONLY (NO BUSINESS LOGIC)

document.addEventListener('DOMContentLoaded', () => {

    const form = document.querySelector('form');
    if (form) {
        form.addEventListener('submit', () => {
            showLoading();
        });
    }

    // Auto-hide flash messages
    document.querySelectorAll('.success-message, .error-message')
        .forEach(msg => {
            setTimeout(() => {
                msg.style.opacity = '0';
                setTimeout(() => msg.remove(), 500);
            }, 5000);
        });

    // Confirmation buttons (UX only)
    document.querySelectorAll('.btn-danger, .btn-warning')
        .forEach(btn => {
            btn.addEventListener('click', e => {
                if (!confirm('Yakin melanjutkan aksi ini?')) {
                    e.preventDefault();
                }
            });
        });

    // Hover effect (UI only)
    document.querySelectorAll('.data-table tbody tr')
        .forEach(row => {
            row.addEventListener('mouseenter', () => {
                row.style.transform = 'translateY(-2px)';
                row.style.boxShadow = '0 4px 8px rgba(0,0,0,0.1)';
            });
            row.addEventListener('mouseleave', () => {
                row.style.transform = 'none';
                row.style.boxShadow = 'none';
            });
        });

});

/* ================= UX HELPERS ================= */

function showLoading() {
    const btn = document.querySelector('button[type="submit"]');
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '⏳ Memproses...';
    }
}

function formatCurrencyPreview(amount) {
    if (!amount) amount = 0;
    return 'Rp ' + Number(amount).toLocaleString('id-ID');
}

// OPTIONAL: preview subtotal (tidak authoritative)
function previewSubtotal(inputQty, price, targetElement) {
    const qty = parseInt(inputQty.value) || 0;
    const subtotal = qty * price;
    targetElement.textContent = formatCurrencyPreview(subtotal);
}
/* ================= ADD / REMOVE ROW (UX ONLY) ================= */

/* ================= ADD / REMOVE ROW (UX ONLY) ================= */

const MAX_ROWS = 5;

function addItemRow() {
    const items = document.getElementById('items');
    const template = document.getElementById('productTemplate');

    if (items.children.length >= MAX_ROWS) {
        alert('Maksimal 5 produk per invoice');
        return;
    }

    const index = items.children.length;

    const row = document.createElement('div');
    row.className = 'item-row';

    row.innerHTML = `
        <select name="invoiceDetails[${index}].product.id"
                onchange="onProductChange(this)">
            ${template.innerHTML}
        </select>

        <input type="number"
               name="invoiceDetails[${index}].quantity"
               placeholder="Quantity"
               inputmode="numeric"
               pattern="[0-9]*"
               min="1" />

        <button type="button"
                class="btn btn-danger btn-small"
                onclick="removeRow(this)">
            ✕
        </button>

        <div class="subtotal-preview">Rp 0</div>
    `;

    items.appendChild(row);
    updateRowCount();
}

function removeRow(button) {
    const row = button.closest('.item-row');
    if (row) {
        row.remove();
        reindexRows();
        updateRowCount();
    }
}

/* ================= REINDEX (SPRING MVC SAFE) ================= */

function reindexRows() {
    const rows = document.querySelectorAll('#items .item-row');

    rows.forEach((row, index) => {
        const select = row.querySelector('select');
        const qtyInput = row.querySelector('input[type="number"]');

        select.name = `invoiceDetails[${index}].product.id`;
        qtyInput.name = `invoiceDetails[${index}].quantity`;
    });
}

/* ================= ROW COUNTER (UX ONLY) ================= */

function updateRowCount() {
    const countEl = document.getElementById('rowCount');
    const items = document.getElementById('items');
    if (countEl && items) {
        countEl.textContent = items.children.length;
    }
}


/* ================= PREVIEW ONLY ================= */

function onProductChange(selectEl) {
    const row = selectEl.closest('.item-row');
    const price = selectEl.options[selectEl.selectedIndex]
        ?.getAttribute('data-price') || 0;

    const qtyInput = row.querySelector('input[type="number"]');
    const pricePreview = row.querySelector('.price-preview');
    const subtotalPreview = row.querySelector('.subtotal-preview');

    pricePreview.textContent = formatCurrencyPreview(price);

    const qty = parseInt(qtyInput.value) || 0;
    subtotalPreview.textContent =
        formatCurrencyPreview(price * qty);
}

function onQtyChange(inputEl) {
    const row = inputEl.closest('.item-row');
    const selectEl = row.querySelector('select');

    const price = selectEl.options[selectEl.selectedIndex]
        ?.getAttribute('data-price') || 0;

    const subtotalPreview = row.querySelector('.subtotal-preview');
    const qty = parseInt(inputEl.value) || 0;

    subtotalPreview.textContent =
        formatCurrencyPreview(price * qty);
}
