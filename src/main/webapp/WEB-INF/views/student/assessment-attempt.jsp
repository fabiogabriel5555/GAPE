<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ include file="/WEB-INF/fragments/student-dashboard-start.jspf" %>

<style>
    .gape-student-rating-picker {
        display: flex;
        flex-direction: column;
        gap: 10px;
    }

    .gape-student-rating-picker__units {
        align-items: center;
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
    }

    .gape-student-rating-unit {
        --rating-fill: 0%;
        align-items: center;
        background: var(--neutral-20);
        border: 1px solid var(--neutral-30);
        border-radius: 8px;
        color: var(--neutral-300, #b8bcc5);
        cursor: pointer;
        display: inline-flex;
        flex: 0 0 auto;
        height: 44px;
        justify-content: center;
        padding: 0;
        position: relative;
        transition: .2s ease;
        width: 44px;
    }

    .gape-student-rating-unit:hover,
    .gape-student-rating-unit:focus-visible {
        border-color: var(--main-600);
        box-shadow: 0 0 0 4px rgba(0, 169, 145, .12);
        color: var(--neutral-500, #6f7785);
        outline: 0;
        transform: translateY(-1px);
    }

    .gape-student-rating-unit.is-half,
    .gape-student-rating-unit.is-full {
        background: var(--main-50);
        border-color: rgba(0, 169, 145, .35);
    }

    .gape-student-rating-unit__base,
    .gape-student-rating-unit__fill {
        align-items: center;
        display: flex;
        font-size: 30px;
        inset: 0;
        justify-content: center;
        line-height: 1;
        pointer-events: none;
        position: absolute;
    }

    .gape-student-rating-unit__fill {
        color: var(--main-600);
        justify-content: flex-start;
        overflow: hidden;
        width: var(--rating-fill);
    }

    .gape-student-rating-unit__fill-icon {
        align-items: center;
        display: flex;
        flex: 0 0 44px;
        height: 44px;
        justify-content: center;
        width: 44px;
    }

    [data-rating-style="hearts"] .gape-student-rating-unit__fill {
        color: var(--danger-500, #e14d63);
    }

    [data-rating-style="circles"] .gape-student-rating-unit__base,
    [data-rating-style="circles"] .gape-student-rating-unit__fill {
        font-size: 28px;
    }

    .gape-student-rating-picker__meta {
        align-items: center;
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
    }

    .gape-student-rating-readout {
        background: var(--neutral-20);
        border: 1px solid var(--neutral-30);
        border-radius: 999px;
        color: var(--neutral-600);
        display: inline-flex;
        font-size: 12px;
        font-weight: 600;
        line-height: 1;
        padding: 8px 10px;
    }

    .gape-student-rating-picker.is-invalid .gape-student-rating-unit {
        border-color: var(--main-600);
    }

    .gape-student-rating-error[hidden] {
        display: none;
    }
</style>

<section class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30">
    <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap mb-24">
        <div class="d-flex align-items-start gap-14 min-w-0">
            <span class="gape-student-class-group-card__icon text-26"><i class="${assessment.iconClass}"></i></span>
            <div class="min-w-0">
                <div class="d-flex align-items-center gap-8 flex-wrap mb-8">
                    <h3 class="text-24 fw-semibold text-neutral-800 mb-0"><c:out value="${assessment.title}"/></h3>
                    <span class="${assessment.softClass} px-12 py-7 rounded-pill text-12"><c:out value="${assessment.typeLabel}"/></span>
                    <span class="${attempt.stateBadgeClass} px-12 py-7 rounded-pill text-12"><c:out value="${attempt.stateLabel}"/></span>
                </div>
                <p class="text-14 text-neutral-500 mb-0"><c:out value="${assessment.contextLabel}"/> | Attempt #${attempt.attemptNumber}</p>
            </div>
        </div>
        <a href="${pageContext.request.contextPath}/student/assessments" class="gape-student-card-icon-button" aria-label="Back to assessments" title="Back to assessments">
            <i class="ph ph-arrow-left"></i>
        </a>
    </div>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger mb-24" role="alert">
            <c:out value="${errorMessage}"/>
        </div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/student/assessments/attempts/${attempt.id}/responses" enctype="multipart/form-data">
        <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
        <div class="d-flex flex-column gap-18">
            <c:forEach var="question" items="${questions}" varStatus="questionLoop">
                <c:set var="savedResponse" value="${responseByQuestionId[question.id]}"/>
                <c:set var="selectedOptionIds" value="${selectedOptionIdsByQuestionId[question.id]}"/>
                <article class="border border-neutral-30 rounded-10 px-20 py-20">
                    <div class="d-flex align-items-start gap-12 mb-16">
                        <span class="w-44 h-44 rounded-8 bg-main-50 text-main-600 d-inline-flex align-items-center justify-content-center text-22 flex-shrink-0">
                            <i class="${question.iconClass}"></i>
                        </span>
                        <div class="min-w-0">
                            <div class="d-flex align-items-center gap-8 flex-wrap mb-6">
                                <span class="text-13 fw-semibold text-main-600">Question ${questionLoop.count}</span>
                                <span class="bg-neutral-20 text-neutral-600 px-10 py-5 rounded-pill text-12"><c:out value="${question.typeLabel}"/></span>
                                <span class="bg-main-50 text-main-600 px-10 py-5 rounded-pill text-12"><c:out value="${question.score}"/> pts</span>
                                <c:if test="${question.required}">
                                    <span class="bg-warning-30 text-warning-600 px-10 py-5 rounded-pill text-12">Required</span>
                                </c:if>
                            </div>
                            <h4 class="text-16 fw-semibold text-neutral-800 mb-0"><c:out value="${question.statement}"/></h4>
                        </div>
                    </div>

                    <c:choose>
                        <c:when test="${question.typeValue == 'rating'}">
                            <div class="gape-student-rating-picker"
                                 data-rating-picker
                                 data-rating-style="${question.ratingStyle}"
                                 data-rating-step="${question.ratingStep}"
                                 data-rating-max="${question.ratingMax}"
                                 data-rating-fractional="${question.ratingFractional}"
                                 data-rating-required="${question.required}">
                                <input type="hidden"
                                       name="question_${question.id}_answer"
                                       value="<c:out value='${savedResponse.answer}'/>"
                                       data-rating-input>
                                <div class="gape-student-rating-picker__units" role="group" aria-label="Rating question ${questionLoop.count}" aria-required="${question.required}">
                                    <c:forEach var="ratingValue" begin="1" end="${question.ratingMax}">
                                        <button type="button"
                                                class="gape-student-rating-unit"
                                                data-rating-unit
                                                data-rating-value="${ratingValue}"
                                                aria-label="${ratingValue} of ${question.ratingMax}"
                                                aria-pressed="false">
                                            <span class="gape-student-rating-unit__base" aria-hidden="true">
                                                <c:choose>
                                                    <c:when test="${question.ratingStyle == 'circles'}"><i class="ph ph-circle"></i></c:when>
                                                    <c:when test="${question.ratingStyle == 'hearts'}"><i class="ph ph-heart"></i></c:when>
                                                    <c:otherwise><i class="ph ph-star"></i></c:otherwise>
                                                </c:choose>
                                            </span>
                                            <span class="gape-student-rating-unit__fill" aria-hidden="true">
                                                <span class="gape-student-rating-unit__fill-icon">
                                                    <c:choose>
                                                        <c:when test="${question.ratingStyle == 'circles'}"><i class="ph-fill ph-circle"></i></c:when>
                                                        <c:when test="${question.ratingStyle == 'hearts'}"><i class="ph-fill ph-heart"></i></c:when>
                                                        <c:otherwise><i class="ph-fill ph-star"></i></c:otherwise>
                                                    </c:choose>
                                                </span>
                                            </span>
                                        </button>
                                    </c:forEach>
                                </div>
                                <div class="gape-student-rating-picker__meta">
                                    <span class="gape-student-rating-readout" data-rating-readout aria-live="polite">No rating</span>
                                    <span class="gape-student-rating-error text-12 text-main-600" data-rating-error hidden>Please choose a rating.</span>
                                </div>
                            </div>
                        </c:when>
                        <c:when test="${false}">
                            <select name="question_${question.id}_option" class="form-select px-16 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-8" ${question.required ? 'required' : ''}>
                                <option value="">Select an option</option>
                                <c:forEach var="option" items="${question.activeOptions}">
                                    <c:set var="optionSelected" value="${false}"/>
                                    <c:forEach var="selectedOptionId" items="${selectedOptionIds}">
                                        <c:if test="${selectedOptionId == option.id}">
                                            <c:set var="optionSelected" value="${true}"/>
                                        </c:if>
                                    </c:forEach>
                                    <option value="${option.id}" ${optionSelected ? 'selected' : ''}>
                                        <c:out value="${option.text}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </c:when>
                        <c:when test="${question.allowsOptions}">
                            <div class="d-flex flex-column gap-10">
                                <c:forEach var="option" items="${question.activeOptions}">
                                    <c:set var="optionSelected" value="${false}"/>
                                    <c:forEach var="selectedOptionId" items="${selectedOptionIds}">
                                        <c:if test="${selectedOptionId == option.id}">
                                            <c:set var="optionSelected" value="${true}"/>
                                        </c:if>
                                    </c:forEach>
                                    <label class="d-flex align-items-start gap-10 border border-neutral-30 rounded-8 px-14 py-12 bg-neutral-20 text-14 text-neutral-700 mb-0">
                                        <input type="${question.singleSelectedOption ? 'radio' : 'checkbox'}"
                                               name="question_${question.id}_option"
                                               value="${option.id}"
                                               ${question.required and question.singleSelectedOption ? 'required' : ''}
                                               data-required-option-group="question_${question.id}_option"
                                               data-required-option="${question.required and not question.singleSelectedOption}"
                                               ${optionSelected ? 'checked' : ''}>
                                        <span><c:out value="${option.text}"/></span>
                                    </label>
                                </c:forEach>
                            </div>
                        </c:when>
                        <c:when test="${question.fileUpload}">
                            <label for="questionAttachment${question.id}" class="text-13 fw-medium text-neutral-700 mb-8">Upload file</label>
                            <input type="hidden" name="question_${question.id}_attachment_existing" value="<c:out value='${savedResponse.attachment}'/>">
                            <input id="questionAttachment${question.id}" name="question_${question.id}_file" type="file" accept="<c:out value='${question.acceptedFileAcceptAttribute}'/>" class="form-control px-16 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-8" ${question.required and empty savedResponse.attachment ? 'required' : ''}>
                            <p class="text-12 text-neutral-500 mt-8 mb-0">
                                Accepted: <c:out value="${question.acceptedFileFormatsLabel}"/>. Limits: <c:out value="${uploadLimitLabelByQuestionId[question.id]}"/>.
                            </p>
                            <c:if test="${not empty savedResponse.attachment}">
                                <p class="text-12 text-neutral-500 mt-8 mb-0">
                                    Current file:
                                    <a class="text-main-600 fw-semibold hover-text-main-700" href="${pageContext.request.contextPath}/student/assessments/responses/${savedResponse.id}/attachment">
                                        <i class="ph ph-download-simple me-4"></i><c:out value="${savedResponse.attachmentFileName}"/>
                                    </a>
                                </p>
                            </c:if>
                        </c:when>
                        <c:when test="${question.paragraph}">
                            <textarea name="question_${question.id}_answer" rows="5" class="form-control px-16 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-8" ${question.required ? 'required' : ''}><c:out value="${savedResponse.answer}"/></textarea>
                        </c:when>
                        <c:otherwise>
                            <input name="question_${question.id}_answer" value="<c:out value='${savedResponse.answer}'/>" class="form-control px-16 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-8" ${question.required ? 'required' : ''} placeholder="Your answer">
                        </c:otherwise>
                    </c:choose>
                </article>
            </c:forEach>

            <c:if test="${empty questions}">
                <div class="gape-student-empty text-center px-24 py-40">
                    <span class="gape-student-icon gape-student-soft-blue text-28 mb-16"><i class="ph ph-seal-question"></i></span>
                    <h4 class="text-18 fw-semibold text-neutral-800 mb-8">No questions available</h4>
                    <p class="text-14 text-neutral-500 mb-0">This assessment has no questions.</p>
                </div>
            </c:if>
        </div>

        <div class="d-flex align-items-center justify-content-between gap-14 flex-wrap border-top-dashed pt-24 mt-28">
            <div class="text-13 text-neutral-500">
                Started at <c:out value="${attempt.startedAt}"/>
            </div>
            <div class="d-flex align-items-center gap-10 flex-wrap">
                <button type="submit" class="border-main-600 border px-20 py-10 fw-semibold rounded-8 hover-bg-main-50 transition-03">
                    <i class="ph ph-floppy-disk me-8"></i>Save
                </button>
                <button type="submit" formaction="${pageContext.request.contextPath}/student/assessments/attempts/${attempt.id}/submit" class="bg-main-600 px-20 py-10 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03" ${empty questions ? 'disabled' : ''}>
                    <i class="ph ph-paper-plane-tilt me-8"></i>Submit
                </button>
            </div>
        </div>
    </form>
</section>

<script>
    (function () {
        function parseRating(value) {
            var parsed = parseFloat(String(value || '').replace(',', '.'));
            return Number.isFinite(parsed) ? parsed : 0;
        }

        function formatRating(value, allowZero) {
            if (value < 0 || (value === 0 && !allowZero)) {
                return '';
            }
            return Number.isInteger(value) ? String(value) : value.toFixed(1);
        }

        function clampRating(value, max, fractional) {
            var bounded = Math.max(0, Math.min(max, value));
            var stepped = fractional ? Math.round(bounded * 2) / 2 : Math.round(bounded);
            return Math.max(0, Math.min(max, stepped));
        }

        function readoutLabel(value, max) {
            return value > 0 ? formatRating(value, true) + ' / ' + max : 'No rating';
        }

        function setPickerInvalid(picker, invalid) {
            var error = picker.querySelector('[data-rating-error]');
            picker.classList.toggle('is-invalid', invalid);
            if (error) {
                error.hidden = !invalid;
            }
        }

        function updatePicker(picker, value) {
            var max = parseInt(picker.dataset.ratingMax || '5', 10);
            var fractional = picker.dataset.ratingFractional === 'true' || picker.dataset.ratingStep === 'half';
            var normalized = clampRating(value, max, fractional);
            var input = picker.querySelector('[data-rating-input]');
            var readout = picker.querySelector('[data-rating-readout]');

            var touched = picker.dataset.ratingTouched === 'true' || (input && input.value !== '');
            if (input) {
                input.value = formatRating(normalized, touched);
            }
            if (readout) {
                readout.textContent = touched ? formatRating(normalized, true) + ' / ' + max : readoutLabel(normalized, max);
            }

            Array.prototype.slice.call(picker.querySelectorAll('[data-rating-unit]')).forEach(function (unit) {
                var unitValue = parseInt(unit.dataset.ratingValue || '0', 10);
                var fill = 0;
                if (normalized >= unitValue) {
                    fill = 100;
                } else if (fractional && normalized === unitValue - 0.5) {
                    fill = 50;
                }
                unit.style.setProperty('--rating-fill', fill + '%');
                unit.classList.toggle('is-full', fill === 100);
                unit.classList.toggle('is-half', fill === 50);
                unit.setAttribute('aria-pressed', fill > 0 ? 'true' : 'false');
            });

            if (normalized > 0) {
                setPickerInvalid(picker, false);
            }
        }

        function ratingAfterClick(current, unitValue, max, fractional) {
            if (fractional) {
                var halfValue = unitValue - 0.5;
                if (current < halfValue || current > unitValue) {
                    return clampRating(unitValue - 0.5, max, true);
                }
                if (current < unitValue) {
                    return clampRating(unitValue, max, true);
                }
                return clampRating(unitValue - 1, max, true);
            }
            return clampRating(current >= unitValue ? unitValue - 1 : unitValue, max, false);
        }

        function moveRating(picker, direction) {
            var max = parseInt(picker.dataset.ratingMax || '5', 10);
            var fractional = picker.dataset.ratingFractional === 'true' || picker.dataset.ratingStep === 'half';
            var step = fractional ? 0.5 : 1;
            var input = picker.querySelector('[data-rating-input]');
            var current = clampRating(parseRating(input ? input.value : ''), max, fractional);
            updatePicker(picker, current + (direction * step));
        }

        Array.prototype.slice.call(document.querySelectorAll('[data-rating-picker]')).forEach(function (picker) {
            var max = parseInt(picker.dataset.ratingMax || '5', 10);
            var fractional = picker.dataset.ratingFractional === 'true' || picker.dataset.ratingStep === 'half';
            var input = picker.querySelector('[data-rating-input]');
            updatePicker(picker, clampRating(parseRating(input ? input.value : ''), max, fractional));

            picker.addEventListener('click', function (event) {
                var unit = event.target.closest('[data-rating-unit]');
                if (!unit || !picker.contains(unit)) {
                    return;
                }
                var unitValue = parseInt(unit.dataset.ratingValue || '0', 10);
                var current = clampRating(parseRating(input ? input.value : ''), max, fractional);
                picker.dataset.ratingTouched = 'true';
                updatePicker(picker, ratingAfterClick(current, unitValue, max, fractional));
            });

            picker.addEventListener('keydown', function (event) {
                if (!event.target.closest('[data-rating-unit]')) {
                    return;
                }
                if (event.key === 'ArrowRight' || event.key === 'ArrowUp') {
                    event.preventDefault();
                    picker.dataset.ratingTouched = 'true';
                    moveRating(picker, 1);
                }
                if (event.key === 'ArrowLeft' || event.key === 'ArrowDown') {
                    event.preventDefault();
                    picker.dataset.ratingTouched = 'true';
                    moveRating(picker, -1);
                }
                if (event.key === 'Home') {
                    event.preventDefault();
                    picker.dataset.ratingTouched = 'true';
                    updatePicker(picker, 0);
                }
                if (event.key === 'End') {
                    event.preventDefault();
                    picker.dataset.ratingTouched = 'true';
                    updatePicker(picker, max);
                }
            });
        });

        Array.prototype.slice.call(document.querySelectorAll('form')).forEach(function (form) {
            form.addEventListener('submit', function (event) {
                var firstInvalid = null;
                Array.prototype.slice.call(form.querySelectorAll('[data-rating-picker]')).forEach(function (picker) {
                    var input = picker.querySelector('[data-rating-input]');
                    var invalid = picker.dataset.ratingRequired === 'true' && !formatRating(parseRating(input ? input.value : ''), input && input.value !== '');
                    setPickerInvalid(picker, invalid);
                    if (invalid && !firstInvalid) {
                        firstInvalid = picker.querySelector('[data-rating-unit]');
                    }
                });
                var requiredGroups = {};
                Array.prototype.slice.call(form.querySelectorAll('[data-required-option="true"]')).forEach(function (input) {
                    requiredGroups[input.dataset.requiredOptionGroup] = true;
                });
                Object.keys(requiredGroups).forEach(function (groupName) {
                    var groupInputs = Array.prototype.slice.call(form.querySelectorAll('[name="' + groupName + '"]'));
                    var checked = groupInputs.some(function (input) {
                        return input.checked;
                    });
                    groupInputs.forEach(function (input, index) {
                        input.setCustomValidity(!checked && index === 0 ? 'Select at least one option.' : '');
                    });
                    if (!checked && !firstInvalid && groupInputs.length > 0) {
                        firstInvalid = groupInputs[0];
                    }
                });
                if (firstInvalid) {
                    event.preventDefault();
                    form.reportValidity();
                    firstInvalid.focus();
                }
            });
        });
    })();
</script>

<%@ include file="/WEB-INF/fragments/student-dashboard-end.jspf" %>
