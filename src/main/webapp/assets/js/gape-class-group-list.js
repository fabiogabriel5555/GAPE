/* Paged Class Groups structure, aligned with Subject Details. */
(function (window) {
    'use strict';

    function initialize() {
        var list = document.querySelector('[data-class-group-list]');
        if (!list || list.dataset.classGroupListReady === 'true') { return; }
        list.dataset.classGroupListReady = 'true';

        var pagination = document.querySelector('[data-class-group-pagination]');
        var paginationPages = document.querySelector('[data-class-group-pagination-pages]');
        var loadAllButton = document.querySelector('[data-class-group-load-all]');
        var sortOptions = Array.prototype.slice.call(document.querySelectorAll('[data-class-group-sort-option]'));
        var groupOptions = Array.prototype.slice.call(document.querySelectorAll('[data-class-group-group-option]'));
        var sortToggle = document.querySelector('[data-class-group-sort-toggle]');
        var groupToggle = document.querySelector('[data-class-group-group-toggle]');
        var metadata = list.querySelector('[data-class-group-load-more-meta]');
        var currentPage = metadata ? (Number(metadata.dataset.currentPage) || 1) : 1;
        var showingAll = metadata && metadata.dataset.loadAll === 'true';
        var loading = false;
        var sortField = null, sortDirection = null, groupField = null, groupDirection = null;
        var completedCollapsed = true;
        var collator = new Intl.Collator(document.documentElement.lang || undefined, { numeric: true, sensitivity: 'base' });
        if (metadata) { metadata.remove(); }

        function rowGroups() {
            return Array.prototype.slice.call(list.querySelectorAll('[data-class-group-row]')).map(function (row) {
                return { row: row };
            });
        }

        function byField(field, direction, first, second) {
            var key = 'sort' + field.charAt(0).toUpperCase() + field.slice(1);
            var a = first.row.dataset[key] || '';
            var b = second.row.dataset[key] || '';
            var result = field === 'date' ? Number(a) - Number(b) : collator.compare(a, b);
            return direction === 'desc' ? -result : result;
        }

        function groupValue(group, field) {
            if (field === 'occurrence') {
                return {
                    key: [group.row.dataset.groupOccurrenceId, group.row.dataset.groupSubjectId].join(':'),
                    label: group.row.dataset.groupOccurrence || 'Academic period',
                    meta: group.row.dataset.groupOccurrenceRange || '',
                    state: group.row.dataset.groupOccurrenceState || '',
                    stateClass: group.row.dataset.groupOccurrenceStateClass || '',
                    context: group.row.dataset.groupContext || '',
                    icon: 'ph-calendar-dots'
                };
            }
            var title = field.charAt(0).toUpperCase() + field.slice(1);
            return {
                key: group.row.dataset['group' + title + 'Id'] || group.row.dataset['group' + title] || 'none',
                label: group.row.dataset['group' + title] || ('No ' + field),
                meta: '', state: '', stateClass: '', context: field === 'course' ? (group.row.dataset.groupContext || '') : '',
                icon: field === 'organization' ? 'ph-buildings' : (field === 'course' ? 'ph-graduation-cap' : 'ph-book-open-text')
            };
        }

        function element(tag, className, text) {
            var node = document.createElement(tag);
            if (className) { node.className = className; }
            if (text !== undefined) { node.textContent = text; }
            return node;
        }

        function iconPane(icon, paletteClass) {
            var pane = element('span', 'gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--table gape-class-group-occurrence-icon' + (paletteClass ? ' ' + paletteClass : ''));
            var iconNode = element('i', 'ph ' + icon);
            iconNode.setAttribute('aria-hidden', 'true');
            pane.appendChild(iconNode);
            return pane;
        }

        function setToggleState(button, open, openLabel, closedLabel) {
            button.setAttribute('aria-expanded', String(open));
            button.setAttribute('title', open ? openLabel : closedLabel);
            button.setAttribute('aria-label', open ? openLabel : closedLabel);
            button.querySelector('i').className = 'ph ' + (open ? 'ph-caret-up' : 'ph-caret-down');
        }

        function eventCount(items) {
            return items.reduce(function (total, group) {
                return total + (Number(group.row.dataset.classGroupEventCount) || 0);
            }, 0);
        }

        function groupSection(bucket, count, completed, pendingEvents) {
            var section = element('section', completed ? 'gape-class-group-completed-wrapper' : 'gape-class-group-occurrence-group');
            section.setAttribute('data-class-group-rendered-group', '');
            var contentHost = section;
            if (completed) {
                section.appendChild(element('div', 'gape-completed-class-groups-divider', 'Completed Class Groups'));
                contentHost = element('article', 'gape-structure-node gape-completed-class-groups-node border rounded-8 px-18 py-16 bg-white');
                section.appendChild(contentHost);
            }
            var heading = element('div', 'gape-class-group-structure-row' + (completed ? '' : ' gape-class-group-occurrence-heading'));
            var intro = element('div', 'd-flex align-items-center gap-12 min-w-0');
            var icon = iconPane(completed ? 'ph-archive' : bucket.icon, completed ? 'bg-danger-50 text-danger-600' : '');
            if (pendingEvents > 0) {
                var iconAnchor = element('span', 'gape-class-group-event-count-anchor');
                iconAnchor.appendChild(element('span', 'gape-class-group-event-count-badge', String(pendingEvents)));
                iconAnchor.appendChild(icon);
                intro.appendChild(iconAnchor);
            } else {
                intro.appendChild(icon);
            }
            var copy = element('div', 'min-w-0');
            copy.appendChild(element('span', 'fw-medium text-14 text-neutral-700 d-block', completed ? 'Completed Class Groups' : bucket.label));
            var meta = element('span', 'gape-node-meta text-12');
            if (completed) {
                meta.appendChild(element('span', '', 'Past class groups'));
            } else {
                if (bucket.meta) { meta.appendChild(element('span', '', bucket.meta)); }
                meta.appendChild(element('span', '', count + ' class group' + (count === 1 ? '' : 's')));
            }
            copy.appendChild(meta); intro.appendChild(copy); heading.appendChild(intro);
            if (completed) {
                heading.appendChild(element('div', 'text-13 text-neutral-500'));
                var countCell = element('div'); countCell.appendChild(element('span', 'cd-element-count', String(count))); heading.appendChild(countCell);
                var stateCell = element('div'); stateCell.appendChild(element('span', 'bg-neutral-20 text-neutral-600 px-14 py-6 border-neutral-30 border rounded-pill text-13', 'Completed')); heading.appendChild(stateCell);
                var actionCell = element('div', 'd-flex justify-content-end');
                var contentId = 'classGroupListGroup_completed';
                var isOpen = !completedCollapsed;
                var button = element('button', 'gape-tree-toggle text-20 text-neutral-500 hover-text-main-600');
                button.type = 'button'; button.setAttribute('aria-controls', contentId);
                button.appendChild(element('i', 'ph'));
                setToggleState(button, isOpen, 'Hide completed class groups', 'Show completed class groups');
                actionCell.appendChild(button); heading.appendChild(actionCell);
            } else {
                heading.appendChild(element('div'));
                heading.appendChild(element('div'));
                heading.appendChild(element('div'));
                heading.appendChild(element('div'));
            }
            contentHost.appendChild(heading);

            var panel = element('div', completed ? 'gape-completed-class-groups-panel' + (isOpen ? '' : ' d-none') : 'gape-class-group-occurrence-children');
            if (completed) { panel.id = contentId; }
            var children = element('div', completed ? 'gape-completed-class-groups-content d-flex flex-column gap-12' : 'gape-class-group-occurrence-content'); panel.appendChild(children); contentHost.appendChild(panel);
            if (completed) {
                button.addEventListener('click', function () {
                    var open = panel.classList.contains('d-none');
                    completedCollapsed = !open;
                    panel.classList.toggle('d-none', !open);
                    setToggleState(button, open, 'Hide completed class groups', 'Show completed class groups');
                });
            }
            return { section: section, children: children };
        }

        function renderCollectionInto(host, items, field) {
            var buckets = [], byKey = new Map();
            items.forEach(function (group) {
                var value = groupValue(group, field);
                if (!byKey.has(value.key)) { value.items = []; byKey.set(value.key, value); buckets.push(value); }
                byKey.get(value.key).items.push(group);
            });
            if (field !== 'occurrence') {
                buckets.sort(function (a, b) { return collator.compare(a.label, b.label) * (groupDirection === 'desc' ? -1 : 1); });
            }
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
            var view = groupSection({ key: 'completed' }, items.length, true, eventCount(items));
            list.appendChild(view.section);
            renderCollectionInto(view.children, items, groupField || 'occurrence');
        }

        function render() {
            var current = rowGroups();
            Array.prototype.slice.call(list.querySelectorAll('[data-class-group-rendered-group]')).forEach(function (node) { node.remove(); });
            current.forEach(function (group) { group.row.remove(); });
            current.sort(function (a, b) { return sortField ? byField(sortField, sortDirection, a, b) : Number(a.row.dataset.sortIndex) - Number(b.row.dataset.sortIndex); });
            var active = current.filter(function (group) { return group.row.dataset.classGroupCompleted !== 'true'; });
            var completed = current.filter(function (group) { return group.row.dataset.classGroupCompleted === 'true'; });
            renderCollection(active, groupField || 'occurrence');
            if (completed.length) { renderCompleted(completed); }
        }

        function pageItems(total) {
            if (total <= 5) { return Array.from({ length: total }, function (_, index) { return index + 1; }); }
            if (currentPage <= 3) { return [1, 2, 3, '…', total]; }
            if (currentPage >= total - 2) { return [1, '…', total - 2, total - 1, total]; }
            return [1, '…', currentPage - 1, currentPage, currentPage + 1, '…', total];
        }

        function pageButton(page, icon, label, disabled) {
            var button = element('button', 'gape-list-pagination-button' + (icon ? ' gape-list-pagination-arrow' : ''));
            button.type = 'button'; button.disabled = disabled || loading;
            if (icon) { var iconNode = element('i', 'ph ' + icon); iconNode.setAttribute('aria-hidden', 'true'); button.appendChild(iconNode); } else { button.textContent = String(page); }
            if (!icon && !showingAll && page === currentPage) { button.classList.add('is-active'); button.setAttribute('aria-current', 'page'); }
            if (label) { button.setAttribute('aria-label', label); }
            button.addEventListener('click', function () { requestPage(page, false); }); return button;
        }

        function renderPagination() {
            if (!pagination || !paginationPages) { return; }
            var total = Math.ceil(Number(pagination.dataset.total || 0) / Number(pagination.dataset.pageSize || 10));
            paginationPages.replaceChildren();
            paginationPages.appendChild(pageButton(currentPage - 1, 'ph-caret-left', 'Previous page', showingAll || currentPage <= 1));
            pageItems(total).forEach(function (item) {
                if (item === '…') { paginationPages.appendChild(element('span', 'gape-list-pagination-ellipsis', item)); }
                else { paginationPages.appendChild(pageButton(item, null, null, false)); }
            });
            paginationPages.appendChild(pageButton(currentPage + 1, 'ph-caret-right', 'Next page', showingAll || currentPage >= total));
            if (loadAllButton) { loadAllButton.disabled = loading || showingAll; loadAllButton.textContent = loading ? 'Loading…' : 'Load All'; }
        }

        function withCurrentSessionUrl(rawUrl) {
            var target = new URL(rawUrl, window.location.href);
            var match = window.location.pathname.match(/^(\/[^/]+)(;jsessionid=[^/]+)/i);
            if (match && target.origin === window.location.origin && target.pathname.indexOf(match[2]) === -1
                    && target.pathname.indexOf(match[1] + '/') === 0) {
                target.pathname = match[1] + match[2] + target.pathname.substring(match[1].length);
            }
            return target.toString();
        }

        function requestPage(page, all) {
            if (loading || page < 1 || !pagination) { return; }
            loading = true; renderPagination();
            var url = new URL(window.location.href);
            url.searchParams.set('fragment', 'rows');
            url.searchParams.set('offset', String((page - 1) * Number(pagination.dataset.pageSize || 10)));
            if (all) { url.searchParams.set('loadAll', 'true'); } else { url.searchParams.delete('loadAll'); }
            fetch(withCurrentSessionUrl(url), { credentials: 'same-origin', headers: { 'Accept': 'text/html', 'X-Requested-With': 'XMLHttpRequest' } })
                .then(function (response) { if (!response.ok) { throw new Error(); } return response.text(); })
                .then(function (html) {
                    var holder = document.createElement('div'); holder.innerHTML = html;
                    var next = holder.querySelector('[data-class-group-load-more-meta]'); if (!next) { throw new Error(); }
                    currentPage = Number(next.dataset.currentPage) || page; showingAll = next.dataset.loadAll === 'true'; next.remove();
                    list.replaceChildren(); while (holder.firstChild) { list.appendChild(holder.firstChild); }
                    render();
                })
                .catch(function () {})
                .finally(function () { loading = false; renderPagination(); });
        }

        function updateOptionStates() {
            sortOptions.forEach(function (option) {
                var normal = option.dataset.sortNormal;
                var state = sortField !== option.dataset.sortField ? 'none' : (sortDirection === normal ? 'normal' : 'reverse');
                option.dataset.sortState = state; option.classList.toggle('is-active', state !== 'none'); option.setAttribute('aria-pressed', String(state !== 'none'));
            });
            groupOptions.forEach(function (option) {
                var normal = option.dataset.groupNormal;
                var state = groupField !== option.dataset.groupField ? 'none' : (groupDirection === normal ? 'normal' : 'reverse');
                option.dataset.groupState = state; option.classList.toggle('is-active', state !== 'none'); option.setAttribute('aria-pressed', String(state !== 'none'));
            });
            if (sortToggle) { sortToggle.classList.toggle('is-active', sortField !== null); }
            if (groupToggle) { groupToggle.classList.toggle('is-active', groupField !== null); }
        }

        sortOptions.forEach(function (option) {
            option.addEventListener('click', function () {
                var field = option.dataset.sortField, normal = option.dataset.sortNormal;
                if (sortField !== field) { sortField = field; sortDirection = normal; }
                else if (sortDirection === normal) { sortDirection = normal === 'asc' ? 'desc' : 'asc'; }
                else { sortField = null; sortDirection = null; }
                updateOptionStates(); render();
            });
        });
        groupOptions.forEach(function (option) {
            option.addEventListener('click', function () {
                var field = option.dataset.groupField, normal = option.dataset.groupNormal;
                if (groupField !== field) { groupField = field; groupDirection = normal; }
                else if (groupDirection === normal) { groupDirection = normal === 'asc' ? 'desc' : 'asc'; }
                else { groupField = null; groupDirection = null; }
                updateOptionStates(); render();
            });
        });
        if (sortToggle) { sortToggle.addEventListener('click', function (event) { if (!sortField) { return; } event.preventDefault(); event.stopPropagation(); sortField = null; sortDirection = null; updateOptionStates(); render(); }); }
        if (groupToggle) { groupToggle.addEventListener('click', function (event) { if (!groupField) { return; } event.preventDefault(); event.stopPropagation(); groupField = null; groupDirection = null; updateOptionStates(); render(); }); }
        if (loadAllButton) { loadAllButton.addEventListener('click', function () { requestPage(1, true); }); }

        updateOptionStates(); render(); renderPagination();
    }

    window.GapeClassGroupList = { initialize: initialize };
}(window));
