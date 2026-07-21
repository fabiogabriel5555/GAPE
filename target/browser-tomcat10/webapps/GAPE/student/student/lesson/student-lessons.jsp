<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    request.setAttribute("activeMenu", "learning");
    request.setAttribute("pageTitle", "Lessons & Assessments");
    request.setAttribute("studentPageTitle", "Lessons & Assessments");
    request.setAttribute("studentPageDescription", "Open lesson access and manage the assessments available through your active class groups.");
%>
<%@ include file="/WEB-INF/fragments/student-dashboard-start.jspf" %>

<div class="gape-student-mode-grid mb-24" data-student-tabs role="tablist" aria-label="Lessons and assessments sections">
    <button type="button" class="gape-student-mode-card is-active" data-student-tab="lessons" role="tab" aria-selected="true" aria-controls="lessons">
        <span class="gape-student-mode-icon bg-main-50 text-main-600 text-24"><i class="ph ph-chalkboard"></i></span>
        <span class="min-w-0">
            <span class="text-20 fw-semibold text-neutral-800 d-block mb-8">Lessons</span>
            <span class="text-13 text-main-600 fw-semibold">${activeLessonCount} active lessons<c:if test="${completedLessonCount gt 0}"> &middot; ${completedLessonCount} completed</c:if></span>
        </span>
    </button>
    <button type="button" class="gape-student-mode-card" data-student-tab="assessments" role="tab" aria-selected="false" aria-controls="assessments">
        <span class="gape-student-mode-icon bg-info-50 text-info-600 text-24"><i class="ph ph-seal-question"></i></span>
        <span class="min-w-0">
            <span class="text-20 fw-semibold text-neutral-800 d-block mb-8">Assessments</span>
            <span class="text-13 text-main-600 fw-semibold">${activeAssessmentCount} active assessments<c:if test="${completedAssessmentCount gt 0}"> &middot; ${completedAssessmentCount} completed</c:if></span>
        </span>
    </button>
    <button type="button" class="gape-student-mode-card" data-student-tab="attendance" role="tab" aria-selected="false" aria-controls="attendance">
        <span class="gape-student-mode-icon bg-success-50 text-success-600 text-24"><i class="ph ph-user-check"></i></span>
        <span class="min-w-0">
            <span class="text-20 fw-semibold text-neutral-800 d-block mb-8">Attendance</span>
            <span class="text-13 text-main-600 fw-semibold">${fn:length(attendanceRecords)} attendance records</span>
        </span>
    </button>
</div>

