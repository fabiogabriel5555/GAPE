(function () {
    function ready(callback) {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', callback);
            return;
        }
        callback();
    }

    function normalizeText(value) {
        return (value || '').replace(/\s+/g, ' ').trim();
    }

    function splitCourseLabel(label) {
        var parts = normalizeText(label).split('|').map(function (part) {
            return part.trim();
        }).filter(function (part) {
            return part;
        });
        return {
            main: parts[0] || normalizeText(label),
            context: parts.length > 1 ? parts.slice(1).join(' | ') : ''
        };
    }

    function contextMatcher(params, data) {
        if (data && data.element && data.element.value && (data.element.hidden || data.element.disabled)) {
            return null;
        }
        var defaults = window.jQuery.fn.select2.defaults.defaults;
        return defaults && defaults.matcher ? defaults.matcher(params, data) : data;
    }

    function courseTemplate(data) {
        var element = data.element;
        if (element && element.value && (element.hidden || element.disabled)) {
            return null;
        }
        var label = splitCourseLabel(data.text || '');
        if (!data.id) {
            return label.main;
        }

        var wrapper = document.createElement('span');
        wrapper.className = 'gape-subject-course-option';

        var main = document.createElement('span');
        main.className = 'gape-subject-course-option-main';
        main.textContent = label.main;
        wrapper.appendChild(main);

        if (label.context) {
            var context = document.createElement('span');
            context.className = 'gape-subject-course-option-context';
            context.textContent = label.context;
            wrapper.appendChild(context);
        }

        return wrapper;
    }

    function courseTokenTemplate(data) {
        var label = splitCourseLabel(data.text || '');
        if (!data.id) {
            return label.main;
        }

        var wrapper = document.createElement('span');
        wrapper.className = 'gape-subject-course-token';
        wrapper.textContent = label.context ? label.main + ' | ' + label.context : label.main;
        return wrapper;
    }

    function markCourseRows() {
        document.querySelectorAll('.gape-eduall-select-dropdown .gape-subject-course-option').forEach(function (option) {
            var row = option.closest('.select2-results__option');
            if (row) {
                row.classList.add('gape-subject-course-option-row');
            }
        });
    }

    function observeCourseRows() {
        var results = document.querySelector('.gape-eduall-select-dropdown .select2-results__options');
        markCourseRows();
        if (!results || results.dataset.gapeSubjectCourseObserver === 'true' || !window.MutationObserver) {
            return;
        }
        results.dataset.gapeSubjectCourseObserver = 'true';
        new MutationObserver(markCourseRows).observe(results, {
            childList: true,
            subtree: true
        });
    }

    function refreshSelectUi(select) {
        window.jQuery(select).trigger('change.select2');
        var container = select.nextElementSibling;
        if (container && container.classList.contains('select2-container')) {
            container.classList.toggle('select2-container--disabled', select.disabled);
        }
    }

    function initSelect(select) {
        if (!select || !window.jQuery || !window.jQuery.fn || !window.jQuery.fn.select2) {
            return;
        }
        var selectUi = window.jQuery(select);
        if (selectUi.data('select2')) {
            selectUi.select2('destroy');
        }
        var options = {
            width: '100%',
            selectionCssClass: 'gape-eduall-selection gape-subject-course-selection',
            dropdownCssClass: 'gape-eduall-select-dropdown gape-subject-course-dropdown',
            matcher: contextMatcher,
            templateResult: courseTemplate,
            templateSelection: select.multiple ? courseTokenTemplate : courseTemplate
        };
        var modalParent = select.closest('.modal');
        if (modalParent) {
            var portal = window.GapeSubjectModalSelect2Portal
                ? window.GapeSubjectModalSelect2Portal.dropdownParent(modalParent)
                : null;
            options.dropdownParent = window.jQuery(portal || modalParent);
        }
        selectUi.select2(options);
        selectUi
            .off('select2:open.gapeSubjectCourseRows')
            .on('select2:open.gapeSubjectCourseRows', function () {
                window.setTimeout(observeCourseRows, 0);
            })
            .off('change.gapeSubjectCourseYear select2:select.gapeSubjectCourseYear select2:unselect.gapeSubjectCourseYear select2:clear.gapeSubjectCourseYear')
            .on('change.gapeSubjectCourseYear select2:select.gapeSubjectCourseYear select2:unselect.gapeSubjectCourseYear select2:clear.gapeSubjectCourseYear', function () {
                window.setTimeout(function () {
                    refreshSelectUi(select);
                    if (window.GapeCourseYearSelect) {
                        window.GapeCourseYearSelect.refreshForSource(select);
                    }
                }, 0);
            });
        select.removeEventListener('gape:dependent-select-sync', select.gapeSubjectCourseSyncHandler || function () {});
        select.gapeSubjectCourseSyncHandler = function () {
            window.setTimeout(function () {
                refreshSelectUi(select);
            }, 0);
        };
        select.addEventListener('gape:dependent-select-sync', select.gapeSubjectCourseSyncHandler);
        refreshSelectUi(select);
    }

    function init(scope) {
        var root = scope || document;
        Array.prototype.slice.call(root.querySelectorAll('select[data-subject-course-select]')).forEach(initSelect);
    }

    window.GapeSubjectCourseSelect = {
        init: init
    };

    ready(function () {
        init(document);
    });
})();
