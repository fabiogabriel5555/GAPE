<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Lesson Form</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        <c:if test="${lessonModal}">
        html, body { background: #fff; min-height: 0; }
        .preloader, .overlay, .side-overlay, .sidebar, .dashboard-sidebar, .dashbord-header, .dashboard-header,
        .gape-dashboard-mobile-menu-slot, .gape-dashboard-page-heading,
        .dashbord-body > .dashboard-footer, .dashbord-body > footer,
        .dashbord-body > .bg-neutral-20.border-top { display: none !important; }
        .dashbord, .dashbord > .d-flex, .dashbord-body { background: #fff !important; display: block !important; min-height: 0 !important; }
        .dashbord-body > .px-24.py-24 { padding: 0 !important; }
        .gape-lesson-form-compact { border: 0 !important; border-radius: 0 !important; }
        </c:if>
        .gape-lesson-context-field select:disabled {
            background-color: #f5f6f8;
            border-color: #d8dde6;
            color: #7b8494;
            cursor: not-allowed;
        }

        .gape-lesson-context-field.is-disabled .select2-container--default .select2-selection--single {
            background-color: #f5f6f8;
            border-color: #d8dde6;
            cursor: not-allowed;
        }

        .gape-lesson-context-field.is-disabled .select2-container--default .select2-selection--single .select2-selection__rendered {
            color: #7b8494;
        }

        .gape-lesson-context-field.is-disabled label {
            color: #7b8494;
        }

        .gape-eduall-select-dropdown .select2-results__option {
            line-height: 1.35;
            white-space: normal;
        }

        .gape-eduall-select-dropdown .select2-results__option.gape-lesson-context-option-row {
            margin-bottom: 2px;
            padding: 10px 14px;
        }

        .gape-eduall-select-dropdown .select2-results__option--disabled {
            display: none;
        }

        .gape-eduall-select-dropdown .gape-lesson-context-option {
            display: block;
        }

        .gape-lesson-course-option,
        .gape-eduall-select-dropdown .gape-lesson-course-option {
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

        .gape-lesson-course-option-main {
            flex: 1 1 0;
            min-width: 0;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .gape-lesson-course-option-context {
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

        .gape-eduall-select-dropdown .gape-lesson-course-option-context {
            color: #64748b;
            opacity: .88;
        }

        .select2-container--default .select2-selection--single .select2-selection__rendered .gape-lesson-course-option {
            align-items: baseline;
            column-gap: 12px;
            display: grid !important;
            grid-template-columns: minmax(0, 1fr) auto;
            max-width: 100%;
            overflow: hidden;
            padding-right: 0;
        }

        .select2-container--default .select2-selection--single .select2-selection__rendered .gape-lesson-course-option-context {
            color: #7b8494;
            justify-self: end;
            margin-left: 0;
            max-width: 100%;
            min-width: 0;
        }

        .select2-container--default .select2-selection--single .select2-selection__rendered .gape-lesson-context-option:not(.gape-lesson-course-option) {
            display: block;
            max-width: 100%;
            min-width: 0;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .gape-eduall-select-dropdown .gape-lesson-context-option.gape-lesson-context-option-muted {
            color: #94a3b8;
            font-size: 14px;
            font-weight: 600;
            font-style: normal;
            line-height: 1.3;
        }

        .gape-eduall-select-dropdown .select2-results__option.select2-results__option--selectable:hover .gape-lesson-context-option {
            color: var(--main-600);
        }

        .gape-eduall-select-dropdown .select2-results__option.select2-results__option--selectable:hover .gape-lesson-context-option-muted {
            color: #64748b;
        }

        .gape-eduall-select-dropdown .select2-results__option.select2-results__option--selectable:hover .gape-lesson-course-option-context {
            color: #475569;
        }

        .gape-eduall-select-dropdown .select2-results__option.select2-results__option--selected.select2-results__option--selectable:hover .gape-lesson-course-option,
        .gape-eduall-select-dropdown .select2-results__option.select2-results__option--selected.select2-results__option--selectable:hover .gape-lesson-course-option-main,
        .gape-eduall-select-dropdown .select2-results__option.select2-results__option--selected.select2-results__option--selectable:hover .gape-lesson-course-option-context {
            color: #fff;
        }

        .gape-eduall-select-dropdown .select2-results__option.select2-results__option--selected.select2-results__option--selectable:hover .gape-lesson-context-option {
            color: #fff;
        }

        .gape-eduall-select-dropdown .select2-results__option--selected .gape-lesson-context-option,
        .gape-eduall-select-dropdown .select2-results__option--selected:hover .gape-lesson-context-option,
        .select2-container--default .gape-eduall-select-dropdown .select2-results__option--highlighted.select2-results__option--selectable:not(:hover) .gape-lesson-context-option {
            color: #fff;
        }

        .gape-eduall-select-dropdown .select2-results__option--selected .gape-lesson-course-option-context,
        .gape-eduall-select-dropdown .select2-results__option--selected:hover .gape-lesson-course-option-context,
        .select2-container--default .gape-eduall-select-dropdown .select2-results__option--highlighted.select2-results__option--selectable:not(:hover) .gape-lesson-course-option-context {
            color: rgba(255, 255, 255, .78);
        }

        .gape-lesson-form-compact label.mb-12,
        .gape-lesson-form-compact .gape-lesson-attendance-spacer.mb-12 {
            margin-bottom: 8px !important;
        }

        .gape-lesson-form-compact .gape-select-field .select2-container--default .select2-selection--single,
        .gape-lesson-form-compact .gape-select-field .select2-selection.gape-eduall-selection,
        .gape-lesson-form-compact .gape-select-field .select2-selection.gape-eduall-select {
            min-height: 50px !important;
        }

        .gape-lesson-form-compact .gape-select-field .select2-container--default .select2-selection--single .select2-selection__rendered {
            padding: 12px 48px 12px 24px !important;
            padding-block: 12px !important;
            padding-inline-end: 48px !important;
            padding-inline-start: 24px !important;
        }

        .gape-lesson-form-compact textarea.form-control {
            min-height: 86px;
        }

        .gape-lesson-form-compact .gape-lesson-attendance-spacer {
            visibility: hidden;
        }

        .gape-lesson-form-compact .gape-lesson-attendance-control {
            min-height: 47px;
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

                <form action="${formAction}" method="post" class="gape-lesson-form-compact bg-white rounded-10 px-24 py-24 border border-neutral-30" data-lesson-form>
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <c:if test="${lessonModal}"><input type="hidden" name="modal" value="1"></c:if>
                    <c:if test="${not empty formReturnTo}">
                        <input type="hidden" name="returnTo" value="<c:out value='${formReturnTo}'/>">
                    </c:if>
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap border-bottom-dashed pb-20 mb-20">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">${creating ? 'Create Lesson' : 'Edit Lesson'}</h2>
                            <span class="text-14 text-neutral-500">Configure schedule, delivery mode and access data.</span>
                        </div>
                        <c:choose>
                            <c:when test="${lessonModal}"><button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-8 hover-bg-main-50 transition-03 bg-white" data-gape-lesson-modal-close>Back</button></c:when>
                            <c:otherwise><a href="${fn:escapeXml(lessonBackHref)}" class="border-main-600 border px-20 py-10 fw-semibold rounded-8 hover-bg-main-50 transition-03">Back</a></c:otherwise>
                        </c:choose>
                    </div>

                    <c:if test="${not empty errorMessage}">
                        <div class="alert alert-danger mb-24" role="alert">
                            <c:out value="${errorMessage}"/>
                        </div>
                    </c:if>

                    <div class="row g-3">
                        <div class="col-lg-8">
                            <label for="title" class="fw-medium text-base text-neutral-800 mb-12">Title</label>
                            <input id="title" name="title" type="text" maxlength="160" required value="<c:out value='${form.title}'/>" class="form-control px-18 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                        </div>
                        <div class="col-lg-4">
                            <label class="fw-medium text-base text-neutral-800 mb-12 d-block">State</label>
                            <span class="d-inline-flex align-items-center gap-8 px-16 py-12 bg-neutral-20 border border-neutral-30 rounded-8 text-14 fw-semibold text-neutral-700" data-lesson-state-display>Draft</span>
                        </div>
                        <div class="col-12">
                            <label for="description" class="fw-medium text-base text-neutral-800 mb-12">Description</label>
                            <textarea id="description" name="description" maxlength="500" rows="2" class="form-control px-18 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-8"><c:out value="${form.description}"/></textarea>
                        </div>
                        <div class="col-xl-5 col-lg-6 gape-select-field gape-lesson-context-field" data-lesson-course-context>
                            <label for="lessonCourseId" class="fw-medium text-base text-neutral-800 mb-12">Course</label>
                            <select id="lessonCourseId" required class="form-select px-24 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-14 gape-eduall-select" data-lesson-course>
                                <option value="">Select course</option>
                            </select>
                        </div>
                        <div class="col-xl-3 col-lg-3 gape-select-field gape-lesson-context-field opacity-75 is-disabled" data-lesson-subject-context>
                            <label for="lessonSubjectId" class="fw-medium text-base text-neutral-800 mb-12">Subject</label>
                            <select id="lessonSubjectId" required class="form-select px-24 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-14 gape-eduall-select" data-lesson-subject disabled>
                                <option value="">Select subject</option>
                            </select>
                        </div>
                        <div class="col-xl-4 col-lg-3 gape-select-field gape-lesson-context-field opacity-75 is-disabled" data-lesson-class-group-field>
                            <label for="classGroupId" class="fw-medium text-base text-neutral-800 mb-12">Class Group</label>
                            <select id="classGroupId" name="classGroupId" required class="form-select px-24 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-14 gape-eduall-select" data-lesson-class-group disabled>
                                <option value="">Select class group</option>
                                <c:forEach var="classGroup" items="${classGroupOptions}">
                                    <option value="${classGroup.id}"
                                            data-organization-id="${classGroup.course.organizationId}"
                                            data-organization-label="<c:out value='${classGroup.course.organizationName}'/>"
                                            data-organization-acronym="<c:out value='${classGroup.course.organizationAcronym}'/>"
                                            data-organic-unit-id="${classGroup.course.organicUnitId}"
                                            data-organic-unit-label="<c:out value='${classGroup.course.organicUnitLabel}'/>"
                                            data-organic-unit-acronym="<c:out value='${classGroup.course.organicUnitAcronym}'/>"
                                            data-course-id="${classGroup.course.id}"
                                            data-course-label="<c:out value='${classGroup.course.name}'/>"
                                            data-course-acronym="<c:out value='${classGroup.course.acronym}'/>"
                                            data-subject-id="${classGroup.subject.id}"
                                            data-subject-label="<c:out value='${classGroup.subject.name}'/>"
                                            data-subject-acronym="<c:out value='${classGroup.subject.acronym}'/>"
                                            data-starts-at="${classGroup.startsAt}"
                                            data-ends-at="${classGroup.endsAt}"
                                            ${form.classGroupId == classGroup.id ? 'selected' : ''}>
                                        <c:out value="${classGroup.code}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-xl-4 col-lg-4 gape-select-field gape-lesson-context-field opacity-75 is-disabled" data-lesson-block-context>
                            <label for="contentBlockId" class="fw-medium text-base text-neutral-800 mb-12">Pedagogical Block</label>
                            <select id="contentBlockId" name="contentBlockId" required class="form-select px-24 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-14 gape-eduall-select" data-lesson-block disabled>
                                <option value="">Select block</option>
                                <c:forEach var="entry" items="${contentBlocksByClassGroup}">
                                    <c:forEach var="block" items="${entry.value}">
                                        <option value="${block.id}" data-class-group-id="${entry.key}" ${form.contentBlockId == block.id ? 'selected' : ''}>
                                            <c:out value="${block.orderNo}"/> - <c:out value="${block.name}"/>
                                        </option>
                                    </c:forEach>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-xl-2 col-lg-2">
                            <label for="type" class="fw-medium text-base text-neutral-800 mb-12">Type</label>
                            <select id="type" name="type" required class="form-select px-18 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-8" data-lesson-type>
                                <c:forEach var="type" items="${lessonTypes}">
                                    <option value="${type.value}" ${type.selected ? 'selected' : ''}><c:out value="${type.label}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-xl-3 col-lg-3 gape-context-date-field">
                            <label for="startsAt" class="fw-medium text-base text-neutral-800 mb-12">Starts At</label>
                            <div class="gape-context-date-control">
                                <i class="ph ph-calendar-dots"></i>
                                <input id="startsAt" name="startsAt" type="datetime-local" required value="<c:out value='${form.startsAt}'/>" class="form-control px-18 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                            </div>
                        </div>
                        <div class="col-xl-3 col-lg-3 gape-context-date-field">
                            <label for="endsAt" class="fw-medium text-base text-neutral-800 mb-12">Ends At</label>
                            <div class="gape-context-date-control">
                                <i class="ph ph-calendar-check"></i>
                                <input id="endsAt" name="endsAt" type="datetime-local" required value="<c:out value='${form.endsAt}'/>" class="form-control px-18 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                            </div>
                        </div>
                        <div class="col-xl-3 col-lg-3 gape-select-field gape-lesson-context-field opacity-75 is-disabled" data-room-field>
                            <label for="physicalRoomCode" class="fw-medium text-base text-neutral-800 mb-12">Physical Room</label>
                            <select id="physicalRoomCode" name="physicalRoomCode" class="form-select px-24 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-14 gape-eduall-select" data-lesson-room disabled>
                                <option value="">No room</option>
                                <c:forEach var="room" items="${physicalRoomOptions}">
                                    <option value="<c:out value='${room.code}'/>" data-organization-id="${room.organizationId}" data-organic-unit-id="${room.organicUnitId}" data-room-group="<c:out value='${room.contextLabel}'/>" title="<c:out value='${room.contextTitle}'/>" ${form.physicalRoomCode == room.code ? 'selected' : ''}>
                                        <c:out value="${room.name}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-xl-2 col-lg-2" data-meeting-field>
                            <label for="provider" class="fw-medium text-base text-neutral-800 mb-12">Provider</label>
                            <select id="provider" name="provider" class="form-select px-18 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-8" data-lesson-provider>
                                <option value="">Select provider</option>
                                <c:forEach var="provider" items="${providerOptions}">
                                    <option value="${provider.value}" ${provider.selected ? 'selected' : ''}><c:out value="${provider.label}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-xl-4 col-lg-4" data-meeting-field>
                            <label for="accessUrl" class="fw-medium text-base text-neutral-800 mb-12">Meeting Link</label>
                            <input id="accessUrl" name="accessUrl" type="url" maxlength="255" value="<c:out value='${form.accessUrl}'/>" class="form-control px-18 py-12 text-14 bg-neutral-20 border-neutral-30 border rounded-8" placeholder="https://...">
                        </div>
                        <div class="col-xl-3 col-lg-3 gape-lesson-attendance-field" data-attendance-field>
                            <span class="gape-lesson-attendance-spacer d-none d-lg-block fw-medium text-base text-neutral-800 mb-12" aria-hidden="true">&nbsp;</span>
                            <label class="gape-lesson-attendance-control w-100 d-inline-flex align-items-center gap-10 px-16 py-12 border border-neutral-30 rounded-8 bg-neutral-20 text-14 text-neutral-700">
                                <input type="checkbox" name="attendanceRequired" value="true" ${form.attendanceRequired ? 'checked' : ''}>
                                Attendance required
                            </label>
                        </div>
                    </div>

                    <div class="d-flex align-items-center gap-14 flex-wrap border-top-dashed pt-20 mt-22">
                        <button type="submit" class="bg-main-600 px-24 py-12 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03">${creating ? 'Create Lesson' : 'Save Changes'}</button>
                        <c:choose>
                            <c:when test="${lessonModal}"><button type="button" class="text-neutral-600 fw-semibold hover-text-main-600 transition-03 border-0 bg-transparent p-0" data-gape-lesson-modal-close>Cancel</button></c:when>
                            <c:otherwise><a href="${fn:escapeXml(lessonBackHref)}" class="text-neutral-600 fw-semibold hover-text-main-600 transition-03">Cancel</a></c:otherwise>
                        </c:choose>
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
            var form = document.querySelector('[data-lesson-form]');
            if (!form) {
                return;
            }
            var course = form.querySelector('[data-lesson-course]');
            var subject = form.querySelector('[data-lesson-subject]');
            var classGroup = form.querySelector('[data-lesson-class-group]');
            var block = form.querySelector('[data-lesson-block]');
            var room = form.querySelector('[data-lesson-room]');
            var courseWrapper = form.querySelector('[data-lesson-course-context]');
            var subjectWrapper = form.querySelector('[data-lesson-subject-context]');
            var classGroupWrapper = form.querySelector('[data-lesson-class-group-field]');
            var blockWrapper = form.querySelector('[data-lesson-block-context]');
            var roomWrapper = form.querySelector('[data-room-field]');
            var type = form.querySelector('[data-lesson-type]');
            var stateDisplay = form.querySelector('[data-lesson-state-display]');
            var provider = form.querySelector('[data-lesson-provider]');
            var accessUrl = form.querySelector('#accessUrl');
            var startsAt = form.querySelector('#startsAt');
            var endsAt = form.querySelector('#endsAt');
            var initialClassGroupValue = classGroup ? classGroup.value : '';
            var initialBlockValue = block ? block.value : '';
            var originalClassGroups = classGroup ? Array.prototype.slice.call(classGroup.options).map(function (option) {
                return {
                    value: option.value,
                    text: option.textContent.trim(),
                    organizationId: option.dataset.organizationId || '',
                    organizationLabel: option.dataset.organizationLabel || '',
                    organizationAcronym: option.dataset.organizationAcronym || '',
                    organicUnitId: option.dataset.organicUnitId || '',
                    organicUnitLabel: option.dataset.organicUnitLabel || '',
                    organicUnitAcronym: option.dataset.organicUnitAcronym || '',
                    courseId: option.dataset.courseId || '',
                    courseLabel: option.dataset.courseLabel || '',
                    courseAcronym: option.dataset.courseAcronym || '',
                    subjectId: option.dataset.subjectId || '',
                    subjectLabel: option.dataset.subjectLabel || '',
                    subjectAcronym: option.dataset.subjectAcronym || '',
                    startsAt: option.dataset.startsAt || '',
                    endsAt: option.dataset.endsAt || '',
                    selected: option.selected
                };
            }) : [];
            var originalBlocks = block ? Array.prototype.slice.call(block.options).map(function (option) {
                return { value: option.value, text: option.textContent.trim(), classGroupId: option.dataset.classGroupId || '', selected: option.selected };
            }) : [];
            var originalRooms = room ? Array.prototype.slice.call(room.options).map(function (option) {
                return {
                    value: option.value,
                    text: option.textContent.trim(),
                    organizationId: option.dataset.organizationId || '',
                    organicUnitId: option.dataset.organicUnitId || '',
                    groupLabel: option.dataset.roomGroup || '',
                    title: option.getAttribute('title') || '',
                    selected: option.selected
                };
            }) : [];

            function rebuildSelect(select, options, placeholder, predicate) {
                if (!select) {
                    return 0;
                }
                var previousValue = select.value;
                Array.prototype.forEach.call(select.options, clearSelect2OptionData);
                select.innerHTML = '';
                var first = document.createElement('option');
                first.value = '';
                first.textContent = placeholder;
                select.appendChild(first);
                var groups = {};
                var count = 0;
                options.filter(predicate).forEach(function (optionData) {
                    var parent = select;
                    if (optionData.groupLabel) {
                        var groupKey = optionData.groupKey || optionData.groupLabel;
                        parent = groups[groupKey];
                        if (!parent) {
                            parent = document.createElement('optgroup');
                            parent.label = optionData.groupLabel;
                            if (optionData.groupTitle) {
                                parent.title = optionData.groupTitle;
                            }
                            groups[groupKey] = parent;
                            select.appendChild(parent);
                        }
                    }
                    var option = document.createElement('option');
                    option.value = optionData.value;
                    option.textContent = optionData.text;
                    if (optionData.title) {
                        option.title = optionData.title;
                    }
                    if (optionData.classGroupId) {
                        option.dataset.classGroupId = optionData.classGroupId;
                    }
                    if (optionData.organizationId) {
                        option.dataset.organizationId = optionData.organizationId;
                    }
                    if (optionData.organizationLabel) {
                        option.dataset.organizationLabel = optionData.organizationLabel;
                    }
                    if (optionData.organizationAcronym) {
                        option.dataset.organizationAcronym = optionData.organizationAcronym;
                    }
                    if (optionData.organicUnitId) {
                        option.dataset.organicUnitId = optionData.organicUnitId;
                    }
                    if (optionData.organicUnitLabel) {
                        option.dataset.organicUnitLabel = optionData.organicUnitLabel;
                    }
                    if (optionData.organicUnitAcronym) {
                        option.dataset.organicUnitAcronym = optionData.organicUnitAcronym;
                    }
                    if (optionData.courseId) {
                        option.dataset.courseId = optionData.courseId;
                    }
                    if (optionData.courseLabel) {
                        option.dataset.courseLabel = optionData.courseLabel;
                    }
                    if (optionData.courseAcronym) {
                        option.dataset.courseAcronym = optionData.courseAcronym;
                    }
                    if (optionData.subjectId) {
                        option.dataset.subjectId = optionData.subjectId;
                    }
                    if (optionData.subjectLabel) {
                        option.dataset.subjectLabel = optionData.subjectLabel;
                    }
                    if (optionData.subjectAcronym) {
                        option.dataset.subjectAcronym = optionData.subjectAcronym;
                    }
                    if (optionData.startsAt) {
                        option.dataset.startsAt = optionData.startsAt;
                    }
                    if (optionData.endsAt) {
                        option.dataset.endsAt = optionData.endsAt;
                    }
                    if (optionData.groupLabel) {
                        option.dataset.roomGroup = optionData.groupLabel;
                        option.dataset.classGroupGroup = optionData.groupLabel;
                    }
                    if (optionData.groupTitle) {
                        option.dataset.classGroupGroupTitle = optionData.groupTitle;
                    }
                    if (optionData.contextLabel) {
                        option.dataset.classGroupContextLabel = optionData.contextLabel;
                    }
                    if (optionData.contextTitle) {
                        option.dataset.classGroupContextTitle = optionData.contextTitle;
                    }
                    if (optionData.contextKind) {
                        option.dataset.contextKind = optionData.contextKind;
                    }
                    if (optionData.contextUnavailable) {
                        option.dataset.contextUnavailable = optionData.contextUnavailable;
                    }
                    if (optionData.disabled) {
                        option.disabled = true;
                    }
                    if (optionData.contextMeta) {
                        option.dataset.contextMeta = optionData.contextMeta;
                    }
                    parent.appendChild(option);
                    count += 1;
                });
                select.value = previousValue;
                if (select.value !== previousValue) {
                    select.value = '';
                }
                refreshSelectUi(select);
                return count;
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

            function displayLabel(acronym, name) {
                var cleanAcronym = (acronym || '').trim();
                var cleanName = (name || '').trim();
                if (cleanAcronym && cleanAcronym !== '-' && cleanName) {
                    return cleanAcronym + ' - ' + cleanName;
                }
                return cleanName || cleanAcronym || '-';
            }

            function courseDisplayLabel(acronym, name) {
                return displayLabel(acronym, name);
            }

            function subjectDisplayLabel(acronym, name) {
                return displayLabel(acronym, name);
            }

            function compactContextPart(acronym, label) {
                var cleanAcronym = (acronym || '').trim();
                var cleanLabel = (label || '').trim();
                if (cleanAcronym && cleanAcronym !== '-') {
                    return cleanAcronym;
                }
                return cleanLabel;
            }

            function courseContextLabel(item) {
                var parts = [];
                var organicUnit = compactContextPart(item.organicUnitAcronym, item.organicUnitLabel);
                var organization = compactContextPart(item.organizationAcronym, item.organizationLabel);
                if (organicUnit) {
                    parts.push(organicUnit);
                }
                if (organization) {
                    parts.push(organization);
                }
                return parts.join(' | ');
            }

            function courseContextTitle(item) {
                var parts = [];
                if (item.organicUnitLabel) {
                    parts.push(item.organicUnitLabel);
                }
                if (item.organizationLabel) {
                    parts.push(item.organizationLabel);
                }
                return parts.join(' | ');
            }

            function courseOptionLabel(item, courseName) {
                var label = courseDisplayLabel(item.courseAcronym, courseName);
                var context = courseContextLabel(item);
                return context ? label + ' | ' + context : label;
            }

            function splitCourseLabel(label) {
                var parts = (label || '').split('|').map(function (part) {
                    return part.trim();
                }).filter(function (part) {
                    return part;
                });
                return {
                    main: parts[0] || (label || '').trim(),
                    context: parts.length > 1 ? parts.slice(1).join(' | ') : ''
                };
            }

            function lessonCourseOptionTemplate(data) {
                var element = data.element;
                var label = splitCourseLabel(data.text || '');
                if (!element || !element.dataset) {
                    return label.context ? label.main + ' | ' + label.context : label.main;
                }
                var option = document.createElement('span');
                option.className = 'gape-lesson-context-option gape-lesson-course-option';
                if (element.dataset.contextUnavailable === 'true') {
                    option.className += ' gape-lesson-context-option-muted';
                }
                var main = document.createElement('span');
                main.className = 'gape-lesson-course-option-main';
                main.textContent = label.main;
                option.appendChild(main);
                if (label.context) {
                    var context = document.createElement('span');
                    context.className = 'gape-lesson-course-option-context';
                    context.textContent = label.context;
                    option.appendChild(context);
                }
                return option;
            }

            function lessonContextOptionTemplate(data, optionClassName) {
                var element = findCurrentContextOption(data, null) || data.element;
                if (element && element.value && (element.hidden || element.disabled)) {
                    return null;
                }
                var text = data.text || '';
                if (!element || !element.dataset) {
                    return text;
                }
                var option = document.createElement('span');
                option.className = 'gape-lesson-context-option ' + optionClassName;
                if (element.dataset.contextUnavailable === 'true') {
                    option.className += ' gape-lesson-context-option-muted';
                }
                option.textContent = text;
                return option;
            }

            function lessonSubjectOptionTemplate(data) {
                return lessonContextOptionTemplate(data, 'gape-lesson-subject-option');
            }

            function lessonClassGroupOptionTemplate(data) {
                return lessonContextOptionTemplate(data, 'gape-lesson-class-group-option');
            }

            function lessonBlockOptionTemplate(data) {
                return lessonContextOptionTemplate(data, 'gape-lesson-block-option');
            }

            function lessonRoomOptionTemplate(data) {
                return lessonContextOptionTemplate(data, 'gape-lesson-room-option');
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
                var selects = [course, subject, classGroup, block, room].filter(function (select) {
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
                document.querySelectorAll('.gape-eduall-select-dropdown .gape-lesson-context-option').forEach(function (option) {
                    var row = option.closest('.select2-results__option');
                    if (row) {
                        var sourceOption = select2ResultOptionElement(row);
                        var muted = sourceOption && sourceOption.dataset
                                ? sourceOption.dataset.contextUnavailable === 'true'
                                : option.classList.contains('gape-lesson-context-option-muted');
                        option.classList.toggle('gape-lesson-context-option-muted', muted);
                        row.classList.add('gape-lesson-context-option-row');
                        row.classList.toggle('gape-lesson-course-option-row', option.classList.contains('gape-lesson-course-option'));
                        row.classList.toggle('gape-lesson-context-option-row-muted', muted);
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

            function bindContextSelect(select, namespace, handler) {
                if (!select || !window.jQuery || !window.jQuery.fn || !window.jQuery.fn.select2) {
                    return;
                }
                window.jQuery(select)
                        .off('change.' + namespace)
                        .on('change.' + namespace, handler)
                        .off('select2:select.' + namespace + ' select2:clear.' + namespace)
                        .on('select2:select.' + namespace + ' select2:clear.' + namespace, handler)
                        .off('select2:close.' + namespace)
                        .on('select2:close.' + namespace, function () {
                            window.setTimeout(handler, 0);
                        })
                        .off('select2:open.' + namespace + 'Rows')
                        .on('select2:open.' + namespace + 'Rows', function () {
                            window.setTimeout(observeMutedCourseOptionRows, 0);
                        });
            }

            function initializeContextSelectUi() {
                initializeSelectUi(course, lessonCourseOptionTemplate, 'gape-eduall-select-dropdown gape-lesson-context-dropdown');
                initializeSelectUi(subject, lessonSubjectOptionTemplate, 'gape-eduall-select-dropdown gape-lesson-context-dropdown');
                initializeSelectUi(classGroup, lessonClassGroupOptionTemplate, 'gape-eduall-select-dropdown gape-lesson-context-dropdown');
                initializeSelectUi(block, lessonBlockOptionTemplate, 'gape-eduall-select-dropdown gape-lesson-context-dropdown');
                initializeSelectUi(room, lessonRoomOptionTemplate, 'gape-eduall-select-dropdown gape-lesson-context-dropdown');
                bindContextSelect(course, 'gapeLessonContext', syncContextFields);
                bindContextSelect(subject, 'gapeLessonContext', syncContextFields);
                bindContextSelect(classGroup, 'gapeLessonContext', syncContextFields);
                bindContextSelect(block, 'gapeLessonContext', syncContextFields);
                bindContextSelect(room, 'gapeLessonContext', syncTypeFields);
            }

            function setAvailability(select, enabled) {
                if (select) {
                    select.disabled = !enabled;
                    refreshSelectUi(select);
                }
            }

            function syncContextWrapper(wrapper, enabled) {
                if (!wrapper) {
                    return;
                }
                wrapper.classList.toggle('opacity-75', !enabled);
                wrapper.classList.toggle('is-disabled', !enabled);
            }

            function pushUnique(options, seen, value, text, extra) {
                if (!value || seen[value]) {
                    return;
                }
                seen[value] = true;
                options.push(Object.assign({
                    value: value,
                    text: text || value
                }, extra || {}));
            }

            function compareText(first, second) {
                return (first.text || '').localeCompare(second.text || '', undefined, { sensitivity: 'base' });
            }

            function selectedTypeRequiresRoom() {
                var selectedType = type ? type.value : 'ONLINE';
                return selectedType === 'ONSITE' || selectedType === 'HYBRID';
            }

            function classGroupData(value) {
                return originalClassGroups.find(function (item) {
                    return item.value === value;
                }) || null;
            }

            function blockData(value) {
                return originalBlocks.find(function (item) {
                    return item.value === value;
                }) || null;
            }

            function roomMatchesClassGroupContext(roomData, groupData) {
                if (!roomData || !roomData.value || !groupData) {
                    return false;
                }
                if (!groupData.organizationId || roomData.organizationId !== groupData.organizationId) {
                    return false;
                }
                return groupData.organicUnitId
                        ? roomData.organicUnitId === groupData.organicUnitId
                        : !roomData.organicUnitId;
            }

            function blockHasPhysicalRoomContext(blockItem) {
                var groupData = blockItem ? classGroupData(blockItem.classGroupId) : null;
                return originalRooms.some(function (roomData) {
                    return roomMatchesClassGroupContext(roomData, groupData);
                });
            }

            function blockHasCreationContext(blockItem) {
                return !!blockItem
                        && !!blockItem.value
                        && (!selectedTypeRequiresRoom() || blockHasPhysicalRoomContext(blockItem));
            }

            function classGroupHasPedagogicalBlock(classGroupId) {
                return originalBlocks.some(function (item) {
                    return item.value && item.classGroupId === classGroupId;
                });
            }

            function classGroupHasCreationContext(classGroupId, courseId, subjectId) {
                var groupData = classGroupData(classGroupId);
                if (!groupData || (courseId && groupData.courseId !== courseId) || (subjectId && groupData.subjectId !== subjectId)) {
                    return false;
                }
                return originalBlocks.some(function (item) {
                    return item.value
                            && item.classGroupId === classGroupId
                            && blockHasCreationContext(item);
                });
            }

            function subjectHasCreationContext(subjectId, courseId) {
                return originalClassGroups.some(function (item) {
                    return item.value
                            && item.courseId === courseId
                            && item.subjectId === subjectId
                            && classGroupHasCreationContext(item.value, courseId, subjectId);
                });
            }

            function courseHasCreationContext(courseId) {
                return originalClassGroups.some(function (item) {
                    return item.value
                            && item.courseId === courseId
                            && item.subjectId
                            && subjectHasCreationContext(item.subjectId, courseId);
                });
            }

            function pushCourseOption(options, seen, item) {
                var courseName = item.courseLabel || 'Course ' + item.courseId;
                var courseAvailable = courseHasCreationContext(item.courseId);
                var contextTitle = courseContextTitle(item);
                pushUnique(options, seen, item.courseId, courseOptionLabel(item, courseName), {
                    organizationId: item.organizationId,
                    organizationLabel: item.organizationLabel,
                    organizationAcronym: item.organizationAcronym,
                    organicUnitId: item.organicUnitId,
                    organicUnitLabel: item.organicUnitLabel,
                    organicUnitAcronym: item.organicUnitAcronym,
                    courseAcronym: item.courseAcronym,
                    courseLabel: courseName,
                    contextKind: 'course',
                    contextUnavailable: courseAvailable ? '' : 'true',
                    disabled: false,
                    title: courseAvailable
                            ? (contextTitle ? courseName + ' | ' + contextTitle : courseName)
                            : courseName + ' does not have a complete lesson context.'
                });
            }

            function courseOptions() {
                var seen = {};
                var options = [];
                originalClassGroups.forEach(function (item) {
                    if (item.value && item.courseId) {
                        pushCourseOption(options, seen, item);
                    }
                });
                return options.sort(compareText);
            }

            function subjectOptionsFor(courseId) {
                var seen = {};
                var options = [];
                originalClassGroups.forEach(function (item) {
                    if (item.value && item.courseId === courseId) {
                        var subjectName = item.subjectLabel || 'Subject ' + item.subjectId;
                        var subjectAvailable = subjectHasCreationContext(item.subjectId, courseId);
                        pushUnique(options, seen, item.subjectId, subjectDisplayLabel(item.subjectAcronym, subjectName), {
                            subjectAcronym: item.subjectAcronym,
                            subjectLabel: subjectName,
                            contextKind: 'subject',
                            contextUnavailable: subjectAvailable ? '' : 'true',
                            disabled: false,
                            title: subjectAvailable
                                    ? subjectName
                                    : subjectName + ' does not have a complete lesson context.'
                        });
                    }
                });
                return options;
            }

            function classGroupOptionsFor(courseId, subjectId) {
                return originalClassGroups
                        .filter(function (item) {
                            return item.value && item.courseId === courseId && item.subjectId === subjectId;
                        })
                        .map(function (item) {
                            var classGroupAvailable = classGroupHasCreationContext(item.value, courseId, subjectId);
                            return {
                                value: item.value,
                                text: item.text || item.value,
                                startsAt: item.startsAt || '',
                                endsAt: item.endsAt || '',
                                contextKind: 'class-group',
                                contextUnavailable: classGroupAvailable ? '' : 'true',
                                disabled: false,
                                title: classGroupAvailable
                                        ? (item.text || item.value)
                                        : (item.text || item.value) + ' does not have a complete lesson context.'
                            };
                        })
                        .sort(compareText);
            }

            function blockOptionsFor(classGroupId) {
                return originalBlocks
                        .filter(function (item) {
                            return item.value && item.classGroupId === classGroupId;
                        })
                        .map(function (item) {
                            var blockAvailable = blockHasCreationContext(item);
                            return Object.assign({}, item, {
                                contextKind: 'block',
                                contextUnavailable: blockAvailable ? '' : 'true',
                                disabled: false,
                                title: blockAvailable
                                        ? (item.text || item.value)
                                        : (item.text || item.value) + ' does not have a complete lesson context.'
                            });
                        })
                        .sort(compareText);
            }

            function roomOptionsFor(groupData) {
                return originalRooms
                        .filter(function (item) {
                            return roomMatchesClassGroupContext(item, groupData);
                        })
                        .map(function (item) {
                            return Object.assign({}, item, {
                                contextKind: 'room',
                                contextUnavailable: '',
                                disabled: false
                            });
                        })
                        .sort(compareText);
            }

            function selectedCourseValue() {
                return course && course.value ? course.value : '';
            }

            function selectedSubjectValue() {
                return subject && subject.value ? subject.value : '';
            }

            function selectedClassGroupValue() {
                return classGroup && classGroup.value ? classGroup.value : '';
            }

            function selectedCourseIsUnavailable() {
                var value = selectedCourseValue();
                return !!value && !courseHasCreationContext(value);
            }

            function selectedSubjectIsUnavailable() {
                var value = selectedSubjectValue();
                return !!value && !subjectHasCreationContext(value, selectedCourseValue());
            }

            function selectedClassGroupIsUnavailable() {
                var value = selectedClassGroupValue();
                return !!value && !classGroupHasCreationContext(value, selectedCourseValue(), selectedSubjectValue());
            }

            function selectedBlockIsUnavailable() {
                var selectedBlock = blockData(block && block.value ? block.value : '');
                return !!selectedBlock && !blockHasCreationContext(selectedBlock);
            }

            function syncContextValidity() {
                if (!course) {
                    return;
                }
                course.setCustomValidity('');
                if (!course.disabled && selectedCourseIsUnavailable()) {
                    course.setCustomValidity('Selected course does not have a complete lesson context.');
                } else if (!course.disabled && !selectedCourseValue()) {
                    course.setCustomValidity('Select a course before selecting a subject.');
                }
                if (subject) {
                    subject.setCustomValidity('');
                    if (!subject.disabled && selectedSubjectIsUnavailable()) {
                        subject.setCustomValidity('Selected subject does not have a complete lesson context.');
                    } else if (!subject.disabled && !selectedSubjectValue()) {
                        subject.setCustomValidity('Select a subject after selecting a course.');
                    }
                }
                if (classGroup) {
                    classGroup.setCustomValidity('');
                    if (!classGroup.disabled && selectedClassGroupIsUnavailable()) {
                        classGroup.setCustomValidity('Selected class group does not have a complete lesson context.');
                    } else if (!classGroup.disabled && !selectedClassGroupValue()) {
                        classGroup.setCustomValidity('Select a class group before selecting a pedagogical block.');
                    }
                }
                if (block) {
                    block.setCustomValidity('');
                    if (!block.disabled && selectedBlockIsUnavailable()) {
                        block.setCustomValidity('Selected pedagogical block does not have a complete lesson context.');
                    } else if (!block.disabled && !block.value) {
                        block.setCustomValidity('Select a pedagogical block after selecting a class group.');
                    }
                }
            }

            function syncContextFields() {
                var selectedCourse = selectedCourseValue();
                var selectedSubject = selectedSubjectValue();
                var selectedClassGroup = selectedClassGroupValue();
                var courseCount;
                var subjectCount;
                var classGroupCount;
                var blockCount;
                var roomCount;
                var selectedCourseReady;
                var selectedSubjectReady;
                var selectedClassGroupReady;
                var selectedBlockReady;

                courseCount = rebuildSelect(course, courseOptions(), 'Select course', function (optionData) {
                    return optionData.value;
                });
                setAvailability(course, courseCount > 0);
                selectedCourse = selectedCourseValue();
                selectedCourseReady = !!selectedCourse && !selectedCourseIsUnavailable();
                syncContextWrapper(courseWrapper, courseCount > 0);

                subjectCount = rebuildSelect(subject, subjectOptionsFor(selectedCourseReady ? selectedCourse : ''), 'Select subject', function (optionData) {
                    return optionData.value;
                });
                setAvailability(subject, selectedCourseReady && subjectCount > 0);
                selectedSubject = selectedSubjectValue();
                selectedSubjectReady = selectedCourseReady && !!selectedSubject && !selectedSubjectIsUnavailable();
                syncContextWrapper(subjectWrapper, selectedCourseReady);

                classGroupCount = rebuildSelect(classGroup, classGroupOptionsFor(
                        selectedCourseReady ? selectedCourse : '',
                        selectedSubject
                ), 'Select class group', function (optionData) {
                    return optionData.value;
                });
                setAvailability(classGroup, selectedSubjectReady && classGroupCount > 0);
                selectedClassGroup = selectedClassGroupValue();
                selectedClassGroupReady = selectedSubjectReady && !!selectedClassGroup && !selectedClassGroupIsUnavailable();
                syncContextWrapper(classGroupWrapper, selectedSubjectReady);

                blockCount = rebuildSelect(block, blockOptionsFor(selectedClassGroupReady ? selectedClassGroup : ''), 'Select block', function (optionData) {
                    return optionData.value;
                });
                setAvailability(block, selectedClassGroupReady && blockCount > 0);
                selectedBlockReady = selectedClassGroupReady && !!(block && block.value) && !selectedBlockIsUnavailable();
                syncContextWrapper(blockWrapper, selectedClassGroupReady);

                roomCount = rebuildSelect(room, roomOptionsFor(selectedBlockReady ? classGroupData(selectedClassGroup) : null), 'No room', function (optionData) {
                    return optionData.value;
                });
                if (room) {
                    room.dataset.contextReady = selectedBlockReady && roomCount > 0 ? 'true' : 'false';
                }
                syncContextValidity();
                syncTypeFields();
            }

            function selectInitialClassGroupContext() {
                var data = classGroupData(initialClassGroupValue);
                if (!data) {
                    return;
                }
                syncContextFields();
                if (course) {
                    course.value = data.courseId;
                }
                syncContextFields();
                if (subject) {
                    subject.value = data.subjectId;
                }
                syncContextFields();
                if (classGroup) {
                    classGroup.value = data.value;
                }
                syncContextFields();
                if (block && initialBlockValue) {
                    block.value = initialBlockValue;
                }
                syncContextFields();
            }

            function syncTypeFields() {
                var selectedType = type ? type.value : 'ONLINE';
                var onsite = selectedType === 'ONSITE';
                var online = selectedType === 'ONLINE';
                var hybrid = selectedType === 'HYBRID';
                var blockReady = selectedClassGroupValue() && block && block.value && !selectedBlockIsUnavailable();
                var roomCount = room ? Array.prototype.filter.call(room.options, function (option) {
                    return option.value;
                }).length : 0;
                form.querySelectorAll('[data-meeting-field]').forEach(function (wrapper) {
                    wrapper.classList.toggle('d-none', onsite);
                    Array.prototype.forEach.call(wrapper.querySelectorAll('input'), function (input) {
                        input.required = !onsite && input.name === 'accessUrl';
                        input.disabled = onsite;
                    });
                    Array.prototype.forEach.call(wrapper.querySelectorAll('select'), function (select) {
                        select.required = !onsite && select.name === 'provider';
                        select.disabled = onsite;
                    });
                });
                form.querySelectorAll('[data-room-field]').forEach(function (wrapper) {
                    wrapper.classList.toggle('d-none', online);
                    Array.prototype.forEach.call(wrapper.querySelectorAll('select'), function (select) {
                        select.required = (onsite || hybrid) && !!blockReady && roomCount > 0;
                        select.disabled = online || !blockReady || roomCount === 0;
                        select.setCustomValidity('');
                        if ((onsite || hybrid) && blockReady && roomCount === 0) {
                            select.setCustomValidity('Selected pedagogical block does not have an available physical room.');
                        } else if ((onsite || hybrid) && blockReady && !select.value) {
                            select.setCustomValidity('Select a physical room for in-person or hybrid lessons.');
                        }
                        if (online || !blockReady) {
                            select.value = '';
                        }
                        refreshSelectUi(select);
                    });
                    syncContextWrapper(wrapper, !online && !!blockReady && roomCount > 0);
                });
                validateMeetingLink();
                validateSchedule();
            }

            function providerHost(providerValue) {
                switch ((providerValue || '').toLowerCase()) {
                    case 'zoom':
                        return 'zoom.us';
                    case 'teams':
                        return 'teams.microsoft.com';
                    case 'meet':
                        return 'meet.google.com';
                    default:
                        return '';
                }
            }

            function hostMatches(host, expectedHost) {
                return host === expectedHost || host.endsWith('.' + expectedHost);
            }

            function validateMeetingLink() {
                if (!provider || !accessUrl || (type && type.value === 'ONSITE')) {
                    return;
                }
                accessUrl.setCustomValidity('');
                var expectedHost = providerHost(provider.value);
                if (!accessUrl.value || !expectedHost) {
                    return;
                }
                try {
                    var parsed = new URL(accessUrl.value);
                    if (parsed.protocol !== 'https:' || !hostMatches(parsed.hostname.toLowerCase(), expectedHost)) {
                        accessUrl.setCustomValidity('The meeting link must match the selected provider.');
                    }
                } catch (ignored) {
                    accessUrl.setCustomValidity('The meeting link must be a valid HTTPS URL.');
                }
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

            function selectedClassGroupDateWindow() {
                var selected = classGroup && classGroup.value ? classGroup.options[classGroup.selectedIndex] : null;
                return {
                    start: selected ? (selected.dataset.startsAt || '') : '',
                    end: selected ? (selected.dataset.endsAt || '') : ''
                };
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

            function minDateTime(first, second) {
                if (!first) {
                    return second || '';
                }
                if (!second) {
                    return first;
                }
                return first < second ? first : second;
            }

            function addMinutes(value, minutes) {
                if (!value) {
                    return '';
                }
                var date = new Date(value);
                if (Number.isNaN(date.getTime())) {
                    return value;
                }
                date.setMinutes(date.getMinutes() + minutes);
                var month = String(date.getMonth() + 1).padStart(2, '0');
                var day = String(date.getDate()).padStart(2, '0');
                var hours = String(date.getHours()).padStart(2, '0');
                var mins = String(date.getMinutes()).padStart(2, '0');
                return date.getFullYear() + '-' + month + '-' + day + 'T' + hours + ':' + mins;
            }

            function applyDateAction(action) {
                if (!startsAt || !endsAt) {
                    return;
                }
                var now = localMinuteValue();
                var classGroupWindow = selectedClassGroupDateWindow();
                var lowerBound = maxDateTime(now, dayStart(classGroupWindow.start));
                var upperBound = dayEnd(classGroupWindow.end);
                if (!classGroupWindow.start || !classGroupWindow.end || lowerBound > upperBound) {
                    validateSchedule();
                    return;
                }
                if (action === 'fill-start') {
                    startsAt.value = lowerBound;
                } else if (action === 'fill-end') {
                    endsAt.value = upperBound;
                } else if (action === 'fill-slot') {
                    startsAt.value = lowerBound;
                    endsAt.value = minDateTime(addMinutes(lowerBound, 90), upperBound);
                }
                validateSchedule();
            }

            function validateSchedule() {
                if (!startsAt || !endsAt) {
                    return;
                }
                var now = localMinuteValue();
                var classGroupWindow = selectedClassGroupDateWindow();
                var lowerBound = maxDateTime(now, dayStart(classGroupWindow.start));
                var upperBound = dayEnd(classGroupWindow.end);
                startsAt.min = lowerBound;
                startsAt.max = upperBound;
                startsAt.required = true;
                endsAt.required = true;
                endsAt.min = maxDateTime(startsAt.value || lowerBound, dayStart(classGroupWindow.start));
                endsAt.max = upperBound;
                startsAt.setCustomValidity('');
                endsAt.setCustomValidity('');
                if (classGroupWindow.start && classGroupWindow.end && lowerBound > upperBound) {
                    startsAt.setCustomValidity('The selected class group has no available schedule window.');
                }
                if (!startsAt.value) {
                    startsAt.setCustomValidity('Lesson start date is required.');
                }
                if (!endsAt.value) {
                    endsAt.setCustomValidity('Lesson end date is required.');
                }
                if (startsAt.value && startsAt.value < now) {
                    startsAt.setCustomValidity('Start date cannot be in the past.');
                }
                if (endsAt.value && endsAt.value < now) {
                    endsAt.setCustomValidity('End date cannot be in the past.');
                }
                if (classGroupWindow.start && startsAt.value && startsAt.value < dayStart(classGroupWindow.start)) {
                    startsAt.setCustomValidity('Start date cannot be before the class group start date.');
                }
                if (classGroupWindow.end && startsAt.value && startsAt.value > dayEnd(classGroupWindow.end)) {
                    startsAt.setCustomValidity('Start date cannot be after the class group end date.');
                }
                if (classGroupWindow.start && endsAt.value && endsAt.value < dayStart(classGroupWindow.start)) {
                    endsAt.setCustomValidity('End date cannot be before the class group start date.');
                }
                if (classGroupWindow.end && endsAt.value && endsAt.value > dayEnd(classGroupWindow.end)) {
                    endsAt.setCustomValidity('End date cannot be after the class group end date.');
                }
                if (startsAt.value && endsAt.value && endsAt.value <= startsAt.value) {
                    endsAt.setCustomValidity('End date must be after start date.');
                }
                if (stateDisplay) {
                    stateDisplay.textContent = computedStateLabel();
                }
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
                if (!startsAt.value) {
                    return 'Draft';
                }
                if (endsAt.value && endsAt.value <= now) {
                    return 'Completed';
                }
                return startsAt.value <= now ? 'Active' : 'Scheduled';
            }

            if (course) {
                course.addEventListener('change', syncContextFields);
            }
            if (subject) {
                subject.addEventListener('change', syncContextFields);
            }
            if (classGroup) {
                classGroup.addEventListener('change', syncContextFields);
            }
            if (block) {
                block.addEventListener('change', syncContextFields);
            }
            if (room) {
                room.addEventListener('change', syncTypeFields);
            }
            if (type) {
                type.addEventListener('change', syncContextFields);
            }
            if (provider) {
                provider.addEventListener('change', validateMeetingLink);
            }
            if (accessUrl) {
                accessUrl.addEventListener('input', validateMeetingLink);
            }
            if (startsAt) {
                startsAt.addEventListener('input', validateSchedule);
            }
            if (endsAt) {
                endsAt.addEventListener('input', validateSchedule);
            }
            form.querySelectorAll('[data-date-action]').forEach(function (button) {
                button.addEventListener('click', function () {
                    applyDateAction(button.getAttribute('data-date-action'));
                });
            });
            form.addEventListener('submit', function (event) {
                syncContextValidity();
                if (!form.checkValidity()) {
                    event.preventDefault();
                    form.reportValidity();
                }
            });
            if (initialClassGroupValue) {
                selectInitialClassGroupContext();
            } else {
                syncContextFields();
            }
            window.setTimeout(function () {
                initializeContextSelectUi();
                syncContextFields();
            }, 0);
            window.setInterval(validateSchedule, 30000);
        });
    })();
</script>
</body>
</html>
