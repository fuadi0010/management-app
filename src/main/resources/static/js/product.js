
document.addEventListener('DOMContentLoaded', function() {
    console.log('Pure UI JS loaded');
    
    const numberInputs = document.querySelectorAll('input[type="number"]');
    numberInputs.forEach(input => {
        input.addEventListener('focus', function() {
            this.style.backgroundColor = '#f5f9ff';
            this.style.borderColor = '#234C6A';
        });
        
        input.addEventListener('blur', function() {
            this.style.backgroundColor = '';
            this.style.borderColor = '';
        });
    });
    
    const buttons = document.querySelectorAll('.btn, .btn-action');
    buttons.forEach(button => {
        button.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-2px)';
            this.style.boxShadow = '0 4px 8px rgba(0,0,0,0.1)';
        });
        
        button.addEventListener('mouseleave', function() {
            this.style.transform = '';
            this.style.boxShadow = '';
        });
    });
    const tableRows = document.querySelectorAll('.data-table tbody tr');
    tableRows.forEach(row => {
        row.addEventListener('mouseenter', function() {
            this.style.backgroundColor = '#f5f9ff';
        });
        
        row.addEventListener('mouseleave', function() {
            this.style.backgroundColor = '';
        });
    });
    
    const formInputs = document.querySelectorAll('.form-input');
    formInputs.forEach(input => {
        input.addEventListener('input', function() {
            this.style.borderColor = '';
            const errorMsg = this.parentNode.querySelector('.error-message');
            if (errorMsg) {
                errorMsg.style.opacity = '0.5';
            }
        });
    });
    
    function makeTablesResponsive() {
        const tables = document.querySelectorAll('.data-table');
        tables.forEach(table => {
            if (window.innerWidth < 768) {
                table.style.fontSize = '14px';
                table.querySelectorAll('td, th').forEach(cell => {
                    cell.style.padding = '10px 8px';
                });
            } else {
                table.style.fontSize = '';
                table.querySelectorAll('td, th').forEach(cell => {
                    cell.style.padding = '';
                });
            }
        });
    }
    
    const forms = document.querySelectorAll('form');
    forms.forEach(form => {
        form.addEventListener('submit', function() {
            const submitBtn = this.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.innerHTML = 'Loading...';
                submitBtn.disabled = true;
                submitBtn.style.opacity = '0.7';
                submitBtn.style.cursor = 'wait';
            }
        });
    });
    
    const actionLinks = document.querySelectorAll('a[href*="#"], .btn-action');
    actionLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            // Hanya untuk internal anchor links
            if (this.getAttribute('href').startsWith('#')) {
                e.preventDefault();
                const targetId = this.getAttribute('href').substring(1);
                const targetElement = document.getElementById(targetId);
                if (targetElement) {
                    window.scrollTo({
                        top: targetElement.offsetTop - 100,
                        behavior: 'smooth'
                    });
                }
            }
        });
    });
    
    // 8. AUTO-RESIZE TEXTAREA
    const textareas = document.querySelectorAll('textarea');
    textareas.forEach(textarea => {
        textarea.addEventListener('input', function() {
            this.style.height = 'auto';
            this.style.height = (this.scrollHeight) + 'px';
        });
    });
    
    // 9. COPY TO CLIPBOARD FOR CODES 
    const copyableCodes = document.querySelectorAll('.product-code');
    copyableCodes.forEach(code => {
        code.addEventListener('click', function() {
            const textToCopy = this.textContent;
            navigator.clipboard.writeText(textToCopy).then(() => {
                
                const originalText = this.textContent;
                this.textContent = '✓ Copied!';
                this.style.backgroundColor = '#d4e6ed';
                
                setTimeout(() => {
                    this.textContent = originalText;
                    this.style.backgroundColor = '';
                }, 1500);
            });
        });
        
        code.style.cursor = 'pointer';
        code.title = 'Click to copy';
    });
    
    // 10. TOGGLE VISIBILITY FOR SENSITIVE DATA 
    const toggleButtons = document.querySelectorAll('.toggle-visibility');
    toggleButtons.forEach(button => {
        button.addEventListener('click', function() {
            const targetId = this.getAttribute('data-target');
            const targetElement = document.getElementById(targetId);
            if (targetElement) {
                if (targetElement.type === 'password') {
                    targetElement.type = 'text';
                    this.textContent = 'Hide';
                } else {
                    targetElement.type = 'password';
                    this.textContent = 'Show';
                }
            }
        });
    });
    
    // ===== PURE UI ANIMATIONS =====
    const style = document.createElement('style');
    style.textContent = `
        /* Pure UI Animations - No Business Logic */
        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(10px); }
            to { opacity: 1; transform: translateY(0); }
        }
        
        @keyframes gentlePulse {
            0% { box-shadow: 0 0 0 0 rgba(35, 76, 106, 0.2); }
            70% { box-shadow: 0 0 0 6px rgba(35, 76, 106, 0); }
            100% { box-shadow: 0 0 0 0 rgba(35, 76, 106, 0); }
        }
        
        .fade-in {
            animation: fadeIn 0.5s ease;
        }
        
        .pulse-once {
            animation: gentlePulse 1s ease;
        }
    `;
    document.head.appendChild(style);
    
    // Tambahkan animasi fade in untuk konten
    const mainContent = document.querySelector('.content-container, .form-container, .table-container');
    if (mainContent) {
        mainContent.classList.add('fade-in');
    }
    
    // 11. LAZY LOAD IMAGES (Pure Performance)
    const lazyImages = document.querySelectorAll('img[data-src]');
    if ('IntersectionObserver' in window) {
        const imageObserver = new IntersectionObserver((entries, observer) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const img = entry.target;
                    img.src = img.dataset.src;
                    img.classList.add('fade-in');
                    observer.unobserve(img);
                }
            });
        });
        
        lazyImages.forEach(img => imageObserver.observe(img));
    }
    
    // 12. DEBOUNCE FOR SEARCH INPUT (Pure Performance - No Business Logic)
    const debounce = (func, wait) => {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    };
    
    // 13. INITIALIZE ON RESIZE
    window.addEventListener('resize', makeTablesResponsive);
    
    // Initial calls
    makeTablesResponsive();
    
    console.log('Pure UI JS initialization complete - No business logic');
});
/**
 * Toggle element visibility (Pure UI)
 */
