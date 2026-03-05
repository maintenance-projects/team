/* =============================================
   SpaceNX - Project Management System
   Main JavaScript
   ============================================= */

// Apply theme immediately to prevent flash of wrong theme
(function() {
    const saved = localStorage.getItem('spacenx-theme');
    if (saved === 'dark' || (!saved && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
        document.documentElement.setAttribute('data-theme', 'dark');
    }
}
)();

document.addEventListener('DOMContentLoaded', () => {
    initThemeToggle();
    initBoardDragDrop();
    initCalendar();
    initTimeline();
    initReportCharts();
    initSidebarToggle();
    initSpaceKeyGenerator();
    initFilterAutoSubmit();
    initDeleteConfirmation();
    initFlashMessageDismiss();
    initDropdowns();
    initMentionAutocomplete();
});

/* ----- Theme Toggle ----- */
function initThemeToggle() {
    const toggle = document.getElementById('themeToggle');
    if (!toggle) return;

    const icon = toggle.querySelector('i');
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';

    // Set initial icon
    if (icon) {
        icon.className = isDark ? 'fas fa-sun' : 'fas fa-moon';
    }

    toggle.addEventListener('click', () => {
        const currentTheme = document.documentElement.getAttribute('data-theme');
        const newTheme = currentTheme === 'dark' ? 'light' : 'dark';

        if (newTheme === 'dark') {
            document.documentElement.setAttribute('data-theme', 'dark');
        } else {
            document.documentElement.removeAttribute('data-theme');
        }

        localStorage.setItem('spacenx-theme', newTheme);

        if (icon) {
            icon.className = newTheme === 'dark' ? 'fas fa-sun' : 'fas fa-moon';
        }
    });
}

/* ----- Board Drag and Drop (Kanban) ----- */
function initBoardDragDrop() {
    const cards = document.querySelectorAll('.board-card');
    const columns = document.querySelectorAll('.board-column');

    if (!cards.length) return;

    cards.forEach(card => {
        card.setAttribute('draggable', 'true');

        card.addEventListener('dragstart', (e) => {
            card.classList.add('dragging');
            e.dataTransfer.setData('text/plain', card.dataset.issueId);
            e.dataTransfer.effectAllowed = 'move';
        });

        card.addEventListener('dragend', () => {
            card.classList.remove('dragging');
            columns.forEach(col => col.classList.remove('drag-over'));
        });
    });

    columns.forEach(column => {
        column.addEventListener('dragover', (e) => {
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            column.classList.add('drag-over');

            // Determine insertion position among existing cards
            const cardList = column.querySelector('.board-card-list');
            if (cardList) {
                const afterElement = getDragAfterElement(cardList, e.clientY);
                const dragging = document.querySelector('.board-card.dragging');
                if (dragging) {
                    if (afterElement == null) {
                        cardList.appendChild(dragging);
                    } else {
                        cardList.insertBefore(dragging, afterElement);
                    }
                }
            }
        });

        column.addEventListener('dragleave', (e) => {
            // Only remove the class if we are truly leaving the column
            if (!column.contains(e.relatedTarget)) {
                column.classList.remove('drag-over');
            }
        });

        column.addEventListener('drop', (e) => {
            e.preventDefault();
            column.classList.remove('drag-over');

            const issueId = e.dataTransfer.getData('text/plain');
            const newStatus = column.dataset.status;
            const spaceKey = column.dataset.spaceKey;

            if (!issueId || !newStatus) return;

            // Move card visually (it may already be moved via dragover)
            const card = document.querySelector(`[data-issue-id="${issueId}"]`);
            const cardList = column.querySelector('.board-card-list');
            if (card && cardList && card.parentElement !== cardList) {
                cardList.appendChild(card);
            }

            // Update status badge on the card if present
            const badge = card ? card.querySelector('.badge') : null;
            if (badge) {
                badge.className = 'badge badge-' + statusToCssClass(newStatus);
                badge.textContent = formatStatus(newStatus);
            }

            // Update on server
            const csrfToken = document.querySelector('meta[name="_csrf"]');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]');

            const headers = {
                'Content-Type': 'application/x-www-form-urlencoded'
            };

            if (csrfToken) {
                const headerName = csrfHeader ? csrfHeader.content : 'X-CSRF-TOKEN';
                headers[headerName] = csrfToken.content;
            }

            fetch(`/spaces/${spaceKey}/board/move`, {
                method: 'POST',
                headers: headers,
                body: `issueId=${encodeURIComponent(issueId)}&status=${encodeURIComponent(newStatus)}`
            }).then(response => {
                if (!response.ok) {
                    console.error('Failed to update issue status');
                    location.reload();
                }
                updateColumnCounts();
            }).catch(err => {
                console.error('Error updating issue status:', err);
                location.reload();
            });
        });
    });
}

