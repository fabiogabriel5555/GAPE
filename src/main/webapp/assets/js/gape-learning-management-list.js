/* Paged learning-management lists share the Class Group Management contract:
 * ten initial rows, deterministic local sorting/grouping, a completed block,
 * and a session-safe fragment request for further pages. */
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

    function initializeList(list) {
        if (!list || list.dataset.learningManagementReady === 'true') {
            return;
        }
        list.dataset.learningManagementReady = 'true';
        var panel = list.closest('[data-learning-management-panel]');
        if (!panel) { return; }
        var kind = list.dataset.learningManagementKind || panel.dataset.learningManagementKind || 'lessons';
        var itemLabel = kind === 'assessments' ? 'assessment' : 'lesson';
        var completedLabel = kind === 'assessments' ? 'Completed Assessments' : 'Completed Lessons';
        var pagination = panel.querySelector('[data-learning-management-pagination]');
        var paginationPages = panel.querySelector('[data-learning-management-pagination-pages]');
        var loadAllButton = panel.querySelector('[data-learning-management-load-all]');
        var sortOptions = Array.prototype.slice.call(panel.querySelectorAll('[data-learning-management-sort-option]'));
        var groupOptions = Array.prototype.slice.call(panel.querySelectorAll('[data-learning-management-group-option]'));
        var sortToggle = panel.querySelector('[data-learning-management-sort-toggle]');
        var groupToggle = panel.querySelector('[data-learning-management-group-toggle]');
        var metadata = list.querySelector('[data-learning-management-meta]');
        var currentPage = metadata ? (Number(metadata.dataset.currentPage) || 1) : 1;
        var showingAll = metadata && metadata.dataset.loadAll === 'true';
        var loading = false;
        var sortField = null;
        var sortDirection = null;
        var groupField = null;
        var groupDirection = null;
        var completedCollapsed = true;
        var collator = new Intl.Collator(document.documentElement.lang || undefined, { numeric: true, sensitivity: 'base' });
        if (metadata) { metadata.remove(); }

        function rowGroups() {
            return Array.prototype.slice.call(list.querySelectorAll('[data-learning-management-row]')).map(function (row) {
                return { row: row };
            });
        }

        function byField(field, direction, first, second) {
            var key = 'sort' + field.charAt(0).toUpperCase() + field.slice(1);
            var a = first.row.dataset[key] || '';
            var b = second.row.dataset[key] || '';
            var result;
            if (field === 'date') {
                var firstDate = Date.parse(a);
                var secondDate = Date.parse(b);
                result = (Number.isNaN(firstDate) ? 0 : firstDate) - (Number.isNaN(secondDate) ? 0 : secondDate);
            } else {
                result = collator.compare(a, b);
            }
            if (result === 0) {
                result = Number(first.row.dataset.sortIndex || '0') - Number(second.row.dataset.sortIndex || '0');
            }
            return direction === 'desc' ? -result : result;
        }

        function groupValue(group, field) {
            var title = field.charAt(0).toUpperCase() + field.slice(1);
            var label = group.row.dataset['group' + title] || ('No ' + itemLabel + ' group');
            var context = group.row.dataset['group' + title + 'Context'] || '';
            var display = group.row.dataset['group' + title + 'Title'] || label;
            return {
                key: group.row.dataset['group' + title + 'Id'] || label,
                label: display,
                meta: context,
                icon: field === 'organization' ? 'ph-buildings'
                    : (field === 'course' ? 'ph-graduation-cap'
                        : (field === 'subject' ? 'ph-book-open-text' : 'ph-users-three'))
            };
        }

        function iconPane(icon, tone) {
            var pane = element('span', 'gape-learning-management-group-icon ' + (tone || 'bg-main-50 text-main-600'));
            var iconNode = element('i', 'ph ' + icon);
            iconNode.setAttribute('aria-hidden', 'true');
            pane.appendChild(iconNode);
            return pane;
        }

        function setToggleState(button, open) {
            button.setAttribute('aria-expanded', String(open));
            button.setAttribute('title', open ? 'Hide ' + completedLabel.toLowerCase() : 'Show ' + completedLabel.toLowerCase());
            button.setAttribute('aria-label', button.getAttribute('title'));
            var icon = button.querySelector('i');
            if (icon) { icon.className = 'ph ' + (open ? 'ph-caret-up' : 'ph-caret-down'); }
        }

        function eventCount(items) {
            return items.reduce(function (total, group) {
                return total + (Number(group.row.dataset.learningManagementEventCount) || 0);
            }, 0);
        }

        function groupSection(bucket, count, completed, pendingEvents, onCompletedOpened) {
            var section = element('section', completed ? 'gape-learning-management-completed-wrapper' : 'gape-learning-management-group');
            section.setAttribute('data-learning-management-rendered-group', '');
            var host = section;
            if (completed) {
                section.appendChild(element('div', 'gape-learning-management-completed-divider', completedLabel));
                host = element('article', 'gape-learning-management-completed-node border rounded-8 px-18 py-16 bg-white');
                section.appendChild(host);
            }
            var heading = element('div', 'gape-learning-management-row' + (completed ? '' : ' gape-learning-management-group-heading'));
            var intro = element('div', 'd-flex align-items-center gap-12 min-w-0');
            var icon = iconPane(completed ? 'ph-archive' : bucket.icon, completed ? 'bg-danger-50 text-danger-600' : '');
            if (pendingEvents > 0) {
                var iconAnchor = element('span', 'gape-learning-management-event-count-anchor');
                iconAnchor.appendChild(element('span', 'gape-learning-pending-corner-badge', String(pendingEvents)));
                iconAnchor.appendChild(icon);
                intro.appendChild(iconAnchor);
            } else {
                intro.appendChild(icon);
            }
            var copy = element('div', 'min-w-0');
            copy.appendChild(element('span', 'fw-medium text-14 text-neutral-700 d-block', completed ? completedLabel : bucket.label));
            var meta = element('span', 'gape-node-meta text-12');
            if (completed) {
                meta.appendChild(element('span', '', 'Past ' + itemLabel + 's'));
            } else {
                if (bucket.meta) { meta.appendChild(element('span', '', bucket.meta)); }
                meta.appendChild(element('span', '', count + ' ' + itemLabel + (count === 1 ? '' : 's')));
            }
            copy.appendChild(meta);
            intro.appendChild(copy);
            heading.appendChild(intro);
            if (completed) {
                heading.appendChild(element('div', 'text-13 text-neutral-500'));
                var countCell = element('div');
                countCell.appendChild(element('span', 'cd-element-count', String(count)));
                heading.appendChild(countCell);
                var stateCell = element('div');
                stateCell.appendChild(element('span', 'bg-neutral-20 text-neutral-600 px-14 py-6 border-neutral-30 border rounded-pill text-13', 'Completed'));
                heading.appendChild(stateCell);
                var actionCell = element('div', 'd-flex justify-content-end');
                var button = element('button', 'gape-tree-toggle text-20 text-neutral-500 hover-text-main-600');
                button.type = 'button';
                button.appendChild(element('i', 'ph'));
                setToggleState(button, !completedCollapsed);
                actionCell.appendChild(button);
                heading.appendChild(actionCell);
            } else {
                heading.appendChild(element('div'));
                heading.appendChild(element('div'));
                heading.appendChild(element('div'));
                heading.appendChild(element('div'));
            }
            host.appendChild(heading);
            var panelNode = element('div', completed
                ? 'gape-learning-management-completed-panel' + (completedCollapsed ? ' d-none' : '')
                : 'gape-learning-management-children');
            var children = element('div', completed
                ? 'gape-learning-management-completed-content d-flex flex-column gap-12'
                : 'gape-learning-management-content');
            panelNode.appendChild(children);
            host.appendChild(panelNode);
            if (completed) {
                button.addEventListener('click', function () {
                    var open = panelNode.classList.contains('d-none');
                    completedCollapsed = !open;
                    panelNode.classList.toggle('d-none', !open);
                    setToggleState(button, open);
                    if (open && onCompletedOpened) { onCompletedOpened(panelNode, children); }
                });
            }
            return { section: section, children: children };
        }

        function renderCollectionInto(host, items, field) {
            if (!field) {
                items.forEach(function (group) { host.appendChild(group.row); });
                return;
            }
            var buckets = [];
            var byKey = new Map();
            items.forEach(function (group) {
                var value = groupValue(group, field);
                if (!byKey.has(value.key)) {
                    value.items = [];
                    byKey.set(value.key, value);
                    buckets.push(value);
                }
                byKey.get(value.key).items.push(group);
            });
            buckets.sort(function (first, second) {
                return collator.compare(first.label, second.label) * (groupDirection === 'desc' ? -1 : 1);
            });
            buckets.forEach(function (bucket) {
                var view = groupSection(bucket, bucket.items.length, false, 0);
                host.appendChild(view.section);
                bucket.items.forEach(function (group) { view.children.appendChild(group.row); });
            });
        }

        function renderCollection(items, field) {
            renderCollectionInto(list, items, field);
        }

        function renderCompleted(items) {
            var count = items.length;
            var view = groupSection(
                { key: 'completed' },
                count,
                true,
                eventCount(items)
            );
            list.appendChild(view.section);
            renderCollectionInto(view.children, items, groupField);
        }

        function render() {
            var current = rowGroups();
            Array.prototype.slice.call(list.querySelectorAll('[data-learning-management-rendered-group]')).forEach(function (node) { node.remove(); });
            current.forEach(function (group) { group.row.remove(); });
            current.sort(function (first, second) {
                return sortField ? byField(sortField, sortDirection, first, second)
                    : Number(first.row.dataset.sortIndex || '0') - Number(second.row.dataset.sortIndex || '0');
            });
            var active = current.filter(function (group) { return group.row.dataset.learningManagementCompleted !== 'true'; });
            var completed = current.filter(function (group) { return group.row.dataset.learningManagementCompleted === 'true'; });
            renderCollection(active, groupField);
            if (completed.length) { renderCompleted(completed); }
        }

        function pageItems(total) {
            if (total <= 5) { return Array.from({ length: total }, function (_, index) { return index + 1; }); }
            if (currentPage <= 3) { return [1, 2, 3, 'ellipsis', total]; }
            if (currentPage >= total - 2) { return [1, 'ellipsis', total - 2, total - 1, total]; }
            return [1, 'ellipsis', currentPage - 1, currentPage, currentPage + 1, 'ellipsis', total];
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
            }
            if (!icon && !showingAll && page === currentPage) {
                button.classList.add('is-active');
                button.setAttribute('aria-current', 'page');
            }
            if (label) { button.setAttribute('aria-label', label); }
            button.addEventListener('click', function () { requestPage(page, false); });
            return button;
        }

        function renderPagination() {
            if (!pagination || !paginationPages) { return; }
            var total = Math.ceil(Number(pagination.dataset.total || '0') / Number(pagination.dataset.pageSize || '10'));
            paginationPages.replaceChildren();
            paginationPages.appendChild(pageButton(currentPage - 1, 'ph-caret-left', 'Previous page', showingAll || currentPage <= 1));
            pageItems(total).forEach(function (item) {
                if (item === 'ellipsis') {
                    paginationPages.appendChild(element('span', 'gape-list-pagination-ellipsis', '...'));
                } else {
                    paginationPages.appendChild(pageButton(item, null, null, false));
                }
            });
            paginationPages.appendChild(pageButton(currentPage + 1, 'ph-caret-right', 'Next page', showingAll || currentPage >= total));
            if (loadAllButton) {
                loadAllButton.disabled = loading || showingAll;
                loadAllButton.textContent = loading ? 'Loading...' : 'Load All';
            }
        }

        function requestPage(page, all) {
            if (loading || page < 1 || !pagination) { return; }
            loading = true;
            renderPagination();
            var url = new URL(window.location.href);
            url.searchParams.set('fragment', 'learning-management-rows');
            url.searchParams.set('kind', kind);
            url.searchParams.set('offset', String((page - 1) * Number(pagination.dataset.pageSize || '10')));
            if (all) { url.searchParams.set('loadAll', 'true'); } else { url.searchParams.delete('loadAll'); }
            fetch(withCurrentSessionUrl(url), {
                credentials: 'same-origin',
                headers: { 'Accept': 'text/html', 'X-Requested-With': 'XMLHttpRequest' }
            }).then(function (response) {
                if (!response.ok) { throw new Error('Could not load this page.'); }
                return response.text();
            }).then(function (html) {
                var holder = document.createElement('div');
                holder.innerHTML = html;
                var next = holder.querySelector('[data-learning-management-meta]');
                if (!next) { throw new Error('Invalid learning list fragment.'); }
                currentPage = Number(next.dataset.currentPage) || page;
                showingAll = next.dataset.loadAll === 'true';
                next.remove();
                list.replaceChildren();
                while (holder.firstChild) { list.appendChild(holder.firstChild); }
                render();
            }).catch(function () {
                /* Existing content remains usable if a transient request fails. */
            }).finally(function () {
                loading = false;
                renderPagination();
            });
        }

        function updateOptionStates() {
            sortOptions.forEach(function (option) {
                var normal = option.dataset.sortNormal;
                var state = sortField !== option.dataset.sortField ? 'none' : (sortDirection === normal ? 'normal' : 'reverse');
                option.dataset.sortState = state;
                option.classList.toggle('is-active', state !== 'none');
                option.setAttribute('aria-pressed', String(state !== 'none'));
            });
            groupOptions.forEach(function (option) {
                var normal = option.dataset.groupNormal;
                var state = groupField !== option.dataset.groupField ? 'none' : (groupDirection === normal ? 'normal' : 'reverse');
                option.dataset.groupState = state;
                option.classList.toggle('is-active', state !== 'none');
                option.setAttribute('aria-pressed', String(state !== 'none'));
            });
            if (sortToggle) { sortToggle.classList.toggle('is-active', sortField !== null); }
            if (groupToggle) { groupToggle.classList.toggle('is-active', groupField !== null); }
        }

        sortOptions.forEach(function (option) {
            option.addEventListener('click', function () {
                var field = option.dataset.sortField;
                var normal = option.dataset.sortNormal;
                if (sortField !== field) { sortField = field; sortDirection = normal; }
                else if (sortDirection === normal) { sortDirection = normal === 'asc' ? 'desc' : 'asc'; }
                else { sortField = null; sortDirection = null; }
                updateOptionStates();
                render();
            });
        });
        groupOptions.forEach(function (option) {
            option.addEventListener('click', function () {
                var field = option.dataset.groupField;
                var normal = option.dataset.groupNormal;
                if (groupField !== field) { groupField = field; groupDirection = normal; }
                else if (groupDirection === normal) { groupDirection = normal === 'asc' ? 'desc' : 'asc'; }
                else { groupField = null; groupDirection = null; }
                updateOptionStates();
                render();
            });
        });
        if (sortToggle) {
            sortToggle.addEventListener('click', function (event) {
                if (!sortField) { return; }
                event.preventDefault();
                event.stopPropagation();
                sortField = null;
                sortDirection = null;
                updateOptionStates();
                render();
            });
        }
        if (groupToggle) {
            groupToggle.addEventListener('click', function (event) {
                if (!groupField) { return; }
                event.preventDefault();
                event.stopPropagation();
                groupField = null;
                groupDirection = null;
                updateOptionStates();
                render();
            });
        }
        if (loadAllButton) { loadAllButton.addEventListener('click', function () { requestPage(1, true); }); }

        updateOptionStates();
        render();
        renderPagination();
    }

    function initialize() {
        Array.prototype.slice.call(document.querySelectorAll('[data-learning-management-list]')).forEach(initializeList);
    }

    window.GapeLearningManagementList = { initialize: initialize };
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initialize);
    } else {
        initialize();
    }
}(window, document));
