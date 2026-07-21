(function () {
    var frequencies = {
        MONTHLY: [
            ['MONTH_1', '1st Month'], ['MONTH_2', '2nd Month'], ['MONTH_3', '3rd Month'], ['MONTH_4', '4th Month'],
            ['MONTH_5', '5th Month'], ['MONTH_6', '6th Month'], ['MONTH_7', '7th Month'], ['MONTH_8', '8th Month'],
            ['MONTH_9', '9th Month'], ['MONTH_10', '10th Month'], ['MONTH_11', '11th Month'], ['MONTH_12', '12th Month']
        ],
        BIMONTHLY: [
            ['BIMESTER_1', '1st Bimester'], ['BIMESTER_2', '2nd Bimester'], ['BIMESTER_3', '3rd Bimester'],
            ['BIMESTER_4', '4th Bimester'], ['BIMESTER_5', '5th Bimester'], ['BIMESTER_6', '6th Bimester']
        ],
        TRIMESTER: [
            ['TRIMESTER_1', '1st Trimester'], ['TRIMESTER_2', '2nd Trimester'],
            ['TRIMESTER_3', '3rd Trimester'], ['TRIMESTER_4', '4th Trimester']
        ],
        QUADRIMESTER: [
            ['QUADRIMESTER_1', '1st Quadrimester'], ['QUADRIMESTER_2', '2nd Quadrimester'],
            ['QUADRIMESTER_3', '3rd Quadrimester']
        ],
        SEMESTER: [['SEMESTER_1', '1st Semester'], ['SEMESTER_2', '2nd Semester']],
        ANNUAL: [['ANNUAL', 'Annual']]
    };

    function selectedOption(select) {
        return select && select.selectedIndex >= 0 ? select.options[select.selectedIndex] : null;
    }

    function refreshSelect2(select) {
        if (window.jQuery && window.jQuery.fn && window.jQuery(select).data('select2')) {
            window.jQuery(select).prop('disabled', select.disabled).trigger('change.select2');
        }
        var container = select.nextElementSibling;
        if (container && container.classList.contains('select2-container')) {
            container.classList.toggle('select2-container--disabled', select.disabled);
            var selection = container.querySelector('.select2-selection');
            if (selection) {
                selection.classList.add('gape-course-term-selection');
                selection.classList.toggle('gape-course-term-ready', !select.disabled);
                selection.setAttribute('aria-disabled', select.disabled ? 'true' : 'false');
            }
        }
        var field = select.closest('.gape-select-field');
        if (field) {
            field.classList.toggle('is-disabled', select.disabled);
            field.classList.toggle('is-available', !select.disabled);
            field.dataset.courseTermState = select.disabled ? 'disabled' : 'available';
        }
    }

    function syncRequiredSubmit(select) {
        var form = select.form;
        if (!form) {
            return;
        }
        var canSubmit = !select.disabled && Boolean(select.value);
        Array.prototype.slice.call(form.querySelectorAll('[data-course-term-submit]')).forEach(function (submit) {
            submit.disabled = !canSubmit;
            submit.setAttribute('aria-disabled', canSubmit ? 'false' : 'true');
        });
    }

    function rebuildTermSelect(select) {
        var source = document.querySelector(select.getAttribute('data-course-source'));
        var courseOption = selectedOption(source);
        var previousValue = select.value || select.getAttribute('data-selected-term') || '';
        var frequency = courseOption ? courseOption.getAttribute('data-frequency') : '';
        var terms = frequencies[frequency] || [];
        var yearSourceSelector = select.getAttribute('data-course-term-year-source');
        var yearSource = yearSourceSelector ? document.querySelector(yearSourceSelector) : null;
        var hasSelectedYear = !yearSource || Boolean(yearSource.value) && !yearSource.disabled;

        select.innerHTML = '';
        select.appendChild(new Option(select.getAttribute('data-placeholder') || 'Select period', ''));
        terms.forEach(function (term) {
            select.appendChild(new Option(term[1], term[0]));
        });
        select.disabled = terms.length === 0 || !hasSelectedYear;
        if (!hasSelectedYear) {
            select.value = '';
        } else if (previousValue && terms.some(function (term) { return term[0] === previousValue; })) {
            select.value = previousValue;
        } else if (terms.length === 1) {
            select.value = terms[0][0];
        } else {
            select.value = '';
        }
        refreshSelect2(select);
        syncRequiredSubmit(select);
    }

    function init(scope) {
        Array.prototype.slice.call((scope || document).querySelectorAll('[data-course-term-select]')).forEach(function (select) {
            if (select.dataset.courseTermBound !== 'true') {
                var source = document.querySelector(select.getAttribute('data-course-source'));
                var yearSourceSelector = select.getAttribute('data-course-term-year-source');
                var yearSource = yearSourceSelector ? document.querySelector(yearSourceSelector) : null;
                var sync = function () {
                    select.setAttribute('data-selected-term', '');
                    window.setTimeout(function () {
                        rebuildTermSelect(select);
                    }, 0);
                };
                if (source) {
                    source.addEventListener('change', sync);
                    if (window.jQuery && window.jQuery.fn) {
                        window.jQuery(source)
                            .off('select2:select.gapeCourseTerm select2:clear.gapeCourseTerm')
                            .on('select2:select.gapeCourseTerm select2:clear.gapeCourseTerm', sync);
                    }
                }
                if (yearSource) {
                    yearSource.addEventListener('change', sync);
                    yearSource.addEventListener('gape:course-year-rebuilt', sync);
                    if (window.jQuery && window.jQuery.fn) {
                        window.jQuery(yearSource)
                            .off('select2:select.gapeCourseTermYear select2:clear.gapeCourseTermYear')
                            .on('select2:select.gapeCourseTermYear select2:clear.gapeCourseTermYear', sync);
                    }
                }
                select.dataset.courseTermBound = 'true';
            }
            if (select.dataset.courseTermSubmitBound !== 'true') {
                var syncSubmit = function () {
                    syncRequiredSubmit(select);
                };
                select.addEventListener('change', syncSubmit);
                if (window.jQuery && window.jQuery.fn) {
                    window.jQuery(select)
                        .off('select2:select.gapeCourseTermSubmit select2:unselect.gapeCourseTermSubmit select2:clear.gapeCourseTermSubmit')
                        .on('select2:select.gapeCourseTermSubmit select2:unselect.gapeCourseTermSubmit select2:clear.gapeCourseTermSubmit', syncSubmit);
                }
                select.dataset.courseTermSubmitBound = 'true';
            }
            rebuildTermSelect(select);
        });
    }

    window.GapeCourseTermSelect = {
        init: init
    };

    document.addEventListener('DOMContentLoaded', function () {
        init(document);
    });
})();
