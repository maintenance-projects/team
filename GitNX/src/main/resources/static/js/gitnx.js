// GitNX Custom JavaScript
document.addEventListener('DOMContentLoaded', function() {
    console.log('GitNX JS Loaded');

    // Auto-dismiss alerts
    document.querySelectorAll('.alert:not(.alert-permanent)').forEach(function(alert) {
        setTimeout(function() {
            var bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            bsAlert.close();
        }, 5000);
    });

    // Tom Select - User Search (Settings)
    const userSelectEl = document.getElementById('userSearchSelect');
    if (userSelectEl) {
        console.log('Initializing User Search Select:', userSelectEl);
        new TomSelect(userSelectEl, {
            valueField: 'username',
            labelField: 'username',
            searchField: ['username', 'displayName'],
            maxItems: 1,
            load: function(query, callback) {
                const url = '/api/users/search?q=' + encodeURIComponent(query);
                fetch(url)
                    .then(response => response.json())
                    .then(json => {
                        callback(json);
                    }).catch(() => {
                        callback();
                    });
            },
            render: {
                option: function(item, escape) {
                    return `<div class="py-2 px-3">
                                <div class="fw-bold text-dark">${escape(item.displayName || item.username)}</div>
                                <div class="small text-muted">@${escape(item.username)}</div>
                            </div>`;
                },
                item: function(item, escape) {
                    return `<div class="py-1 px-2">
                                <span class="fw-bold">${escape(item.displayName || item.username)}</span>
                                <span class="small text-muted ms-1">@${escape(item.username)}</span>
                            </div>`;
                }
            }
        });
    }

    // Profile Modal - Refresh data on show
    const profileModal = document.getElementById('profileModal');
    if (profileModal) {
        profileModal.addEventListener('show.bs.modal', function() {
            fetch('/api/users/me')
                .then(response => response.json())
                .then(data => {
                    document.getElementById('modalDisplayName').textContent = data.displayName || data.username;
                    document.getElementById('modalUsername').textContent = '@' + data.username;
                    document.getElementById('modalEmail').textContent = data.email;
                })
                .catch(err => console.error('Failed to fetch profile:', err));
        });
    }
});