<section id="lessons" class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30 mb-24" data-student-tab-panel="lessons" role="tabpanel">
    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-24">
        <div>
            <h6 class="text-20 fw-semibold text-neutral-800 mb-4">Lessons</h6>
            <span class="text-14 text-neutral-500">All lessons visible from your active class group enrollments.</span>
        </div>
        <a href="${pageContext.request.contextPath}/student/events" class="gape-student-card-icon-button" aria-label="Open events" title="Open events">
            <i class="ph ph-calendar-dots"></i>
        </a>
    </div>
    <div class="d-flex flex-column gap-24" data-student-lessons-modern>
        <c:forEach var="courseGroup" items="${lessonCourseGroups}">
            <section class="gape-student-class-course-group">
                <div class="gape-student-class-course-group__header d-flex align-items-center justify-content-between gap-16 flex-wrap">
                    <div class="d-flex align-items-center gap-12 min-w-0"><span class="gape-student-icon gape-student-soft-blue text-22"><i class="ph ph-books"></i></span><div class="min-w-0"><h4 class="text-18 fw-semibold text-neutral-800 mb-4 text-line-1"><c:out value="${courseGroup.courseName}"/></h4><span class="text-13 text-neutral-500">${fn:length(courseGroup.subjectGroups)} subjects &middot; ${courseGroup.lessonCount} lessons</span></div></div>
                </div>
                <c:forEach var="subjectGroup" items="${courseGroup.subjectGroups}">
                    <section class="gape-student-class-subject-group">
                        <div class="d-flex align-items-center gap-12 mb-14"><span class="gape-student-icon gape-student-soft-violet text-22"><i class="ph ph-book-open-text"></i></span><div class="min-w-0"><h5 class="text-16 fw-semibold text-neutral-800 mb-4 text-line-1"><c:out value="${subjectGroup.subjectName}"/></h5><span class="text-13 text-neutral-500" title="<c:out value='${subjectGroup.contextTitle}'/>"><c:out value="${subjectGroup.contextHtml}" escapeXml="false"/> &middot; ${subjectGroup.lessonCount} lessons</span></div></div>
                        <c:forEach var="occurrenceGroup" items="${subjectGroup.occurrenceGroups}">
                            <div class="gape-student-class-occurrence-group border border-neutral-30 rounded-10 px-16 py-14 mb-16">
                                <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap mb-12"><div class="d-flex align-items-center gap-10 min-w-0"><span class="gape-student-icon gape-student-soft-blue text-18"><i class="ph ph-calendar-blank"></i></span><div class="min-w-0"><span class="text-14 fw-semibold text-neutral-800 d-block text-line-1"><c:out value="${occurrenceGroup.label}"/></span><span class="text-12 text-neutral-500"><c:out value="${occurrenceGroup.dateRangeLabel}"/> &middot; ${occurrenceGroup.lessonCount} lessons</span></div></div></div>
                                <c:forEach var="classGroupLessons" items="${occurrenceGroup.classGroups}">
                                    <div class="gape-student-assessment-class-group gape-student-lesson-class-group border border-neutral-30 rounded-10 px-14 py-12 mb-12">
                                        <div class="d-flex align-items-center gap-10 mb-10"><span class="gape-student-icon gape-student-soft-blue text-18"><i class="ph ph-users-three"></i></span><div class="min-w-0"><span class="text-13 fw-semibold text-neutral-800 d-block text-line-1"><c:out value="${classGroupLessons.code}"/></span><span class="text-12 text-neutral-500">${classGroupLessons.lessonCount} lessons</span></div></div>
                                        <div class="row gy-4">
                                            <c:forEach var="lesson" items="${classGroupLessons.lessons}">
                                                <div class="col-xxl-3 col-xl-4 col-md-6"><article class="gape-student-card gape-student-class-group-card px-14 py-14 h-100 position-relative"><div class="gape-student-class-group-card__top mb-10"><span class="gape-student-class-group-card__icon gape-student-class-group-card__icon--compact ${lesson.typeBadgeClass}"><i class="${lesson.typeIconClass}"></i></span><div class="min-w-0"><h6 class="text-14 fw-semibold text-neutral-800 mb-4 text-line-1"><c:out value="${lesson.title}"/></h6><span class="text-12 text-neutral-500"><c:out value="${lesson.typeLabel}"/></span></div></div><div class="gape-student-class-group-card__badges mb-12"><span class="${lesson.stateBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12"><c:out value="${lesson.stateLabel}"/></span></div><div class="gape-student-class-group-card__meta mb-12"><span class="gape-student-enrollment-period" data-gape-datetime-display>Date: <c:out value="${lesson.dateRangeLabel}"/></span><span><c:out value="${lesson.durationLabel}"/> &middot; <c:out value="${lesson.attendanceLabel}"/></span></div><div class="gape-student-card-actions"><button type="button" class="gape-student-card-icon-button" data-bs-toggle="modal" data-bs-target="#studentLessonModernDetail${lesson.id}" aria-label="View lesson details" title="View lesson details"><i class="ph ph-eye"></i></button><c:if test="${(lesson.online or lesson.hybrid) and lesson.hasMeetingLink}"><a href="${pageContext.request.contextPath}/student/lessons/${lesson.id}/access" target="_blank" rel="noopener noreferrer" class="gape-student-card-icon-button gape-student-card-icon-button--meeting" aria-label="Open meeting" title="Open meeting"><i class="ph ph-video-camera"></i></a></c:if></div></article></div>
                                                <div class="modal fade gape-student-lesson-modal" id="studentLessonModernDetail${lesson.id}" tabindex="-1" aria-labelledby="studentLessonModernDetailTitle${lesson.id}" aria-hidden="true"><div class="modal-dialog modal-dialog-centered modal-dialog-scrollable"><div class="modal-content rounded-10 border-0"><div class="modal-header"><div><h5 class="modal-title text-18 fw-semibold" id="studentLessonModernDetailTitle${lesson.id}"><c:out value="${lesson.title}"/></h5><span class="text-13 text-neutral-500"><c:out value="${classGroupLessons.code}"/> &middot; <c:out value="${subjectGroup.subjectName}"/></span></div><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div><div class="modal-body"><div class="gape-student-class-group-card__meta gape-student-class-group-card__meta--wide mb-16"><span>State: <c:out value="${lesson.stateLabel}"/></span><span>Type: <c:out value="${lesson.typeLabel}"/></span><span data-gape-datetime-display>Date: <c:out value="${lesson.dateRangeLabel}"/></span><span>Duration: <c:out value="${lesson.durationLabel}"/></span><span>Attendance: <c:out value="${lesson.attendanceLabel}"/></span><c:if test="${lesson.hasRoom}"><span>Room: <c:out value="${lesson.physicalRoomCode}"/></span></c:if><c:if test="${lesson.hasMeetingLink}"><span>Provider: <c:out value="${lesson.providerLabel}"/></span></c:if></div><p class="text-14 text-neutral-600 mb-0"><c:out value="${lesson.description}"/></p></div><div class="modal-footer"><button type="button" class="bg-main-600 px-20 py-10 rounded-8 fw-semibold text-white" data-bs-dismiss="modal">Close</button></div></div></div></div>
                                            </c:forEach>
                                        </div>
                                    </div>
                                </c:forEach>
                            </div>
                        </c:forEach>
                    </section>
                </c:forEach>
            </section>
        </c:forEach>
        <c:if test="${empty lessonCourseGroups}"><div class="gape-student-empty text-center px-24 py-40"><span class="gape-student-icon gape-student-soft-amber text-28 mb-16"><i class="ph ph-calendar-check"></i></span><h4 class="text-18 fw-semibold text-neutral-800 mb-8">No active lessons available</h4><p class="text-14 text-neutral-500 mb-0">Lessons appear after your active class groups publish their schedules.</p></div></c:if>
    </div>
    <c:if test="${not empty lessonCompletedCourseGroups}">
        <section class="gape-learning-management-completed-wrapper gape-student-completed-lessons mt-20" data-student-completed-lessons data-student-completed-archive>
            <div class="gape-learning-management-completed-divider">Completed Lessons</div>
            <article class="gape-learning-management-completed-node border rounded-8 px-18 py-16 bg-white">
                <div class="gape-deferred-management-archive-row">
                    <div class="d-flex align-items-center gap-12 min-w-0"><span class="gape-learning-management-group-icon bg-danger-50 text-danger-600"><i class="ph ph-archive" aria-hidden="true"></i></span><div class="min-w-0"><span class="fw-medium text-14 text-neutral-700 d-block">Completed Lessons</span><span class="gape-node-meta text-12" data-student-completed-lessons-summary>Past lessons grouped by course, subject and class group</span></div></div>
                    <div class="gape-deferred-management-archive-spacer"></div>
                    <div><span class="cd-element-count" data-student-completed-lessons-count>${completedLessonCount}</span></div>
                    <div><span class="bg-neutral-20 text-neutral-600 px-14 py-6 border-neutral-30 border rounded-pill text-13">Completed</span></div>
                    <div class="d-flex justify-content-end"><button type="button" class="gape-tree-toggle text-20 text-neutral-500 hover-text-main-600" data-student-completed-lessons-toggle aria-expanded="false" title="Show completed lessons" aria-label="Show completed lessons"><i class="ph ph-caret-down" aria-hidden="true"></i></button></div>
                </div>
                <div class="gape-learning-management-completed-panel d-none" data-student-completed-lessons-panel>
                <div class="d-flex flex-column gap-24 mt-20">
                    <c:forEach var="courseGroup" items="${lessonCompletedCourseGroups}">
                        <section class="gape-student-class-course-group">
                            <div class="gape-student-class-course-group__header d-flex align-items-center gap-12"><span class="gape-student-icon gape-student-soft-blue text-22"><i class="ph ph-books"></i></span><div class="min-w-0"><h4 class="text-18 fw-semibold text-neutral-800 mb-4 text-line-1"><c:out value="${courseGroup.courseName}"/></h4><span class="text-13 text-neutral-500">${courseGroup.lessonCount} lessons</span></div></div>
                            <c:forEach var="subjectGroup" items="${courseGroup.subjectGroups}">
                                <section class="gape-student-class-subject-group"><div class="d-flex align-items-center gap-12 mb-14"><span class="gape-student-icon gape-student-soft-violet text-22"><i class="ph ph-book-open-text"></i></span><h5 class="text-16 fw-semibold text-neutral-800 mb-0 text-line-1"><c:out value="${subjectGroup.subjectName}"/></h5></div>
                                    <c:forEach var="occurrenceGroup" items="${subjectGroup.occurrenceGroups}"><div class="gape-student-class-occurrence-group border border-neutral-30 rounded-10 px-16 py-14 mb-16"><div class="d-flex align-items-center gap-10 mb-12"><span class="gape-student-icon gape-student-soft-blue text-18"><i class="ph ph-calendar-blank"></i></span><div class="min-w-0"><span class="text-14 fw-semibold text-neutral-800 d-block text-line-1"><c:out value="${occurrenceGroup.label}"/></span><span class="text-12 text-neutral-500"><c:out value="${occurrenceGroup.dateRangeLabel}"/> &middot; ${occurrenceGroup.lessonCount} lessons</span></div></div>
                                        <c:forEach var="classGroupLessons" items="${occurrenceGroup.classGroups}"><div class="gape-student-assessment-class-group gape-student-lesson-class-group border border-neutral-30 rounded-10 px-14 py-12 mb-12"><div class="d-flex align-items-center gap-10 mb-10"><span class="gape-student-icon gape-student-soft-blue text-18"><i class="ph ph-users-three"></i></span><div class="min-w-0"><span class="text-13 fw-semibold text-neutral-800 d-block text-line-1"><c:out value="${classGroupLessons.code}"/></span><span class="text-12 text-neutral-500">${classGroupLessons.lessonCount} lessons</span></div></div><div class="row gy-4"><c:forEach var="lesson" items="${classGroupLessons.lessons}"><div class="col-xxl-3 col-xl-4 col-md-6"><article class="gape-student-card gape-student-class-group-card px-14 py-14 h-100 position-relative"><div class="gape-student-class-group-card__top mb-10"><span class="gape-student-class-group-card__icon gape-student-class-group-card__icon--compact ${lesson.typeBadgeClass}"><i class="${lesson.typeIconClass}"></i></span><div class="min-w-0"><h6 class="text-14 fw-semibold text-neutral-800 mb-4 text-line-1"><c:out value="${lesson.title}"/></h6><span class="text-12 text-neutral-500"><c:out value="${lesson.typeLabel}"/></span></div></div><div class="gape-student-class-group-card__badges mb-12"><span class="${lesson.stateBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12"><c:out value="${lesson.stateLabel}"/></span></div><div class="gape-student-class-group-card__meta mb-12"><span class="gape-student-enrollment-period" data-gape-datetime-display>Date: <c:out value="${lesson.dateRangeLabel}"/></span><span><c:out value="${lesson.durationLabel}"/> &middot; <c:out value="${lesson.attendanceLabel}"/></span></div><div class="gape-student-card-actions"><button type="button" class="gape-student-card-icon-button" data-bs-toggle="modal" data-bs-target="#studentLessonModernDetail${lesson.id}" aria-label="View lesson details" title="View lesson details"><i class="ph ph-eye"></i></button></div></article></div><div class="modal fade gape-student-lesson-modal" id="studentLessonModernDetail${lesson.id}" tabindex="-1" aria-labelledby="studentLessonModernDetailTitle${lesson.id}" aria-hidden="true"><div class="modal-dialog modal-dialog-centered modal-dialog-scrollable"><div class="modal-content rounded-10 border-0"><div class="modal-header"><div><h5 class="modal-title text-18 fw-semibold" id="studentLessonModernDetailTitle${lesson.id}"><c:out value="${lesson.title}"/></h5><span class="text-13 text-neutral-500"><c:out value="${classGroupLessons.code}"/> &middot; <c:out value="${subjectGroup.subjectName}"/></span></div><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div><div class="modal-body"><div class="gape-student-class-group-card__meta gape-student-class-group-card__meta--wide mb-16"><span>State: <c:out value="${lesson.stateLabel}"/></span><span>Type: <c:out value="${lesson.typeLabel}"/></span><span data-gape-datetime-display>Date: <c:out value="${lesson.dateRangeLabel}"/></span><span>Duration: <c:out value="${lesson.durationLabel}"/></span><span>Attendance: <c:out value="${lesson.attendanceLabel}"/></span><c:if test="${lesson.hasRoom}"><span>Room: <c:out value="${lesson.physicalRoomCode}"/></span></c:if><c:if test="${lesson.hasMeetingLink}"><span>Provider: <c:out value="${lesson.providerLabel}"/></span></c:if></div><p class="text-14 text-neutral-600 mb-0"><c:out value="${lesson.description}"/></p></div><div class="modal-footer"><button type="button" class="bg-main-600 px-20 py-10 rounded-8 fw-semibold text-white" data-bs-dismiss="modal">Close</button></div></div></div></div></c:forEach></div></div></c:forEach>
                                    </div></c:forEach>
                                </section>
                            </c:forEach>
                        </section>
                    </c:forEach>
                </div>
                </div>
            </article>
        </section>
    </c:if>
    <div class="d-none">
        <c:forEach var="courseGroup" items="${lessonCourseGroups}">
            <section class="gape-student-class-course-group">
                <div class="gape-student-class-course-group__header d-flex align-items-center justify-content-between gap-16 flex-wrap">
                    <div class="d-flex align-items-center gap-12 min-w-0">
                        <span class="gape-student-icon gape-student-soft-blue text-22"><i class="ph ph-books"></i></span>
                        <div class="min-w-0">
                            <h4 class="text-18 fw-semibold text-neutral-800 mb-4"><c:out value="${courseGroup.courseName}"/></h4>
                            <span class="text-13 text-neutral-500">${fn:length(courseGroup.subjectGroups)} subjects &middot; ${courseGroup.lessonCount} lessons</span>
                        </div>
                    </div>
                </div>
                <c:forEach var="subjectGroup" items="${courseGroup.subjectGroups}">
                    <section class="gape-student-class-subject-group" data-lesson-count="${fn:length(subjectGroup.lessons)}">
                        <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-14">
                            <div class="d-flex align-items-center gap-12 min-w-0">
                                <span class="gape-student-icon gape-student-soft-violet text-22"><i class="ph ph-book-open-text"></i></span>
                                <div class="min-w-0">
                                    <h5 class="text-16 fw-semibold text-neutral-800 mb-4"><c:out value="${subjectGroup.subjectName}"/></h5>
                                    <span class="text-13 text-neutral-500" title="<c:out value='${subjectGroup.contextTitle}'/>">
                                        <c:out value="${subjectGroup.contextHtml}" escapeXml="false"/> &middot; ${subjectGroup.lessonCount} lessons
                                    </span>
                                </div>
                            </div>
                        </div>
                        <c:forEach var="classGroupLessons" items="${subjectGroup.classGroups}">
                            <div class="d-flex align-items-center gap-10 mt-16 mb-10">
                                <span class="gape-student-structure-group-chip"><i class="ph ph-users-three"></i><c:out value="${classGroupLessons.code}"/></span>
                                <span class="text-12 text-neutral-500">${classGroupLessons.lessonCount} lessons</span>
                            </div>
                            <div class="row gy-3">
                                <c:forEach var="lesson" items="${classGroupLessons.lessons}">
                                    <div class="col-xxl-3 col-xl-4 col-md-6">
                                        <article class="gape-student-card gape-student-class-group-card px-14 py-14 h-100">
                                            <div class="d-flex align-items-start justify-content-between gap-12 mb-12">
                                                <span class="${lesson.typeBadgeClass} w-40 h-40 rounded-10 d-inline-flex align-items-center justify-content-center text-20"><i class="${lesson.typeIconClass}"></i></span>
                                                <span class="${lesson.stateBadgeClass} px-10 py-5 border-neutral-30 border rounded-pill text-12"><c:out value="${lesson.stateLabel}"/></span>
                                            </div>
                                            <h4 class="text-16 fw-semibold text-neutral-800 mb-6 text-line-2"><c:out value="${lesson.title}"/></h4>
                                            <div class="gape-student-class-group-card__meta mb-14">
                                                <span data-gape-datetime-display><c:out value="${lesson.dateRangeLabel}"/></span>
                                                <span><c:out value="${lesson.typeLabel}"/> &middot; <c:out value="${lesson.durationLabel}"/></span>
                                            </div>
                                            <div class="gape-student-card-actions">
                                                <button type="button" class="gape-student-card-icon-button" data-bs-toggle="modal" data-bs-target="#studentLessonDetail${lesson.id}" aria-label="View lesson details" title="View lesson details"><i class="ph ph-eye"></i></button>
                                                <c:if test="${(lesson.online or lesson.hybrid) and lesson.hasMeetingLink}"><a href="${pageContext.request.contextPath}/student/lessons/${lesson.id}/access" target="_blank" rel="noopener noreferrer" class="gape-student-card-icon-button gape-student-card-icon-button--meeting" aria-label="Open meeting" title="Open meeting"><i class="ph ph-video-camera"></i></a></c:if>
                                            </div>
                                        </article>
                                    </div>
                                    <div class="modal fade" id="studentLessonDetail${lesson.id}" tabindex="-1" aria-labelledby="studentLessonDetailTitle${lesson.id}" aria-hidden="true">
                                        <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable"><div class="modal-content rounded-10 border-0">
                                            <div class="modal-header"><div><h5 class="modal-title text-18 fw-semibold" id="studentLessonDetailTitle${lesson.id}"><c:out value="${lesson.title}"/></h5><span class="text-13 text-neutral-500"><c:out value="${classGroupLessons.code}"/> · <c:out value="${subjectGroup.subjectName}"/></span></div><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div>
                                            <div class="modal-body"><div class="gape-student-class-group-card__meta gape-student-class-group-card__meta--wide mb-16"><span>State: <c:out value="${lesson.stateLabel}"/></span><span>Type: <c:out value="${lesson.typeLabel}"/></span><span data-gape-datetime-display>Date: <c:out value="${lesson.dateRangeLabel}"/></span><span>Duration: <c:out value="${lesson.durationLabel}"/></span><span>Attendance: <c:out value="${lesson.attendanceLabel}"/></span><c:if test="${lesson.hasRoom}"><span>Room: <c:out value="${lesson.physicalRoomCode}"/></span></c:if><c:if test="${lesson.hasMeetingLink}"><span>Provider: <c:out value="${lesson.providerLabel}"/></span></c:if></div><p class="text-14 text-neutral-600 mb-0"><c:out value="${lesson.description}"/></p></div>
                                            <div class="modal-footer"><button type="button" class="bg-main-600 px-20 py-10 rounded-8 fw-semibold text-white" data-bs-dismiss="modal">Close</button></div>
                                        </div></div>
                                    </div>
                                </c:forEach>
                            </div>
                        </c:forEach>
                    </section>
                </c:forEach>
            </section>
        </c:forEach>
        <c:if test="${empty lessons}">
            <div class="gape-student-empty text-center px-24 py-40">
                <span class="gape-student-icon gape-student-soft-amber text-28 mb-16"><i class="ph ph-calendar-check"></i></span>
                <h4 class="text-18 fw-semibold text-neutral-800 mb-8">No lessons available yet</h4>
                <p class="text-14 text-neutral-500 mb-0">Lessons appear after your active class groups publish their schedules.</p>
            </div>
        </c:if>
    </div>
