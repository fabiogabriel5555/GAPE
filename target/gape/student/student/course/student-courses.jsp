<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    request.setAttribute("activeMenu", "courses");
    request.setAttribute("pageTitle", "Courses");
    request.setAttribute("studentPageTitle", "Courses");
    request.setAttribute("studentPageDescription", "Follow your enrolled courses as learning paths, with subjects and class groups connected to each course.");
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
            <span class="text-14 text-neutral-500">Class groups visible</span>
            <h3 class="text-32 fw-semibold text-success-600 mb-0">${fn:length(studentClassGroups)}</h3>
        </div>
    </div>
</div>

<section class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30 mb-24">
    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-24">
        <div>
            <h3 class="text-20 fw-semibold text-neutral-800 mb-4">My Courses</h3>
            <span class="text-14 text-neutral-500">Courses are shown as cards, with their visible student context and next actions.</span>
        </div>
    </div>
    <div class="row gy-4">
        <c:forEach var="enrollment" items="${courseEnrollments}">
            <c:set var="course" value="${enrollment.course}"/>
            <c:set var="courseOccurrence" value="${courseOccurrencesByCourseId[course.id]}"/>
            <c:set var="coursePhotoUrl" value=""/>
            <c:if test="${course.hasPhoto}">
                <c:set var="coursePhotoUrl" value="${pageContext.request.contextPath}/media/${course.photo}?v=${mediaCacheVersion}"/>
            </c:if>
            <div class="col-xxl-3 col-xl-4 col-md-6">
                <article class="gape-student-card gape-student-structure-card h-100 overflow-hidden">
                    <div class="gape-student-structure-card__thumb m-10 mb-0">
                        <c:choose>
                            <c:when test="${course.hasPhoto}">
                                <img src="${coursePhotoUrl}" alt="" onerror="this.classList.add('d-none');this.nextElementSibling.classList.remove('d-none');">
                                <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--table d-none" aria-label="No course photo">
                                    <i class="ph ph-image"></i>
                                </span>
                            </c:when>
                            <c:otherwise>
                                <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--table" aria-label="No course photo">
                                    <i class="ph ph-image"></i>
                                </span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <div class="gape-student-structure-card__body px-16 py-16">
                        <div class="d-flex align-items-start justify-content-between gap-12 mb-12">
                            <span class="gape-student-icon gape-student-soft-blue text-20"><i class="ph ph-books"></i></span>
                            <span class="${enrollment.badgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12">
                                <c:out value="${enrollment.stateLabel}"/>
                            </span>
                        </div>
                        <h4 class="text-16 fw-semibold text-neutral-800 mb-6"><c:out value="${course.name}"/></h4>
                        <p class="text-12 text-neutral-500 text-line-2 mb-12"><c:out value="${course.description}"/></p>
                        <div class="d-flex align-items-center gap-8 flex-wrap mb-14">
                            <span class="bg-neutral-20 text-neutral-600 px-9 py-5 rounded-8 text-12"><c:out value="${course.ectsLabel}"/></span>
                            <span class="bg-neutral-20 text-neutral-600 px-9 py-5 rounded-8 text-12"><c:out value="${course.subjectCountLabel}"/></span>
                            <c:if test="${not empty courseOccurrence}">
                                <span class="bg-main-50 text-main-600 px-9 py-5 rounded-8 text-12" title="Current course occurrence">
                                    Occurrence: <c:out value="${courseOccurrence.label}"/>
                                </span>
                            </c:if>
                        </div>
                        <div class="gape-student-card-actions">
                            <a href="${pageContext.request.contextPath}/student/courses/${course.id}" class="gape-student-card-icon-button" aria-label="Open course" title="Open course">
                                <i class="ph ph-eye"></i>
                            </a>
                        </div>
                    </div>
                </article>
            </div>
        </c:forEach>
        <c:if test="${empty courseEnrollments}">
            <div class="col-12">
                <div class="gape-student-empty text-center px-24 py-40">
                    <span class="gape-student-icon gape-student-soft-blue text-28 mb-16"><i class="ph ph-books"></i></span>
                    <h4 class="text-18 fw-semibold text-neutral-800 mb-8">No class courses yet</h4>
                    <p class="text-14 text-neutral-500 mb-0">Course enrollment is managed by authorized profiles.</p>
                </div>
            </div>
        </c:if>
    </div>
</section>

<%@ include file="/WEB-INF/fragments/student-dashboard-end.jspf" %>
