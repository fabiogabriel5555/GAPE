<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - ${calendarMode ? 'Calendar' : 'Lessons'}</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
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
                            <span class="text-14 text-neutral-500">Total Lessons</span>
                            <h2 class="text-32 fw-semibold text-neutral-700 mb-0">${lessonCount}</h2>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Scheduled</span>
                            <h2 class="text-32 fw-semibold text-main-600 mb-0">${scheduledLessonCount}</h2>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Active</span>
                            <h2 class="text-32 fw-semibold text-success-600 mb-0">${activeLessonCount}</h2>
                        </div>
                    </div>
                </div>

                <div class="bg-white rounded-10 px-24 py-24">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">${calendarMode ? 'Calendar' : 'Lessons Calendar'}</h2>
                            <span class="text-14 text-neutral-500">${calendarMode ? 'Lessons and room schedules ordered by date.' : 'Online, presential and hybrid lessons ordered by date.'}</span>
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
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
</body>
</html>
