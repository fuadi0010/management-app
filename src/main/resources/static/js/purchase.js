let rowIndex = 0;

function addRow() {
    const container = document.getElementById('items');
    const template = document.getElementById('productTemplate');

    const row = document.createElement('div');
    row.className = 'detail-item';

    // PRODUCT
    const productSelect = template.cloneNode(true);
    productSelect.style.display = 'block';
    productSelect.name = `purchaseDetails[${rowIndex}].product.id`;
    productSelect.required = true;

    // QUANTITY
    const qtyInput = document.createElement('input');
    qtyInput.type = 'number';
    qtyInput.min = '1';
    qtyInput.placeholder = 'Qty';
    qtyInput.name = `purchaseDetails[${rowIndex}].quantity`;
    qtyInput.required = true;

    // UNIT PRICE
    const priceInput = document.createElement('input');
    priceInput.type = 'number';
    priceInput.min = '0';
    priceInput.placeholder = 'Harga Beli';
    priceInput.name = `purchaseDetails[${rowIndex}].unitPurchasePrice`;
    priceInput.required = true;

    // REMOVE BUTTON
    const removeBtn = document.createElement('button');
    removeBtn.type = 'button';
    removeBtn.textContent = 'Hapus';
    removeBtn.onclick = () => row.remove();

    row.append(productSelect, qtyInput, priceInput, removeBtn);
    container.appendChild(row);

    rowIndex++;
}

// otomatis 1 baris saat load
document.addEventListener('DOMContentLoaded', () => {
    addRow();
});
