<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    request.setAttribute("activeMenu", "lessons");
    request.setAttribute("pageTitle", "Lesson Access");
    request.setAttribute("studentPageTitle", "My Lessons");
    request.setAttribute("studentPageDescription", "Open online, hybrid and in-person lesson access from the class groups where you are enrolled.");
%>
<%@ include file="/WEB-INF/fragments/student-dashboard-start.jspf" %>

<section class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30">
    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-24">
        <div>
            <h6 class="text-20 fw-semibold text-neutral-800 mb-4">Lesson access</h6>
            <span class="text-14 text-neutral-500">All lessons visible from your active class group enrollments.</span>
        </div>
        <a href="${pageContext.request.contextPath}/student/calendar" class="gape-student-card-icon-button" aria-label="Open calendar" title="Open calendar">
            <i class="ph ph-calendar-dots"></i>
        </a>
    </div>
    <div class="d-flex flex-column gap-24">
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
                    <section class="gape-student-class-subject-group">
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
                        <div class="row gy-3">
                            <c:forEach var="lesson" items="${subjectGroup.lessons}">
                                <c:set var="classGroup" value="${classGroupById[lesson.classGroupId]}"/>
                                <div class="col-xxl-4 col-xl-6 col-md-6">
                                    <article class="gape-student-card gape-student-card--actionable px-18 py-18 h-100">
                                        <div class="d-flex align-items-start justify-content-between gap-12 mb-12">
                                            <span class="${lesson.typeBadgeClass} w-40 h-40 rounded-10 d-inline-flex align-items-center justify-content-center text-20">
                                                <i class="${lesson.typeIconClass}"></i>
                                            </span>
                                            <span class="${lesson.stateBadgeClass} px-10 py-5 border-neutral-30 border rounded-pill text-12">
                                                <c:out value="${lesson.stateLabel}"/>
                                            </span>
                                        </div>
                                        <h4 class="text-16 fw-semibold text-neutral-800 mb-6"><c:out value="${lesson.title}"/></h4>
                                        <p class="text-12 text-neutral-500 mb-12"><c:out value="${lesson.description}"/></p>
                                        <div class="gape-student-class-group-card__meta gape-student-class-group-card__meta--wide mb-14">
                                            <span>Date: <c:out value="${lesson.dateRangeLabel}"/></span>
                                            <span>Duration: <c:out value="${lesson.durationLabel}"/></span>
                                            <c:if test="${not empty classGroup}">
                                                <span title="<c:out value='${classGroup.contextTitle}'/>">
                                                    Group: <c:out value="${classGroup.code}"/>
                                                </span>
                                            </c:if>
                                            <c:if test="${lesson.hasRoom}">
                                                <span>Room: <c:out value="${lesson.physicalRoomCode}"/></span>
                                            </c:if>
                                            <c:if test="${(lesson.online or lesson.hybrid) and lesson.hasMeetingLink}">
                                                <span>Access: <c:out value="${lesson.providerLabel}"/></span>
                                            </c:if>
                                        </div>
                                        <div class="d-flex align-items-center gap-8 flex-wrap mb-14">
                                            <span class="bg-neutral-20 text-neutral-600 px-9 py-5 rounded-8 text-12"><c:out value="${lesson.typeLabel}"/></span>
                                            <span class="bg-neutral-20 text-neutral-600 px-9 py-5 rounded-8 text-12"><c:out value="${lesson.attendanceLabel}"/></span>
                                        </div>
                                        <c:if test="${(lesson.online or lesson.hybrid) and lesson.hasMeetingLink}">
                                            <div class="gape-student-card-actions">
                                                <a href="${pageContext.request.contextPath}/student/lessons/${lesson.id}/access" target="_blank" rel="noopener noreferrer" class="gape-student-card-icon-button gape-student-card-icon-button--meeting" aria-label="Open meeting" title="Open meeting">
                                                    <i class="ph ph-video-camera"></i>
                                                </a>
                                            </div>
                                        </c:if>
                                    </article>
                                </div>
                            </c:forEach>
                        </div>
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

<%@ include file="/WEB-INF/fragments/student-dashboard-end.jspf" %>
