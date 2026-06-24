(function () {
    var POLL_INTERVAL_MS = 5000;
    var syncInFlight = false;

    function ready(callback) {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', callback);
            return;
        }
        callback();
    }

    function isStudentEnrollmentForm(form) {
        if (!form || String(form.method || '').toLowerCase() !== 'post') {
            return false;
        }
        if (!document.querySelector('.gape-student-dashboard-main')) {
            return false;
        }
        try {
            var url = new URL(form.action, window.location.href);
            return url.pathname.indexOf('/student/enrollments/') >= 0;
        } catch (ignored) {
            return false;
        }
    }

    function studentEnrollmentActionUrl(action) {
        var url = new URL(action, window.location.href);
        var sessionMatch = window.location.pathname.match(/^(\/[^/]+);jsessionid=([^/]+)\//i);
        if (sessionMatch && url.pathname.indexOf(sessionMatch[1] + '/') === 0) {
            url.pathname = sessionMatch[1] + ';jsessionid=' + sessionMatch[2] + url.pathname.substring(sessionMatch[1].length);
        }
        return url.href;
    }

    function studentEnrollmentBasePath() {
        var form = document.querySelector('form[action*="/student/enrollments/"]');
        var path = '';
        if (form) {
            try {
                path = new URL(form.action, window.location.href).pathname;
            } catch (ignored) {
                path = '';
            }
        }
        if (!path) {
            path = window.location.pathname;
        }
        path = path.replace(/;jsessionid=[^/]+/i, '');
        var marker = path.indexOf('/student/');
        return marker >= 0 ? path.substring(0, marker) : '';
    }

    function studentEnrollmentUpdatesUrl(targets) {
        var url = new URL(studentEnrollmentBasePath() + '/student/enrollments/updates', window.location.origin);
        targets.forEach(function (target) {
            url.searchParams.append('target', target);
        });
        return studentEnrollmentActionUrl(url.href);
    }

    function setBusy(form, busy) {
        form.dataset.gapeStudentEnrollmentBusy = busy ? 'true' : 'false';
        form.querySelectorAll('button, input[type="submit"]').forEach(function (control) {
            if (busy) {
                control.dataset.gapeOriginalText = control.textContent || control.value || '';
                control.disabled = true;
                if (control.tagName === 'BUTTON') {
                    control.textContent = 'Updating...';
                }
            } else {
                control.disabled = false;
                if (control.dataset.gapeOriginalText && control.tagName === 'BUTTON') {
                    control.textContent = control.dataset.gapeOriginalText;
                }
                delete control.dataset.gapeOriginalText;
            }
        });
    }

    function updateBadge(scope, update) {
        scope.querySelectorAll('[data-gape-enrollment-badge]').forEach(function (badge) {
            var fixedClasses = badge.getAttribute('data-gape-enrollment-badge-fixed') || '';
            badge.className = [update.badgeClass || '', fixedClasses].filter(Boolean).join(' ');
            badge.textContent = update.stateLabel || '';
        });
    }

    function updateActions(scope, update) {
        scope.querySelectorAll('[data-gape-enrollment-actions]').forEach(function (actions) {
            var kind = actions.getAttribute('data-gape-enrollment-actions-kind') || '';
            if (!update.actions || !Object.prototype.hasOwnProperty.call(update.actions, kind)) {
                return;
            }
            var target = actions.querySelector('[data-gape-enrollment-action-items]') || actions;
            target.innerHTML = update.actions[kind] || '';
        });
    }

    function enrollmentTargetScopes(target) {
        if (window.CSS && typeof window.CSS.escape === 'function') {
            return document.querySelectorAll('[data-gape-enrollment-target="' + CSS.escape(target) + '"]');
        }
        return Array.prototype.filter.call(
            document.querySelectorAll('[data-gape-enrollment-target]'),
            function (scope) {
                return scope.getAttribute('data-gape-enrollment-target') === target;
            }
        );
    }

    function applyEnrollmentUpdates(data) {
        (data.updates || []).forEach(function (update) {
            if (!update.target) {
                return;
            }
            Array.prototype.forEach.call(enrollmentTargetScopes(update.target), function (scope) {
                updateBadge(scope, update);
                updateActions(scope, update);
            });
        });
    }

    function collectEnrollmentTargets() {
        var seen = {};
        var targets = [];
        document.querySelectorAll('[data-gape-enrollment-target]').forEach(function (scope) {
            var target = scope.getAttribute('data-gape-enrollment-target') || '';
            if (target && !seen[target]) {
                seen[target] = true;
                targets.push(target);
            }
        });
        return targets;
    }

    function syncVisibleEnrollmentTargets() {
        if (syncInFlight || document.hidden || !document.querySelector('.gape-student-dashboard-main')) {
            return Promise.resolve();
        }
        var targets = collectEnrollmentTargets();
        if (!targets.length) {
            return Promise.resolve();
        }

        syncInFlight = true;
        return fetch(studentEnrollmentUpdatesUrl(targets), {
            method: 'GET',
            credentials: 'same-origin',
            cache: 'no-store',
            headers: {
                'Accept': 'application/json',
                'X-Requested-With': 'fetch'
            }
        }).then(function (response) {
            return response.json().catch(function () {
                return null;
            }).then(function (data) {
                if (response.ok && data && data.success !== false) {
                    applyEnrollmentUpdates(data);
                }
            });
        }).catch(function () {
            // Polling should never interrupt the student's current page.
        }).finally(function () {
            syncInFlight = false;
        });
    }

    function showFeedback(message, success) {
        if (!message) {
            return;
        }
        var main = document.querySelector('.gape-student-dashboard-main');
        if (!main) {
            return;
        }
        var feedback = main.querySelector('[data-gape-enrollment-feedback]');
        if (!feedback) {
            feedback = document.createElement('div');
            feedback.setAttribute('data-gape-enrollment-feedback', '');
            main.prepend(feedback);
        }
        feedback.className = success
            ? 'alert alert-success border-0 rounded-10 px-16 py-12 text-14 mb-16'
            : 'alert alert-danger border-0 rounded-10 px-16 py-12 text-14 mb-16';
        feedback.textContent = message;
    }

    ready(function () {
        document.addEventListener('submit', function (event) {
            var form = event.target;
            if (!isStudentEnrollmentForm(form) || form.dataset.gapeStudentEnrollmentBusy === 'true') {
                return;
            }

            event.preventDefault();
            setBusy(form, true);

            fetch(studentEnrollmentActionUrl(form.action), {
                method: 'POST',
                body: new URLSearchParams(new FormData(form)),
                credentials: 'same-origin',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                    'X-Requested-With': 'fetch'
                }
            }).then(function (response) {
                return response.json().catch(function () {
                    return null;
                }).then(function (data) {
                    if (!response.ok || !data || data.success === false) {
                        throw new Error(data && data.message ? data.message : 'Enrollment update failed');
                    }
                    applyEnrollmentUpdates(data);
                    showFeedback(data.message, true);
                    window.setTimeout(syncVisibleEnrollmentTargets, 300);
                });
            }).catch(function (error) {
                setBusy(form, false);
                showFeedback(error && error.message ? error.message : 'Enrollment update failed', false);
            });
        }, true);

        syncVisibleEnrollmentTargets();
        window.setInterval(syncVisibleEnrollmentTargets, POLL_INTERVAL_MS);
        document.addEventListener('visibilitychange', function () {
            if (!document.hidden) {
                syncVisibleEnrollmentTargets();
            }
        });
    });
})();
