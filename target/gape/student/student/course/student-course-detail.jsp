<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    request.setAttribute("activeMenu", "courses");
    if (request.getAttribute("course") == null) {
        response.sendRedirect(request.getContextPath() + "/student/courses");
        return;
    }
%>
<%@ include file="/WEB-INF/fragments/student-dashboard-start.jspf" %>

<section class="gape-student-course-visual-stack gape-student-class-group-detail-hero px-24 py-24 mb-24">
    <div class="d-flex align-items-start justify-content-between gap-18 flex-wrap mb-22">
        <div class="d-flex align-items-start gap-14 min-w-0">
            <span class="gape-student-class-group-card__icon text-26"><i class="ph ph-books"></i></span>
            <div class="min-w-0">
                <div class="d-flex align-items-center gap-8 flex-wrap mb-8">
                    <h2 class="text-24 fw-semibold text-neutral-800 mb-0"><c:out value="${course.name}"/></h2>
                    <span class="${course.enrollmentBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12">
                        <c:out value="${course.enrollmentStateLabel}"/>
                    </span>
                    <span class="${course.stateBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12">
                        <c:out value="${course.stateLabel}"/>
                    </span>
                </div>
                <p class="text-14 text-neutral-500 mb-0"><c:out value="${course.description}"/></p>
            </div>
        </div>
    </div>

    <div class="gape-student-class-group-metrics">
        <div class="gape-student-class-group-metric">
            <span class="text-12 text-neutral-500 d-block mb-4">Course</span>
            <span class="text-14 fw-semibold text-neutral-800"><c:out value="${course.typeLabel}"/> | <c:out value="${course.ectsLabel}"/></span>
        </div>
        <div class="gape-student-class-group-metric">
            <span class="text-12 text-neutral-500 d-block mb-4">Subjects</span>
            <span class="text-14 fw-semibold text-neutral-800">${curricularSubjectCount} in the curricular structure</span>
        </div>
        <div class="gape-student-class-group-metric">
            <span class="text-12 text-neutral-500 d-block mb-4">Class groups</span>
            <span class="text-14 fw-semibold text-neutral-800">${activeClassGroupCount} enrolled | ${classGroupCount} listed</span>
        </div>
        <div class="gape-student-class-group-metric">
            <span class="text-12 text-neutral-500 d-block mb-4">Current enrollment period</span>
            <span class="text-14 fw-semibold text-neutral-800" data-gape-datetime-display>
                <c:out value="${course.enrollmentStartDate}"/> to <c:out value="${course.enrollmentEndDate}"/>
            </span>
        </div>
    </div>
</section>

<section class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30">
    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-24">
        <div>
            <h3 class="text-20 fw-semibold text-neutral-800 mb-4">Course structure</h3>
            <span class="text-14 text-neutral-500">Subjects connected to this course.</span>
        </div>
        <span class="bg-main-50 text-main-600 px-14 py-8 rounded-pill text-13 fw-semibold">
            ${fn:length(courseSubjects)} subjects
        </span>
    </div>

    <div class="row gy-4">
        <c:set var="currentCurricularPeriod" value=""/>
        <c:forEach var="subject" items="${courseSubjects}">
            <c:if test="${currentCurricularPeriod ne subject.curricularPositionLabel}">
                <c:set var="currentCurricularPeriod" value="${subject.curricularPositionLabel}"/>
                <div class="col-12">
                    <div class="gape-student-subject-period-divider" role="separator" aria-label="<c:out value='${subject.curricularPositionLabel}'/>">
                        <span><c:out value="${subject.curricularPositionLabel}"/></span>
                    </div>
                </div>
            </c:if>
            <c:set var="subjectPhotoUrl" value=""/>
            <c:if test="${subject.subject.hasPhoto}">
                <c:set var="subjectPhotoUrl" value="${pageContext.request.contextPath}/media/${subject.subject.photo}?v=${mediaCacheVersion}"/>
            </c:if>
            <div class="col-xxl-3 col-xl-4 col-md-6">
                <article id="course-subject-${subject.subjectId}"
                         class="gape-student-card gape-student-structure-card h-100 overflow-hidden">
                    <div class="gape-student-structure-card__thumb m-10 mb-0">
                        <c:choose>
                            <c:when test="${subject.subject.hasPhoto}">
                                <img src="${subjectPhotoUrl}" alt="" onerror="this.classList.add('d-none');this.nextElementSibling.classList.remove('d-none');">
                                <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--table d-none" aria-label="No subject photo">
                                    <i class="ph ph-image"></i>
                                </span>
                            </c:when>
                            <c:otherwise>
                                <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--table" aria-label="No subject photo">
                                    <i class="ph ph-image"></i>
                                </span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <div class="gape-student-structure-card__body px-16 py-16">
                        <div class="d-flex align-items-start justify-content-between gap-12 mb-12">
                            <span class="gape-student-icon gape-student-soft-green text-20"><i class="ph ph-book-open-text"></i></span>
                            <span class="${subject.mandatory ? 'gape-student-mandatory-badge' : 'gape-student-optional-badge'} px-12 py-7 border rounded-pill text-12"><c:out value="${subject.mandatoryLabel}"/></span>
                        </div>
                        <h4 class="text-16 fw-semibold text-neutral-800 mb-6">
                            <a href="${pageContext.request.contextPath}/student/subjects/${course.id}/${subject.subjectId}" class="text-neutral-800 hover-text-main-600">
                                <c:out value="${subject.subjectName}"/>
                            </a>
                        </h4>
                        <p class="text-12 text-neutral-500 mb-12"><c:out value="${subject.subject.description}"/></p>
                        <div class="d-flex align-items-center gap-8 flex-wrap mb-14">
                            <span class="bg-neutral-20 text-neutral-600 px-9 py-5 rounded-8 text-12"><c:out value="${subject.subjectAcronym}"/></span>
                            <span class="bg-neutral-20 text-neutral-600 px-9 py-5 rounded-8 text-12"><c:out value="${subject.subjectEctsLabel}"/></span>
                            <span class="bg-neutral-20 text-neutral-600 px-9 py-5 rounded-8 text-12"><c:out value="${subject.curricularPositionLabel}"/></span>
                            <span class="${subject.mandatory ? 'gape-student-mandatory-badge' : 'gape-student-optional-badge'} px-9 py-5 border rounded-8 text-12"><c:out value="${subject.mandatoryLabel}"/></span>
                        </div>
                        <div class="gape-student-card-actions">
                            <a href="${pageContext.request.contextPath}/student/subjects/${course.id}/${subject.subjectId}" class="gape-student-card-icon-button" aria-label="Open subject" title="Open subject">
                                <i class="ph ph-eye"></i>
                            </a>
                        </div>
                    </div>
                </article>
            </div>
        </c:forEach>
    </div>

    <c:if test="${empty courseSubjects}">
        <div class="gape-student-empty text-center px-24 py-32">
            <span class="gape-student-icon gape-student-soft-green text-28 mb-16"><i class="ph ph-book-open-text"></i></span>
            <h4 class="text-18 fw-semibold text-neutral-800 mb-8">No subjects yet</h4>
            <p class="text-14 text-neutral-500 mb-0">This course does not have active subjects available to students yet.</p>
        </div>
    </c:if>
</section>

<%@ include file="/WEB-INF/fragments/student-dashboard-end.jspf" %>
