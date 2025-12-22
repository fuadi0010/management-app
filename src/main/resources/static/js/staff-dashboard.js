// ============================================
// STAFF DASHBOARD JAVASCRIPT
// File: staff-dashboard.js
// ============================================

class StaffDashboard {
    constructor() {
        this.currentPath = window.location.pathname;
        this.init();
    }

    // Inisialisasi semua fungsi
    init() {
        console.log('Staff Dashboard initialized');
        
        this.highlightActiveMenu();
        this.updateRealTime();
        this.setupEventListeners();
        this.initSidebarClock();
    }

    // Highlight menu aktif berdasarkan URL
    highlightActiveMenu() {
        const menuItems = document.querySelectorAll('.menu-item');
        const currentPath = this.currentPath.toLowerCase();
        
        menuItems.forEach(item => {
            const href = item.getAttribute('href');
            if (href) {
                // Remove query parameters for comparison
                const hrefPath = href.split('?')[0].toLowerCase();
                
                // Check if current path contains the href path
                if (currentPath.includes(hrefPath) && hrefPath !== '') {
                    item.classList.add('active');
                }
                
                // Special case for dashboard
                if (currentPath.includes('/staff/dashboard') && href.includes('dashboard')) {
                    item.classList.add('active');
                }
            }
        });
    }

    // Update waktu real-time di sidebar
    updateRealTime() {
        const updateTime = () => {
            const now = new Date();
            const timeElement = document.querySelector('.detail-value:nth-child(2)');
            
            if (timeElement && timeElement.textContent.includes(':')) {
                timeElement.textContent = now.toLocaleTimeString('id-ID', { 
                    hour: '2-digit', 
                    minute: '2-digit' 
                });
            }
        };

        // Update immediately
        updateTime();
        
        // Update every minute
        setInterval(updateTime, 60000);
    }

    // Setup event listeners
    setupEventListeners() {
        // Action card hover effects
        this.setupActionCardEffects();
        
        // Hapus event listener logout karena sudah menggunakan onclick di link
    }

    // Setup action card effects
    setupActionCardEffects() {
        const actionCards = document.querySelectorAll('.action-card');
        
        actionCards.forEach(card => {
            card.addEventListener('mouseenter', () => {
                card.style.transform = 'translateY(-5px)';
            });
            
            card.addEventListener('mouseleave', () => {
                card.style.transform = 'translateY(0)';
            });
            
            card.addEventListener('click', (e) => {
                // Add click animation
                card.style.transform = 'scale(0.95)';
                setTimeout(() => {
                    card.style.transform = '';
                }, 150);
                
                // Log the action for analytics
                const title = card.querySelector('.action-title').textContent;
                console.log(`Quick action clicked: ${title}`);
            });
        });
    }

    // Initialize real-time clock in sidebar
    initSidebarClock() {
        const clockElement = document.querySelector('.sidebar-clock');
        if (!clockElement) return;
        
        const updateClock = () => {
            const now = new Date();
            clockElement.textContent = now.toLocaleTimeString('id-ID', { 
                hour: '2-digit', 
                minute: '2-digit' 
            });
        };
        
        updateClock();
        setInterval(updateClock, 1000);
    }
}

// Initialize dashboard when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    const dashboard = new StaffDashboard();
    
    // Make dashboard available globally for debugging
    window.staffDashboard = dashboard;
});