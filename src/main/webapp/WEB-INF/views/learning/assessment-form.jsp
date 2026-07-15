<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - ${creating ? 'Create Assessment' : 'Edit Assessment'}</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        .gape-assessment-context-field select:disabled {
            background-color: #f5f6f8;
            border-color: #d8dde6;
            color: #7b8494;
            cursor: not-allowed;
        }

        .gape-assessment-context-field.is-disabled .select2-container--default .select2-selection--single {
            background-color: #f5f6f8;
            border-color: #d8dde6;
            cursor: not-allowed;
        }

        .gape-assessment-context-field.is-disabled .select2-container--default .select2-selection--single .select2-selection__rendered {
            color: #7b8494;
        }

        .gape-assessment-context-field.is-disabled label {
            color: #7b8494;
        }

        .gape-eduall-select-dropdown .select2-results__option {
            line-height: 1.35;
            white-space: normal;
        }

        .gape-eduall-select-dropdown .select2-results__option.gape-assessment-context-option-row {
            margin-bottom: 2px;
            padding: 10px 14px;
        }

        .gape-eduall-select-dropdown .select2-results__option--disabled {
            display: none;
        }

        .gape-eduall-select-dropdown .gape-assessment-context-option {
            display: block;
        }

        .gape-assessment-course-option,
        .gape-eduall-select-dropdown .gape-assessment-course-option {
            align-items: baseline;
            color: inherit;
            display: flex;
            font-size: 14px;
            font-weight: inherit;
            gap: 16px;
            justify-content: space-between;
            line-height: inherit;
            min-width: 0;
            overflow: hidden;
            width: 100%;
        }

        .gape-assessment-course-option-main {
            flex: 1 1 0;
            min-width: 0;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .gape-assessment-course-option-context {
            color: #64748b;
            flex: 0 0 auto;
            font-size: 11px;
            font-weight: 600;
            line-height: 1.2;
            margin-left: auto;
            max-width: 42%;
            overflow: hidden;
            text-align: right;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .gape-eduall-select-dropdown .gape-assessment-course-option-context {
            color: #64748b;
            opacity: .88;
        }

        .select2-container--default .select2-selection--single .select2-selection__rendered .gape-assessment-course-option {
            align-items: baseline;
            column-gap: 12px;
            display: grid !important;
            grid-template-columns: minmax(0, 1fr) auto;
            max-width: 100%;
            overflow: hidden;
            padding-right: 0;
        }

        .select2-container--default .select2-selection--single .select2-selection__rendered .gape-assessment-course-option-context {
            color: #7b8494;
            justify-self: end;
            margin-left: 0;
            max-width: 100%;
            min-width: 0;
        }

        .select2-container--default .select2-selection--single .select2-selection__rendered .gape-assessment-context-option:not(.gape-assessment-course-option) {
            display: block;
            max-width: 100%;
            min-width: 0;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .gape-eduall-select-dropdown .gape-assessment-context-option.gape-assessment-context-option-muted {
            color: #94a3b8;
            font-size: 14px;
            font-weight: 600;
            font-style: normal;
            line-height: 1.3;
        }

        .gape-eduall-select-dropdown .select2-results__option.select2-results__option--selectable:hover .gape-assessment-context-option {
            color: var(--main-600);
        }

        .gape-eduall-select-dropdown .select2-results__option.select2-results__option--selectable:hover .gape-assessment-context-option-muted {
            color: #64748b;
        }

        .gape-eduall-select-dropdown .select2-results__option.select2-results__option--selectable:hover .gape-assessment-course-option-context {
            color: #475569;
        }

        .gape-eduall-select-dropdown .select2-results__option.select2-results__option--selected.select2-results__option--selectable:hover .gape-assessment-course-option,
        .gape-eduall-select-dropdown .select2-results__option.select2-results__option--selected.select2-results__option--selectable:hover .gape-assessment-course-option-main,
        .gape-eduall-select-dropdown .select2-results__option.select2-results__option--selected.select2-results__option--selectable:hover .gape-assessment-course-option-context {
            color: #fff;
        }

        .gape-eduall-select-dropdown .select2-results__option.select2-results__option--selected.select2-results__option--selectable:hover .gape-assessment-context-option {
            color: #fff;
        }

        .gape-eduall-select-dropdown .select2-results__option--selected .gape-assessment-context-option,
        .gape-eduall-select-dropdown .select2-results__option--selected:hover .gape-assessment-context-option,
        .select2-container--default .gape-eduall-select-dropdown .select2-results__option--highlighted.select2-results__option--selectable:not(:hover) .gape-assessment-context-option {
            color: #fff;
        }

        .gape-eduall-select-dropdown .select2-results__option--selected .gape-assessment-course-option-context,
        .gape-eduall-select-dropdown .select2-results__option--selected:hover .gape-assessment-course-option-context,
        .select2-container--default .gape-eduall-select-dropdown .select2-results__option--highlighted.select2-results__option--selectable:not(:hover) .gape-assessment-course-option-context {
            color: rgba(255, 255, 255, .78);
        }
    </style>
</head>
<body>
<div class="preloader">
    <img src="${pageContext.request.contextPath}/assets/images/icons/preloader.gif" alt="">
</div>
<div class="overlay"></div>
<div class="side-overlay"></div>
<div class="dashbord bg-main-25 w-100 overflow-hidden">
    <div class="d-flex">
        <%@ include file="/WEB-INF/fragments/dashboard-sidebar.jspf" %>
        <div class="dashbord-body flex-grow-1 d-flex flex-column min-vh-100">
            <%@ include file="/WEB-INF/fragments/dashboard-topbar.jspf" %>
            <div class="px-24 py-24 flex-grow-1">
                <%@ include file="/WEB-INF/fragments/flash-messages.jspf" %>

                <c:set var="assessmentFormAction" value="${pageContext.request.contextPath}/learning/assessments"/>
                <c:set var="assessmentBackUrl" value="${pageContext.request.contextPath}/learning/assessments"/>
                <c:if test="${not creating}">
                    <c:set var="assessmentFormAction" value="${pageContext.request.contextPath}/learning/assessments/${form.id}"/>
                    <c:set var="assessmentBackUrl" value="${pageContext.request.contextPath}/learning/assessments/${form.id}"/>
                </c:if>

                <form action="${assessmentFormAction}" method="post" class="bg-white rounded-10 px-32 py-32 border border-neutral-30" data-assessment-form data-creating="${creating}">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap border-bottom-dashed pb-24 mb-28">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">${creating ? 'Create Assessment' : 'Edit Assessment'}</h2>
                            <span class="text-14 text-neutral-500">Configure type, context, grading, availability and correction mode.</span>
                        </div>
                        <a href="${assessmentBackUrl}" class="border-main-600 border px-20 py-10 fw-semibold rounded-8 hover-bg-main-50 transition-03">Back</a>
                    </div>

                    <c:if test="${not empty errorMessage}">
                        <div class="alert alert-danger mb-24" role="alert">
                            <c:out value="${errorMessage}"/>
                        </div>
                    </c:if>

                    <div class="row gy-4">
                        <div class="col-lg-8">
                            <label for="title" class="fw-medium text-base text-neutral-800 mb-12">Title</label>
                            <input id="title" name="title" type="text" maxlength="160" required value="<c:out value='${form.title}'/>" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                        </div>
                        <div class="col-lg-3">
                            <label class="fw-medium text-base text-neutral-800 mb-12 d-block">State</label>
                            <span class="d-inline-flex align-items-center gap-8 px-16 py-14 bg-neutral-20 border border-neutral-30 rounded-8 text-14 fw-semibold text-neutral-700" data-assessment-state-display>Draft</span>
                        </div>
                        <div class="col-12">
                            <label for="description" class="fw-medium text-base text-neutral-800 mb-12">Description</label>
                            <textarea id="description" name="description" maxlength="500" rows="3" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8"><c:out value="${form.description}"/></textarea>
                        </div>

                        <div class="col-lg-3">
                            <label for="mode" class="fw-medium text-base text-neutral-800 mb-12">Mode</label>
                            <select id="mode" name="mode" required class="form-select px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                <option value="online" ${form.mode == 'online' ? 'selected' : ''}>Live</option>
                                <option value="onsite" ${form.mode == 'onsite' ? 'selected' : ''}>In-Person</option>
                            </select>
                        </div>
                        <div class="col-lg-3">
                            <label for="correctionMode" class="fw-medium text-base text-neutral-800 mb-12">Correction</label>
                            <select id="correctionMode" name="correctionMode" required class="form-select px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                    <option value="automatic" ${form.correctionMode == 'automatic' ? 'selected' : ''}>Automatic</option>
                                    <option value="mixed" ${form.correctionMode == 'mixed' ? 'selected' : ''}>Mixed</option>
                                    <option value="manual" ${form.correctionMode == 'manual' ? 'selected' : ''}>Manual</option>
                                </select>
                            <span class="text-12 text-neutral-500 mt-8 d-none" data-correction-mode-note>In-person assessments require manual correction.</span>
                            </div>
                            <div class="col-lg-6 gape-select-field gape-assessment-context-field" data-course-context>
                                <label for="courseId" class="fw-medium text-base text-neutral-800 mb-12">Course</label>
                                <select id="courseId" required class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 gape-eduall-select" data-context-course-filter data-assessment-course>
                                    <option value="">Select course</option>
                                    <c:forEach var="course" items="${courseOptions}">
                                        <option value="${course.value}" title="<c:out value='${course.title}'/>" data-organization-id="${course.organizationId}" data-organic-unit-ids="${course.organicUnitIds}" ${course.selected ? 'selected' : ''}>
                                            <c:out value="${course.label}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-lg-4 gape-select-field gape-assessment-context-field opacity-75 is-disabled" data-subject-context>
                                <label for="subjectId" class="fw-medium text-base text-neutral-800 mb-12">Subject</label>
                                <select id="subjectId" name="subjectId" required class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 gape-eduall-select" data-assessment-subject disabled>
                                    <option value="">Select subject</option>
                                    <c:forEach var="subject" items="${subjectOptions}">
                                        <option value="${subject.value}" title="<c:out value='${subject.title}'/>" data-organization-id="${subject.organizationId}" data-organic-unit-ids="${subject.organicUnitIds}" data-course-ids="${subject.courseIds}" data-subject-id="${subject.subjectId}" ${subject.selected ? 'selected' : ''}>
                                            <c:out value="${subject.label}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-lg-4 gape-select-field gape-assessment-context-field opacity-75 is-disabled" data-class-group-context>
                                <label for="classGroupIds" class="fw-medium text-base text-neutral-800 mb-12">Class Group</label>
                                <select id="classGroupIds" name="classGroupIds" required class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 gape-eduall-select" data-assessment-class-group disabled>
                                    <option value="">Select class group</option>
                                    <c:forEach var="group" items="${classGroupOptions}">
                                        <option value="${group.value}" title="<c:out value='${group.title}'/>" data-organization-id="${group.organizationId}" data-organic-unit-ids="${group.organicUnitIds}" data-course-ids="${group.courseIds}" data-subject-id="${group.subjectId}" data-class-group-id="${group.classGroupId}" data-starts-at="${group.startsAt}" data-ends-at="${group.endsAt}" ${group.selected ? 'selected' : ''}>
                                            <c:out value="${group.label}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-lg-4 gape-select-field gape-assessment-context-field opacity-75 is-disabled" data-block-context>
                                <label for="contentBlockId" class="fw-medium text-base text-neutral-800 mb-12">Pedagogical Block</label>
                                <select id="contentBlockId" name="contentBlockId" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 gape-eduall-select" data-assessment-block disabled>
                                    <option value="">Select block</option>
                                    <c:forEach var="block" items="${contentBlockOptions}">
                                        <option value="${block.value}" title="<c:out value='${block.title}'/>" data-organization-id="${block.organizationId}" data-organic-unit-ids="${block.organicUnitIds}" data-course-ids="${block.courseIds}" data-subject-id="${block.subjectId}" data-class-group-id="${block.classGroupId}" data-starts-at="${block.startsAt}" data-ends-at="${block.endsAt}" ${block.selected ? 'selected' : ''}>
                                            <c:out value="${block.label}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-lg-4 gape-select-field gape-assessment-context-field opacity-75 is-disabled d-none" data-physical-room-context>
                                <label for="physicalRoomCode" class="fw-medium text-base text-neutral-800 mb-12">Physical Room</label>
                                <select id="physicalRoomCode" name="physicalRoomCode" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 gape-eduall-select" data-assessment-room disabled>
                                    <option value="">Select room</option>
                                    <c:forEach var="room" items="${physicalRoomOptions}">
                                        <option value="${room.value}" title="<c:out value='${room.title}'/>" data-organization-id="${room.organizationId}" data-organic-unit-ids="${room.organicUnitIds}" ${room.selected ? 'selected' : ''}>
                                            <c:out value="${room.label}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>

                        <div class="col-lg-3">
                            <label for="maxGrade" class="fw-medium text-base text-neutral-800 mb-12">Maximum Grade</label>
                            <input id="maxGrade" name="maxGrade" type="number" step="0.01" min="0.01" required value="<c:out value='${form.maxGrade}'/>" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                        </div>
                        <div class="col-lg-3">
                            <label for="passingGrade" class="fw-medium text-base text-neutral-800 mb-12">Passing Grade</label>
                            <input id="passingGrade" name="passingGrade" type="number" step="0.01" min="0" required value="<c:out value='${form.passingGrade}'/>" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                        </div>
                        <div class="col-lg-3">
                            <label for="finalGradeWeight" class="fw-medium text-base text-neutral-800 mb-12">Final Weight (%)</label>
                            <input id="finalGradeWeight" name="finalGradeWeight" type="number" step="0.01" min="0" max="100" required value="<c:out value='${form.finalGradeWeight}'/>" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                        </div>
                        <div class="col-lg-3">
                            <label for="attemptsLimit" class="fw-medium text-base text-neutral-800 mb-12">Attempts Limit</label>
                            <input id="attemptsLimit" name="attemptsLimit" type="number" min="1" value="<c:out value='${form.attemptsLimit}'/>" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8" placeholder="Unlimited">
                        </div>
                        <div class="col-lg-3">
                            <label for="type" class="fw-medium text-base text-neutral-800 mb-12">Type</label>
                            <select id="type" name="type" required class="form-select px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8" data-assessment-type>
                                <option value="exam" ${form.type == 'exam' ? 'selected' : ''}>Exam</option>
                                <option value="test" ${form.type == 'test' ? 'selected' : ''}>Test</option>
                                <option value="form" ${form.type == 'form' ? 'selected' : ''}>Form</option>
                            </select>
                        </div>
                        <div class="col-lg-3">
                            <label for="enrollmentMode" class="fw-medium text-base text-neutral-800 mb-12">Enrollment</label>
                            <select id="enrollmentMode" name="enrollmentMode" required class="form-select px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                <option value="auto_approve" ${form.enrollmentMode == 'auto_approve' ? 'selected' : ''}>Automatic</option>
                                <option value="manual" ${form.enrollmentMode == 'manual' ? 'selected' : ''}>Manual</option>
                            </select>
                        </div>
                        <div class="col-lg-6 gape-context-date-field">
                            <label for="availableFrom" class="fw-medium text-base text-neutral-800 mb-12">Available From</label>
                            <div class="gape-context-date-control">
                                <i class="ph ph-calendar-dots"></i>
                                <input id="availableFrom" name="availableFrom" type="datetime-local" required value="<c:out value='${form.availableFrom}'/>" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                            </div>
                        </div>
                        <div class="col-lg-6 gape-context-date-field">
                            <label for="availableUntil" class="fw-medium text-base text-neutral-800 mb-12">Available Until</label>
                            <div class="gape-context-date-control">
                                <i class="ph ph-calendar-check"></i>
                                <input id="availableUntil" name="availableUntil" type="datetime-local" required value="<c:out value='${form.availableUntil}'/>" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                            </div>
                        </div>
                        <div class="col-12">
                            <div class="gape-date-context" data-date-context>
                                <div class="gape-date-context__main">
                                    <span class="gape-date-context__icon"><i class="ph ph-calendar-dots text-20"></i></span>
                                    <span class="gape-date-context__text">
                                        <strong class="gape-date-context__title" data-date-context-title>Class group window</strong>
                                        <span class="gape-date-context__copy" data-date-context-copy>Select a class group or pedagogical block to calculate availability.</span>
                                    </span>
                                </div>
                                <div class="gape-date-context__actions">
                                    <button type="button" class="gape-date-context__action" data-date-action="fill-start"><i class="ph ph-skip-back"></i>Start</button>
                                    <button type="button" class="gape-date-context__action" data-date-action="fill-end"><i class="ph ph-skip-forward"></i>End</button>
                                    <button type="button" class="gape-date-context__action" data-date-action="fill-window"><i class="ph ph-arrows-out-line-horizontal"></i>Full Window</button>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="d-flex align-items-center gap-14 flex-wrap border-top-dashed pt-24 mt-28">
                        <button type="submit" class="bg-main-600 px-24 py-12 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03">${creating ? 'Create Assessment' : 'Save Changes'}</button>
                        <a href="${assessmentBackUrl}" class="text-neutral-600 fw-semibold hover-text-main-600 transition-03">Cancel</a>
                    </div>
                </form>

            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
<script>
    (function () {
        function ready(callback) {
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', callback);
                return;
            }
            callback();
        }

        ready(function () {
            var form = document.querySelector('[data-assessment-form]');
            if (!form) {
                return;
            }
            var type = form.querySelector('[data-assessment-type]');
            var mode = form.querySelector('#mode');
            var correctionMode = form.querySelector('#correctionMode');
            var correctionModeNote = form.querySelector('[data-correction-mode-note]');
            var stateDisplay = form.querySelector('[data-assessment-state-display]');
            var creating = form.getAttribute('data-creating') === 'true';
            var course = form.querySelector('#courseId');
            var subject = form.querySelector('#subjectId');
            var block = form.querySelector('#contentBlockId');
            var classGroups = form.querySelector('#classGroupIds');
            var physicalRoom = form.querySelector('#physicalRoomCode');
            var availableFrom = form.querySelector('#availableFrom');
            var availableUntil = form.querySelector('#availableUntil');
            var courseWrapper = form.querySelector('[data-course-context]');
            var subjectWrapper = form.querySelector('[data-subject-context]');
            var blockWrapper = form.querySelector('[data-block-context]');
            var classGroupWrapper = form.querySelector('[data-class-group-context]');
            var physicalRoomWrapper = form.querySelector('[data-physical-room-context]');
            var initialBlockOption = block ? block.options[block.selectedIndex] : null;
            var initialBlockClassGroupId = initialBlockOption ? initialBlockOption.getAttribute('data-class-group-id') || '' : '';
            if (initialBlockClassGroupId && classGroups) {
                Array.prototype.forEach.call(classGroups.options, function (option) {
                    if (option.value === initialBlockClassGroupId) {
                        option.selected = true;
                    }
                });
            }

            function organicUnitTokens(option) {
                return option.getAttribute('data-organic-unit-ids') || '';
            }

            function courseTokens(option) {
                return option.getAttribute('data-course-ids') || '';
            }

            function tokenListContains(tokens, value) {
                return !value || tokens.indexOf('|' + value + '|') !== -1;
            }

            function tokenList(tokens) {
                return (tokens || '').split('|').filter(function (value) {
                    return value;
                });
            }

            function tokenListHasAny(tokens, candidates) {
                if (!candidates.length) {
                    return !tokens;
                }
                return candidates.some(function (candidate) {
                    return tokenListContains(tokens, candidate);
                });
            }

            function selectedCourseValue() {
                return course && course.value ? course.value : '';
            }

            function selectedSubjectValue() {
                return subject && subject.value ? subject.value : '';
            }

            function selectedClassGroupValue() {
                return classGroups && classGroups.value ? classGroups.value : '';
            }

            function normalizeCourseLabel(label) {
                var parts = (label || '').split('|').map(function (part) {
                    return part.trim();
                }).filter(function (part) {
                    return part;
                });
                if (parts.length < 2) {
                    return (label || '').trim();
                }
                if (parts[0].indexOf(' - ') !== -1) {
                    return parts.join(' | ');
                }
                return parts[0] + ' - ' + parts[1]
                        + (parts.length > 2 ? ' | ' + parts.slice(2).join(' | ') : '');
            }

            function splitCourseLabel(label) {
                var normalized = normalizeCourseLabel(label);
                var parts = normalized.split('|').map(function (part) {
                    return part.trim();
                }).filter(function (part) {
                    return part;
                });
                return {
                    main: parts[0] || normalized,
                    context: parts.length > 1 ? parts.slice(1).join(' | ') : ''
                };
            }

            function leadingContextLabel(label) {
                var parts = (label || '').split('|');
                return (parts[0] || label || '').trim();
            }

            function trailingContextLabel(label) {
                var parts = (label || '').split('|');
                if (parts.length > 1) {
                    return parts.slice(1).join('|').trim();
                }
                return (label || '').trim();
            }

            function classGroupHasPedagogicalBlock(classGroupId, courseId, subjectId) {
                if (!block || !classGroupId) {
                    return false;
                }
                return Array.prototype.some.call(block.options, function (option) {
                    return option.value
                            && option.getAttribute('data-class-group-id') === classGroupId
                            && (!subjectId || option.getAttribute('data-subject-id') === subjectId)
                            && tokenListContains(courseTokens(option), courseId);
                });
            }

            function classGroupOptionHasPedagogicalBlock(option, courseId, subjectId) {
                if (!option || !option.value) {
                    return false;
                }
                var optionSubjectId = option.getAttribute('data-subject-id') || '';
                var classGroupId = option.getAttribute('data-class-group-id') || option.value;
                if ((!courseId || tokenListContains(courseTokens(option), courseId))
                        && (!subjectId || optionSubjectId === subjectId)) {
                    return Array.prototype.some.call(block.options, function (blockOption) {
                        return blockOption.value
                                && blockOption.getAttribute('data-class-group-id') === classGroupId
                                && (!optionSubjectId || blockOption.getAttribute('data-subject-id') === optionSubjectId)
                                && tokenListContains(courseTokens(blockOption), courseId)
                                && blockHasCreationContext(blockOption);
                    });
                }
                return false;
            }

            function subjectHasCreationContext(subjectId, courseId) {
                if (!subjectId || !classGroups) {
                    return false;
                }
                return Array.prototype.some.call(classGroups.options, function (option) {
                    return option.value
                            && option.getAttribute('data-subject-id') === subjectId
                            && tokenListContains(courseTokens(option), courseId)
                            && classGroupOptionHasPedagogicalBlock(option, courseId, subjectId);
                });
            }

            function courseHasCreationContext(courseId) {
                if (!courseId || !subject) {
                    return false;
                }
                return Array.prototype.some.call(subject.options, function (option) {
                    return option.value
                            && tokenListContains(courseTokens(option), courseId)
                            && subjectHasCreationContext(option.value, courseId);
                });
            }

            function selectedSubjectIsUnavailable() {
                var value = selectedSubjectValue();
                return !!value && !subjectHasCreationContext(value, selectedCourseValue());
            }

            function selectedClassGroupIsUnavailable() {
                var selected = classGroups && classGroups.value ? classGroups.options[classGroups.selectedIndex] : null;
                return !!selected && !classGroupOptionHasPedagogicalBlock(selected, selectedCourseValue(), selectedSubjectValue());
            }

            function selectedBlockOption() {
                if (!block || !block.value) {
                    return null;
                }
                return block.options[block.selectedIndex] || null;
            }

            function optionMatchesSelectedBlockContext(option) {
                var selectedBlock = selectedBlockOption();
                if (!selectedBlock) {
                    return false;
                }
                var blockOrganizationId = selectedBlock.getAttribute('data-organization-id') || '';
                var blockOrganicUnitIds = tokenList(organicUnitTokens(selectedBlock));
                var optionOrganizationId = option.getAttribute('data-organization-id') || '';
                var optionOrganicUnitIds = organicUnitTokens(option);
                if (!blockOrganizationId || !optionOrganizationId || optionOrganizationId !== blockOrganizationId) {
                    return false;
                }
                return blockOrganicUnitIds.length
                        ? tokenListHasAny(optionOrganicUnitIds, blockOrganicUnitIds)
                        : !optionOrganicUnitIds;
            }

            function blockHasPhysicalRoomContext(option) {
                if (!option || !option.value || !physicalRoom) {
                    return false;
                }
                var blockOrganizationId = option.getAttribute('data-organization-id') || '';
                var blockOrganicUnitIds = tokenList(organicUnitTokens(option));
                return Array.prototype.some.call(physicalRoom.options, function (room) {
                    var roomOrganizationId = room.getAttribute('data-organization-id') || '';
                    var roomOrganicUnitIds = organicUnitTokens(room);
                    return room.value
                            && blockOrganizationId
                            && roomOrganizationId === blockOrganizationId
                            && (blockOrganicUnitIds.length
                                    ? tokenListHasAny(roomOrganicUnitIds, blockOrganicUnitIds)
                                    : !roomOrganicUnitIds);
                });
            }

            function blockHasCreationContext(option) {
                if (!option || !option.value) {
                    return false;
                }
                return !mode || mode.value !== 'onsite' || blockHasPhysicalRoomContext(option);
            }

            function selectedBlockIsUnavailable() {
                var selected = selectedBlockOption();
                return !!selected && !blockHasCreationContext(selected);
            }

            function markOptionAvailability(select, predicate, unavailableTitle) {
                if (!select) {
                    return;
                }
                Array.prototype.forEach.call(select.options, function (option) {
                    if (!option.value) {
                        return;
                    }
                    if (typeof option.dataset.originalTitle === 'undefined') {
                        option.dataset.originalTitle = option.title || '';
                    }
                    var available = predicate(option);
                    option.dataset.contextUnavailable = available ? '' : 'true';
                    option.disabled = false;
                    if (!available) {
                        option.title = unavailableTitle;
                    } else {
                        option.title = option.dataset.originalTitle;
                    }
                });
            }

            function markContextAvailability() {
                markOptionAvailability(course, function (option) {
                    return courseHasCreationContext(option.value);
                }, 'Course has insufficient assessment context.');
                markOptionAvailability(subject, function (option) {
                    return subjectHasCreationContext(option.value, selectedCourseValue());
                }, 'Subject has insufficient assessment context.');
                markOptionAvailability(classGroups, function (option) {
                    return classGroupOptionHasPedagogicalBlock(option, selectedCourseValue(), selectedSubjectValue());
                }, 'Class group has insufficient assessment context.');
                markOptionAvailability(block, blockHasCreationContext, 'Pedagogical block has insufficient assessment context.');
            }

            function selectedCourseIsUnavailable() {
                var value = selectedCourseValue();
                return !!value && !courseHasCreationContext(value);
            }

            function assessmentCourseOptionTemplate(data) {
                var element = data.element;
                var label = splitCourseLabel(data.text || '');
                if (!element || !element.dataset) {
                    return label.context ? label.main + ' | ' + label.context : label.main;
                }
                var option = document.createElement('span');
                option.className = 'gape-assessment-context-option gape-assessment-course-option';
                if (element.dataset.contextUnavailable === 'true') {
                    option.className += ' gape-assessment-context-option-muted';
                }
                var main = document.createElement('span');
                main.className = 'gape-assessment-course-option-main';
                main.textContent = label.main;
                option.appendChild(main);
                if (label.context) {
                    var context = document.createElement('span');
                    context.className = 'gape-assessment-course-option-context';
                    context.textContent = label.context;
                    option.appendChild(context);
                }
                return option;
            }

            function assessmentContextOptionTemplate(data, formatter, optionClassName) {
                var element = findCurrentContextOption(data, null) || data.element;
                if (element && element.value && (element.hidden || element.disabled)) {
                    return null;
                }
                var text = formatter(data.text || '');
                if (!element || !element.dataset) {
                    return text;
                }
                var option = document.createElement('span');
                option.className = 'gape-assessment-context-option ' + optionClassName;
                if (element.dataset.contextUnavailable === 'true') {
                    option.className += ' gape-assessment-context-option-muted';
                }
                option.textContent = text;
                return option;
            }

            function assessmentSubjectOptionTemplate(data) {
                return assessmentContextOptionTemplate(data, normalizeCourseLabel, 'gape-assessment-subject-option');
            }

            function assessmentClassGroupOptionTemplate(data) {
                return assessmentContextOptionTemplate(data, leadingContextLabel, 'gape-assessment-class-group-option');
            }

            function assessmentBlockOptionTemplate(data) {
                return assessmentContextOptionTemplate(data, trailingContextLabel, 'gape-assessment-block-option');
            }

            function assessmentRoomOptionTemplate(data) {
                return assessmentContextOptionTemplate(data, normalizeCourseLabel, 'gape-assessment-room-option');
            }

            function rewriteOptionLabels(select, formatter) {
                if (!select) {
                    return;
                }
                Array.prototype.forEach.call(select.options, function (option) {
                    if (!option.value) {
                        return;
                    }
                    option.textContent = formatter(option.textContent || '');
                });
            }

            function normalizeContextOptionLabels() {
                rewriteOptionLabels(course, normalizeCourseLabel);
                rewriteOptionLabels(subject, normalizeCourseLabel);
                rewriteOptionLabels(classGroups, leadingContextLabel);
                rewriteOptionLabels(block, trailingContextLabel);
            }

            function currentOpenSelect2Element() {
                var container = document.querySelector('.select2-container--open');
                var select = container ? container.previousElementSibling : null;
                return select && select.tagName === 'SELECT' ? select : null;
            }

            function findCurrentContextOption(data, row) {
                if (!data || !data.id) {
                    return null;
                }
                var rowText = (row && row.textContent ? row.textContent : (data.text || '')).trim();
                var exactTextFallback = null;
                var selects = [course, subject, classGroups, block, physicalRoom].filter(function (select) {
                    return !!select;
                });
                for (var selectIndex = 0; selectIndex < selects.length; selectIndex += 1) {
                    var match = Array.prototype.find.call(selects[selectIndex].options, function (option) {
                        return option.value === String(data.id) && (!rowText || option.textContent.trim() === rowText);
                    });
                    if (match) {
                        return match;
                    }
                    exactTextFallback = exactTextFallback || Array.prototype.find.call(selects[selectIndex].options, function (option) {
                        return option.value === String(data.id);
                    }) || null;
                }
                return exactTextFallback;
            }

            function select2ResultOptionElement(row) {
                if (!row || !window.jQuery || !window.jQuery.fn || !window.jQuery.fn.select2 || !window.jQuery.fn.select2.amd) {
                    return null;
                }
                var utils = window.jQuery.fn.select2.amd.require('select2/utils');
                var data = utils && utils.GetData ? utils.GetData(row, 'data') : null;
                var contextOption = findCurrentContextOption(data, row);
                if (contextOption) {
                    return contextOption;
                }
                var openSelect = currentOpenSelect2Element();
                if (openSelect && data && data.id) {
                    var currentOption = Array.prototype.find.call(openSelect.options, function (option) {
                        return option.value === String(data.id);
                    });
                    if (currentOption) {
                        return currentOption;
                    }
                }
                return data && data.element ? data.element : null;
            }

            function markMutedCourseOptionRows() {
                document.querySelectorAll('.gape-eduall-select-dropdown .gape-assessment-context-option').forEach(function (option) {
                    var row = option.closest('.select2-results__option');
                    if (row) {
                        var sourceOption = select2ResultOptionElement(row);
                        var muted = sourceOption && sourceOption.dataset
                                ? sourceOption.dataset.contextUnavailable === 'true'
                                : option.classList.contains('gape-assessment-context-option-muted');
                        option.classList.toggle('gape-assessment-context-option-muted', muted);
                        row.classList.add('gape-assessment-context-option-row');
                        row.classList.toggle('gape-assessment-course-option-row', option.classList.contains('gape-assessment-course-option'));
                        row.classList.toggle('gape-assessment-context-option-row-muted', muted);
                    }
                });
            }

            function observeMutedCourseOptionRows() {
                var results = document.querySelector('.gape-eduall-select-dropdown .select2-results__options');
                markMutedCourseOptionRows();
                if (!results || results.dataset.gapeMutedObserver === 'true' || !window.MutationObserver) {
                    return;
                }
                results.dataset.gapeMutedObserver = 'true';
                new MutationObserver(markMutedCourseOptionRows).observe(results, {
                    childList: true,
                    subtree: true
                });
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

            function contextSelectMatcher(params, data) {
                if (data && data.element && data.element.value && (data.element.hidden || data.element.disabled)) {
                    return null;
                }
                var defaults = window.jQuery.fn.select2.defaults.defaults;
                return defaults && defaults.matcher ? defaults.matcher(params, data) : data;
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

            function initializeSelectUi(select, templateResult, dropdownCssClass) {
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
                    dropdownCssClass: dropdownCssClass || 'gape-eduall-select-dropdown',
                    matcher: contextSelectMatcher,
                    templateResult: templateResult,
                    templateSelection: templateResult
                });
                refreshSelectUi(select);
            }

            function initializeContextSelectUi() {
                initializeSelectUi(course, assessmentCourseOptionTemplate, 'gape-eduall-select-dropdown gape-assessment-context-dropdown');
                initializeSelectUi(subject, assessmentSubjectOptionTemplate, 'gape-eduall-select-dropdown gape-assessment-context-dropdown');
                initializeSelectUi(classGroups, assessmentClassGroupOptionTemplate, 'gape-eduall-select-dropdown gape-assessment-context-dropdown');
                initializeSelectUi(block, assessmentBlockOptionTemplate, 'gape-eduall-select-dropdown gape-assessment-context-dropdown');
                initializeSelectUi(physicalRoom, assessmentRoomOptionTemplate, 'gape-eduall-select-dropdown gape-assessment-context-dropdown');
                if (!window.jQuery || !window.jQuery.fn || !window.jQuery.fn.select2) {
                    return;
                }
                if (course) {
                    window.jQuery(course)
                            .off('change.gapeAssessmentContext')
                            .on('change.gapeAssessmentContext', syncContextFilters)
                            .off('select2:select.gapeAssessmentContext select2:clear.gapeAssessmentContext')
                            .on('select2:select.gapeAssessmentContext select2:clear.gapeAssessmentContext', syncContextFilters)
                            .off('select2:close.gapeAssessmentContext')
                            .on('select2:close.gapeAssessmentContext', function () {
                                window.setTimeout(syncContextFilters, 0);
                            })
                            .off('select2:open.gapeAssessmentContextRows')
                            .on('select2:open.gapeAssessmentContextRows', function () {
                                window.setTimeout(observeMutedCourseOptionRows, 0);
                            });
                }
                if (subject) {
                    window.jQuery(subject)
                            .off('change.gapeAssessmentContext')
                            .on('change.gapeAssessmentContext', syncContextFilters)
                            .off('select2:select.gapeAssessmentContext select2:clear.gapeAssessmentContext')
                            .on('select2:select.gapeAssessmentContext select2:clear.gapeAssessmentContext', syncContextFilters)
                            .off('select2:close.gapeAssessmentContext')
                            .on('select2:close.gapeAssessmentContext', function () {
                                window.setTimeout(syncContextFilters, 0);
                            });
                }
                if (classGroups) {
                    window.jQuery(classGroups)
                            .off('change.gapeAssessmentContext')
                            .on('change.gapeAssessmentContext', syncContextFilters)
                            .off('select2:select.gapeAssessmentContext select2:clear.gapeAssessmentContext')
                            .on('select2:select.gapeAssessmentContext select2:clear.gapeAssessmentContext', syncContextFilters)
                            .off('select2:close.gapeAssessmentContext')
                            .on('select2:close.gapeAssessmentContext', function () {
                                window.setTimeout(syncContextFilters, 0);
                            });
                }
                if (block) {
                    window.jQuery(block)
                            .off('change.gapeAssessmentContext')
                            .on('change.gapeAssessmentContext', syncContextFilters)
                            .off('select2:select.gapeAssessmentContext select2:clear.gapeAssessmentContext')
                            .on('select2:select.gapeAssessmentContext select2:clear.gapeAssessmentContext', syncContextFilters)
                            .off('select2:close.gapeAssessmentContext')
                            .on('select2:close.gapeAssessmentContext', function () {
                                window.setTimeout(syncContextFilters, 0);
                            });
                }
                if (physicalRoom) {
                    window.jQuery(physicalRoom)
                            .off('change.gapeAssessmentContext')
                            .on('change.gapeAssessmentContext', syncCorrectionMode)
                            .off('select2:select.gapeAssessmentContext select2:clear.gapeAssessmentContext')
                            .on('select2:select.gapeAssessmentContext select2:clear.gapeAssessmentContext', syncCorrectionMode)
                            .off('select2:close.gapeAssessmentContext')
                            .on('select2:close.gapeAssessmentContext', function () {
                                window.setTimeout(syncCorrectionMode, 0);
                            });
                }
                [course, subject, classGroups, block, physicalRoom].forEach(function (select) {
                    if (!select) {
                        return;
                    }
                    window.jQuery(select)
                            .off('select2:open.gapeAssessmentContextRows')
                            .on('select2:open.gapeAssessmentContextRows', function () {
                                window.setTimeout(observeMutedCourseOptionRows, 0);
                            });
                });
            }

            function syncContextWrapper(wrapper, enabled) {
                if (!wrapper) {
                    return;
                }
                wrapper.classList.toggle('opacity-75', !enabled);
                wrapper.classList.toggle('is-disabled', !enabled);
            }

            function selectedClassGroupIds() {
                if (!classGroups) {
                    return [];
                }
                return Array.prototype.filter.call(classGroups.options, function (option) {
                    return option.selected && option.value;
                }).map(function (option) {
                    return option.value;
                });
            }

            function selectedCourseOption() {
                if (!course || !course.value) {
                    return null;
                }
                return course.options[course.selectedIndex] || null;
            }

            function optionMatchesSelectedCourseContext(option) {
                var selectedCourse = selectedCourseOption();
                if (!selectedCourse) {
                    return false;
                }
                var courseOrganizationId = selectedCourse.getAttribute('data-organization-id') || '';
                var courseOrganicUnitIds = tokenList(organicUnitTokens(selectedCourse));
                var optionOrganizationId = option.getAttribute('data-organization-id') || '';
                var optionOrganicUnitIds = organicUnitTokens(option);
                if (!courseOrganizationId || !optionOrganizationId || optionOrganizationId !== courseOrganizationId) {
                    return false;
                }
                return courseOrganicUnitIds.length
                        ? tokenListHasAny(optionOrganicUnitIds, courseOrganicUnitIds)
                        : !optionOrganicUnitIds;
            }

            function optionMatchesCourse(option) {
                return course
                        && course.value
                        && tokenListContains(courseTokens(option), course.value);
            }

            function optionMatchesSubject(option) {
                return optionMatchesCourse(option)
                        && subject
                        && subject.value
                        && option.getAttribute('data-subject-id') === subject.value;
            }

            function optionMatchesClassGroup(option) {
                var classGroupId = option.getAttribute('data-class-group-id') || '';
                return optionMatchesSubject(option)
                        && classGroupId
                        && selectedClassGroupIds().indexOf(classGroupId) !== -1;
            }

            function filterSelect(select, predicate) {
                if (!select) {
                    return 0;
                }
                var count = 0;
                Array.prototype.forEach.call(select.options, function (option) {
                    if (!option.value) {
                        option.hidden = false;
                        option.disabled = false;
                        return;
                    }
                    var visible = predicate(option);
                    option.hidden = !visible;
                    option.disabled = !visible;
                    clearSelect2OptionData(option);
                    if (!visible && option.selected) {
                        option.selected = false;
                    }
                    if (visible) {
                        count += 1;
                    }
                });
                refreshSelectUi(select);
                return count;
            }

            function syncContextFilters() {
                markContextAvailability();
                var courseCount = filterSelect(course, function (option) {
                    return !!option.value;
                });
                if (course) {
                    course.disabled = courseCount === 0;
                }
                var selectedCourseReady = !!selectedCourseValue() && !selectedCourseIsUnavailable();
                var subjectCount = filterSelect(subject, function (option) {
                    return selectedCourseReady && optionMatchesCourse(option);
                });
                if (subject) {
                    subject.disabled = !selectedCourseReady || subjectCount === 0;
                }
                var selectedSubjectReady = selectedCourseReady && !!selectedSubjectValue() && !selectedSubjectIsUnavailable();
                var classGroupCount = filterSelect(classGroups, function (option) {
                    return selectedSubjectReady && optionMatchesSubject(option);
                });
                if (classGroups) {
                    classGroups.disabled = !selectedSubjectReady || classGroupCount === 0;
                }
                var selectedClassGroupReady = selectedSubjectReady && !!selectedClassGroupValue() && !selectedClassGroupIsUnavailable();
                var blockCount = filterSelect(block, function (option) {
                    return selectedClassGroupReady && optionMatchesClassGroup(option);
                });
                if (block) {
                    block.disabled = !selectedClassGroupReady || blockCount === 0;
                }
                var selectedBlockReady = selectedClassGroupReady && !!(block && block.value) && !selectedBlockIsUnavailable();
                var roomCount = filterSelect(physicalRoom, function (option) {
                    return selectedBlockReady && optionMatchesSelectedBlockContext(option);
                });
                if (physicalRoom) {
                    physicalRoom.dataset.contextReady = selectedBlockReady && roomCount > 0 ? 'true' : 'false';
                }
                syncContext();
                syncCorrectionMode();
            }

            function syncContext() {
                var isExam = type && type.value === 'exam';
                var isInPerson = mode && mode.value === 'onsite';
                var courseReady = !!selectedCourseValue() && !selectedCourseIsUnavailable();
                var subjectReady = courseReady && !!selectedSubjectValue() && !selectedSubjectIsUnavailable();
                var classGroupReady = subjectReady && !!selectedClassGroupValue() && !selectedClassGroupIsUnavailable();
                var blockReady = classGroupReady && !!(block && block.value) && !selectedBlockIsUnavailable();
                var hasSubject = subject && subject.value;
                var hasBlock = block && block.value;
                var hasClassGroups = classGroups && Array.prototype.some.call(classGroups.options, function (option) {
                    return option.selected && option.value;
                });
                if (course) {
                    course.setCustomValidity('');
                    if (!course.disabled && selectedCourseIsUnavailable()) {
                        course.setCustomValidity('Selected course does not have a complete assessment context.');
                    } else if (!course.disabled && !course.value) {
                        course.setCustomValidity('Select a course before selecting a subject.');
                    }
                }
                if (subject) {
                    subject.required = true;
                    subject.setCustomValidity('');
                    if (!subject.disabled && selectedSubjectIsUnavailable()) {
                        subject.setCustomValidity('Selected subject does not have a complete assessment context.');
                    } else if (!subject.disabled && !hasSubject) {
                        subject.setCustomValidity('Select a subject after selecting a course.');
                    }
                }
                if (block) {
                    block.required = !isExam || isInPerson;
                    block.setCustomValidity('');
                    if (!block.disabled && selectedBlockIsUnavailable()) {
                        block.setCustomValidity('Selected pedagogical block does not have a complete assessment context.');
                    } else if (!block.disabled && (!isExam || isInPerson) && !hasBlock) {
                        block.setCustomValidity('Select a pedagogical block after selecting a class group.');
                    }
                }
                if (classGroups) {
                    classGroups.required = !hasBlock;
                    classGroups.setCustomValidity('');
                    if (!classGroups.disabled && selectedClassGroupIsUnavailable()) {
                        classGroups.setCustomValidity('Selected class group does not have a complete assessment context.');
                    } else if (!classGroups.disabled && !hasBlock && !hasClassGroups) {
                        classGroups.setCustomValidity('Select a class group before selecting a pedagogical block.');
                    }
                }
                syncContextWrapper(courseWrapper, !!course && !course.disabled);
                syncContextWrapper(subjectWrapper, courseReady);
                syncContextWrapper(classGroupWrapper, subjectReady);
                syncContextWrapper(blockWrapper, classGroupReady);
                syncContextWrapper(physicalRoomWrapper, isInPerson && blockReady && physicalRoom && physicalRoom.dataset.contextReady === 'true');
            }

            function maxDateTime(first, second) {
                if (!first) {
                    return second || '';
                }
                if (!second) {
                    return first;
                }
                return first > second ? first : second;
            }

            function dayStart(dateValue) {
                return dateValue ? dateValue + 'T00:00' : '';
            }

            function dayEnd(dateValue) {
                return dateValue ? dateValue + 'T23:59' : '';
            }

            function formatDateTimeLabel(value) {
                if (!value) {
                    return 'not set';
                }
                if (window.GapeDateTimeFormat) {
                    return window.GapeDateTimeFormat.formatTechnicalDateTime(value);
                }
                var normalized = value.replace('T', ' ');
                var date = normalized.substring(0, 10).split('-');
                var time = normalized.substring(11, 16);
                return date.length === 3 ? date[2] + '-' + date[1] + '-' + date[0] + (time ? ' ' + time.replace(':', '-') + '-00' : '') : value;
            }

            function syncDateContext(dateWindow, lowerBound, upperBound) {
                var panel = form.querySelector('[data-date-context]');
                if (!panel) {
                    return;
                }
                var title = panel.querySelector('[data-date-context-title]');
                var copy = panel.querySelector('[data-date-context-copy]');
                var hasWindow = !!(dateWindow.start && dateWindow.end);
                var usable = hasWindow && (!lowerBound || !upperBound || lowerBound <= upperBound);
                if (title) {
                    title.textContent = hasWindow ? 'Class group window' : 'Availability window';
                }
                if (copy) {
                    copy.textContent = hasWindow
                            ? formatDateTimeLabel(lowerBound || dayStart(dateWindow.start)) + ' to ' + formatDateTimeLabel(upperBound)
                            : 'Select a class group or pedagogical block to calculate availability.';
                }
                panel.querySelectorAll('[data-date-action]').forEach(function (button) {
                    button.disabled = !usable;
                });
            }

            function applyDateAction(action) {
                if (!availableFrom || !availableUntil) {
                    return;
                }
                var now = localMinuteValue();
                var dateWindow = selectedAvailabilityWindow();
                var lowerBound = maxDateTime(creating ? now : '', dayStart(dateWindow.start));
                var upperBound = dayEnd(dateWindow.end);
                if (!dateWindow.start || !dateWindow.end || lowerBound > upperBound) {
                    syncAvailability();
                    return;
                }
                if (action === 'fill-start') {
                    availableFrom.value = lowerBound;
                } else if (action === 'fill-end') {
                    availableUntil.value = upperBound;
                } else if (action === 'fill-window') {
                    availableFrom.value = lowerBound;
                    availableUntil.value = upperBound;
                }
                syncAvailability();
            }

            function selectedClassGroupOption() {
                return classGroups && classGroups.value ? classGroups.options[classGroups.selectedIndex] : null;
            }

            function selectedAvailabilityWindow() {
                var selectedBlock = selectedBlockOption();
                var selectedGroup = selectedClassGroupOption();
                var source = selectedBlock || selectedGroup;
                return {
                    start: source ? (source.getAttribute('data-starts-at') || '') : '',
                    end: source ? (source.getAttribute('data-ends-at') || '') : ''
                };
            }

            function syncAvailability() {
                if (!availableFrom || !availableUntil) {
                    return;
                }
                var now = localMinuteValue();
                var dateWindow = selectedAvailabilityWindow();
                var lowerBound = maxDateTime(creating ? now : '', dayStart(dateWindow.start));
                var upperBound = dayEnd(dateWindow.end);
                availableFrom.min = lowerBound;
                availableFrom.max = upperBound;
                availableUntil.min = maxDateTime(availableFrom.value || lowerBound, dayStart(dateWindow.start));
                availableUntil.max = upperBound;
                availableFrom.required = true;
                availableUntil.required = true;
                availableFrom.setCustomValidity('');
                availableUntil.setCustomValidity('');
                if (dateWindow.start && dateWindow.end && lowerBound > upperBound) {
                    availableFrom.setCustomValidity('The selected context has no available assessment window.');
                }
                if (!availableFrom.value) {
                    availableFrom.setCustomValidity('Assessment availability start is required.');
                }
                if (!availableUntil.value) {
                    availableUntil.setCustomValidity('Assessment availability end is required.');
                }
                if (creating && availableFrom.value && availableFrom.value < now) {
                    availableFrom.setCustomValidity('Availability start cannot be in the past.');
                }
                if (creating && availableUntil.value && availableUntil.value < now) {
                    availableUntil.setCustomValidity('Availability end cannot be in the past.');
                }
                if (dateWindow.start && availableFrom.value && availableFrom.value < dayStart(dateWindow.start)) {
                    availableFrom.setCustomValidity('Availability start cannot be before the class group start date.');
                }
                if (dateWindow.end && availableFrom.value && availableFrom.value > dayEnd(dateWindow.end)) {
                    availableFrom.setCustomValidity('Availability start cannot be after the class group end date.');
                }
                if (dateWindow.start && availableUntil.value && availableUntil.value < dayStart(dateWindow.start)) {
                    availableUntil.setCustomValidity('Availability end cannot be before the class group start date.');
                }
                if (dateWindow.end && availableUntil.value && availableUntil.value > dayEnd(dateWindow.end)) {
                    availableUntil.setCustomValidity('Availability end cannot be after the class group end date.');
                }
                if (availableFrom.value && availableUntil.value && availableUntil.value < availableFrom.value) {
                    availableUntil.setCustomValidity('Availability end cannot be before start.');
                }
                if (stateDisplay) {
                    stateDisplay.textContent = computedStateLabel();
                }
                syncDateContext(dateWindow, lowerBound, upperBound);
            }

            function syncPhysicalRoomContext() {
                if (!physicalRoom) {
                    return;
                }
                var isInPerson = mode && mode.value === 'onsite';
                var selectedBlock = selectedBlockOption();
                var roomCount = 0;
                Array.prototype.forEach.call(physicalRoom.options, function (option) {
                    if (!option.value) {
                        option.hidden = false;
                        option.disabled = false;
                        return;
                    }
                    var visible = isInPerson && !!selectedBlock && optionMatchesSelectedBlockContext(option);
                    option.hidden = !visible;
                    option.disabled = !visible;
                    if (!visible && option.selected) {
                        option.selected = false;
                    }
                    if (visible) {
                        roomCount += 1;
                    }
                });
                physicalRoom.dataset.contextReady = isInPerson && !!selectedBlock && roomCount > 0 ? 'true' : 'false';
                physicalRoom.required = isInPerson && !!selectedBlock && roomCount > 0;
                physicalRoom.disabled = !isInPerson || !selectedBlock || roomCount === 0;
                physicalRoom.setCustomValidity('');
                if (isInPerson && selectedBlock && roomCount === 0) {
                    physicalRoom.setCustomValidity('Selected pedagogical block does not have an available physical room.');
                } else if (isInPerson && selectedBlock && !physicalRoom.value) {
                    physicalRoom.setCustomValidity('Select a physical room for in-person assessments.');
                }
                if (!isInPerson || !selectedBlock) {
                    physicalRoom.value = '';
                }
                if (physicalRoomWrapper) {
                    physicalRoomWrapper.classList.toggle('d-none', !isInPerson);
                    syncContextWrapper(physicalRoomWrapper, isInPerson && !!selectedBlock && roomCount > 0);
                }
                refreshSelectUi(physicalRoom);
            }

            function syncCorrectionMode() {
                if (!mode || !correctionMode) {
                    return;
                }
                var isInPerson = mode.value === 'onsite';
                if (isInPerson) {
                    correctionMode.value = 'manual';
                }
                correctionMode.disabled = isInPerson;
                syncPhysicalRoomContext();
                if (correctionModeNote) {
                    correctionModeNote.classList.toggle('d-none', !isInPerson);
                }
                syncAvailability();
            }

            function localMinuteValue() {
                var now = new Date();
                now.setSeconds(0, 0);
                var month = String(now.getMonth() + 1).padStart(2, '0');
                var day = String(now.getDate()).padStart(2, '0');
                var hours = String(now.getHours()).padStart(2, '0');
                var minutes = String(now.getMinutes()).padStart(2, '0');
                return now.getFullYear() + '-' + month + '-' + day + 'T' + hours + ':' + minutes;
            }

            function computedStateLabel() {
                var now = localMinuteValue();
                if (!availableFrom.value) {
                    return 'Draft';
                }
                if (availableUntil.value && availableUntil.value <= now) {
                    return 'Completed';
                }
                return availableFrom.value <= now ? 'Active' : 'Scheduled';
            }

            function firstSelectedOption(select) {
                if (!select) {
                    return null;
                }
                return Array.prototype.find.call(select.options, function (option) {
                    return option.selected && option.value;
                }) || null;
            }

            function firstCourseToken(option, requireSingle) {
                var tokens = option ? tokenList(courseTokens(option)) : [];
                if (requireSingle && tokens.length !== 1) {
                    return '';
                }
                return tokens.length ? tokens[0] : '';
            }

            function courseHasOption(value) {
                return !!course && Array.prototype.some.call(course.options, function (option) {
                    return option.value === value;
                });
            }

            function hydrateCourseSelection() {
                if (!course) {
                    return;
                }
                var inferredCourse = firstCourseToken(firstSelectedOption(block), false)
                        || firstCourseToken(firstSelectedOption(classGroups), false)
                        || (!course.value ? firstCourseToken(firstSelectedOption(subject), true) : '');
                if (inferredCourse && courseHasOption(inferredCourse)) {
                    course.value = inferredCourse;
                }
            }

            function hydrateInitialContextSelection() {
                var selectedBlock = firstSelectedOption(block);
                var selectedClassGroup = firstSelectedOption(classGroups);
                var contextOption = selectedBlock || selectedClassGroup;
                if (!contextOption) {
                    return;
                }
                hydrateCourseSelection();
                var subjectId = contextOption.getAttribute('data-subject-id') || '';
                if (subject && subjectId) {
                    subject.value = subjectId;
                }
                var classGroupId = contextOption.getAttribute('data-class-group-id') || '';
                if (classGroups && classGroupId) {
                    classGroups.value = classGroupId;
                }
            }

            if (type) {
                type.addEventListener('change', syncContextFilters);
            }
            if (mode) {
                mode.addEventListener('change', syncContextFilters);
            }
            if (physicalRoom) {
                physicalRoom.addEventListener('change', syncCorrectionMode);
            }
            if (subject) {
                subject.addEventListener('change', syncContextFilters);
            }
            if (block) {
                block.addEventListener('change', syncContextFilters);
            }
            if (classGroups) {
                classGroups.addEventListener('change', syncContextFilters);
            }
            if (course) {
                course.addEventListener('change', syncContextFilters);
            }
            if (availableFrom) {
                availableFrom.addEventListener('input', syncAvailability);
            }
            if (availableUntil) {
                availableUntil.addEventListener('input', syncAvailability);
            }
            form.querySelectorAll('[data-date-action]').forEach(function (button) {
                button.addEventListener('click', function () {
                    applyDateAction(button.getAttribute('data-date-action'));
                });
            });
            document.addEventListener('click', function () {
                window.setTimeout(syncContextFilters, 50);
                window.setTimeout(syncContextFilters, 250);
            }, true);
            normalizeContextOptionLabels();
            hydrateInitialContextSelection();
            syncContextFilters();
            initializeContextSelectUi();
            syncContextFilters();
            syncCorrectionMode();
            syncAvailability();
            window.setInterval(syncAvailability, 30000);
        });
    })();
</script>
</body>
</html>
