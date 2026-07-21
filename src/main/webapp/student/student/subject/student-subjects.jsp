<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    request.setAttribute("activeMenu", "subjects");
    request.setAttribute("pageTitle", "Subjects");
    request.setAttribute("studentPageTitle", "Subjects");
    request.setAttribute("studentPageDescription", "See the subjects attached to your active course enrollments and their academic context.");
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
            <h6 class="text-20 fw-semibold text-neutral-800 mb-4">My Subjects</h6>
            <span class="text-14 text-neutral-500">Subjects are grouped by the active course occurrence that grants access to them.</span>
        </div>
        <span class="bg-success-50 text-success-600 px-14 py-8 rounded-pill text-13 fw-semibold">
            ${fn:length(subjectCourseGroups)} courses
        </span>
    </div>

    <div class="d-flex flex-column gap-24">
        <c:forEach var="group" items="${subjectCourseGroups}">
            <c:set var="course" value="${group.course}"/>
            <div class="gape-student-subject-course-group">
                <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-16">
                    <div class="d-flex align-items-center gap-12">
                        <span class="gape-student-icon gape-student-soft-blue text-22"><i class="ph ph-books"></i></span>
                        <div>
                            <h4 class="text-18 fw-semibold text-neutral-800 mb-2">
                                <a href="${pageContext.request.contextPath}/student/courses/${course.id}" class="text-neutral-800 hover-text-main-600">
                                    <c:out value="${course.name}"/>
                                </a>
                            </h4>
                            <span class="text-13 text-neutral-500"><c:out value="${fn:length(group.subjects)}"/> subjects in this course</span>
                        </div>
                    </div>
                    <a href="${pageContext.request.contextPath}/student/courses/${course.id}" class="gape-student-card-icon-button" aria-label="Open course detail" title="Open course detail">
                        <i class="ph ph-eye"></i>
                    </a>
                </div>

                <div class="row gy-4">
                    <c:set var="currentCurricularPeriod" value=""/>
                    <c:forEach var="curricularSubject" items="${group.subjects}">
                        <c:set var="subject" value="${curricularSubject.subject}"/>
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
                            <article id="subject-${course.id}-${subject.subjectId}"
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
                                    <h4 class="text-16 fw-semibold text-neutral-800 mb-6"><c:out value="${subject.subjectName}"/></h4>
                                    <p class="text-12 text-neutral-500 text-line-2 mb-12"><c:out value="${subject.subject.description}"/></p>
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
            </div>
        </c:forEach>
    </div>

    <c:if test="${empty subjectCourseGroups}">
        <div class="gape-student-empty text-center px-24 py-40">
            <span class="gape-student-icon gape-student-soft-green text-28 mb-16"><i class="ph ph-book-open-text"></i></span>
            <h4 class="text-18 fw-semibold text-neutral-800 mb-8">No subjects yet</h4>
            <p class="text-14 text-neutral-500 mb-0">Subjects appear when an authorized profile activates a course occurrence for your student account.</p>
        </div>
    </c:if>
</section>

<%@ include file="/WEB-INF/fragments/student-dashboard-end.jspf" %>
