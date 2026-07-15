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
<c:set var="coursePhotoUrl" value=""/>
<c:if test="${course.hasPhoto}">
    <c:set var="coursePhotoUrl" value="${pageContext.request.contextPath}/media/${course.photo}?v=${mediaCacheVersion}"/>
</c:if>
<%@ include file="/WEB-INF/fragments/student-dashboard-start.jspf" %>

<section class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30 mb-24">
    <div class="gape-student-course-summary">
        <div class="gape-student-course-visual-stack">
            <div class="gape-student-course-detail-thumb">
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
            <div class="gape-student-course-side-grid">
                <div class="gape-student-course-side-item">
                    <span class="text-12 text-neutral-500 d-block mb-6">Acronym</span>
                    <strong class="text-15 text-neutral-800"><c:out value="${course.acronym}"/></strong>
                </div>
                <div class="gape-student-course-side-item">
                    <span class="text-12 text-neutral-500 d-block mb-6">Unit</span>
                    <strong class="text-15 text-neutral-800"><c:out value="${course.organicUnitAcronym}"/></strong>
                </div>
                <div class="gape-student-course-side-item gape-student-course-side-item--wide">
                    <span class="text-12 text-neutral-500 d-block mb-6">Context</span>
                    <strong class="text-15 text-neutral-800"><c:out value="${course.courseManagementContextLabel}"/></strong>
                </div>
            </div>
        </div>

        <div class="min-w-0">
            <div class="d-flex align-items-center gap-10 flex-wrap mb-16">
                <span class="${course.enrollmentBadgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13">
                    <c:out value="${course.enrollmentStateLabel}"/>
                </span>
                <span class="${course.stateBadgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13">
                    <c:out value="${course.stateLabel}"/>
                </span>
            </div>
            <h2 class="text-24 fw-semibold text-neutral-800 mb-10"><c:out value="${course.name}"/></h2>
            <p class="text-14 text-neutral-500 mb-20"><c:out value="${course.description}"/></p>

            <div class="gape-student-course-metrics-grid">
                <div class="gape-student-course-metric">
                    <span class="text-13 text-neutral-500 d-block mb-8">Course</span>
                    <strong class="text-16 text-neutral-800"><c:out value="${course.typeLabel}"/> | <c:out value="${course.ectsLabel}"/></strong>
                </div>
                <div class="gape-student-course-metric">
                    <span class="text-13 text-neutral-500 d-block mb-8">Duration</span>
                    <strong class="text-16 text-neutral-800"><c:out value="${course.durationLabel}"/></strong>
                </div>
                <div class="gape-student-course-metric">
                    <span class="text-13 text-neutral-500 d-block mb-8">Subjects</span>
                    <strong class="text-16 text-neutral-800">${curricularSubjectCount} in the curricular structure</strong>
                </div>
                <div class="gape-student-course-metric">
                    <span class="text-13 text-neutral-500 d-block mb-8">Class groups</span>
                    <strong class="text-16 text-neutral-800">${classGroupCount} listed | ${activeClassGroupCount} active</strong>
                </div>
            </div>

            <div class="gape-student-course-actions">
                <a href="${pageContext.request.contextPath}/student/courses" class="border border-neutral-30 px-16 py-9 rounded-8 text-14 fw-semibold text-neutral-600 hover-bg-neutral-20 transition-03">
                    Back to Courses
                </a>
            </div>
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
        <c:forEach var="subject" items="${courseSubjects}">
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
                            <span class="bg-neutral-20 text-neutral-600 px-12 py-7 border-neutral-30 border rounded-pill text-12"><c:out value="${subject.mandatoryLabel}"/></span>
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
                            <span class="bg-neutral-20 text-neutral-600 px-9 py-5 rounded-8 text-12"><c:out value="${subject.mandatoryLabel}"/></span>
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
