// GitNX Custom JavaScript
document.addEventListener('DOMContentLoaded', function() {
    // Auto-dismiss alerts after 5 seconds
    document.querySelectorAll('.alert:not(.alert-permanent)').forEach(function(alert) {
        setTimeout(function() {
            var bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            bsAlert.close();
        }, 5000);
    });

    // Tom Select - User Search
    document.querySelectorAll('.user-search-select').forEach(function(el) {
        new TomSelect(el, {
            valueField: 'username',
            labelField: 'username',
            searchField: ['username', 'displayName'],
            openOnFocus: true,
            shouldLoad: function(query) {
                return true;
            },
            load: function(query, callback) {
                fetch('/api/users/search?q=' + encodeURIComponent(query))
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