</section>

<div data-student-tab-panel="assessments" role="tabpanel" hidden>
    <%@ include file="/WEB-INF/fragments/student-assessment-catalog.jspf" %>
</div>

<section id="attendance-legacy" class="d-none" data-student-attendance-legacy>
    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-24">
        <div>
            <h6 class="text-20 fw-semibold text-neutral-800 mb-4">Attendance</h6>
            <span class="text-14 text-neutral-500">Presence, absence and permanence records from your lessons.</span>
        </div>
        <span class="bg-success-50 text-success-600 px-14 py-8 rounded-pill text-13 fw-semibold">${fn:length(attendanceRecords)} records</span>
    </div>
    <div class="d-flex flex-column gap-24">
        <c:forEach var="courseGroup" items="${attendanceCourseGroups}">
            <section class="gape-student-class-course-group">
                <div class="gape-student-class-course-group__header d-flex align-items-center gap-12"><span class="gape-student-icon gape-student-soft-blue text-22"><i class="ph ph-books"></i></span><div><h4 class="text-18 fw-semibold text-neutral-800 mb-4"><c:out value="${courseGroup.name}"/></h4><span class="text-13 text-neutral-500">${courseGroup.recordCount} attendance records</span></div></div>
                <c:forEach var="subjectGroup" items="${courseGroup.subjectGroups}">
                    <section class="gape-student-class-subject-group"><div class="d-flex align-items-center gap-12 mb-12"><span class="gape-student-icon gape-student-soft-violet text-22"><i class="ph ph-book-open-text"></i></span><h5 class="text-16 fw-semibold text-neutral-800 mb-0"><c:out value="${subjectGroup.name}"/></h5></div>
                        <c:forEach var="classGroup" items="${subjectGroup.classGroups}">
                            <div class="d-flex align-items-center gap-10 mt-12 mb-10"><span class="gape-student-structure-group-chip"><i class="ph ph-users-three"></i><c:out value="${classGroup.code}"/></span><span class="text-12 text-neutral-500">${classGroup.recordCount} records</span></div>
                            <div class="row gy-3">
                                <c:forEach var="record" items="${classGroup.records}">
            <div class="col-xxl-3 col-xl-4 col-md-6"><article class="gape-student-card gape-student-class-group-card px-14 py-14 h-100">
                    <div class="gape-student-class-group-card__top mb-10">
                        <span class="gape-student-class-group-card__icon gape-student-class-group-card__icon--compact"><i class="ph ph-user-check"></i></span>
                        <div class="min-w-0">
                            <h6 class="text-14 fw-semibold text-neutral-800 mb-4 text-line-2"><c:out value="${record.lessonTitle}"/></h6>
                            <span class="text-12 text-neutral-500"><c:out value="${record.statusLabel}"/></span>
                        </div>
                    </div>
                    <div class="gape-student-class-group-card__badges mb-12">
                        <span class="${record.statusBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12"><c:out value="${record.statusLabel}"/></span>
                        <span class="${record.stateBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12"><c:out value="${record.stateLabel}"/></span>
                    </div>
                    <div class="gape-student-class-group-card__meta mb-12">
                        <span data-gape-datetime-display>In: <c:out value="${record.checkIn}"/></span>
                        <span data-gape-datetime-display>Out: <c:out value="${record.checkOut}"/></span>
                        <span><c:out value="${record.permanenceLabel}"/></span>
                        <span><c:out value="${record.sourceLabel}"/></span>
                    </div>
                    <div class="gape-student-card-actions">
                        <button type="button" class="gape-student-card-icon-button" aria-label="View attendance details" title="View attendance details" data-bs-toggle="modal" data-bs-target="#studentAttendanceDetail${record.id}"><i class="ph ph-eye"></i></button>
                    </div>
                </article>
            </div>
            <div class="modal fade" id="studentAttendanceDetail${record.id}" tabindex="-1" aria-labelledby="studentAttendanceDetailTitle${record.id}" aria-hidden="true"><div class="modal-dialog modal-dialog-centered modal-dialog-scrollable"><div class="modal-content rounded-10 border-0"><div class="modal-header"><div><h5 class="modal-title text-18 fw-semibold" id="studentAttendanceDetailTitle${record.id}"><c:out value="${record.lessonTitle}"/></h5><span class="text-13 text-neutral-500"><c:out value="${courseGroup.name}"/> · <c:out value="${subjectGroup.name}"/> · <c:out value="${classGroup.code}"/></span></div><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div><div class="modal-body"><div class="gape-student-class-group-card__meta gape-student-class-group-card__meta--wide mb-16"><span>Status: <c:out value="${record.statusLabel}"/></span><span>State: <c:out value="${record.stateLabel}"/></span><span>Source: <c:out value="${record.sourceLabel}"/></span><span data-gape-datetime-display>In: <c:out value="${record.checkIn}"/></span><span data-gape-datetime-display>Out: <c:out value="${record.checkOut}"/></span><span>Duration: <c:out value="${record.permanenceLabel}"/></span></div><p class="text-14 text-neutral-600 mb-0">Notes: <c:out value="${record.notes}"/></p></div><div class="modal-footer"><button type="button" class="bg-main-600 px-20 py-10 rounded-8 fw-semibold text-white" data-bs-dismiss="modal">Close</button></div></div></div></div>
                                </c:forEach>
                            </div>
                        </c:forEach>
                    </section>
                </c:forEach>
            </section>
        </c:forEach>
        <c:if test="${empty attendanceRecords}">
            <div class="col-12"><div class="gape-student-empty text-center px-24 py-40"><span class="gape-student-icon gape-student-soft-main text-28 mb-16"><i class="ph ph-user-check"></i></span><h4 class="text-18 fw-semibold text-neutral-800 mb-8">No attendance records</h4><p class="text-14 text-neutral-500 mb-0">Attendance records will appear after lessons are processed.</p></div></div>
        </c:if>
    </div>
