(function () {
    function ordinalSuffix(year) {
        var remainder = year % 100;
        if (remainder >= 11 && remainder <= 13) {
            return 'th';
        }
        switch (year % 10) {
            case 1:
                return 'st';
            case 2:
                return 'nd';
            case 3:
                return 'rd';
            default:
                return 'th';
        }
    }

    function yearLabel(year) {
        return year + ordinalSuffix(year) + ' Year';
    }

    function positiveInteger(value) {
        var number = parseInt(value, 10);
        return Number.isFinite(number) && number > 0 ? number : 0;
    }

    function selectedDurations(source) {
        if (!source) {
            return [];
        }
        var selectedOptions = Array.prototype.slice.call(source.selectedOptions || []);
        if (!selectedOptions.length && window.jQuery && window.jQuery.fn && window.jQuery(source).data('select2')) {
            var selectedValues = window.jQuery(source).val();
            if (!Array.isArray(selectedValues)) {
                selectedValues = selectedValues ? [selectedValues] : [];
            }
            selectedOptions = Array.prototype.slice.call(source.options || []).filter(function (option) {
                return selectedValues.indexOf(option.value) >= 0;
            });
        }
        return selectedOptions
            .map(function (option) {
                return positiveInteger(option.getAttribute('data-duration-years'));
            })
            .filter(function (duration) {
                return duration > 0;
            });
    }

    function durationFor(select) {
        var fixedDuration = positiveInteger(select.getAttribute('data-fixed-duration-years'));
        if (fixedDuration > 0) {
            return fixedDuration;
        }

        var sourceSelector = select.getAttribute('data-course-year-source');
        var source = sourceSelector ? document.querySelector(sourceSelector) : null;
        var durations = selectedDurations(source);
        if (!durations.length) {
            return 0;
        }

        if (select.getAttribute('data-course-year-selection') === 'max') {
            return Math.max.apply(Math, durations);
        }
        return Math.min.apply(Math, durations);
    }

    function select2Container(select) {
        var container = select.nextElementSibling;
        if (container && container.classList.contains('select2-container')) {
            return container;
        }
        return null;
    }

    function clearVisualInvalidState(select) {
        var container = select2Container(select);
        select.classList.remove('is-invalid', 'gape-document-field-error');
        select.removeAttribute('aria-invalid');
        if (!container) {
            return;
        }

        container.classList.remove('is-invalid');
        var selection = container.querySelector('.select2-selection');
        if (selection) {
            selection.classList.add('gape-course-year-selection');
            selection.classList.remove('is-invalid', 'gape-document-field-error', 'gape-course-year-user-invalid');
            selection.setAttribute('aria-disabled', select.disabled ? 'true' : 'false');
        }
    }

    function notifySelect2(select) {
        if (window.jQuery && window.jQuery.fn && window.jQuery(select).data('select2')) {
            window.jQuery(select).prop('disabled', select.disabled).trigger('change.select2');
        }
        var container = select2Container(select);
        if (container) {
            container.classList.toggle('select2-container--disabled', select.disabled);
            var selection = container.querySelector('.select2-selection');
            if (selection) {
                selection.classList.add('gape-course-year-selection');
                selection.classList.toggle('gape-course-year-ready', !select.disabled);
                selection.setAttribute('aria-disabled', select.disabled ? 'true' : 'false');
            }
        }
        if (!select.disabled) {
            clearVisualInvalidState(select);
        }
    }

    function rebuild(select) {
        var previousValue = select.value || select.getAttribute('data-selected-year') || '';
        var duration = durationFor(select);
        var placeholderText = select.getAttribute('data-placeholder') || 'Select year';

        select.innerHTML = '';
        select.appendChild(new Option(placeholderText, ''));
        for (var year = 1; year <= duration; year++) {
            select.appendChild(new Option(yearLabel(year), String(year)));
        }

        select.disabled = duration <= 0 && !select.getAttribute('data-fixed-duration-years');
        if (previousValue && positiveInteger(previousValue) <= duration) {
            select.value = previousValue;
        } else if (duration === 1) {
            select.value = '1';
        } else {
            select.value = '';
        }
        notifySelect2(select);
    }

    function scheduleRebuild(select) {
        if (select.gapeCourseYearRebuildTimer) {
            window.clearTimeout(select.gapeCourseYearRebuildTimer);
        }
        select.gapeCourseYearRebuildTimer = window.setTimeout(function () {
            select.gapeCourseYearRebuildTimer = null;
            rebuild(select);
        }, 0);
    }

    function bindSource(select) {
        if (select.dataset.courseYearBound === 'true') {
            return;
        }

        var sourceSelector = select.getAttribute('data-course-year-source');
        var source = sourceSelector ? document.querySelector(sourceSelector) : null;
        var sync = function () {
            select.setAttribute('data-selected-year', '');
            scheduleRebuild(select);
        };
        if (source) {
            source.addEventListener('change', sync);
            source.addEventListener('gape:dependent-select-sync', sync);
        }
        select.dataset.courseYearBound = 'true';
    }

    function init(scope) {
        var root = scope || document;
        Array.prototype.slice.call(root.querySelectorAll('select[data-course-year-select]')).forEach(function (select) {
            bindSource(select);
            if (select.dataset.courseYearValidationBound !== 'true') {
                select.addEventListener('invalid', function () {
                    var container = select2Container(select);
                    var selection = container ? container.querySelector('.select2-selection') : null;
                    if (selection) {
                        selection.classList.add('gape-course-year-user-invalid');
                    }
                });
                select.addEventListener('change', function () {
                    if (select.value) {
                        clearVisualInvalidState(select);
                    }
                });
                select.dataset.courseYearValidationBound = 'true';
            }
            rebuild(select);
        });
    }

    window.GapeCourseYearSelect = {
        init: init,
        refreshForSource: function (source) {
            if (!source || !source.id) {
                return;
            }
            Array.prototype.slice.call(document.querySelectorAll('select[data-course-year-source="#' + source.id + '"]')).forEach(function (select) {
                select.setAttribute('data-selected-year', '');
                scheduleRebuild(select);
            });
        }
    };

    document.addEventListener('DOMContentLoaded', function () {
        init(document);
    });
})();
