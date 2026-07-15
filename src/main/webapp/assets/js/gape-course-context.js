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

    function clearSelect2OptionData(option) {
        if (!option || !window.jQuery || !window.jQuery.fn || !window.jQuery.fn.select2 || !window.jQuery.fn.select2.amd) {
            return;
        }
        var utils = window.jQuery.fn.select2.amd.require('select2/utils');
        if (utils && utils.RemoveData) {
            utils.RemoveData(option);
        }
        window.jQuery(option).removeData('data');
    }

    function refreshSelectUi(select) {
        if (!select || !window.jQuery || !window.jQuery.fn || !window.jQuery.fn.select2) {
            return;
        }
        window.jQuery(select).trigger('change.select2');
        var container = select.nextElementSibling;
        if (container && container.classList.contains('select2-container')) {
            container.classList.toggle('select2-container--disabled', select.disabled);
        }
    }

    function contextMatcher(params, data) {
        if (data && data.element && data.element.value && (data.element.hidden || data.element.disabled)) {
            return null;
        }
        var defaults = window.jQuery.fn.select2.defaults.defaults;
        return defaults && defaults.matcher ? defaults.matcher(params, data) : data;
    }

    function contextOptionTemplate(data) {
        var element = data.element;
        if (element && element.value && (element.hidden || element.disabled)) {
            return null;
        }
        var option = document.createElement('span');
        option.className = 'gape-course-context-option';
        option.textContent = normalizeText(data.text || '');
        return window.jQuery ? window.jQuery(option) : option;
    }

    function markCourseContextRows() {
        document.querySelectorAll('.gape-eduall-select-dropdown .gape-course-context-option').forEach(function (option) {
            var row = option.closest('.select2-results__option');
            if (row) {
                row.classList.add('gape-course-context-option-row');
            }
        });
    }

    function observeCourseContextRows() {
        var results = document.querySelector('.gape-eduall-select-dropdown .select2-results__options');
        markCourseContextRows();
        if (!results || results.dataset.gapeCourseContextObserver === 'true' || !window.MutationObserver) {
            return;
        }
        results.dataset.gapeCourseContextObserver = 'true';
        new MutationObserver(markCourseContextRows).observe(results, {
            childList: true,
            subtree: true
        });
    }

    function initializeSelectUi(select) {
        if (!select || !window.jQuery || !window.jQuery.fn || !window.jQuery.fn.select2) {
            return;
        }
        var selectUi = window.jQuery(select);
        if (selectUi.data('select2')) {
            selectUi.select2('destroy');
        }
        selectUi.select2({
            width: '100%',
            selectionCssClass: 'gape-eduall-selection',
            dropdownCssClass: 'gape-eduall-select-dropdown gape-course-context-dropdown',
            matcher: contextMatcher,
            templateResult: contextOptionTemplate,
            templateSelection: contextOptionTemplate
        });
        selectUi
                .off('select2:open.gapeCourseContextRows')
                .on('select2:open.gapeCourseContextRows', function () {
                    window.setTimeout(observeCourseContextRows, 0);
                });
        refreshSelectUi(select);
    }

    function setWrapperState(wrapper, enabled) {
        if (!wrapper) {
            return;
        }
        wrapper.classList.toggle('opacity-75', !enabled);
        wrapper.classList.toggle('is-disabled', !enabled);
    }

    function initForm(form) {
        var contextType = form.hasAttribute('data-subject-context-form') ? 'subject' : 'course';
        var organization = form.querySelector('[data-' + contextType + '-organization]');
        var organicUnit = form.querySelector('[data-' + contextType + '-organic-unit]');
        var organizationWrapper = form.querySelector('[data-' + contextType + '-organization-context]');
        var organicUnitWrapper = form.querySelector('[data-' + contextType + '-organic-unit-context]');
        if (!organization || !organicUnit) {
            return;
        }

        var originalOrganicUnitOptions = Array.prototype.slice.call(organicUnit.options).map(function (option) {
            return {
                value: option.value,
                text: normalizeText(option.textContent),
                organizationId: option.dataset.organizationId || '',
                title: option.title || '',
                selected: option.selected
            };
        });

        function organicUnitOptionsFor(organizationId) {
            return originalOrganicUnitOptions.filter(function (option) {
                return option.value && option.organizationId === organizationId;
            });
        }

        function rebuildOrganicUnitOptions() {
            var previousValue = organicUnit.value;
            var selectedOrganizationId = organization.value || '';
            var availableOptions = organicUnitOptionsFor(selectedOrganizationId);
            var hasSelectedOrganization = !!selectedOrganizationId;
            var hasAvailableOptions = availableOptions.length > 0;
            Array.prototype.forEach.call(organicUnit.options, clearSelect2OptionData);
            organicUnit.innerHTML = '';

            var placeholder = document.createElement('option');
            placeholder.value = '';
            placeholder.textContent = !hasSelectedOrganization
                    ? 'Select an organization first'
                    : hasAvailableOptions
                            ? 'No organic unit'
                            : 'No organic units in selected organization';
            placeholder.disabled = hasSelectedOrganization && !hasAvailableOptions;
            organicUnit.appendChild(placeholder);

            availableOptions.forEach(function (optionData) {
                var option = document.createElement('option');
                option.value = optionData.value;
                option.textContent = optionData.text;
                option.dataset.organizationId = optionData.organizationId;
                if (optionData.title) {
                    option.title = optionData.title;
                }
                organicUnit.appendChild(option);
            });

            organicUnit.value = hasSelectedOrganization && hasAvailableOptions ? previousValue : '';
            if (organicUnit.value !== previousValue) {
                organicUnit.value = '';
            }
            return hasAvailableOptions;
        }

        function syncCourseContext() {
            var selectedOrganizationReady = !!organization.value;
            var hasAvailableOrganicUnits = rebuildOrganicUnitOptions();

            organicUnit.disabled = !selectedOrganizationReady;
            organicUnit.setAttribute('aria-label', selectedOrganizationReady && !hasAvailableOrganicUnits
                    ? 'No organic units in selected organization'
                    : 'Organic Unit');
            setWrapperState(organizationWrapper, !organization.disabled);
            setWrapperState(organicUnitWrapper, selectedOrganizationReady);

            organization.setCustomValidity('');
            if (!organization.disabled && !selectedOrganizationReady) {
                organization.setCustomValidity('Select an organization before selecting an organic unit.');
            }
            organicUnit.setCustomValidity('');

            refreshSelectUi(organization);
            refreshSelectUi(organicUnit);
            form.dispatchEvent(new CustomEvent('gape:' + contextType + '-context-sync', { bubbles: true }));
        }

        initializeSelectUi(organization);
        initializeSelectUi(organicUnit);
        window.jQuery(organization)
                .off('change.gapeCourseContext select2:select.gapeCourseContext select2:clear.gapeCourseContext')
                .on('change.gapeCourseContext select2:select.gapeCourseContext select2:clear.gapeCourseContext', syncCourseContext);
        window.jQuery(organicUnit)
                .off('change.gapeCourseContext select2:select.gapeCourseContext select2:clear.gapeCourseContext')
                .on('change.gapeCourseContext select2:select.gapeCourseContext select2:clear.gapeCourseContext', function () {
                    refreshSelectUi(organicUnit);
                });
        syncCourseContext();
    }

    ready(function () {
        window.setTimeout(function () {
            document.querySelectorAll('[data-course-context-form], [data-subject-context-form]').forEach(initForm);
        }, 0);
    });
})();