/**
 * Determine the element to insert the dragged card before,
 * based on the current mouse Y position.
 */
function getDragAfterElement(container, y) {
    const draggableElements = [...container.querySelectorAll('.board-card:not(.dragging)')];

    return draggableElements.reduce((closest, child) => {
        const box = child.getBoundingClientRect();
        const offset = y - box.top - box.height / 2;
        if (offset < 0 && offset > closest.offset) {
            return { offset: offset, element: child };
        } else {
            return closest;
        }
    }, { offset: Number.NEGATIVE_INFINITY }).element;
}

/**
 * Update the count badges in each board column header.
 */
function updateColumnCounts() {
    const columns = document.querySelectorAll('.board-column');
    columns.forEach(column => {
        const cardList = column.querySelector('.board-card-list');
        const countBadge = column.querySelector('.board-column-count');
        if (cardList && countBadge) {
            const count = cardList.querySelectorAll('.board-card').length;
            countBadge.textContent = count;
        }
    });
}

/**
 * Convert a status value to a CSS badge class suffix.
 */
function statusToCssClass(status) {
    if (!status) return 'todo';
    const map = {
        'TODO': 'todo',
        'TO_DO': 'todo',
        'IN_PROGRESS': 'in-progress',
        'IN_REVIEW': 'in-review',
        'DONE': 'done'
    };
    return map[status.toUpperCase()] || status.toLowerCase().replace(/_/g, '-');
}

/**
 * Format a status enum value into a display string.
 */
function formatStatus(status) {
    if (!status) return '';
    const map = {
        'TODO': 'To Do',
        'TO_DO': 'To Do',
        'IN_PROGRESS': 'In Progress',
        'IN_REVIEW': 'In Review',
        'DONE': 'Done'
    };
    return map[status.toUpperCase()] || status.replace(/_/g, ' ');
}


/* ----- Calendar (FullCalendar Integration) ----- */
function initCalendar() {
    const calendarEl = document.getElementById('calendar');
    if (!calendarEl) return;

    // Requires FullCalendar to be loaded
    if (typeof FullCalendar === 'undefined') {
        console.warn('FullCalendar library not loaded. Skipping calendar initialization.');
        return;
    }

    const spaceKey = calendarEl.dataset.spaceKey;

    const calendar = new FullCalendar.Calendar(calendarEl, {
        initialView: 'dayGridMonth',
        headerToolbar: {
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,timeGridWeek'
        },
        events: `/spaces/${spaceKey}/calendar/events`,
        eventClick: function(info) {
            info.jsEvent.preventDefault();
            const issueKey = info.event.extendedProps.issueKey;
            if (issueKey) {
                window.location.href = `/spaces/${spaceKey}/issues/${issueKey}`;
            }
        },
        eventDidMount: function(info) {
            // Color events based on status
            const status = info.event.extendedProps.status;
            const colors = {
                'TODO': '#3b82f6',
                'TO_DO': '#3b82f6',
                'IN_PROGRESS': '#f59e0b',
                'IN_REVIEW': '#8b5cf6',
                'DONE': '#22c55e'
            };
            if (status && colors[status.toUpperCase()]) {
                info.el.style.backgroundColor = colors[status.toUpperCase()];
                info.el.style.borderColor = colors[status.toUpperCase()];
            }
        },
        height: 'auto',
        dayMaxEvents: 3,
        eventDisplay: 'block'
    });

    calendar.render();
}


