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

    // Tom Select - User Search
    document.querySelectorAll('.user-search-select').forEach(function(el) {
        console.log('Initializing User Search Select:', el);
        
        const ts = new TomSelect(el, {
            valueField: 'username',
            labelField: 'username',
            searchField: ['username', 'displayName'],
            openOnFocus: true,
            shouldLoad: function(query) {
                return true;
            },
            onFocus: function() {
                console.log('User Search Focus - triggering load');
                this.load('');
            },
            load: function(query, callback) {
                console.log('Loading users for query:', query);
                fetch('/api/users/search?q=' + encodeURIComponent(query))
                    .then(response => {
                        console.log('API Response status:', response.status);
                        return response.json();
                    })
                    .then(json => {
                        console.log('Users found:', json.length);
                        callback(json);
                    }).catch(err => {
                        console.error('User search failed:', err);
                        callback();
                    });
            },
            render: {
                option: function(item, escape) {
                    return `<div class="py-2 px-3">
                                <div class="fw-bold">${escape(item.displayName)}</div>
                                <div class="small text-muted">@${escape(item.username)}</div>
                            </div>`;
                },
                item: function(item, escape) {
                    return `<div>${escape(item.displayName)} (@${escape(item.username)})</div>`;
                }
            }
        });
    });
});
