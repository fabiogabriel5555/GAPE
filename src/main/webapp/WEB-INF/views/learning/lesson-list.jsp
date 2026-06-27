<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - ${calendarMode ? 'Calendar' : 'Lessons'}</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        .gape-class-group-picker {
            position: relative;
        }

        .gape-class-group-picker__summary {
            min-height: 44px;
            cursor: pointer;
            list-style: none;
        }

        .gape-class-group-picker__summary::-webkit-details-marker {
            display: none;
        }

        .gape-class-group-picker__panel {
            position: absolute;
            inset-block-start: calc(100% + 6px);
            inset-inline-start: 0;
            z-index: 30;
            width: min(420px, 100vw - 48px);
            max-height: 220px;
            box-shadow: 0 12px 32px rgba(15, 23, 42, 0.12);
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

                <div class="row gy-4 mb-24">
                    <div class="col-md-4">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">${calendarMode ? 'Calendar Items' : 'Total Lessons'}</span>
                            <h2 class="text-32 fw-semibold text-neutral-700 mb-0">${calendarMode ? calendarItemCount : lessonCount}</h2>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">${calendarMode ? 'Events' : 'Scheduled'}</span>
                            <h2 class="text-32 fw-semibold text-main-600 mb-0">${calendarMode ? scheduleEventCount : scheduledLessonCount}</h2>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">${calendarMode ? 'Lessons' : 'Active'}</span>
                            <h2 class="text-32 fw-semibold text-success-600 mb-0">${calendarMode ? lessonCount : activeLessonCount}</h2>
                        </div>
                    </div>
                </div>

                <div class="bg-white rounded-10 px-24 py-24">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">${calendarMode ? 'Calendar' : 'Lessons Calendar'}</h2>
                            <span class="text-14 text-neutral-500">${calendarMode ? 'Schedule events, lessons and room schedules ordered by date.' : 'Online, presential and hybrid lessons ordered by date.'}</span>
                        </div>
                        <div class="d-flex align-items-center gap-10 flex-wrap">
                            <form action="${pageContext.request.contextPath}${calendarMode ? '/learning/calendar' : '/learning/lessons'}" method="get" class="d-flex align-items-center gap-10 flex-wrap mb-0">
                                <c:choose>
                                    <c:when test="${calendarMode}">
                                        <select name="contentType" class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8" style="min-width: 220px; min-height: 44px;" aria-label="Calendar filter" onchange="this.form.submit()">
                                            <c:forEach var="option" items="${calendarFilterOptions}">
                                                <option value="${option.value}" ${option.selected ? 'selected' : ''}>
                                                    <c:out value="${option.label}"/>
                                                </option>
                                            </c:forEach>
                                        </select>
                                        <select name="classGroupId" class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8" style="min-width: 260px; min-height: 44px;" aria-label="Calendar class group filter" onchange="this.form.submit()">
                                            <option value="">All class groups</option>
                                            <c:forEach var="classGroup" items="${classGroupOptions}">
                                                <option value="${classGroup.id}" ${selectedClassGroupId == classGroup.id ? 'selected' : ''}>
                                                    <c:out value="${classGroup.code}"/> - <c:out value="${classGroup.subjectName}"/>
                                                </option>
                                            </c:forEach>
                                        </select>
                                    </c:when>
                                    <c:otherwise>
                                        <select name="classGroupId" class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8" style="min-width: 280px; min-height: 44px;" aria-label="Lesson class group filter" onchange="this.form.submit()">
                                            <option value="">All class groups</option>
                                            <c:forEach var="classGroup" items="${classGroupOptions}">
                                                <option value="${classGroup.id}" ${selectedClassGroupId == classGroup.id ? 'selected' : ''}>
                                                    <c:out value="${classGroup.code}"/> - <c:out value="${classGroup.subjectName}"/>
                                                </option>
                                            </c:forEach>
                                        </select>
                                    </c:otherwise>
                                </c:choose>
                            </form>
                            <c:if test="${not empty topActionHref}">
                                <a href="${pageContext.request.contextPath}${topActionHref}" class="bg-main-600 px-20 py-10 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03 d-inline-flex align-items-center" style="min-height: 44px;">
                                    <i class="ph ph-plus-circle me-8"></i>${topActionLabel}
                                </a>
                            </c:if>
                        </div>
                    </div>

                    <c:if test="${calendarMode and not empty manageableClassGroupOptions}">
                        <div class="mb-24 pb-24 border-bottom border-neutral-30">
                            <h3 class="text-16 fw-semibold text-neutral-700 mb-14">New schedule event</h3>
                            <form action="${pageContext.request.contextPath}/learning/calendar" method="post" class="row gy-3 align-items-end">
                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                <div class="col-xl-3 col-lg-4 col-md-6">
                                    <label class="text-13 text-neutral-600 mb-6 d-block" for="schedule-event-title">Title</label>
                                    <input type="text" id="schedule-event-title" name="title" class="form-control px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8" maxlength="160" required>
                                </div>
                                <div class="col-xl-2 col-lg-3 col-md-6">
                                    <label class="text-13 text-neutral-600 mb-6 d-block" for="schedule-event-type">Type</label>
                                    <select id="schedule-event-type" name="type" class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8" required>
                                        <option value="other">Event</option>
                                        <option value="meeting">Meeting</option>
                                        <option value="reminder">Reminder</option>
                                        <option value="lesson">Lesson</option>
                                        <option value="assessment">Assessment</option>
                                    </select>
                                </div>
                                <div class="col-xl-2 col-lg-3 col-md-6" data-schedule-reference="lesson" hidden>
                                    <label class="text-13 text-neutral-600 mb-6 d-block" for="schedule-event-lesson-id">Lesson ID</label>
                                    <input type="number" id="schedule-event-lesson-id" name="lessonId" min="1" class="form-control px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                </div>
                                <div class="col-xl-2 col-lg-3 col-md-6" data-schedule-reference="assessment" hidden>
                                    <label class="text-13 text-neutral-600 mb-6 d-block" for="schedule-event-assessment-id">Assessment ID</label>
                                    <input type="number" id="schedule-event-assessment-id" name="assessmentId" min="1" class="form-control px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                </div>
                                <div class="col-xl-3 col-lg-4 col-md-6">
                                    <span class="text-13 text-neutral-600 mb-6 d-block">Class groups</span>
                                    <details class="gape-class-group-picker" data-class-group-picker>
                                        <summary class="gape-class-group-picker__summary form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8 d-flex align-items-center justify-content-between">
                                            <span data-class-group-picker-label>Select class groups</span>
                                        </summary>
                                        <div class="gape-class-group-picker__panel bg-white border border-neutral-30 rounded-8 px-14 py-12 overflow-auto">
                                            <ul class="list-unstyled mb-0 d-flex flex-column gap-8">
                                                <c:forEach var="classGroup" items="${manageableClassGroupOptions}">
                                                    <li>
                                                        <label class="d-flex align-items-start gap-8 text-14 text-neutral-700 mb-0">
                                                            <input type="checkbox" name="classGroupIds" value="${classGroup.id}" class="mt-4" data-class-group-picker-input>
                                                            <span><c:out value="${classGroup.code}"/> - <c:out value="${classGroup.subjectName}"/></span>
                                                        </label>
                                                    </li>
                                                </c:forEach>
                                            </ul>
                                        </div>
                                    </details>
                                </div>
                                <div class="col-xl-2 col-lg-3 col-md-6">
                                    <label class="text-13 text-neutral-600 mb-6 d-block" for="schedule-event-starts">Starts</label>
                                    <input type="datetime-local" id="schedule-event-starts" name="startsAt" class="form-control px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8" required>
                                </div>
                                <div class="col-xl-2 col-lg-3 col-md-6">
                                    <label class="text-13 text-neutral-600 mb-6 d-block" for="schedule-event-ends">Ends</label>
                                    <input type="datetime-local" id="schedule-event-ends" name="endsAt" class="form-control px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8" required>
                                </div>
                                <div class="col-xl-3 col-lg-4 col-md-6">
                                    <label class="text-13 text-neutral-600 mb-6 d-block" for="schedule-event-description">Description</label>
                                    <input type="text" id="schedule-event-description" name="description" class="form-control px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8" maxlength="500">
                                </div>
                                <div class="col-xl-3 col-lg-4 col-md-6">
                                    <label class="text-13 text-neutral-600 mb-6 d-block" for="schedule-event-reminder">Reminder</label>
                                    <div class="d-flex align-items-center gap-10">
                                        <label class="d-inline-flex align-items-center gap-8 text-14 text-neutral-600 mb-0">
                                            <input type="checkbox" name="reminderEnabled" value="true">
                                            Enabled
                                        </label>
                                        <input type="number" id="schedule-event-reminder" name="reminderMinutesBefore" min="0" class="form-control px-14 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8" placeholder="Minutes">
                                    </div>
                                </div>
                                <div class="col-xl-2 col-lg-3 col-md-6">
                                    <label class="text-13 text-neutral-600 mb-6 d-block" for="schedule-event-state">State</label>
                                    <select id="schedule-event-state" name="state" class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                        <option value="active">Active</option>
                                        <option value="draft">Draft</option>
                                    </select>
                                </div>
                                <div class="col-xl-2 col-lg-3 col-md-6">
                                    <button type="submit" class="bg-main-600 px-20 py-10 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03 border-0 d-inline-flex align-items-center" style="min-height: 44px;">
                                        <i class="ph ph-plus-circle me-8"></i>Create
                                    </button>
                                </div>
                            </form>
                        </div>
                    </c:if>

                    <c:if test="${calendarMode}">
                        <div class="mb-28">
                            <h3 class="text-16 fw-semibold text-neutral-700 mb-14">Schedule events</h3>
                            <div class="overflow-x-auto">
                                <table class="table mb-0">
                                    <thead>
                                    <tr>
                                        <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Event</th>
                                        <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Class Groups</th>
                                        <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Date</th>
                                        <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Reminder</th>
                                        <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <c:forEach var="event" items="${scheduleEvents}">
                                        <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                            <td class="py-20 px-20">
                                                <div class="d-flex align-items-start gap-12">
                                                    <span class="${event.typeBadgeClass} w-40 h-40 rounded-8 d-inline-flex align-items-center justify-content-center text-20 flex-shrink-0">
                                                        <i class="${event.typeIconClass}"></i>
                                                    </span>
                                                    <div>
                                                        <span class="fw-medium text-14 text-neutral-700"><c:out value="${event.title}"/></span>
                                                        <span class="d-block text-12 text-neutral-500"><c:out value="${event.typeLabel}"/> | <c:out value="${event.durationLabel}"/></span>
                                                        <span class="d-block text-12 text-neutral-500"><c:out value="${event.description}"/></span>
                                                    </div>
                                                </div>
                                            </td>
                                            <td class="py-20 px-20 text-14 text-neutral-500">
                                                <c:out value="${event.classGroupLabel}"/>
                                            </td>
                                            <td class="py-20 px-20 text-14 text-neutral-500">
                                                <c:out value="${event.startsAt}"/>
                                                <span class="d-block text-12 text-neutral-500">to <c:out value="${event.endsAt}"/></span>
                                            </td>
                                            <td class="py-20 px-20 text-14 text-neutral-500">
                                                <c:out value="${event.reminderLabel}"/>
                                            </td>
                                            <td class="py-20 px-20">
                                                <span class="${event.stateBadgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13">
                                                    <c:out value="${event.stateLabel}"/>
                                                </span>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    <c:if test="${empty scheduleEvents}">
                                        <tr>
                                            <td colspan="5" class="py-28 px-20 text-center text-14 text-neutral-500">No schedule events available in your context.</td>
                                        </tr>
                                    </c:if>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </c:if>

                    <c:if test="${calendarMode}">
                        <h3 class="text-16 fw-semibold text-neutral-700 mb-14">Lessons</h3>
                    </c:if>
                    <div class="overflow-x-auto">
                        <table class="table mb-0">
                            <thead>
                            <tr>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Lesson</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Class Group</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Date</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Access</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="lesson" items="${lessons}">
                                <c:set var="classGroup" value="${classGroupById[lesson.classGroupId]}"/>
                                <c:set var="canManageLessonRow" value="${canManageClassGroupById[lesson.classGroupId]}"/>
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-20 px-20">
                                        <div class="d-flex align-items-start gap-12">
                                            <span class="${lesson.typeBadgeClass} w-40 h-40 rounded-8 d-inline-flex align-items-center justify-content-center text-20 flex-shrink-0">
                                                <i class="${lesson.typeIconClass}"></i>
                                            </span>
                                            <div>
                                                <a href="${pageContext.request.contextPath}/learning/lessons/${lesson.id}?returnTo=${currentReturnToParam}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                    <c:out value="${lesson.title}"/>
                                                </a>
                                                <span class="d-block text-12 text-neutral-500"><c:out value="${lesson.typeLabel}"/> | <c:out value="${lesson.durationLabel}"/></span>
                                            </div>
                                        </div>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500">
                                        <c:choose>
                                            <c:when test="${not empty classGroup}">
                                                <c:out value="${classGroup.contextHtml}" escapeXml="false"/>
                                            </c:when>
                                            <c:otherwise>-</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500">
                                        <c:out value="${lesson.startsAt}"/>
                                        <span class="d-block text-12 text-neutral-500">to <c:out value="${lesson.endsAt}"/></span>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500">
                                        <c:choose>
                                            <c:when test="${lesson.hasRoom}">
                                                <span class="bg-main-two-50 text-main-two-600 px-12 py-7 rounded-pill text-12">
                                                    <i class="ph ph-door me-6"></i><c:out value="${lesson.physicalRoomCode}"/>
                                                </span>
                                            </c:when>
                                            <c:when test="${lesson.hasMeetingLink}">
                                                <span class="bg-main-50 text-main-600 px-12 py-7 rounded-pill text-12">
                                                    <i class="ph ph-video-camera me-6"></i>Meeting link
                                                </span>
                                            </c:when>
                                            <c:otherwise>-</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="py-20 px-20">
                                        <span class="${lesson.stateBadgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13">
                                            <c:out value="${lesson.stateLabel}"/>
                                        </span>
                                    </td>
                                    <td class="py-20 px-20 text-end">
                                        <div class="d-flex align-items-center gap-12 justify-content-end">
                                            <a href="${pageContext.request.contextPath}/learning/lessons/${lesson.id}?returnTo=${currentReturnToParam}" class="text-22 text-neutral-500 hover-text-main-600" title="Detail">
                                                <i class="ph ph-eye"></i>
                                            </a>
                                            <c:if test="${canManageLessonRow}">
                                                <a href="${pageContext.request.contextPath}/learning/lessons/${lesson.id}/edit?returnTo=${currentReturnToParam}" class="text-22 text-neutral-500 hover-text-main-600" title="Edit">
                                                    <i class="ph ph-pencil-simple-line"></i>
                                                </a>
                                            </c:if>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty lessons}">
                                <tr>
                                    <td colspan="6" class="py-32 px-20 text-center text-14 text-neutral-500">No lessons available in your context.</td>
                                </tr>
                            </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<script>
    (function () {
        var pickers = document.querySelectorAll('[data-class-group-picker]');
        pickers.forEach(function (picker) {
            var label = picker.querySelector('[data-class-group-picker-label]');
            var inputs = picker.querySelectorAll('[data-class-group-picker-input]');
            var updateLabel = function () {
                var selected = Array.prototype.filter.call(inputs, function (input) {
                    return input.checked;
                }).length;
                if (label) {
                    label.textContent = selected === 0
                            ? 'Select class groups'
                            : selected + (selected === 1 ? ' class group selected' : ' class groups selected');
                }
            };
            inputs.forEach(function (input) {
                input.addEventListener('change', updateLabel);
            });
            updateLabel();
        });

        document.addEventListener('click', function (event) {
            pickers.forEach(function (picker) {
                if (!picker.contains(event.target)) {
                    picker.removeAttribute('open');
                }
            });
        });

        var eventType = document.getElementById('schedule-event-type');
        var lessonReference = document.querySelector('[data-schedule-reference="lesson"]');
        var assessmentReference = document.querySelector('[data-schedule-reference="assessment"]');
        var lessonInput = document.getElementById('schedule-event-lesson-id');
        var assessmentInput = document.getElementById('schedule-event-assessment-id');
        var syncReferences = function () {
            if (!eventType) {
                return;
            }
            var lessonSelected = eventType.value === 'lesson';
            var assessmentSelected = eventType.value === 'assessment';
            if (lessonReference) {
                lessonReference.hidden = !lessonSelected;
            }
            if (assessmentReference) {
                assessmentReference.hidden = !assessmentSelected;
            }
            if (lessonInput) {
                lessonInput.required = lessonSelected;
                if (!lessonSelected) {
                    lessonInput.value = '';
                }
            }
            if (assessmentInput) {
                assessmentInput.required = assessmentSelected;
                if (!assessmentSelected) {
                    assessmentInput.value = '';
                }
            }
        };
        if (eventType) {
            eventType.addEventListener('change', syncReferences);
            syncReferences();
        }
    }());
</script>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
</body>
</html>