/* ----- Timeline / Gantt Chart ----- */
function initTimeline() {
    const timelineContainer = document.querySelector('.timeline-container');
    if (!timelineContainer) return;

    const timelineBody = timelineContainer.querySelector('.timeline-body');
    const timelineHeaderDates = timelineContainer.querySelector('.timeline-header-dates');

    if (!timelineBody || !timelineHeaderDates) return;

    const rows = timelineBody.querySelectorAll('.timeline-row');
    if (!rows.length) return;

    // Calculate date range from data attributes
    let minDate = null;
    let maxDate = null;

    rows.forEach(row => {
        const bar = row.querySelector('.timeline-bar');
        if (!bar) return;

        const start = bar.dataset.start ? new Date(bar.dataset.start) : null;
        const end = bar.dataset.end ? new Date(bar.dataset.end) : null;

        if (start && (!minDate || start < minDate)) minDate = start;
        if (end && (!maxDate || end > maxDate)) maxDate = end;
    });

    if (!minDate || !maxDate) return;

    // Add padding of a few days on each side
    const paddedMin = new Date(minDate);
    paddedMin.setDate(paddedMin.getDate() - 3);
    const paddedMax = new Date(maxDate);
    paddedMax.setDate(paddedMax.getDate() + 3);

    const totalDays = Math.ceil((paddedMax - paddedMin) / (1000 * 60 * 60 * 24));
    const dayWidth = 40; // px per day
    const totalWidth = totalDays * dayWidth;

    // Clear existing date cells and render header dates using safe DOM methods
    while (timelineHeaderDates.firstChild) {
        timelineHeaderDates.removeChild(timelineHeaderDates.firstChild);
    }
    timelineHeaderDates.style.width = totalWidth + 'px';

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    for (let i = 0; i < totalDays; i++) {
        const date = new Date(paddedMin);
        date.setDate(date.getDate() + i);

        const cell = document.createElement('div');
        cell.className = 'timeline-date-cell';
        cell.style.width = dayWidth + 'px';
        cell.style.minWidth = dayWidth + 'px';

        if (date.getTime() === today.getTime()) {
            cell.classList.add('today');
        }

        // Show day number, and month on the 1st or first visible day
        if (date.getDate() === 1 || i === 0) {
            cell.textContent = date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
        } else {
            cell.textContent = date.getDate();
        }

        timelineHeaderDates.appendChild(cell);
    }

    // Position bars
    rows.forEach(row => {
        const barsContainer = row.querySelector('.timeline-bars');
        const bar = row.querySelector('.timeline-bar');
        if (!barsContainer || !bar) return;

        barsContainer.style.width = totalWidth + 'px';

        const start = bar.dataset.start ? new Date(bar.dataset.start) : null;
        const end = bar.dataset.end ? new Date(bar.dataset.end) : null;

        if (!start || !end) return;

        const startOffset = Math.floor((start - paddedMin) / (1000 * 60 * 60 * 24));
        const duration = Math.ceil((end - start) / (1000 * 60 * 60 * 24)) + 1;

        bar.style.left = (startOffset * dayWidth) + 'px';
        bar.style.width = Math.max(duration * dayWidth - 4, dayWidth) + 'px';
    });
}


/* ----- Report Charts (Chart.js) ----- */
function initReportCharts() {
    const statusChartEl = document.getElementById('statusChart');
    if (!statusChartEl) return;

    // Requires Chart.js to be loaded
    if (typeof Chart === 'undefined') {
        console.warn('Chart.js library not loaded. Skipping chart initialization.');
        return;
    }

    const spaceKey = statusChartEl.dataset.spaceKey;

    fetch(`/spaces/${spaceKey}/reports/data`)
        .then(response => {
            if (!response.ok) throw new Error('Failed to fetch report data');
            return response.json();
        })
        .then(data => {
            renderStatusChart(statusChartEl, data);
            renderPriorityChart(data);
            renderTypeChart(data);
            renderCreatedVsResolvedChart(data);
        })
        .catch(err => {
            console.error('Error loading report data:', err);
        });
}

