(function () {
    'use strict';

    var documentRoot = document.documentElement;
    var dashboardData = document.getElementById('management-dashboard-data');
    var chartInstances = [];
    var latestDashboardRequest = 0;

    function number(name) {
        if (!dashboardData) {
            return 0;
        }
        var value = Number(dashboardData.dataset[name]);
        return Number.isFinite(value) ? value : 0;
    }

    function destroyCharts() {
        chartInstances.forEach(function (chart) {
            if (chart && typeof chart.destroy === 'function') {
                chart.destroy();
            }
        });
        chartInstances = [];
    }

    function renderCharts() {
        if (!window.ApexCharts || !dashboardData) {
            return;
        }

        var overviewTarget = document.getElementById('management-overview-chart');
        if (overviewTarget) {
            var overviewChart = new window.ApexCharts(overviewTarget, {
                series: [{
                    name: 'Indicator value',
                    data: [
                        number('courseCount'),
                        number('subjectCount'),
                        number('classGroupCount'),
                        number('enrollmentCount'),
                        number('lessonCount')
                    ]
                }],
                chart: {
                    height: 310,
                    type: 'line',
                    toolbar: {show: false},
                    zoom: {enabled: false}
                },
                colors: ['#066AC9'],
                dataLabels: {enabled: false},
                stroke: {curve: 'smooth', width: 3},
                markers: {size: 4},
                grid: {
                    borderColor: '#e7e7e7',
                    row: {colors: ['#f8fafc', 'transparent'], opacity: 0.6}
                },
                xaxis: {
                    categories: ['Courses', 'Subjects', 'Class Groups', 'Enrollments', 'Lessons'],
                    labels: {style: {colors: '#6b7280', fontSize: '12px'}}
                },
                yaxis: {
                    min: 0,
                    labels: {style: {colors: '#6b7280', fontSize: '12px'}}
                },
                tooltip: {theme: documentRoot.dataset.bsTheme === 'dark' ? 'dark' : 'light'}
            });
            chartInstances.push(overviewChart);
            overviewChart.render();
        }

        var distributionTarget = document.getElementById('management-distribution-chart');
        if (distributionTarget) {
            var distributionChart = new window.ApexCharts(distributionTarget, {
                series: [
                    number('assessmentCount'),
                    number('submittedAttemptCount'),
                    number('correctedAttemptCount')
                ],
                chart: {type: 'donut', height: 230},
                labels: ['Assessments', 'Submitted', 'Corrected'],
                colors: ['#066AC9', '#FFAB00', '#0FBF6A'],
                legend: {show: false},
                dataLabels: {enabled: false},
                stroke: {colors: ['#ffffff'], width: 3},
                plotOptions: {
                    pie: {
                        donut: {
                            size: '67%',
                            labels: {
                                show: true,
                                total: {
                                    show: true,
                                    label: 'Attempts',
                                    formatter: function () {
                                        return String(number('submittedAttemptCount') + number('correctedAttemptCount'));
                                    }
                                }
                            }
                        }
                    }
                },
                responsive: [{
                    breakpoint: 575,
                    options: {chart: {height: 210}}
                }]
            });
            chartInstances.push(distributionChart);
            distributionChart.render();
        }
    }

    function dashboardMain() {
        return document.getElementById('management-dashboard-main');
    }

    function setDashboardBusy(busy) {
        var main = dashboardMain();
        if (!main) {
            return;
        }
        main.classList.toggle('is-management-dashboard-loading', busy);
        main.classList.toggle(
            'is-management-dashboard-tab-loading',
            busy && !!document.querySelector('[data-management-tab-card].is-loading')
        );
        main.setAttribute('aria-busy', busy ? 'true' : 'false');
    }

    function clearManagementTabLoading() {
        Array.prototype.forEach.call(
            document.querySelectorAll('[data-management-tab-card].is-loading'),
            function (card) {
                card.classList.remove('is-loading');
                card.removeAttribute('aria-busy');
            }
        );
    }

    function markManagementTabLoading(card) {
        clearManagementTabLoading();
        if (!card) {
            return;
        }
        card.classList.add('is-loading');
        card.setAttribute('aria-busy', 'true');
    }

    function showFeedback(message, isError) {
        var feedback = document.querySelector('[data-management-async-feedback]');
        if (!feedback) {
            return;
        }
        feedback.hidden = false;
        feedback.className = 'gape-management-dashboard-feedback alert ' +
            (isError ? 'alert-danger' : 'alert-success') + ' mb-16';
        feedback.setAttribute('role', isError ? 'alert' : 'status');
        feedback.textContent = message;
    }

    function currentFilterState() {
        var typeFilter = document.querySelector('[data-management-type-filter]');
        var search = document.querySelector('[data-management-catalogue-search]');
        return {
            type: typeFilter ? typeFilter.value : 'all',
            search: search ? search.value : ''
        };
    }

    function withCurrentUrlSession(url) {
        var target = new window.URL(url, window.location.href);
        if (target.origin !== window.location.origin || /;jsessionid=/i.test(target.pathname)) {
            return target.toString();
        }
        var currentPath = window.location.pathname;
        var sessionStart = currentPath.toLowerCase().indexOf(';jsessionid=');
        if (sessionStart < 0) {
            return target.toString();
        }
        var sessionEnd = currentPath.indexOf('/', sessionStart);
        if (sessionEnd < 0) {
            sessionEnd = currentPath.length;
        }
        var contextPath = currentPath.slice(0, sessionStart);
        if (target.pathname === contextPath || target.pathname.indexOf(contextPath + '/') === 0) {
            target.pathname = contextPath + currentPath.slice(sessionStart, sessionEnd) +
                target.pathname.slice(contextPath.length);
        }
        return target.toString();
    }

    function urlWithViewId(baseUrl, viewId) {
        var url = new window.URL(baseUrl, window.location.href);
        if (viewId) {
            url.searchParams.set('viewId', viewId);
        } else {
            url.searchParams.delete('viewId');
        }
        return withCurrentUrlSession(url.toString());
    }

    function isDashboardUrl(url) {
        try {
            var path = new window.URL(url, window.location.href).pathname
                .replace(/;jsessionid=[^/]+/ig, '');
            return path.endsWith('/dashboard');
        } catch (error) {
            return false;
        }
    }

    function relativeDashboardUrl(url) {
        var parsed = new window.URL(url, window.location.href);
        parsed.searchParams.delete('fragment');
        return parsed.pathname + parsed.search + parsed.hash;
    }

    function navigateToLoginIfRequired(response) {
        if (response.redirected && !isDashboardUrl(response.url)) {
            window.location.assign(response.url);
            return true;
        }
        return false;
    }

    function parseDashboardDocument(html) {
        return new window.DOMParser().parseFromString(html, 'text/html');
    }

    function disposeModal(modal) {
        if (!modal || !window.bootstrap || !window.bootstrap.Modal ||
                typeof window.bootstrap.Modal.getInstance !== 'function') {
            return;
        }
        var instance = window.bootstrap.Modal.getInstance(modal);
        if (instance) {
            instance.dispose();
        }
    }

    function replaceOptionalNode(sourceDocument, id) {
        var current = document.getElementById(id);
        var replacement = sourceDocument.getElementById(id);
        if (current) {
            disposeModal(current);
            if (replacement) {
                current.parentNode.replaceChild(document.importNode(replacement, true), current);
            } else {
                current.remove();
            }
            return;
        }
        if (replacement) {
            document.body.appendChild(document.importNode(replacement, true));
        }
    }

    function replaceDashboardContent(sourceDocument, filterState) {
        var replacementMain = sourceDocument.getElementById('management-dashboard-main');
        var currentMain = dashboardMain();
        if (!replacementMain || !currentMain) {
            return false;
        }

        destroyCharts();
        currentMain.parentNode.replaceChild(document.importNode(replacementMain, true), currentMain);
        replaceOptionalNode(sourceDocument, 'managementViewModal');
        replaceOptionalNode(sourceDocument, 'managementRecipientsModal');
        replaceOptionalNode(sourceDocument, 'management-dashboard-data');
        dashboardData = document.getElementById('management-dashboard-data');
        bindDashboardControls(filterState);
        renderCharts();
        return true;
    }

    function updateHistory(url, mode) {
        if (mode === 'none') {
            return;
        }
        var target = relativeDashboardUrl(url);
        if (mode === 'replace') {
            window.history.replaceState({managementDashboard: true}, '', target);
            return;
        }
        window.history.pushState({managementDashboard: true}, '', target);
    }

    function focusSelectedPanel() {
        var heading = document.getElementById('selected-panel-heading');
        if (!heading) {
            return;
        }
        heading.setAttribute('tabindex', '-1');
        heading.focus({preventScroll: true});
        heading.scrollIntoView({behavior: 'smooth', block: 'start'});
    }

    function loadDashboard(url, options) {
        var settings = options || {};
        var requestUrl = withCurrentUrlSession(url);
        if (!window.fetch || !window.DOMParser) {
            window.location.assign(requestUrl);
            return;
        }

        var requestId = ++latestDashboardRequest;
        var filters = currentFilterState();
        setDashboardBusy(true);

        window.fetch(requestUrl, {
            headers: {
                'X-Requested-With': 'XMLHttpRequest',
                'Accept': 'text/html'
            },
            credentials: 'same-origin'
        }).then(function (response) {
            if (navigateToLoginIfRequired(response)) {
                return null;
            }
            if (!response.ok) {
                throw new Error('The dashboard could not be loaded.');
            }
            return response.text().then(function (html) {
                return {html: html, url: response.url};
            });
        }).then(function (payload) {
            if (!payload || requestId !== latestDashboardRequest) {
                return;
            }
            var sourceDocument = parseDashboardDocument(payload.html);
            if (!replaceDashboardContent(sourceDocument, filters)) {
                // A stale session, proxy response or server-side redirect can
                // occasionally return a valid HTML document without the
                // dashboard shell.  A full navigation is the safe recovery;
                // never leave the user with a misleading fatal toast.
                window.location.assign(payload.url || requestUrl);
                return;
            }
            updateHistory(payload.url, settings.history || 'push');
            if (settings.focusSelected) {
                focusSelectedPanel();
            }
        }).catch(function (error) {
            if (requestId === latestDashboardRequest) {
                clearManagementTabLoading();
                showFeedback(error.message || 'The dashboard could not be loaded.', true);
            }
        }).then(function () {
            if (requestId === latestDashboardRequest) {
                setDashboardBusy(false);
            }
        });
    }

    function bindPanelSelection() {
        var form = document.querySelector('[data-management-panel-form]');
        var select = document.querySelector('[data-management-view-select]');
        if (!form || !select) {
            return;
        }

        var openSelectedView = function (event) {
            if (event) {
                event.preventDefault();
            }
            if (!select.value) {
                showFeedback('No panel is available in your current scope.', true);
                return;
            }
            loadDashboard(urlWithViewId(form.action, select.value), {history: 'push'});
        };

        form.addEventListener('submit', openSelectedView);
        select.addEventListener('change', openSelectedView);
    }

    function isPrimaryNavigation(event) {
        return !event.defaultPrevented
            // Native primary clicks use 0.  HTMLElement.click() and assistive
            // activation may expose -1 or no button, but are still a normal
            // same-tab activation and must preserve the asynchronous route.
            && (typeof event.button === 'undefined' || event.button <= 0)
            && !event.metaKey
            && !event.ctrlKey
            && !event.shiftKey
            && !event.altKey;
    }

    function bindDashboardTabs() {
        Array.prototype.forEach.call(document.querySelectorAll('[data-management-tab]'), function (link) {
            link.addEventListener('click', function (event) {
                if (!isPrimaryNavigation(event)) {
                    return;
                }
                event.preventDefault();
                markManagementTabLoading(link);
                loadDashboard(link.href, {history: 'push'});
            });
        });
    }

    function auditUrlForForm(form) {
        var url = new window.URL(form.action, window.location.href);
        var values = new window.FormData(form);
        url.search = '';
        values.forEach(function (value, key) {
            url.searchParams.append(key, value);
        });
        return withCurrentUrlSession(url.toString());
    }

    function bindAuditFilters() {
        var form = document.querySelector('[data-management-audit-form]');
        if (!form || !window.fetch || !window.DOMParser || !window.FormData) {
            return;
        }

        var applyFilters = function (event) {
            if (event) {
                event.preventDefault();
            }
            loadDashboard(auditUrlForForm(form), {history: 'push'});
        };
        var delayedApply;
        var scheduleFilters = function () {
            if (delayedApply) {
                window.clearTimeout(delayedApply);
            }
            delayedApply = window.setTimeout(function () {
                applyFilters();
            }, 420);
        };

        form.addEventListener('submit', applyFilters);
        Array.prototype.forEach.call(form.querySelectorAll('[data-management-audit-filter]'), function (field) {
            if (field.tagName === 'SELECT') {
                field.addEventListener('change', applyFilters);
                return;
            }
            field.addEventListener('input', scheduleFilters);
            field.addEventListener('change', scheduleFilters);
        });
    }

    function bindCatalogueOpeners() {
        Array.prototype.forEach.call(document.querySelectorAll('[data-management-open-view]'), function (link) {
            link.addEventListener('click', function (event) {
                if (!isPrimaryNavigation(event)) {
                    return;
                }
                event.preventDefault();
                loadDashboard(link.href, {history: 'push', focusSelected: true});
            });
        });
    }

    function bindCatalogueFilter(initialState) {
        var typeFilter = document.querySelector('[data-management-type-filter]');
        var search = document.querySelector('[data-management-catalogue-search]');
        var rows = Array.prototype.slice.call(document.querySelectorAll('[data-management-panel-row]'));
        var noResults = document.querySelector('[data-management-no-results]');
        if (!typeFilter || !rows.length) {
            return;
        }

        if (initialState) {
            typeFilter.value = initialState.type || 'all';
            if (search) {
                search.value = initialState.search || '';
            }
        }

        var applyFilter = function () {
            var type = typeFilter.value || 'all';
            var needle = (search ? search.value : '').trim().toLowerCase();
            var visibleCount = 0;
            rows.forEach(function (row) {
                var matchesType = type === 'all' || row.dataset.managementType === type;
                var matchesText = !needle || (row.dataset.managementSearch || '').indexOf(needle) !== -1;
                var visible = matchesType && matchesText;
                row.hidden = !visible;
                if (visible) {
                    visibleCount += 1;
                }
            });
            if (noResults) {
                noResults.hidden = visibleCount !== 0;
            }
        };

        typeFilter.addEventListener('change', applyFilter);
        if (search) {
            search.addEventListener('input', applyFilter);
        }
        applyFilter();
    }

    function bindConfigurationModal() {
        var form = document.querySelector('[data-management-view-form]');
        if (!form) {
            return;
        }
        var operation = form.querySelector('[data-management-operation]');
        var id = form.querySelector('[data-management-view-id]');
        var title = form.querySelector('[data-management-title]');
        var description = form.querySelector('[data-management-description]');
        var type = form.querySelector('[data-management-type]');
        var scope = form.querySelector('[data-management-scope]');
        var scopeTarget = form.querySelector('[data-management-scope-target]');
        var targetField = form.querySelector('[data-management-target-field]');
        var targetHint = form.querySelector('[data-management-target-hint]');
        var state = form.querySelector('[data-management-state]');
        var modalTitle = form.querySelector('[data-management-modal-title]');
        var saveLabel = form.querySelector('[data-management-save-label]');

        var syncScopeTarget = function (requestedTarget) {
            if (!scope || !scopeTarget) {
                return;
            }
            var selected = scope.options[scope.selectedIndex];
            var requiresTarget = selected && selected.dataset.managementTargetRequired === 'true';
            Array.prototype.forEach.call(scopeTarget.options, function (option) {
                var targetScope = option.dataset.managementScopeTargetOption;
                if (!targetScope) {
                    option.hidden = requiresTarget;
                    return;
                }
                option.hidden = targetScope !== scope.value;
            });
            scopeTarget.required = requiresTarget;
            if (targetField) {
                targetField.classList.toggle('d-none', !requiresTarget);
            }
            if (requiresTarget) {
                var requested = requestedTarget || scopeTarget.value;
                var option = Array.prototype.slice.call(scopeTarget.options).find(function (candidate) {
                    return candidate.value === String(requested) && !candidate.hidden;
                }) || Array.prototype.slice.call(scopeTarget.options).find(function (candidate) {
                    return candidate.value && !candidate.hidden;
                });
                scopeTarget.value = option ? option.value : '';
                if (targetHint) {
                    targetHint.textContent = 'Only contexts currently assigned to your profile are available.';
                }
            } else {
                scopeTarget.value = '';
                if (targetHint) {
                    targetHint.textContent = scope.value === 'personal'
                        ? 'Personal panels are always tied to your own account.'
                        : 'This scope has no target.';
                }
            }
        };

        scope.addEventListener('change', function () {
            syncScopeTarget(null);
        });

        Array.prototype.forEach.call(document.querySelectorAll('[data-management-edit]'), function (button) {
            button.addEventListener('click', function () {
                operation.value = 'update';
                id.value = button.dataset.id || '';
                title.value = button.dataset.title || '';
                description.value = button.dataset.description || '';
                type.value = button.dataset.type || 'dashboard';
                scope.value = button.dataset.scope || scope.value;
                state.value = button.dataset.state || 'active';
                modalTitle.textContent = 'Configure panel';
                saveLabel.textContent = 'Save changes';
                syncScopeTarget(button.dataset.scopeTargetId || null);
            });
        });

        syncScopeTarget(null);
    }

    function closeModal(form) {
        var modal = form.closest ? form.closest('.modal') : null;
        if (!modal || !modal.classList.contains('show') || !window.bootstrap || !window.bootstrap.Modal) {
            return Promise.resolve();
        }
        return new Promise(function (resolve) {
            var completed = false;
            var finish = function () {
                if (!completed) {
                    completed = true;
                    resolve();
                }
            };
            modal.addEventListener('hidden.bs.modal', finish, {once: true});
            window.setTimeout(finish, 350);
            window.bootstrap.Modal.getOrCreateInstance(modal).hide();
        });
    }

    function setFormSubmitting(form, submitting) {
        form.dataset.managementSubmitting = submitting ? 'true' : 'false';
        Array.prototype.forEach.call(form.querySelectorAll('[data-management-submit], button[type="submit"]'), function (button) {
            button.disabled = submitting;
            button.setAttribute('aria-busy', submitting ? 'true' : 'false');
        });
    }

    function encodedFormBody(form) {
        var values = new window.URLSearchParams();
        new window.FormData(form).forEach(function (value, key) {
            values.append(key, value);
        });
        return values.toString();
    }

    function bindAsyncForms() {
        Array.prototype.forEach.call(document.querySelectorAll('[data-management-async-form]'), function (form) {
            form.addEventListener('submit', function (event) {
                event.preventDefault();
                if (form.dataset.managementSubmitting === 'true') {
                    return;
                }
                if (!window.fetch || !window.DOMParser || !window.FormData || !window.URLSearchParams) {
                    form.submit();
                    return;
                }

                setFormSubmitting(form, true);
                setDashboardBusy(true);
                window.fetch(withCurrentUrlSession(form.action), {
                    method: 'POST',
                    headers: {
                        'X-Requested-With': 'XMLHttpRequest',
                        'Accept': 'text/html',
                        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
                    },
                    body: encodedFormBody(form),
                    credentials: 'same-origin'
                }).then(function (response) {
                    if (navigateToLoginIfRequired(response)) {
                        return null;
                    }
                    if (!response.ok) {
                        throw new Error('The panel configuration could not be saved.');
                    }
                    return response.text().then(function (html) {
                        return {html: html, url: response.url};
                    });
                }).then(function (payload) {
                    if (!payload) {
                        return;
                    }
                    var filters = currentFilterState();
                    return closeModal(form).then(function () {
                        var sourceDocument = parseDashboardDocument(payload.html);
                        if (!replaceDashboardContent(sourceDocument, filters)) {
                            window.location.assign(payload.url || form.action);
                            return;
                        }
                        updateHistory(payload.url, 'replace');
                    });
                }).catch(function (error) {
                    showFeedback(error.message || 'The panel configuration could not be saved.', true);
                    setFormSubmitting(form, false);
                }).then(function () {
                    setDashboardBusy(false);
                });
            });
        });
    }

    function bindDashboardControls(filterState) {
        bindDashboardTabs();
        bindAuditFilters();
        bindPanelSelection();
        bindCatalogueOpeners();
        bindCatalogueFilter(filterState);
        bindConfigurationModal();
        bindAsyncForms();
    }

    window.addEventListener('popstate', function () {
        loadDashboard(window.location.href, {history: 'none'});
    });

    renderCharts();
    bindDashboardControls();
}());