</section>

<section id="attendance" class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30" data-student-tab-panel="attendance" role="tabpanel" hidden>
    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-24">
        <div>
            <h6 class="text-20 fw-semibold text-neutral-800 mb-4">Attendance</h6>
            <span class="text-14 text-neutral-500">Presence, absence and permanence records grouped by course, subject, occurrence and class group.</span>
        </div>
        <a href="${pageContext.request.contextPath}/student/events" class="gape-student-card-icon-button" aria-label="Open events" title="Open events"><i class="ph ph-calendar-dots"></i></a>
    </div>
    <div class="d-flex flex-column gap-24" data-student-attendance-modern>
        <%@ include file="/WEB-INF/fragments/student-attendance-catalog-groups.jspf" %>
        <c:if test="${empty attendanceCourseGroups}"><div class="gape-student-empty text-center px-24 py-40"><span class="gape-student-icon gape-student-soft-amber text-28 mb-16"><i class="ph ph-user-check"></i></span><h4 class="text-18 fw-semibold text-neutral-800 mb-8">No attendance records</h4><p class="text-14 text-neutral-500 mb-0">Attendance records will appear after lessons are processed.</p></div></c:if>
    </div>

    <c:if test="${not empty attendanceCompletedCourseGroups}">
        <section class="gape-learning-management-completed-wrapper gape-student-completed-attendance mt-20" data-student-completed-attendance data-student-completed-archive>
            <div class="gape-learning-management-completed-divider">Completed Attendance</div>
            <article class="gape-learning-management-completed-node border rounded-8 px-18 py-16 bg-white">
                <div class="gape-deferred-management-archive-row">
                    <div class="d-flex align-items-center gap-12 min-w-0"><span class="gape-learning-management-group-icon bg-success-50 text-success-600"><i class="ph ph-archive" aria-hidden="true"></i></span><div class="min-w-0"><span class="fw-medium text-14 text-neutral-700 d-block">Completed Attendance</span><span class="gape-node-meta text-12" data-student-completed-attendance-summary>Present and justified attendance records grouped by course, subject, occurrence and class group</span></div></div>
                    <div class="gape-deferred-management-archive-spacer"></div>
                    <div><span class="cd-element-count" data-student-completed-attendance-count>${completedAttendanceCount}</span></div>
                    <div><span class="bg-success-50 text-success-600 px-14 py-6 border-success-30 border rounded-pill text-13">Present / Justified</span></div>
                    <div class="d-flex justify-content-end"><button type="button" class="gape-tree-toggle text-20 text-neutral-500 hover-text-main-600" data-student-completed-attendance-toggle aria-expanded="false" title="Show completed attendance" aria-label="Show completed attendance"><i class="ph ph-caret-down" aria-hidden="true"></i></button></div>
                </div>
                <div class="gape-learning-management-completed-panel d-none" data-student-completed-attendance-panel>
                    <c:set var="attendanceCourseGroups" value="${attendanceCompletedCourseGroups}"/>
                    <%@ include file="/WEB-INF/fragments/student-attendance-catalog-groups.jspf" %>
                </div>
            </article>
        </section>
    </c:if>