function renderStatusChart(canvas, data) {
    new Chart(canvas, {
        type: 'doughnut',
        data: {
            labels: ['To Do', 'In Progress', 'In Review', 'Done'],
            datasets: [{
                data: [
                    data.todo || 0,
                    data.inProgress || 0,
                    data.inReview || 0,
                    data.done || 0
                ],
                backgroundColor: ['#3b82f6', '#f59e0b', '#8b5cf6', '#22c55e'],
                borderWidth: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        padding: 16,
                        usePointStyle: true,
                        pointStyleWidth: 10,
                        font: { size: 12 }
                    }
                }
            },
            cutout: '60%'
        }
    });
}

function renderPriorityChart(data) {
    const priorityChartEl = document.getElementById('priorityChart');
    if (!priorityChartEl) return;

    new Chart(priorityChartEl, {
        type: 'bar',
        data: {
            labels: ['Highest', 'High', 'Medium', 'Low', 'Lowest'],
            datasets: [{
                label: 'Issues',
                data: [
                    data.highest || 0,
                    data.high || 0,
                    data.medium || 0,
                    data.low || 0,
                    data.lowest || 0
                ],
                backgroundColor: ['#ef4444', '#f97316', '#f59e0b', '#3b82f6', '#94a3b8'],
                borderRadius: 4,
                borderSkipped: false
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: { display: false }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: { stepSize: 1 },
                    grid: { color: 'rgba(0,0,0,0.04)' }
                },
                x: {
                    grid: { display: false }
                }
            }
        }
    });
}

function renderTypeChart(data) {
    const typeChartEl = document.getElementById('typeChart');
    if (!typeChartEl) return;

    new Chart(typeChartEl, {
        type: 'pie',
        data: {
            labels: ['Epic', 'Story', 'Task', 'Bug', 'Sub-task'],
            datasets: [{
                data: [
                    data.epic || 0,
                    data.story || 0,
                    data.task || 0,
                    data.bug || 0,
                    data.subtask || 0
                ],
                backgroundColor: ['#8b5cf6', '#22c55e', '#3b82f6', '#ef4444', '#06b6d4'],
                borderWidth: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        padding: 16,
                        usePointStyle: true,
                        pointStyleWidth: 10,
                        font: { size: 12 }
                    }
                }
            }
        }
    });
}

function renderCreatedVsResolvedChart(data) {
    const chartEl = document.getElementById('createdVsResolvedChart');
    if (!chartEl || !data.createdVsResolved) return;

    const labels = data.createdVsResolved.map(item => item.date);
    const created = data.createdVsResolved.map(item => item.created);
    const resolved = data.createdVsResolved.map(item => item.resolved);

    new Chart(chartEl, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [
                {
                    label: 'Created',
                    data: created,
                    borderColor: '#ef4444',
                    backgroundColor: 'rgba(239, 68, 68, 0.1)',
                    fill: true,
                    tension: 0.3
                },
                {
                    label: 'Resolved',
                    data: resolved,
                    borderColor: '#22c55e',
                    backgroundColor: 'rgba(34, 197, 94, 0.1)',
                    fill: true,
                    tension: 0.3
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        padding: 16,
                        usePointStyle: true,
                        font: { size: 12 }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: { stepSize: 1 },
                    grid: { color: 'rgba(0,0,0,0.04)' }
                },
                x: {
                    grid: { display: false }
                }
            }
        }
    });
}


/* ----- Sidebar Toggle (Mobile) ----- */
function initSidebarToggle() {
    const toggleBtn = document.querySelector('.sidebar-toggle');
    const sidebar = document.querySelector('.sidebar');
    const overlay = document.querySelector('.sidebar-overlay');

    if (!toggleBtn || !sidebar) return;

    toggleBtn.addEventListener('click', () => {
        sidebar.classList.toggle('open');
        if (overlay) {
            overlay.classList.toggle('show');
        }
    });

    if (overlay) {
        overlay.addEventListener('click', () => {
            sidebar.classList.remove('open');
            overlay.classList.remove('show');
        });
    }

    // Close sidebar on Escape key
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && sidebar.classList.contains('open')) {
            sidebar.classList.remove('open');
            if (overlay) overlay.classList.remove('show');
        }
    });
}


