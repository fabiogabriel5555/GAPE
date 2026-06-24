<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    request.setAttribute("activeMenu", "class-groups");
    request.setAttribute("pageTitle", "Class Group Enrollments");
    request.setAttribute("studentPageTitle", "My Class Groups");
    request.setAttribute("studentPageDescription", "Request class group enrollment and follow availability by course and subject.");
%>
<%@ include file="/WEB-INF/fragments/student-dashboard-start.jspf" %>

<section class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30">
    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-24">
        <div>
            <h6 class="text-20 fw-semibold text-neutral-800 mb-4">Class group enrollments</h6>
            <span class="text-14 text-neutral-500">Request a group and follow the current state of each course and subject.</span>
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
                    <c:set var="studentClassGroupSubjectKey" value="${group.courseId}-${group.subjectId}"/>
                    <c:set var="enrolledClassGroupCount" value="${0}"/>
                    <c:set var="availableClassGroupCount" value="${0}"/>
                    <c:forEach var="item" items="${group.classGroups}">
                        <c:if test="${item.enrolled}">
                            <c:set var="enrolledClassGroupCount" value="${enrolledClassGroupCount + 1}"/>
                        </c:if>
                        <c:if test="${not item.enrolled}">
                            <c:set var="availableClassGroupCount" value="${availableClassGroupCount + 1}"/>
                        </c:if>
                    </c:forEach>
                    <c:set var="hideAvailableClassGroups" value="${enrolledClassGroupCount gt 0 and availableClassGroupCount gt 0}"/>
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
                                        <c:out value="${group.contextHtml}" escapeXml="false"/> &middot; ${fn:length(group.classGroups)} class groups
                                    </span>
                                </div>
                            </div>
                            <a href="${pageContext.request.contextPath}/student/subjects/${group.courseId}/${group.subjectId}" class="gape-student-card-icon-button" aria-label="Open subject detail" title="Open subject detail">
                                <i class="ph ph-eye"></i>
                            </a>
                        </div>
                        <div class="row gy-3">
                            <c:forEach var="item" items="${group.classGroups}">
                                <c:if test="${item.enrolled}">
                                    <%@ include file="/WEB-INF/fragments/student-class-group-card.jspf" %>
                                </c:if>
                            </c:forEach>
                            <c:forEach var="item" items="${group.classGroups}">
                                <c:if test="${not item.enrolled}">
                                    <%@ include file="/WEB-INF/fragments/student-class-group-card.jspf" %>
                                </c:if>
                            </c:forEach>
                            <c:if test="${hideAvailableClassGroups}">
                                <div class="col-xxl-4 col-xl-6 col-md-6" data-student-unenrolled-summary="${studentClassGroupSubjectKey}">
                                    <article class="gape-student-card gape-student-class-group-card gape-student-class-group-card--summary px-18 py-18 h-100">
                                        <div class="gape-student-class-group-card__top mb-12">
                                            <span class="gape-student-class-group-card__icon"><i class="ph ph-stack-plus"></i></span>
                                            <div class="min-w-0">
                                                <h6 class="text-17 fw-semibold text-neutral-700 mb-5">
                                                    ${availableClassGroupCount} other class group<c:if test="${availableClassGroupCount ne 1}">s</c:if> available
                                                </h6>
                                                <span class="text-13 text-neutral-500" title="<c:out value='${group.contextTitle}'/>">
                                                    <c:out value="${group.contextHtml}" escapeXml="false"/>
                                                </span>
                                            </div>
                                        </div>
                                        <div class="gape-student-class-group-card__badges mb-14">
                                            <span class="bg-neutral-20 text-neutral-500 px-12 py-7 border-neutral-30 border rounded-pill text-12">
                                                More options
                                            </span>
                                        </div>
                                        <div class="gape-student-class-group-card__meta gape-student-class-group-card__meta--wide mb-16">
                                            <span>Enrolled: ${enrolledClassGroupCount}</span>
                                            <span>Available: ${availableClassGroupCount}</span>
                                            <span>Subject: <c:out value="${group.subjectName}"/></span>
                                            <span>Action: Review</span>
                                        </div>
                                        <div class="gape-student-class-group-card__actions">
                                            <button type="button"
                                                    class="gape-student-card-icon-button"
                                                    data-student-show-unenrolled="${studentClassGroupSubjectKey}"
                                                    aria-label="Show other class groups"
                                                    title="Show other class groups">
                                                <i class="ph ph-eye"></i>
                                            </button>
                                        </div>
                                    </article>
                                </div>
                            </c:if>
                        </div>
                    </section>
                </c:forEach>
            </section>
        </c:forEach>
        <c:if test="${empty studentClassGroups}">
            <div class="col-12">
                <div class="gape-student-empty text-center px-24 py-40">
                    <span class="gape-student-icon gape-student-soft-violet text-28 mb-16"><i class="ph ph-users-three"></i></span>
                    <h4 class="text-18 fw-semibold text-neutral-800 mb-8">No class groups available</h4>
                    <p class="text-14 text-neutral-500 mb-0">Class groups appear after you enroll in the corresponding course and subject.</p>
                </div>
            </div>
        </c:if>
    </div>
</section>

<script>
    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-student-show-unenrolled]').forEach(function (button) {
            button.addEventListener('click', function () {
                var groupKey = button.getAttribute('data-student-show-unenrolled');
                document.querySelectorAll('[data-student-unenrolled-summary="' + groupKey + '"]').forEach(function (summary) {
                    summary.classList.add('d-none');
                });
                document.querySelectorAll('[data-student-unenrolled-card="' + groupKey + '"]').forEach(function (card) {
                    card.classList.remove('d-none');
                });
            });
        });
    });
</script>
<%@ include file="/WEB-INF/fragments/student-dashboard-end.jspf" %>
