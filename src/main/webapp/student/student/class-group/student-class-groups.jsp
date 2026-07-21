<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    request.setAttribute("activeMenu", "class-groups");
    request.setAttribute("pageTitle", "Class Groups");
    request.setAttribute("studentPageTitle", "Class Groups");
    request.setAttribute("studentPageDescription", "Review the class groups in which you are currently enrolled.");
%>
<%@ include file="/WEB-INF/fragments/student-dashboard-start.jspf" %>

<div class="row gy-4 mb-24">
    <div class="col-md-4">
        <div class="gape-student-stat-card h-100">
            <span class="text-14 text-neutral-500">Current course enrollments</span>
            <h3 class="text-32 fw-semibold text-neutral-800 mb-0">${fn:length(courseEnrollments)}</h3>
        </div>
    </div>
    <div class="col-md-4">
        <div class="gape-student-stat-card h-100">
            <span class="text-14 text-neutral-500">Curricular subjects</span>
            <h3 class="text-32 fw-semibold text-main-600 mb-0">${curricularSubjectCount}</h3>
        </div>
    </div>
    <div class="col-md-4">
        <div class="gape-student-stat-card h-100">
            <span class="text-14 text-neutral-500">Enrolled class groups</span>
            <h3 class="text-32 fw-semibold text-success-600 mb-0">${fn:length(studentClassGroups)}</h3>
        </div>
    </div>
</div>

<section class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30">
    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-24">
        <div>
            <h6 class="text-20 fw-semibold text-neutral-800 mb-4">My Class Groups</h6>
            <span class="text-14 text-neutral-500">Review the class groups in which you are currently enrolled.</span>
        </div>
        <span class="bg-main-50 text-main-600 px-14 py-8 rounded-pill text-13 fw-semibold">
            ${fn:length(studentClassGroups)} groups
        </span>
    </div>
    <div class="d-flex flex-column gap-24">
        <c:forEach var="courseGroup" items="${studentClassGroupCourseGroups}">
            <section class="gape-student-class-course-group">
                <div class="gape-student-class-course-group__header d-flex align-items-center justify-content-between gap-16 flex-wrap">
                    <div class="d-flex align-items-center gap-12 min-w-0">
                        <span class="gape-student-icon gape-student-soft-blue text-22"><i class="ph ph-books"></i></span>
                        <div class="min-w-0">
                            <h4 class="text-18 fw-semibold text-neutral-800 mb-4"><c:out value="${courseGroup.courseName}"/></h4>
                            <span class="text-13 text-neutral-500">${fn:length(courseGroup.subjectGroups)} subjects &middot; ${courseGroup.classGroupCount} class groups</span>
                        </div>
                    </div>
                    <a href="${pageContext.request.contextPath}/student/courses/${courseGroup.courseId}" class="gape-student-card-icon-button" aria-label="Open course detail" title="Open course detail">
                        <i class="ph ph-eye"></i>
                    </a>
                </div>
                <c:forEach var="group" items="${courseGroup.subjectGroups}">
                    <section class="gape-student-class-subject-group">
                        <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-14">
                            <div class="d-flex align-items-center gap-12 min-w-0">
                                <span class="gape-student-icon gape-student-soft-violet text-22"><i class="ph ph-book-open-text"></i></span>
                                <div class="min-w-0">
                                    <h5 class="text-16 fw-semibold text-neutral-800 mb-4">
                                        <a href="${pageContext.request.contextPath}/student/subjects/${group.courseId}/${group.subjectId}" class="hover-text-main-600">
                                            <c:out value="${group.subjectName}"/>
                                        </a>
                                    </h5>
                                    <span class="text-13 text-neutral-500" title="<c:out value='${group.contextTitle}'/>">
                                        <c:out value="${group.contextHtml}" escapeXml="false"/> &middot; ${fn:length(group.classGroups)} enrolled class groups
                                    </span>
                                </div>
                            </div>
                            <a href="${pageContext.request.contextPath}/student/subjects/${group.courseId}/${group.subjectId}" class="gape-student-card-icon-button" aria-label="Open subject detail" title="Open subject detail">
                                <i class="ph ph-eye"></i>
                            </a>
                        </div>
                        <div class="row gy-3">
                            <c:forEach var="item" items="${group.classGroups}">
                                <%@ include file="/WEB-INF/fragments/student-class-group-card.jspf" %>
                            </c:forEach>
                        </div>
                    </section>
                </c:forEach>
            </section>
        </c:forEach>
        <c:if test="${empty studentClassGroups}">
            <div class="col-12">
                <div class="gape-student-empty text-center px-24 py-40">
                    <span class="gape-student-icon gape-student-soft-violet text-28 mb-16"><i class="ph ph-users-three"></i></span>
                    <h4 class="text-18 fw-semibold text-neutral-800 mb-8">No enrolled class groups yet</h4>
                    <p class="text-14 text-neutral-500 mb-0">Your enrolled class groups will appear here.</p>
                </div>
            </div>
        </c:if>
    </div>
</section>

<%@ include file="/WEB-INF/fragments/student-dashboard-end.jspf" %>