function toggleElementVisibility(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
        element.style.display = element.style.display === 'none' ? 'block' : 'none';
    }
}

/**
 * Add loading spinner to element (Pure UI)
 */
function addLoadingSpinner(element) {
    if (element) {
        const spinner = document.createElement('div');
        spinner.className = 'loading-spinner';
        spinner.innerHTML = `
            <div style="
                width: 20px;
                height: 20px;
                border: 2px solid #f3f3f3;
                border-top: 2px solid #234C6A;
                border-radius: 50%;
                animation: spin 1s linear infinite;
            "></div>
        `;
        element.appendChild(spinner);
    }
}

/**
 * Remove loading spinner (Pure UI)
 */
function removeLoadingSpinner(element) {
    if (element) {
        const spinner = element.querySelector('.loading-spinner');
        if (spinner) {
            spinner.remove();
        }
    }
}

/**
 * Show temporary notification (Pure UI)
 */
function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.textContent = message;
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 12px 20px;
        background: ${type === 'success' ? '#d4e6ed' : type === 'error' ? '#f8d7da' : '#f5f9ff'};
        color: ${type === 'success' ? '#0c5460' : type === 'error' ? '#721c24' : '#1B3C53'};
        border-radius: 6px;
        z-index: 1000;
        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        animation: fadeIn 0.3s ease;
    `;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.style.animation = 'fadeIn 0.3s ease reverse';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

// Tambahkan style untuk spinner
const spinnerStyle = document.createElement('style');
spinnerStyle.textContent = `
    @keyframes spin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
    }
    
    .loading-spinner {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        margin-left: 8px;
    }
`;
document.head.appendChild(spinnerStyle);

console.log('Pure UI utility functions loaded');