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

    function positionDropdownBelow(dropdown) {
        if (!dropdown
                || !dropdown.$container
                || !dropdown.$dropdown
                || !dropdown.$dropdownContainer
                || !dropdown.$dropdownParent
                || !dropdown.$container.length
                || !dropdown.$dropdownContainer.length) {
            return;
        }
        var containerOffset = dropdown.$container.offset();
        if (!containerOffset) {
            return;
        }
        var positionParent = dropdown.$dropdownParent;
        if (positionParent.css('position') === 'static') {
            positionParent = positionParent.offsetParent();
        }
        var parentOffset = positionParent && positionParent.length ? positionParent.offset() : null;
        parentOffset = parentOffset || { top: 0, left: 0 };
        dropdown.$dropdown.removeClass('select2-dropdown--above').addClass('select2-dropdown--below');
        dropdown.$container.removeClass('select2-container--above').addClass('select2-container--below');
        dropdown.$dropdownContainer.css({
            left: containerOffset.left - parentOffset.left,
            top: containerOffset.top + dropdown.$container.outerHeight(false) - parentOffset.top
        });
    }

    function installBelowDropdownPositioning(select) {
        if (!select || !window.jQuery || !window.jQuery.fn || !window.jQuery.fn.select2) {
            return;
        }
        var instance = window.jQuery(select).data('select2');
        var dropdown = instance && instance.dropdown;
        if (!dropdown || dropdown.gapeBelowPositioningInstalled) {
            return;
        }
        var originalPositionDropdown = dropdown._positionDropdown;
        if (typeof originalPositionDropdown !== 'function') {
            return;
        }
        dropdown._positionDropdown = function () {
            originalPositionDropdown.apply(this, arguments);
            positionDropdownBelow(this);
        };
        dropdown.gapeBelowPositioningInstalled = true;
    }

    function forceOpenDropdownBelow(select) {
        installBelowDropdownPositioning(select);
        var instance = window.jQuery(select).data('select2');
        if (instance && instance.dropdown) {
            positionDropdownBelow(instance.dropdown);
        }
    }

    function sourceOptionForResult(select, row) {
        return select2ResultOptionElement(row, [select]);
    }

    function unavailableReasonFor(option) {
        if (!option || !option.disabled) {
            return '';
        }
        return option.dataset.unavailableReason
                || option.dataset.contextUnavailableReason
                || option.title
                || 'This option is currently unavailable.';
    }

    function decorateUnavailableOptions(select) {
        if (!select || !window.jQuery || !window.jQuery.fn || !window.jQuery.fn.select2) {
            return;
        }
        var instance = window.jQuery(select).data('select2');
        var dropdownContainer = instance && instance.dropdown && instance.dropdown.$dropdownContainer;
        if (!dropdownContainer || !dropdownContainer.length) {
            return;
        }
        dropdownContainer[0].querySelectorAll('.select2-results__option--disabled').forEach(function (row) {
            var sourceOption = sourceOptionForResult(select, row);
            var reason = unavailableReasonFor(sourceOption);
            if (!reason) {
                return;
            }
            row.classList.add('gape-class-group-unavailable-option');
            row.dataset.gapeUnavailableReason = reason;
            row.setAttribute('title', reason);
            row.setAttribute('aria-label', normalizeText(row.textContent) + '. ' + reason);
        });
    }

    function bindSelectGuidance(select) {
        if (!select || !window.jQuery || !window.jQuery.fn || !window.jQuery.fn.select2) {
            return;
        }
        window.jQuery(select)
                .off('select2:opening.gapeClassGroupSelectGuidance')
                .on('select2:opening.gapeClassGroupSelectGuidance', function () {
                    installBelowDropdownPositioning(select);
                })
                .off('select2:open.gapeClassGroupSelectGuidance')
                .on('select2:open.gapeClassGroupSelectGuidance', function () {
                    window.setTimeout(function () {
                        forceOpenDropdownBelow(select);
                        decorateUnavailableOptions(select);
                    }, 0);
                });
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

    function currentOpenSelect2Element() {
        var container = document.querySelector('.select2-container--open');
        var select = container ? container.previousElementSibling : null;
        return select && select.tagName === 'SELECT' ? select : null;
    }

    function findCurrentContextOption(data, row, selects) {
        if (!data || !data.id) {
            return null;
        }
        var rowText = normalizeText(row && row.textContent ? row.textContent : (data.text || ''));
        var fallback = null;
        for (var index = 0; index < selects.length; index += 1) {
            var match = Array.prototype.find.call(selects[index].options, function (option) {
                return option.value === String(data.id) && (!rowText || normalizeText(option.textContent) === rowText);
            });
            if (match) {
                return match;
            }
            fallback = fallback || Array.prototype.find.call(selects[index].options, function (option) {
                return option.value === String(data.id);
            }) || null;
        }
        return fallback;
    }

    function select2ResultOptionElement(row, selects) {
        if (!row || !window.jQuery || !window.jQuery.fn || !window.jQuery.fn.select2 || !window.jQuery.fn.select2.amd) {
            return null;
        }
        var utils = window.jQuery.fn.select2.amd.require('select2/utils');
        var data = utils && utils.GetData ? utils.GetData(row, 'data') : null;
        var contextOption = findCurrentContextOption(data, row, selects);
        if (contextOption) {
            return contextOption;
        }
        var openSelect = currentOpenSelect2Element();
        if (openSelect && data && data.id) {
            return Array.prototype.find.call(openSelect.options, function (option) {
                return option.value === String(data.id);
            }) || null;
        }
        return data && data.element ? data.element : null;
    }

    function markMutedRows(selects) {
        document.querySelectorAll('.gape-eduall-select-dropdown .gape-class-group-context-option').forEach(function (option) {
            var row = option.closest('.select2-results__option');
            if (!row) {
                return;
            }
            var sourceOption = select2ResultOptionElement(row, selects);
            var muted = sourceOption && sourceOption.dataset
                    ? sourceOption.dataset.contextUnavailable === 'true'
                    : option.classList.contains('gape-class-group-context-option-muted');
            option.classList.toggle('gape-class-group-context-option-muted', muted);
            row.classList.add('gape-class-group-context-option-row');
            row.classList.toggle('gape-class-group-context-option-row-muted', muted);
            row.classList.toggle('gape-class-group-course-option-row', option.classList.contains('gape-class-group-course-option'));
        });
    }

    function observeMutedRows(selects) {
        var results = document.querySelector('.gape-eduall-select-dropdown .select2-results__options');
        markMutedRows(selects);
        if (!results || results.dataset.gapeClassGroupMutedObserver === 'true' || !window.MutationObserver) {
            return;
        }
        results.dataset.gapeClassGroupMutedObserver = 'true';
        new MutationObserver(function () {
            markMutedRows(selects);
        }).observe(results, {
            childList: true,
            subtree: true
        });
    }

    function contextMatcher(params, data) {
        if (data && data.element && data.element.value && data.element.hidden) {
            return null;
        }
        var defaults = window.jQuery.fn.select2.defaults.defaults;
        return defaults && defaults.matcher ? defaults.matcher(params, data) : data;
    }

    function courseTemplate(data) {
        var element = data.element;
        var label = splitCourseLabel(data.text || '');
        if (!element || !element.dataset) {
            return label.context ? label.main + ' | ' + label.context : label.main;
        }
        var wrapper = document.createElement('span');
        wrapper.className = 'gape-class-group-context-option gape-class-group-course-option';
        if (element.dataset.contextUnavailable === 'true') {
            wrapper.className += ' gape-class-group-context-option-muted';
        }
        var main = document.createElement('span');
        main.className = 'gape-class-group-course-option-main';
        main.textContent = label.main;
        wrapper.appendChild(main);
        if (label.context) {
            var context = document.createElement('span');
            context.className = 'gape-class-group-course-option-context';
            context.textContent = label.context;
            wrapper.appendChild(context);
        }
        return wrapper;
    }

    function subjectTemplate(data) {
        var element = data.element;
        if (element && element.value && (element.hidden || element.disabled)) {
            return null;
        }
        var wrapper = document.createElement('span');
        wrapper.className = 'gape-class-group-context-option gape-class-group-subject-option';
        if (element && element.dataset && element.dataset.contextUnavailable === 'true') {
            wrapper.className += ' gape-class-group-context-option-muted';
        }
        wrapper.textContent = normalizeText(data.text || '');
        return wrapper;
    }

    function initializeSelectUi(select, templateResult, selects) {
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
            dropdownCssClass: 'gape-eduall-select-dropdown gape-class-group-context-dropdown',
            matcher: contextMatcher,
            templateResult: templateResult,
            templateSelection: templateResult
        });
        selectUi
                .off('select2:open.gapeClassGroupContextRows')
                .on('select2:open.gapeClassGroupContextRows', function () {
                    window.setTimeout(function () {
                        observeMutedRows(selects);
                    }, 0);
                });
        refreshSelectUi(select);
    }

    function initForm(form) {
        var course = form.querySelector('[data-class-group-course]');
        var subject = form.querySelector('[data-class-group-subject]');
        if (!course || !subject) {
            return;
        }
        var courseWrapper = form.querySelector('[data-class-group-course-context]');
        var subjectWrapper = form.querySelector('[data-class-group-subject-context]');
        var subjectIsLocked = subject.matches('[data-class-group-locked-subject]');
        var selects = subjectIsLocked ? [course] : [course, subject];
        var originalSubjectOptions = subjectIsLocked ? [] : Array.prototype.slice.call(subject.options).map(function (option) {
            return {
                value: option.value,
                text: normalizeText(option.textContent),
                parentValue: option.dataset.parentValue || option.dataset.courseId || '',
                subjectAcronym: option.dataset.subjectAcronym || '',
                subjectLabel: option.dataset.subjectLabel || '',
                curricularYear: option.dataset.curricularYear || '',
                term: option.dataset.term || '',
                title: option.title || '',
                selected: option.selected
            };
        });

        function subjectOptionsFor(courseId) {
            if (subjectIsLocked) {
                var courseOption = Array.prototype.find.call(course.options, function (option) {
                    return option.value === courseId;
                });
                return courseOption && courseOption.dataset.subjectAssociationAvailable === 'true'
                        ? [courseOption]
                        : [];
            }
            return originalSubjectOptions.filter(function (option) {
                return option.value && option.parentValue === courseId;
            });
        }

        function courseHasCreationContext(courseId) {
            if (subjectIsLocked) {
                var courseOption = Array.prototype.find.call(course.options, function (option) {
                    return option.value === courseId;
                });
                return !!courseOption && courseOption.dataset.subjectContextSelectable === 'true';
            }
            return !!courseId && subjectOptionsFor(courseId).length > 0;
        }

        function selectedCourseUnavailable() {
            return !!course.value && !courseHasCreationContext(course.value);
        }

        function setWrapperState(wrapper, enabled) {
            if (!wrapper) {
                return;
            }
            wrapper.classList.toggle('opacity-75', !enabled);
            wrapper.classList.toggle('is-disabled', !enabled);
        }

        function markCourseAvailability() {
            Array.prototype.forEach.call(course.options, function (option) {
                if (!option.value) {
                    return;
                }
                var available = courseHasCreationContext(option.value);
                option.dataset.contextUnavailable = available ? '' : 'true';
                option.disabled = !available;
                if (typeof option.dataset.originalTitle === 'undefined') {
                    option.dataset.originalTitle = option.title || '';
                }
                var unavailableReason = option.dataset.contextUnavailableReason || 'Course has insufficient class group context.';
                option.dataset.unavailableReason = available ? '' : unavailableReason;
                option.title = available ? option.dataset.originalTitle : unavailableReason;
            });
        }

        function rebuildSubjectOptions() {
            if (subjectIsLocked) {
                setWrapperState(subjectWrapper, true);
                return;
            }
            var previousValue = subject.value;
            Array.prototype.forEach.call(subject.options, clearSelect2OptionData);
            subject.innerHTML = '';
            var placeholder = document.createElement('option');
            placeholder.value = '';
            placeholder.textContent = 'Select subject';
            subject.appendChild(placeholder);

            var selectedCourseReady = !!course.value && !selectedCourseUnavailable();
            var count = 0;
            subjectOptionsFor(selectedCourseReady ? course.value : '').forEach(function (optionData) {
                var option = document.createElement('option');
                option.value = optionData.value;
                option.textContent = optionData.text;
                option.dataset.parentValue = optionData.parentValue;
                option.dataset.courseId = optionData.parentValue;
                option.dataset.subjectAcronym = optionData.subjectAcronym;
                option.dataset.subjectLabel = optionData.subjectLabel;
                option.dataset.curricularYear = optionData.curricularYear || '';
                option.dataset.term = optionData.term || '';
                option.dataset.contextUnavailable = '';
                if (optionData.title) {
                    option.title = optionData.title;
                }
                subject.appendChild(option);
                count += 1;
            });

            var canKeepPreviousValue = previousValue && Array.prototype.some.call(subject.options, function (option) {
                return option.value === previousValue;
            });
            subject.value = canKeepPreviousValue ? previousValue : '';
            subject.disabled = !selectedCourseReady || count === 0;
            setWrapperState(subjectWrapper, selectedCourseReady);
            refreshSelectUi(subject);
        }

        function syncValidity() {
            course.setCustomValidity('');
            if (!course.disabled && selectedCourseUnavailable()) {
                course.setCustomValidity('Selected course does not have a complete class group context.');
            } else if (!course.disabled && !course.value) {
                course.setCustomValidity('Select a course before selecting a subject.');
            }
            if (subjectIsLocked) {
                return;
            }
            subject.setCustomValidity('');
            if (!subject.disabled && !subject.value) {
                subject.setCustomValidity('Select a subject after selecting a course.');
            }
        }

        function syncContext() {
            markCourseAvailability();
            setWrapperState(courseWrapper, !course.disabled);
            rebuildSubjectOptions();
            syncValidity();
            refreshSelectUi(course);
            form.dispatchEvent(new CustomEvent('gape:class-group-context-sync', { bubbles: true }));
        }

        initializeSelectUi(course, courseTemplate, selects);
        if (!subjectIsLocked) {
            initializeSelectUi(subject, subjectTemplate, selects);
        }
        window.jQuery(course)
                .off('change.gapeClassGroupContext select2:select.gapeClassGroupContext select2:clear.gapeClassGroupContext')
                .on('change.gapeClassGroupContext select2:select.gapeClassGroupContext select2:clear.gapeClassGroupContext', syncContext);
        if (!subjectIsLocked) {
            window.jQuery(subject)
                    .off('change.gapeClassGroupContext select2:select.gapeClassGroupContext select2:clear.gapeClassGroupContext')
                    .on('change.gapeClassGroupContext select2:select.gapeClassGroupContext select2:clear.gapeClassGroupContext', syncValidity);
        }
        syncContext();
    }

    ready(function () {
        document.querySelectorAll('[data-class-group-context-form]').forEach(function (form) {
            form.querySelectorAll('select').forEach(bindSelectGuidance);
            initForm(form);
        });
    });
})();