/* ----- Space Key Auto-Generator ----- */
function initSpaceKeyGenerator() {
    const nameInput = document.getElementById('spaceName');
    const keyInput = document.getElementById('spaceKey');

    if (!nameInput || !keyInput) return;

    let keyManuallyEdited = false;

    keyInput.addEventListener('input', () => {
        keyManuallyEdited = true;
    });

    nameInput.addEventListener('input', () => {
        if (keyManuallyEdited) return;

        const name = nameInput.value.trim();
        if (!name) {
            keyInput.value = '';
            return;
        }

        // Generate key: take uppercase first letters of each word,
        // or first 2-4 chars if single word
        const words = name.split(/\s+/).filter(w => w.length > 0);
        let key;

        if (words.length === 1) {
            // Single word: use first 3-4 uppercase chars
            key = words[0].substring(0, 4).toUpperCase();
        } else {
            // Multiple words: take first letter of each (up to 5)
            key = words.slice(0, 5).map(w => w[0]).join('').toUpperCase();
        }

        // Clean: only allow letters and digits
        key = key.replace(/[^A-Z0-9]/g, '');

        keyInput.value = key;
    });
}


/* ----- Filter Auto-Submit ----- */
function initFilterAutoSubmit() {
    const filterForm = document.querySelector('.filter-form');
    if (!filterForm) return;

    const selects = filterForm.querySelectorAll('select');
    selects.forEach(select => {
        select.addEventListener('change', () => {
            filterForm.submit();
        });
    });

    // Debounced search input
    const searchInput = filterForm.querySelector('input[type="search"], input[name="search"], .search-input');
    if (searchInput) {
        let searchTimeout;
        searchInput.addEventListener('input', () => {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => {
                filterForm.submit();
            }, 400);
        });
    }
}


/* ----- Delete Confirmation ----- */
function initDeleteConfirmation() {
    document.addEventListener('click', (e) => {
        const deleteBtn = e.target.closest('[data-confirm]');
        if (!deleteBtn) return;

        const message = deleteBtn.dataset.confirm || 'Are you sure you want to delete this item?';
        if (!confirm(message)) {
            e.preventDefault();
            e.stopImmediatePropagation();
        }
    });
}


/* ----- Flash Message Auto-Dismiss ----- */
function initFlashMessageDismiss() {
    const alerts = document.querySelectorAll('.alert');
    if (!alerts.length) return;

    alerts.forEach(alert => {
        // Auto-dismiss after 5 seconds
        setTimeout(() => {
            dismissAlert(alert);
        }, 5000);

        // Manual dismiss button
        const dismissBtn = alert.querySelector('.alert-dismiss-btn');
        if (dismissBtn) {
            dismissBtn.addEventListener('click', () => {
                dismissAlert(alert);
            });
        }
    });
}

function dismissAlert(alert) {
    alert.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
    alert.style.opacity = '0';
    alert.style.transform = 'translateY(-8px)';
    setTimeout(() => {
        alert.remove();
    }, 300);
}


/* ----- Dropdowns ----- */
function initDropdowns() {
    // Toggle dropdown on click
    document.addEventListener('click', (e) => {
        const toggle = e.target.closest('[data-dropdown-toggle]');

        if (toggle) {
            e.preventDefault();
            e.stopPropagation();
            const targetId = toggle.dataset.dropdownToggle;
            const menu = document.getElementById(targetId);
            if (menu) {
                // Close all other dropdowns
                document.querySelectorAll('.dropdown-menu.show').forEach(m => {
                    if (m !== menu) m.classList.remove('show');
                });
                menu.classList.toggle('show');
            }
            return;
        }

        // Close all dropdowns when clicking outside
        document.querySelectorAll('.dropdown-menu.show').forEach(menu => {
            menu.classList.remove('show');
        });
    });
}


