<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    request.setAttribute("activeMenu", "subjects");
    request.setAttribute("pageTitle", "Subject Details");
    request.setAttribute("studentPageTitle", "Subject Details");
    request.setAttribute("studentPageDescription", "Review curricular subject details and class groups in your active course occurrence.");
%>
<%@ include file="/WEB-INF/fragments/student-dashboard-start.jspf" %>

<section class="gape-student-course-visual-stack gape-student-class-group-detail-hero px-24 py-24 mb-24">
    <div class="d-flex align-items-start justify-content-between gap-18 flex-wrap mb-22">
        <div class="d-flex align-items-start gap-14 min-w-0">
            <span class="gape-student-class-group-card__icon text-26"><i class="ph ph-book-open-text"></i></span>
            <div class="min-w-0">
                <div class="d-flex align-items-center gap-8 flex-wrap mb-8">
                    <h2 class="text-24 fw-semibold text-neutral-800 mb-0"><c:out value="${subject.subjectName}"/></h2>
                    <span class="${subject.subject.stateBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12">
                        <c:out value="${subject.subject.stateLabel}"/>
                    </span>
                </div>
                <p class="text-14 text-neutral-500 mb-0"><c:out value="${subject.subject.description}"/></p>
            </div>
        </div>
    </div>

    <div class="gape-student-class-group-metrics">
        <div class="gape-student-class-group-metric">
            <span class="text-12 text-neutral-500 d-block mb-4">Course</span>
            <a href="${pageContext.request.contextPath}/student/courses/${course.id}" class="text-14 fw-semibold text-neutral-800 hover-text-main-600"><c:out value="${course.name}"/></a>
        </div>
        <div class="gape-student-class-group-metric">
            <span class="text-12 text-neutral-500 d-block mb-4">ECTS</span>
            <span class="text-14 fw-semibold text-neutral-800"><c:out value="${subject.subjectEctsLabel}"/></span>
        </div>
        <div class="gape-student-class-group-metric">
            <span class="text-12 text-neutral-500 d-block mb-4">Type</span>
            <span class="text-14 fw-semibold text-neutral-800"><c:out value="${subject.mandatoryLabel}"/></span>
        </div>
        <div class="gape-student-class-group-metric">
            <span class="text-12 text-neutral-500 d-block mb-4">Class groups</span>
            <span class="text-14 fw-semibold text-neutral-800">${fn:length(subjectClassGroups)} visible</span>
        </div>
    </div>
</section>

<section class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30">
    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-24">
        <div>
            <h3 class="text-20 fw-semibold text-neutral-800 mb-4">Class groups</h3>
            <span class="text-14 text-neutral-500">Groups connected to this subject in <c:out value="${course.name}"/>.</span>
        </div>
        <span class="bg-main-50 text-main-600 px-14 py-8 rounded-pill text-13 fw-semibold">
            ${fn:length(subjectClassGroups)} groups
        </span>
    </div>

    <c:set var="studentSubjectClassGroupKey" value="${course.id}-${subject.subjectId}"/>
    <c:set var="enrolledSubjectClassGroupCount" value="${0}"/>
    <c:set var="unenrolledSubjectClassGroupCount" value="${0}"/>
    <c:forEach var="item" items="${subjectClassGroups}">
        <c:choose>
            <%-- Active enrollments and pending requests stay visible. A left
                 group no longer grants a current place, so it is aggregated. --%>
            <c:when test="${item.currentOrPendingEnrollment}">
                <c:set var="enrolledSubjectClassGroupCount" value="${enrolledSubjectClassGroupCount + 1}"/>
            </c:when>
            <c:otherwise>
                <c:set var="unenrolledSubjectClassGroupCount" value="${unenrolledSubjectClassGroupCount + 1}"/>
            </c:otherwise>
        </c:choose>
    </c:forEach>
    <c:set var="hideUnenrolledSubjectClassGroups" value="${enrolledSubjectClassGroupCount gt 0 and unenrolledSubjectClassGroupCount gt 0}"/>

    <div class="row gy-3">
        <c:forEach var="item" items="${subjectClassGroups}">
            <c:if test="${item.currentOrPendingEnrollment}">
                <%@ include file="/WEB-INF/fragments/student-subject-class-group-card.jspf" %>
            </c:if>
        </c:forEach>
        <c:forEach var="item" items="${subjectClassGroups}">
            <c:if test="${not item.currentOrPendingEnrollment}">
                <%@ include file="/WEB-INF/fragments/student-subject-class-group-card.jspf" %>
            </c:if>
        </c:forEach>
        <c:if test="${hideUnenrolledSubjectClassGroups}">
            <div class="col-xxl-3 col-xl-4 col-md-6" data-student-subject-unenrolled-summary="${studentSubjectClassGroupKey}">
                <article class="gape-student-card gape-student-class-group-card gape-student-class-group-card--summary px-14 py-14 h-100">
                    <div class="gape-student-class-group-card__top mb-10">
                        <span class="gape-student-class-group-card__icon gape-student-class-group-card__icon--compact"><i class="ph ph-stack-plus"></i></span>
                        <div class="min-w-0">
                            <h6 class="text-14 fw-semibold text-neutral-700 mb-4">
                                ${unenrolledSubjectClassGroupCount} other class group<c:if test="${unenrolledSubjectClassGroupCount ne 1}">s</c:if> without enrollment
                            </h6>
                            <span class="text-12 text-neutral-500">Show the remaining groups for this subject.</span>
                        </div>
                    </div>
                    <div class="gape-student-class-group-card__badges mb-12">
                        <span class="bg-neutral-20 text-neutral-500 px-12 py-7 border-neutral-30 border rounded-pill text-12">Not enrolled</span>
                    </div>
                    <div class="gape-student-class-group-card__meta mb-12">
                        <span>Enrolled: ${enrolledSubjectClassGroupCount}</span>
                        <span>Other groups: ${unenrolledSubjectClassGroupCount}</span>
                    </div>
                    <div class="gape-student-class-group-card__actions">
                        <button type="button"
                                class="gape-student-card-icon-button gape-student-card-icon-button--muted"
                                data-student-show-subject-unenrolled="${studentSubjectClassGroupKey}"
                                aria-label="Show class groups without enrollment"
                                title="Show class groups without enrollment">
                            <i class="ph ph-eye"></i>
                        </button>
                    </div>
                </article>
            </div>
        </c:if>
        <c:if test="${empty subjectClassGroups}">
            <div class="col-12">
                <div class="gape-student-empty text-center px-24 py-40">
                    <span class="gape-student-icon gape-student-soft-violet text-28 mb-16"><i class="ph ph-users-three"></i></span>
                    <h4 class="text-18 fw-semibold text-neutral-800 mb-8">No class groups yet</h4>
                    <p class="text-14 text-neutral-500 mb-0">Class groups appear here when they are created for this subject.</p>
                </div>
            </div>
        </c:if>
    </div>
</section>

<script>
    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-student-show-subject-unenrolled]').forEach(function (button) {
            button.addEventListener('click', function () {
                var subjectKey = button.getAttribute('data-student-show-subject-unenrolled');
                document.querySelectorAll('[data-student-subject-unenrolled-summary="' + subjectKey + '"]').forEach(function (summary) {
                    summary.classList.add('d-none');
                });
                document.querySelectorAll('[data-student-subject-unenrolled-card="' + subjectKey + '"]').forEach(function (card) {
                    card.classList.remove('d-none');
                });
            });
        });
    });
</script>

<%@ include file="/WEB-INF/fragments/student-dashboard-end.jspf" %>
