<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    request.setAttribute("activeMenu", "calendar");
    request.setAttribute("pageTitle", "Learning Schedule");
    request.setAttribute("studentPageTitle", "Study Schedule");
    request.setAttribute("studentPageDescription", "A student timeline with schedule events and lessons from your active class group enrollments.");
%>
<%@ include file="/WEB-INF/fragments/student-dashboard-start.jspf" %>

<section class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30">
    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-24">
        <div>
            <h6 class="text-20 fw-semibold text-neutral-800 mb-4">Learning schedule</h6>
            <span class="text-14 text-neutral-500">A student timeline for events and lessons in your active class groups.</span>
        </div>
        <a href="${pageContext.request.contextPath}/student/lessons" class="gape-student-card-icon-button" aria-label="Open lessons" title="Open lessons">
            <i class="ph ph-list-bullets"></i>
        </a>
    </div>
    <div class="d-flex flex-column gap-14">
        <c:forEach var="item" items="${calendarItems}">
            <c:choose>
                <c:when test="${item.eventItem}">
                    <c:set var="event" value="${item.event}"/>
                    <article class="gape-student-line-card gape-student-timeline-item px-18 py-18 d-flex align-items-start justify-content-between gap-16 flex-wrap">
                        <div class="d-flex align-items-start gap-14 min-w-0">
                            <span class="${event.typeBadgeClass} w-48 h-48 rounded-12 d-inline-flex align-items-center justify-content-center text-24 flex-shrink-0">
                                <i class="${event.typeIconClass}"></i>
                            </span>
                            <div class="min-w-0">
                                <h4 class="text-17 fw-semibold text-neutral-800 mb-0"><c:out value="${event.title}"/></h4>
                                <div class="d-flex align-items-center gap-10 flex-wrap mt-8">
                                    <span class="text-13 text-neutral-500"><i class="ph ph-clock me-6"></i><c:out value="${event.startsAt}"/> - <c:out value="${event.endsAt}"/></span>
                                    <span class="text-13 text-neutral-500"><i class="ph ph-timer me-6"></i><c:out value="${event.durationLabel}"/></span>
                                    <span class="text-13 text-neutral-500"><i class="ph ph-users-three me-6"></i><c:out value="${event.classGroupLabel}"/></span>
                                    <span class="text-13 text-neutral-500"><i class="ph ph-bell-ringing me-6"></i><c:out value="${event.reminderLabel}"/></span>
                                </div>
                                <p class="text-13 text-neutral-500 mt-8 mb-0"><c:out value="${event.description}"/></p>
                            </div>
                        </div>
                        <div class="d-flex align-items-center gap-10 flex-wrap">
                            <span class="${event.stateBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12">
                                <c:out value="${event.stateLabel}"/>
                            </span>
                        </div>
                    </article>
                </c:when>
                <c:otherwise>
                    <c:set var="lesson" value="${item.lesson}"/>
                    <c:set var="classGroup" value="${classGroupById[lesson.classGroupId]}"/>
                    <c:set var="block" value="${contentBlockById[lesson.contentBlockId]}"/>
                    <article class="gape-student-line-card gape-student-timeline-item px-18 py-18 d-flex align-items-start justify-content-between gap-16 flex-wrap">
                        <div class="d-flex align-items-start gap-14 min-w-0">
                            <span class="${lesson.typeBadgeClass} w-48 h-48 rounded-12 d-inline-flex align-items-center justify-content-center text-24 flex-shrink-0">
                                <i class="${lesson.typeIconClass}"></i>
                            </span>
                            <div class="min-w-0">
                                <h4 class="text-17 fw-semibold text-neutral-800 mb-0"><c:out value="${lesson.title}"/></h4>
                                <div class="d-flex align-items-center gap-10 flex-wrap mt-8">
                                    <span class="text-13 text-neutral-500"><i class="ph ph-clock me-6"></i><c:out value="${lesson.startsAt}"/> - <c:out value="${lesson.endsAt}"/></span>
                                    <span class="text-13 text-neutral-500"><i class="ph ph-timer me-6"></i><c:out value="${lesson.durationLabel}"/></span>
                                    <span class="text-13 text-neutral-500"><i class="ph ph-stack me-6"></i><c:out value="${block.name}"/></span>
                                    <span class="text-13 text-neutral-500"><i class="ph ph-user-check me-6"></i><c:out value="${lesson.attendanceLabel}"/></span>
                                    <c:if test="${lesson.hasRoom}">
                                        <span class="text-13 text-neutral-500"><i class="ph ph-map-pin me-6"></i><c:out value="${lesson.physicalRoomCode}"/></span>
                                    </c:if>
                                    <c:if test="${(lesson.online or lesson.hybrid) and lesson.hasMeetingLink}">
                                        <span class="text-13 text-neutral-500"><i class="ph ph-video-camera me-6"></i><c:out value="${lesson.providerLabel}"/></span>
                                    </c:if>
                                </div>
                                <c:if test="${not empty classGroup}">
                                    <div class="text-13 text-neutral-500 mt-8" title="<c:out value='${classGroup.contextTitle}'/>">
                                        <i class="ph ph-users-three me-6"></i><c:out value="${classGroup.contextHtml}" escapeXml="false"/>
                                    </div>
                                </c:if>
                            </div>
                        </div>
                        <div class="d-flex align-items-center gap-10 flex-wrap">
                            <span class="${lesson.stateBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12">
                                <c:out value="${lesson.stateLabel}"/>
                            </span>
                            <c:if test="${(lesson.online or lesson.hybrid) and lesson.hasMeetingLink}">
                                <a href="${pageContext.request.contextPath}/student/lessons/${lesson.id}/access" target="_blank" rel="noopener noreferrer" class="gape-student-card-icon-button gape-student-card-icon-button--meeting" aria-label="Open meeting" title="Open meeting">
                                    <i class="ph ph-video-camera"></i>
                                </a>
                            </c:if>
                        </div>
                    </article>
                </c:otherwise>
            </c:choose>
        </c:forEach>
        <c:if test="${empty calendarItems}">
            <div class="gape-student-empty text-center px-24 py-40">
                <span class="gape-student-icon gape-student-soft-amber text-28 mb-16"><i class="ph ph-calendar-dots"></i></span>
                <h4 class="text-18 fw-semibold text-neutral-800 mb-8">No calendar items yet</h4>
                <p class="text-14 text-neutral-500 mb-0">Your student calendar is filled automatically by events and enrolled class group lessons.</p>
            </div>
        </c:if>
    </div>
</section>

<%@ include file="/WEB-INF/fragments/student-dashboard-end.jspf" %>