/* ----- Modal Utilities ----- */
function openModal(modalId) {
    const overlay = document.getElementById(modalId);
    if (overlay) {
        overlay.classList.add('show');
        document.body.style.overflow = 'hidden';
    }
}

function closeModal(modalId) {
    const overlay = document.getElementById(modalId);
    if (overlay) {
        overlay.classList.remove('show');
        document.body.style.overflow = '';
    }
}

// Close modal on overlay click or Escape
document.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal-overlay')) {
        e.target.classList.remove('show');
        document.body.style.overflow = '';
    }
});

document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        const openModals = document.querySelectorAll('.modal-overlay.show');
        openModals.forEach(modal => {
            modal.classList.remove('show');
        });
        document.body.style.overflow = '';
    }
});


/* ----- Utility: CSRF Token Helper ----- */
function getCsrfToken() {
    const meta = document.querySelector('meta[name="_csrf"]');
    return meta ? meta.content : null;
}

function getCsrfHeaderName() {
    const meta = document.querySelector('meta[name="_csrf_header"]');
    return meta ? meta.content : 'X-CSRF-TOKEN';
}

/**
 * Make a fetch request with CSRF token included.
 */
function fetchWithCsrf(url, options) {
    const opts = options || {};
    const token = getCsrfToken();
    const headerName = getCsrfHeaderName();

    if (!opts.headers) {
        opts.headers = {};
    }

    if (token) {
        opts.headers[headerName] = token;
    }

    return fetch(url, opts);
}


