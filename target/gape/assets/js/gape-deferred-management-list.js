/*
 * Shared management-list contract.
 *
 * Lessons and Assessments already use a ten-item asynchronous paginator and a
 * deferred archive.  This component gives Attendance, Enrollments, Grades and
 * Certificates the same interaction: the visible panel stays open, pages are
 * requested in the background and archived work is fetched only after its
 * archive row is opened.
 */
(function (window, document) {
    'use strict';

    function withCurrentSessionUrl(rawUrl) {
        var target = new URL(rawUrl, window.location.href);
        var match = window.location.pathname.match(/^(\/[^/]+)(;jsessionid=[^/]+)/i);
        if (match && target.origin === window.location.origin && target.pathname.indexOf(match[2]) === -1
                && target.pathname.indexOf(match[1] + '/') === 0) {
            target.pathname = match[1] + match[2] + target.pathname.substring(match[1].length);
        }
        return target.toString();
    }

    function element(tag, className, text) {
        var node = document.createElement(tag);
        if (className) { node.className = className; }
        if (text !== undefined) { node.textContent = text; }
        return node;
    }

    function numberValue(value, fallback) {
        var parsed = Number(value);
        return Number.isFinite(parsed) ? parsed : fallback;
    }

    function pageItems(total, currentPage) {
        if (total <= 5) {
            return Array.from({ length: total }, function (_, index) { return index + 1; });
        }
        if (currentPage <= 3) { return [1, 2, 3, 'ellipsis', total]; }
        if (currentPage >= total - 2) { return [1, 'ellipsis', total - 2, total - 1, total]; }
        return [1, 'ellipsis', currentPage - 1, currentPage, currentPage + 1, 'ellipsis', total];
    }

    function importChildren(target, source) {
        target.replaceChildren();
        Array.prototype.slice.call(source.childNodes).forEach(function (node) {
            target.appendChild(document.importNode(node, true));
        });
    }

    function setArchiveToggle(button, open) {
        if (!button) { return; }
        button.setAttribute('aria-expanded', String(open));
        button.setAttribute('title', open ? 'Hide archived items' : 'Show archived items');
        button.setAttribute('aria-label', button.getAttribute('title'));
        var icon = button.querySelector('i');
        if (icon) { icon.className = 'ph ' + (open ? 'ph-caret-up' : 'ph-caret-down'); }
    }

    function initializeRoot(root) {
        if (!root || root.dataset.deferredManagementReady === 'true') {
            return;
        }
        root.dataset.deferredManagementReady = 'true';

        var endpoint = root.dataset.deferredManagementEndpoint || window.location.href;
        var kind = root.dataset.deferredManagementKind || 'management';
        var pageParameter = root.dataset.deferredManagementPageParameter || 'page';
        var scopeParameter = root.dataset.deferredManagementScopeParameter || 'scope';
        var loadAllParameter = root.dataset.deferredManagementLoadAllParameter || 'loadAll';
        var activeScope = root.dataset.deferredManagementActiveScope || 'active';
        var archiveScope = root.dataset.deferredManagementArchiveScope || '';
        var filterName = root.dataset.deferredManagementFilterName || '';
        var filterValue = root.dataset.deferredManagementFilterValue || '';
        var activeContent = root.querySelector('[data-deferred-management-active-content]');
        var archive = root.querySelector('[data-deferred-management-archive]');
        var archivePanel = archive ? archive.querySelector('[data-deferred-management-archive-panel]') : null;
        var archiveContent = archive ? archive.querySelector('[data-deferred-management-archive-content]') : null;
        var archiveButton = archive ? archive.querySelector('[data-deferred-management-archive-toggle]') : null;
        var pagination = root.querySelector('[data-deferred-management-pagination]');
        var paginationPages = root.querySelector('[data-deferred-management-pagination-pages]');
        var loadAllButton = root.querySelector('[data-deferred-management-load-all]');
        var loading = false;

        if (!activeContent) {
            return;
        }

        function total() {
            return numberValue(root.dataset.deferredManagementTotal, 0);
        }

        function pageSize() {
            return Math.max(1, numberValue(root.dataset.deferredManagementPageSize, 10));
        }

        function currentPage() {
            return Math.max(1, numberValue(root.dataset.deferredManagementCurrentPage, 1));
        }

        function showingAll() {
            return root.dataset.deferredManagementShowingAll === 'true';
        }

        function pageButton(page, icon, label, disabled) {
            var button = element('button', 'gape-list-pagination-button' + (icon ? ' gape-list-pagination-arrow' : ''));
            button.type = 'button';
            button.disabled = disabled || loading;
            if (icon) {
                var iconNode = element('i', 'ph ' + icon);
                iconNode.setAttribute('aria-hidden', 'true');
                button.appendChild(iconNode);
            } else {
                button.textContent = String(page);
                if (!showingAll() && page === currentPage()) {
                    button.classList.add('is-active');
                    button.setAttribute('aria-current', 'page');
                }
            }
            if (label) { button.setAttribute('aria-label', label); }
            button.addEventListener('click', function () { requestActivePage(page, false); });
            return button;
        }

        function renderPagination() {
            if (!pagination || !paginationPages) { return; }
            var pages = Math.ceil(total() / pageSize());
            var visible = pages > 1;
            pagination.hidden = !visible;
            if (!visible) { return; }
            paginationPages.replaceChildren();
            paginationPages.appendChild(pageButton(currentPage() - 1, 'ph-caret-left', 'Previous page', showingAll() || currentPage() <= 1));
            pageItems(pages, currentPage()).forEach(function (item) {
                if (item === 'ellipsis') {
                    paginationPages.appendChild(element('span', 'gape-list-pagination-ellipsis', '...'));
                } else {
                    paginationPages.appendChild(pageButton(item, null, null, false));
                }
            });
            paginationPages.appendChild(pageButton(currentPage() + 1, 'ph-caret-right', 'Next page', showingAll() || currentPage() >= pages));
            if (loadAllButton) {
                loadAllButton.disabled = loading || showingAll();
                loadAllButton.textContent = loading ? 'Loading...' : 'Load All';
            }
        }

        function requestUrl(scope, page, loadAll) {
            var url = new URL(endpoint, window.location.href);
            url.searchParams.set('fragment', 'deferred-management');
            url.searchParams.set(scopeParameter, scope);
            url.searchParams.set(pageParameter, String(page));
            if (filterName && filterValue) {
                url.searchParams.set(filterName, filterValue);
            }
            if (loadAll) {
                url.searchParams.set(loadAllParameter, 'true');
            } else {
                url.searchParams.delete(loadAllParameter);
            }
            return withCurrentSessionUrl(url);
        }

        function responseRootFor(html) {
            var parsed = new window.DOMParser().parseFromString(html, 'text/html');
            var roots = Array.prototype.slice.call(parsed.querySelectorAll('[data-deferred-management-root]'));
            var responseRoot = roots.find(function (candidate) {
                return candidate.dataset.deferredManagementKind === kind;
            });
            if (!responseRoot) {
                throw new Error('Invalid management-list response.');
            }
            var source = responseRoot.querySelector('[data-deferred-management-page-content]');
            if (!source) {
                throw new Error('Management-list response has no items.');
            }
            return { root: responseRoot, source: source };
        }

        function resetInteractiveInitializers(clone) {
            clone.removeAttribute('data-deferred-management-ready');
            clone.removeAttribute('data-gape-sort-group-ready');
            clone.querySelectorAll('[data-gape-sort-group-ready]').forEach(function (node) {
                node.removeAttribute('data-gape-sort-group-ready');
            });
            clone.querySelectorAll('[data-learning-list-ready]').forEach(function (node) {
                node.removeAttribute('data-learning-list-ready');
            });
        }

        function refreshClientEnhancements(clone) {
            if (window.GapeDateTimeFormat && typeof window.GapeDateTimeFormat.formatWithin === 'function') {
                window.GapeDateTimeFormat.formatWithin(clone);
            }
            if (window.GapeSortGroupControls && typeof window.GapeSortGroupControls.initAll === 'function') {
                window.GapeSortGroupControls.initAll();
            }
            if (window.GapeLessonListControls && typeof window.GapeLessonListControls.initialize === 'function') {
                window.GapeLessonListControls.initialize();
            }
            if (window.GapeGradeWeightForms && typeof window.GapeGradeWeightForms.initialize === 'function') {
                window.GapeGradeWeightForms.initialize();
            }
        }

        function replaceFromResponse(response, bucket) {
            var clone = root.cloneNode(true);
            resetInteractiveInitializers(clone);
            if (bucket === 'archive') {
                var nextArchive = clone.querySelector('[data-deferred-management-archive]');
                var nextArchiveContent = nextArchive && nextArchive.querySelector('[data-deferred-management-archive-content]');
                var nextArchivePanel = nextArchive && nextArchive.querySelector('[data-deferred-management-archive-panel]');
                var nextArchiveButton = nextArchive && nextArchive.querySelector('[data-deferred-management-archive-toggle]');
                if (!nextArchiveContent || !nextArchivePanel) {
                    throw new Error('The archive is not available.');
                }
                importChildren(nextArchiveContent, response.source);
                nextArchive.dataset.deferredManagementArchiveLoaded = 'true';
                nextArchivePanel.classList.remove('d-none');
                setArchiveToggle(nextArchiveButton, true);
            } else {
                var nextActiveContent = clone.querySelector('[data-deferred-management-active-content]');
                if (!nextActiveContent) {
                    throw new Error('The active item list is not available.');
                }
                importChildren(nextActiveContent, response.source);
                clone.dataset.deferredManagementTotal = response.root.dataset.deferredManagementTotal || '0';
                clone.dataset.deferredManagementCurrentPage = response.root.dataset.deferredManagementCurrentPage || '1';
                clone.dataset.deferredManagementShowingAll = response.root.dataset.deferredManagementShowingAll || 'false';
            }
            root.replaceWith(clone);
            refreshClientEnhancements(clone);
            initializeRoot(clone);
        }

        function request(scope, page, all, bucket) {
            if (loading) { return; }
            loading = true;
            renderPagination();
            fetch(requestUrl(scope, page, all), {
                credentials: 'same-origin',
                headers: { 'Accept': 'text/html', 'X-Requested-With': 'XMLHttpRequest' }
            }).then(function (response) {
                if (!response.ok) { throw new Error('Could not load the requested items.'); }
                return response.text();
            }).then(function (html) {
                replaceFromResponse(responseRootFor(html), bucket);
            }).catch(function () {
                if (bucket === 'archive' && archiveContent) {
                    archiveContent.replaceChildren(element('div', 'gape-learning-management-deferred-copy text-13 text-danger-600', 'Archived items could not be loaded. Please try again.'));
                }
            }).finally(function () {
                loading = false;
                renderPagination();
            });
        }

        function requestActivePage(page, all) {
            if (page < 1) { return; }
            request(activeScope, page, all, 'active');
        }

        if (loadAllButton) {
            loadAllButton.addEventListener('click', function () {
                requestActivePage(1, true);
            });
        }

        if (archiveButton && archivePanel && archiveContent) {
            setArchiveToggle(archiveButton, !archivePanel.classList.contains('d-none'));
            archiveButton.addEventListener('click', function () {
                var open = archivePanel.classList.contains('d-none');
                archivePanel.classList.toggle('d-none', !open);
                setArchiveToggle(archiveButton, open);
                if (open && archive.dataset.deferredManagementArchiveLoaded !== 'true' && archiveScope) {
                    archiveContent.replaceChildren(element('div', 'gape-learning-management-deferred-copy text-13 text-neutral-500', 'Loading archived items...'));
                    request(archiveScope, 1, true, 'archive');
                }
            });
        }

        renderPagination();
    }

    function initialize() {
        Array.prototype.slice.call(document.querySelectorAll('[data-deferred-management-root]')).forEach(initializeRoot);
    }

    window.GapeDeferredManagementList = { initialize: initialize };
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initialize);
    } else {
        initialize();
    }
}(window, document));