</section>

<script>
    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('.gape-student-lesson-modal, .gape-student-assessment-modal, .gape-student-attendance-modal').forEach(function (modal) {
            if (modal.parentElement !== document.body) document.body.appendChild(modal);
        });
        var completedLessons = document.querySelector('[data-student-completed-lessons]');
        if (completedLessons) {
            var completedLessonsToggle = completedLessons.querySelector('[data-student-completed-lessons-toggle]');
            var completedLessonsPanel = completedLessons.querySelector('[data-student-completed-lessons-panel]');
            if (completedLessonsToggle && completedLessonsPanel) {
                completedLessonsToggle.addEventListener('click', function () {
                    var open = completedLessonsPanel.classList.toggle('d-none') === false;
                    completedLessonsToggle.setAttribute('aria-expanded', open ? 'true' : 'false');
                    completedLessonsToggle.setAttribute('title', open ? 'Hide completed lessons' : 'Show completed lessons');
                    completedLessonsToggle.setAttribute('aria-label', open ? 'Hide completed lessons' : 'Show completed lessons');
                    var icon = completedLessonsToggle.querySelector('i');
                    if (icon) {
                        icon.classList.toggle('ph-caret-up', open);
                        icon.classList.toggle('ph-caret-down', !open);
                    }
                });
            }
        }
        var completedAssessments = document.querySelector('[data-student-completed-assessments]');
        if (completedAssessments) {
            var completedAssessmentsToggle = completedAssessments.querySelector('[data-student-completed-assessments-toggle]');
            var completedAssessmentsPanel = completedAssessments.querySelector('[data-student-completed-assessments-panel]');
            if (completedAssessmentsToggle && completedAssessmentsPanel) {
                completedAssessmentsToggle.addEventListener('click', function () {
                    var open = completedAssessmentsPanel.classList.toggle('d-none') === false;
                    completedAssessmentsToggle.setAttribute('aria-expanded', open ? 'true' : 'false');
                    completedAssessmentsToggle.setAttribute('title', open ? 'Hide completed assessments' : 'Show completed assessments');
                    completedAssessmentsToggle.setAttribute('aria-label', open ? 'Hide completed assessments' : 'Show completed assessments');
                    var icon = completedAssessmentsToggle.querySelector('i');
                    if (icon) {
                        icon.classList.toggle('ph-caret-up', open);
                        icon.classList.toggle('ph-caret-down', !open);
                    }
                });
            }
        }
        var completedAttendance = document.querySelector('[data-student-completed-attendance]');
        if (completedAttendance) {
            var completedAttendanceToggle = completedAttendance.querySelector('[data-student-completed-attendance-toggle]');
            var completedAttendancePanel = completedAttendance.querySelector('[data-student-completed-attendance-panel]');
            if (completedAttendanceToggle && completedAttendancePanel) {
                completedAttendanceToggle.addEventListener('click', function () {
                    var open = completedAttendancePanel.classList.toggle('d-none') === false;
                    completedAttendanceToggle.setAttribute('aria-expanded', open ? 'true' : 'false');
                    completedAttendanceToggle.setAttribute('title', open ? 'Hide completed attendance' : 'Show completed attendance');
                    completedAttendanceToggle.setAttribute('aria-label', open ? 'Hide completed attendance' : 'Show completed attendance');
                    var icon = completedAttendanceToggle.querySelector('i');
                    if (icon) {
                        icon.classList.toggle('ph-caret-up', open);
                        icon.classList.toggle('ph-caret-down', !open);
                    }
                });
            }
        }
        var root = document.querySelector('[data-student-tabs]');
        if (!root) return;
        var tabs = Array.prototype.slice.call(root.querySelectorAll('[data-student-tab]'));
        var panels = Array.prototype.slice.call(document.querySelectorAll('[data-student-tab-panel]'));
        function activate(name, updateHash) {
            if (!panels.some(function (panel) { return panel.getAttribute('data-student-tab-panel') === name; })) return;
            tabs.forEach(function (tab) {
                var active = tab.getAttribute('data-student-tab') === name;
                tab.classList.toggle('is-active', active);
                tab.setAttribute('aria-selected', active ? 'true' : 'false');
            });
            panels.forEach(function (panel) { panel.hidden = panel.getAttribute('data-student-tab-panel') !== name; });
            if (updateHash && window.history && window.history.replaceState) window.history.replaceState({studentLessonsTab: name}, '', window.location.pathname + window.location.search + '#' + name);
        }
        tabs.forEach(function (tab) { tab.addEventListener('click', function () { activate(tab.getAttribute('data-student-tab'), true); }); });
        activate((window.location.hash || '#lessons').slice(1), false);
    });
</script>

<%@ include file="/WEB-INF/fragments/student-dashboard-end.jspf" %>