/* ----- Utility: Format Date ----- */
function formatDate(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

function formatDateTime(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function timeAgo(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const seconds = Math.floor((now - date) / 1000);

    const intervals = [
        { label: 'year', seconds: 31536000 },
        { label: 'month', seconds: 2592000 },
        { label: 'week', seconds: 604800 },
        { label: 'day', seconds: 86400 },
        { label: 'hour', seconds: 3600 },
        { label: 'minute', seconds: 60 }
    ];

    for (const interval of intervals) {
        const count = Math.floor(seconds / interval.seconds);
        if (count >= 1) {
            return count === 1
                ? `1 ${interval.label} ago`
                : `${count} ${interval.label}s ago`;
        }
    }

    return 'just now';
}


/* ----- @Mention Autocomplete ----- */
function initMentionAutocomplete() {
    const textarea = document.getElementById('commentTextarea');
    const dropdown = document.getElementById('mentionDropdown');

    if (!textarea || !dropdown) return;

    const spaceKey = textarea.dataset.spaceKey;
    if (!spaceKey) return;

    let mentionActive = false;
    let mentionStartPos = -1;
    let activeIndex = 0;
    let currentMembers = [];
    let fetchController = null;

    textarea.addEventListener('input', () => {
        const cursorPos = textarea.selectionStart;
        const text = textarea.value;

        // Find the last '@' before the cursor that starts a mention
        const textBeforeCursor = text.substring(0, cursorPos);
        const lastAtIndex = textBeforeCursor.lastIndexOf('@');

        if (lastAtIndex === -1) {
            closeMentionDropdown();
            return;
        }

        // Check that '@' is at start of text or preceded by whitespace
        if (lastAtIndex > 0 && !/\s/.test(text[lastAtIndex - 1])) {
            closeMentionDropdown();
            return;
        }

        // Extract the query after '@'
        const query = textBeforeCursor.substring(lastAtIndex + 1);

        // If the query contains whitespace, the mention is complete
        if (/\s/.test(query)) {
            closeMentionDropdown();
            return;
        }

        mentionActive = true;
        mentionStartPos = lastAtIndex;
        activeIndex = 0;

        fetchMembers(spaceKey, query);
    });

    textarea.addEventListener('keydown', (e) => {
        if (!mentionActive || currentMembers.length === 0) return;

        if (e.key === 'ArrowDown') {
            e.preventDefault();
            activeIndex = (activeIndex + 1) % currentMembers.length;
            renderDropdown();
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            activeIndex = (activeIndex - 1 + currentMembers.length) % currentMembers.length;
            renderDropdown();
        } else if (e.key === 'Enter' || e.key === 'Tab') {
            if (mentionActive && currentMembers.length > 0) {
                e.preventDefault();
                selectMember(currentMembers[activeIndex]);
            }
        } else if (e.key === 'Escape') {
            e.preventDefault();
            closeMentionDropdown();
        }
    });

    // Close dropdown when clicking outside
    document.addEventListener('click', (e) => {
        if (!textarea.contains(e.target) && !dropdown.contains(e.target)) {
            closeMentionDropdown();
        }
    });

    function fetchMembers(spaceKey, query) {
        // Cancel any in-flight request
        if (fetchController) {
            fetchController.abort();
        }
        fetchController = new AbortController();

        const url = '/api/spaces/' + encodeURIComponent(spaceKey) + '/members/search?q=' + encodeURIComponent(query);

        fetch(url, { signal: fetchController.signal })
            .then(response => {
                if (!response.ok) throw new Error('Failed to fetch members');
                return response.json();
            })
            .then(members => {
                currentMembers = members;
                activeIndex = 0;
                if (members.length > 0) {
                    renderDropdown();
                    positionDropdown();
                    dropdown.style.display = 'block';
                } else {
                    closeMentionDropdown();
                }
            })
            .catch(err => {
                if (err.name !== 'AbortError') {
                    console.error('Error fetching members:', err);
                    closeMentionDropdown();
                }
            });
    }

    function renderDropdown() {
        // Build dropdown using safe DOM methods
        while (dropdown.firstChild) {
            dropdown.removeChild(dropdown.firstChild);
        }

        currentMembers.forEach((member, index) => {
            const item = document.createElement('div');
            item.className = 'mention-item' + (index === activeIndex ? ' active' : '');

            const avatar = document.createElement('div');
            avatar.className = 'mention-item-avatar';
            const displayName = member.displayName || member.username;
            avatar.textContent = displayName.charAt(0).toUpperCase();

            const info = document.createElement('div');
            info.className = 'mention-item-info';

            const nameEl = document.createElement('span');
            nameEl.className = 'mention-item-name';
            nameEl.textContent = displayName;

            const usernameEl = document.createElement('span');
            usernameEl.className = 'mention-item-username';
            usernameEl.textContent = '@' + member.username;

            info.appendChild(nameEl);
            info.appendChild(usernameEl);
            item.appendChild(avatar);
            item.appendChild(info);

            item.addEventListener('mouseenter', () => {
                activeIndex = index;
                renderDropdown();
            });

            item.addEventListener('mousedown', (e) => {
                e.preventDefault(); // Prevent blur on textarea
                selectMember(member);
            });

            dropdown.appendChild(item);
        });
    }

    function positionDropdown() {
        // Position the dropdown relative to the textarea wrapper
        // The dropdown is already inside the mention-input-wrapper, so
        // we just need to set its position below the textarea
        dropdown.style.top = (textarea.offsetHeight + 2) + 'px';
        dropdown.style.left = '0';
        dropdown.style.width = textarea.offsetWidth + 'px';
    }

    function selectMember(member) {
        const text = textarea.value;
        const beforeMention = text.substring(0, mentionStartPos);
        const afterCursor = text.substring(textarea.selectionStart);

        const insertText = '@' + member.username + ' ';
        textarea.value = beforeMention + insertText + afterCursor;

        // Set cursor position after the inserted mention
        const newCursorPos = mentionStartPos + insertText.length;
        textarea.setSelectionRange(newCursorPos, newCursorPos);
        textarea.focus();

        closeMentionDropdown();
    }

    function closeMentionDropdown() {
        mentionActive = false;
        mentionStartPos = -1;
        currentMembers = [];
        activeIndex = 0;
        dropdown.style.display = 'none';
        while (dropdown.firstChild) {
            dropdown.removeChild(dropdown.firstChild);
        }
    }
}
