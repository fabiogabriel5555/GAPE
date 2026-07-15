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
                selection.setAttribute('aria-disabled', select.disabled ? 'true' : 'false');
            }
        }
    }

    function rebuildTermSelect(select) {
        var source = document.querySelector(select.getAttribute('data-course-source'));
        var courseOption = selectedOption(source);
        var previousValue = select.value || select.getAttribute('data-selected-term') || '';
        var frequency = courseOption ? courseOption.getAttribute('data-frequency') : '';
        var terms = frequencies[frequency] || [];

        select.innerHTML = '';
        select.appendChild(new Option(select.getAttribute('data-placeholder') || 'Select period', ''));
        terms.forEach(function (term) {
            select.appendChild(new Option(term[1], term[0]));
        });
        select.disabled = terms.length === 0;
        if (previousValue && terms.some(function (term) { return term[0] === previousValue; })) {
            select.value = previousValue;
        } else if (terms.length === 1) {
            select.value = terms[0][0];
        } else {
            select.value = '';
        }
        refreshSelect2(select);
    }

    function init(scope) {
        Array.prototype.slice.call((scope || document).querySelectorAll('[data-course-term-select]')).forEach(function (select) {
            if (select.dataset.courseTermBound !== 'true') {
                var source = document.querySelector(select.getAttribute('data-course-source'));
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
                select.dataset.courseTermBound = 'true';
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
