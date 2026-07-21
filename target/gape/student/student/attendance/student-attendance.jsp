<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    request.setAttribute("activeMenu", "attendance");
    request.setAttribute("pageTitle", "Enrollments & Certificates");
    request.setAttribute("studentPageTitle", "Enrollments & Certificates");
    request.setAttribute("studentPageDescription", "Your active enrollments, published grades and certificates.");
%>
<%@ include file="/WEB-INF/fragments/student-dashboard-start.jspf" %>

<div class="gape-student-mode-grid mb-24" data-student-tabs role="tablist" aria-label="Enrollment, grade and certificate sections">
    <button type="button" class="gape-student-mode-card is-active" data-student-tab="enrollments" role="tab" aria-selected="true" aria-controls="enrollments">
        <span class="gape-student-mode-icon bg-main-50 text-main-600 text-24"><i class="ph ph-student"></i></span>
        <span class="min-w-0"><span class="text-20 fw-semibold text-neutral-800 d-block mb-8">Enrollments</span><span class="text-13 text-main-600 fw-semibold">${fn:length(studentActiveCourseEnrollments)} course enrollments</span></span>
    </button>
    <button type="button" class="gape-student-mode-card" data-student-tab="grades" role="tab" aria-selected="false" aria-controls="grades">
        <span class="gape-student-mode-icon bg-warning-50 text-warning-600 text-24"><i class="ph ph-seal-check"></i></span>
        <span class="min-w-0"><span class="text-20 fw-semibold text-neutral-800 d-block mb-8">Grades</span><span class="text-13 text-main-600 fw-semibold">${gradeRecordCount} published grades</span></span>
    </button>
    <button type="button" class="gape-student-mode-card position-relative" data-student-tab="certificates" role="tab" aria-selected="false" aria-controls="certificates">
        <span class="gape-student-mode-icon bg-info-50 text-info-600 text-24"><i class="ph ph-certificate"></i></span>
        <span class="min-w-0"><span class="text-20 fw-semibold text-neutral-800 d-block mb-8">Certificates</span><span class="text-13 text-main-600 fw-semibold">${certificateCount} certificates</span></span>
        <c:if test="${studentCertificateEventCount gt 0}"><span class="gape-student-event-marker gape-student-event-marker--floating" title="${studentCertificateEventCount} published certificate event(s)">${studentCertificateEventCount}</span></c:if>
    </button>
</div>

<section id="enrollments" class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30" data-student-tab-panel="enrollments" role="tabpanel">
    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-24"><div><h6 class="text-20 fw-semibold text-neutral-800 mb-4">Enrollments</h6><span class="text-14 text-neutral-500">Course, class-group and assessment enrollments.</span></div></div>
    <div class="gape-student-mode-grid mb-24" data-enrollment-kind-tabs>
    <button type="button" class="gape-student-mode-card gape-student-mode-card--secondary is-active" data-enrollment-kind="courses"><span class="gape-student-mode-icon bg-main-50 text-main-600 text-22"><i class="ph ph-graduation-cap"></i></span><span><span class="text-17 fw-semibold d-block">Courses</span><span class="text-13 text-neutral-500">${fn:length(studentActiveCourseEnrollments)} enrollments</span></span></button>
        <button type="button" class="gape-student-mode-card gape-student-mode-card--secondary" data-enrollment-kind="classes"><span class="gape-student-mode-icon bg-info-50 text-info-600 text-22"><i class="ph ph-users-three"></i></span><span><span class="text-17 fw-semibold d-block">Class groups</span><span class="text-13 text-neutral-500">${fn:length(studentActiveClassEnrollments)} enrollments</span></span></button>
        <button type="button" class="gape-student-mode-card gape-student-mode-card--secondary" data-enrollment-kind="assessments"><span class="gape-student-mode-icon bg-warning-50 text-warning-600 text-22"><i class="ph ph-seal-question"></i></span><span><span class="text-17 fw-semibold d-block">Assessments</span><span class="text-13 text-neutral-500">${fn:length(studentAssessmentEnrollments)} enrollments</span></span></button>
    </div>
    <%-- studentCourseEnrollments remains the complete source list; this panel
         renders only its non-archived partition. --%>
    <div data-enrollment-kind-panel="courses" class="row gy-4 gape-student-card-grid--aligned">
        <c:forEach var="enrollment" items="${studentActiveCourseEnrollments}"><div class="col-xxl-3 col-xl-4 col-md-6"><article class="gape-student-card gape-student-class-group-card px-14 py-14 h-100"><div class="gape-student-class-group-card__top mb-10"><span class="gape-student-class-group-card__icon gape-student-class-group-card__icon--compact"><i class="ph ph-graduation-cap"></i></span><div class="min-w-0"><h6 class="text-14 fw-semibold text-neutral-800 mb-4 text-line-2"><c:out value="${enrollment.courseName}"/></h6><span class="text-12 text-neutral-500"><c:out value="${enrollment.occurrenceLabel}"/></span></div></div><div class="gape-student-class-group-card__badges mb-12"><span class="${enrollment.stateBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12"><c:out value="${enrollment.stateLabel}"/></span></div><div class="gape-student-class-group-card__meta mb-12"><span>From: <c:out value="${enrollment.startDate}"/></span><span>To: <c:out value="${enrollment.endDate}"/></span><span><c:out value="${enrollment.courseEctsLabel}"/></span></div><div class="gape-student-card-actions"><button type="button" class="gape-student-card-icon-button" data-bs-toggle="modal" data-bs-target="#studentCourseEnrollment${enrollment.courseId}_${enrollment.courseOccurrenceId}" aria-label="View enrollment details" title="View enrollment details"><i class="ph ph-eye"></i></button></div></article></div><div class="modal fade" id="studentCourseEnrollment${enrollment.courseId}_${enrollment.courseOccurrenceId}" tabindex="-1" aria-hidden="true"><div class="modal-dialog modal-dialog-centered"><div class="modal-content rounded-10 border-0"><div class="modal-header"><h5 class="modal-title text-18 fw-semibold">Course enrollment details</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div><div class="modal-body"><div class="gape-student-class-group-card__meta"><span>Course: <c:out value="${enrollment.courseName}"/></span><span>Occurrence: <c:out value="${enrollment.occurrenceLabel}"/></span><span>State: <c:out value="${enrollment.stateLabel}"/></span><span>Dates: <c:out value="${enrollment.startDate}"/> – <c:out value="${enrollment.endDate}"/></span></div></div></div></div></div></c:forEach>
    </div>
    <div data-enrollment-kind-panel="classes" class="d-none d-flex flex-column gap-20">
        <c:forEach var="courseGroup" items="${studentClassEnrollmentGroups}"><section class="gape-student-class-course-group"><h4 class="text-17 fw-semibold text-neutral-800 mb-12"><c:out value="${courseGroup.courseName}"/></h4><c:forEach var="subjectGroup" items="${courseGroup.subjects}"><h5 class="text-15 fw-semibold text-neutral-700 mb-10"><c:out value="${subjectGroup.subjectName}"/></h5><div class="row gy-3 mb-16"><c:forEach var="enrollment" items="${subjectGroup.classEnrollments}"><div class="col-xxl-3 col-xl-4 col-md-6"><article class="gape-student-card gape-student-class-group-card px-14 py-14 h-100"><div class="gape-student-class-group-card__top mb-10"><span class="gape-student-class-group-card__icon gape-student-class-group-card__icon--compact"><i class="ph ph-users-three"></i></span><div class="min-w-0"><h6 class="text-14 fw-semibold text-neutral-800 mb-4 text-line-2"><c:out value="${enrollment.code}"/></h6><span class="text-12 text-neutral-500"><c:out value="${enrollment.occurrenceLabel}"/></span></div></div><div class="gape-student-class-group-card__badges mb-12"><span class="${enrollment.stateBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12"><c:out value="${enrollment.stateLabel}"/></span></div><div class="gape-student-class-group-card__meta mb-12"><span>Course: <c:out value="${enrollment.courseName}"/></span><span>Subject: <c:out value="${enrollment.subjectName}"/></span><span>Occurrence: <c:out value="${enrollment.occurrenceLabel}"/></span></div><div class="gape-student-card-actions"><button type="button" class="gape-student-card-icon-button" data-bs-toggle="modal" data-bs-target="#studentClassEnrollment${enrollment.id}" aria-label="View class enrollment details"><i class="ph ph-eye"></i></button></div></article></div><div class="modal fade" id="studentClassEnrollment${enrollment.id}" tabindex="-1" aria-hidden="true"><div class="modal-dialog modal-dialog-centered"><div class="modal-content rounded-10 border-0"><div class="modal-header"><h5 class="modal-title text-18 fw-semibold">Class-group enrollment details</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div><div class="modal-body"><div class="gape-student-class-group-card__meta"><span>Course: <c:out value="${enrollment.courseName}"/></span><span>Subject: <c:out value="${enrollment.subjectName}"/></span><span>Class: <c:out value="${enrollment.code}"/></span><span>Occurrence: <c:out value="${enrollment.occurrenceLabel}"/></span><span>State: <c:out value="${enrollment.stateLabel}"/></span></div></div></div></div></div></c:forEach></div></c:forEach></section></c:forEach>
    </div>
    <div data-enrollment-kind-panel="assessments" data-student-assessment-enrollment-legacy class="d-none">
        <c:forEach var="courseGroup" items="${studentAssessmentEnrollmentGroups}"><section class="gape-student-class-course-group"><h4 class="text-17 fw-semibold text-neutral-800 mb-12"><c:out value="${courseGroup.courseName}"/></h4><c:forEach var="subjectGroup" items="${courseGroup.subjects}"><h5 class="text-15 fw-semibold text-neutral-700 mb-10"><c:out value="${subjectGroup.subjectName}"/></h5><div class="row gy-3 mb-16"><c:forEach var="enrollment" items="${subjectGroup.assessmentEnrollments}"><div class="col-xxl-3 col-xl-4 col-md-6"><article class="gape-student-card gape-student-class-group-card px-14 py-14 h-100"><div class="gape-student-class-group-card__top mb-10"><span class="gape-student-class-group-card__icon gape-student-class-group-card__icon--compact"><i class="ph ph-seal-question"></i></span><div class="min-w-0"><h6 class="text-14 fw-semibold text-neutral-800 mb-4 text-line-2"><c:out value="${enrollment.title}"/></h6><span class="text-12 text-neutral-500"><c:out value="${enrollment.classGroupCode}"/> &middot; <c:out value="${enrollment.occurrenceLabel}"/></span></div></div><div class="gape-student-class-group-card__badges mb-12"><span class="${enrollment.stateBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12"><c:out value="${enrollment.stateLabel}"/></span></div><div class="gape-student-class-group-card__meta mb-12"><span>Course: <c:out value="${enrollment.courseName}"/></span><span>Subject: <c:out value="${enrollment.subjectName}"/></span><span>Class: <c:out value="${enrollment.classGroupCode}"/></span></div><div class="gape-student-card-actions"><button type="button" class="gape-student-card-icon-button" data-bs-toggle="modal" data-bs-target="#studentAssessmentEnrollment${enrollment.id}" aria-label="View assessment enrollment details"><i class="ph ph-eye"></i></button></div></article></div><div class="modal fade" id="studentAssessmentEnrollment${enrollment.id}" tabindex="-1" aria-hidden="true"><div class="modal-dialog modal-dialog-centered"><div class="modal-content rounded-10 border-0"><div class="modal-header"><h5 class="modal-title text-18 fw-semibold">Assessment enrollment details</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div><div class="modal-body"><div class="gape-student-class-group-card__meta"><span>Assessment: <c:out value="${enrollment.title}"/></span><span>Course: <c:out value="${enrollment.courseName}"/></span><span>Subject: <c:out value="${enrollment.subjectName}"/></span><span>Class: <c:out value="${enrollment.classGroupCode}"/></span><span>Occurrence: <c:out value="${enrollment.occurrenceLabel}"/></span><span>State: <c:out value="${enrollment.stateLabel}"/></span></div></div></div></div></div></c:forEach></div></c:forEach></section></c:forEach>
    </div>
    <div data-enrollment-kind-panel="assessments" data-student-assessment-enrollment-modern class="d-none d-flex flex-column gap-24">
        <c:forEach var="courseGroup" items="${studentAssessmentEnrollmentGroups}">
            <section class="gape-student-class-course-group">
                <div class="gape-student-class-course-group__header d-flex align-items-center justify-content-between gap-16 flex-wrap">
                    <div class="d-flex align-items-center gap-12 min-w-0">
                        <span class="gape-student-icon gape-student-soft-blue text-22"><i class="ph ph-books"></i></span>
                        <div class="min-w-0"><h4 class="text-18 fw-semibold text-neutral-800 mb-4"><c:out value="${courseGroup.courseName}"/></h4><span class="text-13 text-neutral-500">${fn:length(courseGroup.subjects)} subjects</span></div>
                    </div>
                </div>
                <c:forEach var="subjectGroup" items="${courseGroup.subjects}">
                    <section class="gape-student-class-subject-group">
                        <div class="d-flex align-items-center gap-12 mb-14"><span class="gape-student-icon gape-student-soft-violet text-22"><i class="ph ph-book-open-text"></i></span><h5 class="text-16 fw-semibold text-neutral-800 mb-0"><c:out value="${subjectGroup.subjectName}"/></h5></div>
                        <c:forEach var="occurrenceGroup" items="${subjectGroup.assessmentOccurrences}">
                            <div class="gape-student-class-occurrence-group border border-neutral-30 rounded-10 px-16 py-14 mb-16">
                                <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap mb-12"><div class="d-flex align-items-center gap-10 min-w-0"><span class="gape-student-icon gape-student-soft-blue text-18"><i class="ph ph-calendar-blank"></i></span><div class="min-w-0"><span class="text-14 fw-semibold text-neutral-800 d-block"><c:out value="${occurrenceGroup.label}"/></span><span class="text-12 text-neutral-500">${occurrenceGroup.enrollmentCount} assessment enrollments</span></div></div></div>
                                 <c:forEach var="classGroup" items="${occurrenceGroup.classGroups}">
                                     <div class="gape-student-assessment-class-group border border-neutral-30 rounded-10 px-14 py-12 mb-12">
                                         <div class="d-flex align-items-center gap-10 mb-10"><span class="gape-student-icon gape-student-soft-blue text-18"><i class="ph ph-users-three"></i></span><div class="min-w-0"><span class="text-13 fw-semibold text-neutral-800 d-block text-line-1"><c:out value="${classGroup.code}"/></span><span class="text-12 text-neutral-500">${classGroup.enrollmentCount} assessment enrollments</span></div></div>
                                         <div class="row gy-4">
                                             <c:forEach var="enrollment" items="${classGroup.assessmentEnrollments}">
                                                 <c:set var="studentAssessmentEnrollmentModalId" value="studentAssessmentEnrollment${enrollment.modalKey}"/>
                                                 <%@ include file="/WEB-INF/fragments/student-assessment-enrollment-card.jspf" %>
                                             </c:forEach>
                                         </div>
                                     </div>
                                 </c:forEach>
                            </div>
                        </c:forEach>
                    </section>
                </c:forEach>
            </section>
        </c:forEach>
        <c:if test="${empty studentAssessmentEnrollments}"><div class="gape-student-empty py-24 px-20 text-center text-14 text-neutral-500">No active assessment enrollments.</div></c:if>
    </div>
    <div data-enrollment-kind-panel="classes" data-student-class-enrollment-modern class="d-none d-flex flex-column gap-24">
        <c:forEach var="courseGroup" items="${studentClassEnrollmentGroups}">
            <section class="gape-student-class-course-group">
                <div class="gape-student-class-course-group__header d-flex align-items-center justify-content-between gap-16 flex-wrap">
                    <div class="d-flex align-items-center gap-12 min-w-0">
                        <span class="gape-student-icon gape-student-soft-blue text-22"><i class="ph ph-books"></i></span>
                        <div class="min-w-0"><h4 class="text-18 fw-semibold text-neutral-800 mb-4"><c:out value="${courseGroup.courseName}"/></h4><span class="text-13 text-neutral-500">${fn:length(courseGroup.subjects)} subjects</span></div>
                    </div>
                </div>
                <c:forEach var="subjectGroup" items="${courseGroup.subjects}">
                    <section class="gape-student-class-subject-group">
                        <div class="d-flex align-items-center gap-12 mb-14"><span class="gape-student-icon gape-student-soft-violet text-22"><i class="ph ph-book-open-text"></i></span><h5 class="text-16 fw-semibold text-neutral-800 mb-0"><c:out value="${subjectGroup.subjectName}"/></h5></div>
                        <c:forEach var="occurrenceGroup" items="${subjectGroup.classOccurrences}">
                            <div class="gape-student-class-occurrence-group border border-neutral-30 rounded-10 px-16 py-14 mb-16">
                                <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap mb-12"><div class="d-flex align-items-center gap-10 min-w-0"><span class="gape-student-icon gape-student-soft-blue text-18"><i class="ph ph-calendar-blank"></i></span><div class="min-w-0"><span class="text-14 fw-semibold text-neutral-800 d-block"><c:out value="${occurrenceGroup.label}"/></span><span class="text-12 text-neutral-500">${occurrenceGroup.enrollmentCount} class enrollments</span></div></div></div>
                                <div class="row gy-4">
                                    <c:forEach var="enrollment" items="${occurrenceGroup.classEnrollments}">
                                        <c:set var="studentClassEnrollmentModalId" value="studentActiveClassEnrollment${enrollment.id}"/>
                                        <%@ include file="/WEB-INF/fragments/student-class-enrollment-card.jspf" %>
                                    </c:forEach>
                                </div>
                            </div>
                        </c:forEach>
                    </section>
                </c:forEach>
            </section>
        </c:forEach>
        <c:if test="${empty studentActiveClassEnrollments}"><div class="gape-student-empty py-24 px-20 text-center text-14 text-neutral-500">No active class-group enrollments.</div></c:if>
    </div>
    <c:if test="${not empty studentCompletedCourseEnrollments or not empty studentCompletedClassEnrollments or not empty studentCompletedAssessmentEnrollments}">
        <section class="gape-learning-management-completed-wrapper gape-student-completed-enrollments mt-20" data-student-completed-enrollments data-student-completed-archive>
            <div class="gape-learning-management-completed-divider">Completed Enrollments</div>
            <article class="gape-learning-management-completed-node border rounded-8 px-18 py-16 bg-white">
                <div class="gape-deferred-management-archive-row">
                    <div class="d-flex align-items-center gap-12 min-w-0">
                        <span class="gape-learning-management-group-icon bg-danger-50 text-danger-600"><i class="ph ph-archive" aria-hidden="true"></i></span>
                        <div class="min-w-0">
                            <span class="fw-medium text-14 text-neutral-700 d-block">Completed Enrollments</span>
                            <span class="gape-node-meta text-12"><span data-student-completed-summary>Past course enrollments</span></span>
                        </div>
                    </div>
                    <div class="gape-deferred-management-archive-spacer"></div>
                    <div><span class="cd-element-count" data-student-completed-count>${studentCompletedEnrollmentCount}</span></div>
                    <div><span class="bg-neutral-20 text-neutral-600 px-14 py-6 border-neutral-30 border rounded-pill text-13">Completed</span></div>
                    <div class="d-flex justify-content-end"><button type="button" class="gape-tree-toggle text-20 text-neutral-500 hover-text-main-600" data-student-completed-toggle aria-expanded="false" title="Show completed enrollments" aria-label="Show completed enrollments"><i class="ph ph-caret-down" aria-hidden="true"></i></button></div>
                </div>
                <div class="gape-learning-management-completed-panel d-none" data-student-completed-panel>
                    <div class="row gy-4 gape-student-card-grid--aligned" data-student-completed-course-cards data-student-completed-course-count="${fn:length(studentCompletedCourseEnrollments)}">
                        <c:forEach var="enrollment" items="${studentCompletedCourseEnrollments}">
                            <c:set var="studentCourseEnrollmentModalId" value="studentCompletedCourseEnrollment${enrollment.courseId}_${enrollment.courseOccurrenceId}"/>
                            <%@ include file="/WEB-INF/fragments/student-course-enrollment-card.jspf" %>
                        </c:forEach>
                    </div>
                    <c:if test="${not empty studentCompletedClassEnrollmentGroups}">
                        <div class="col-12 mt-16 d-none" data-student-completed-class-cards data-student-completed-class-count="${fn:length(studentCompletedClassEnrollments)}">
                            <c:forEach var="courseGroup" items="${studentCompletedClassEnrollmentGroups}">
                                <section class="gape-student-class-course-group mb-16">
                                    <div class="gape-student-class-course-group__header d-flex align-items-center justify-content-between gap-16 flex-wrap">
                                        <div class="d-flex align-items-center gap-12 min-w-0"><span class="gape-student-icon gape-student-soft-blue text-22"><i class="ph ph-books"></i></span><div class="min-w-0"><h4 class="text-18 fw-semibold text-neutral-800 mb-4"><c:out value="${courseGroup.courseName}"/></h4><span class="text-13 text-neutral-500">${fn:length(courseGroup.subjects)} subjects</span></div></div>
                                    </div>
                                    <c:forEach var="subjectGroup" items="${courseGroup.subjects}">
                                        <div class="gape-student-class-subject-group mb-12"><div class="d-flex align-items-center gap-10 mb-10"><span class="gape-student-icon gape-student-soft-violet text-18"><i class="ph ph-book-open-text"></i></span><span class="text-14 fw-semibold text-neutral-800"><c:out value="${subjectGroup.subjectName}"/></span></div>
                                            <c:forEach var="occurrenceGroup" items="${subjectGroup.classOccurrences}">
                                                <div class="gape-student-class-occurrence-group">
                                                <div class="border border-neutral-30 rounded-10 px-14 py-12 mb-12"><div class="d-flex align-items-center gap-10 mb-10"><i class="ph ph-calendar-blank text-main-600"></i><span class="text-13 fw-semibold text-neutral-800"><c:out value="${occurrenceGroup.label}"/></span></div><div class="row gy-4"><c:forEach var="enrollment" items="${occurrenceGroup.classEnrollments}"><c:set var="studentClassEnrollmentModalId" value="studentCompletedClassEnrollment${enrollment.id}"/><%@ include file="/WEB-INF/fragments/student-class-enrollment-card.jspf" %></c:forEach></div></div>
                                                </div>
                                            </c:forEach>
                                        </div>
                                    </c:forEach>
                                </section>
                            </c:forEach>
                        </div>
                    </c:if>
                    <c:if test="${not empty studentCompletedAssessmentEnrollmentGroups}">
                         <div class="col-12 mt-16 d-none" data-student-completed-assessment-cards data-student-completed-assessment-count="${fn:length(studentCompletedAssessmentEnrollments)}">
                            <c:forEach var="courseGroup" items="${studentCompletedAssessmentEnrollmentGroups}">
                                <section class="gape-student-class-course-group mb-16">
                                    <div class="gape-student-class-course-group__header d-flex align-items-center justify-content-between gap-16 flex-wrap">
                                        <div class="d-flex align-items-center gap-12 min-w-0"><span class="gape-student-icon gape-student-soft-blue text-22"><i class="ph ph-books"></i></span><div class="min-w-0"><h4 class="text-18 fw-semibold text-neutral-800 mb-4"><c:out value="${courseGroup.courseName}"/></h4><span class="text-13 text-neutral-500">${fn:length(courseGroup.subjects)} subjects</span></div></div>
                                    </div>
                                    <c:forEach var="subjectGroup" items="${courseGroup.subjects}">
                                        <div class="gape-student-class-subject-group mb-12"><div class="d-flex align-items-center gap-10 mb-10"><span class="gape-student-icon gape-student-soft-violet text-18"><i class="ph ph-book-open-text"></i></span><span class="text-14 fw-semibold text-neutral-800"><c:out value="${subjectGroup.subjectName}"/></span></div>
                                            <c:forEach var="occurrenceGroup" items="${subjectGroup.assessmentOccurrences}">
                                                <div class="gape-student-class-occurrence-group">
                                                     <div class="border border-neutral-30 rounded-10 px-14 py-12 mb-12"><div class="d-flex align-items-center gap-10 mb-10"><i class="ph ph-calendar-blank text-main-600"></i><span class="text-13 fw-semibold text-neutral-800"><c:out value="${occurrenceGroup.label}"/></span></div><c:forEach var="classGroup" items="${occurrenceGroup.classGroups}"><div class="gape-student-assessment-class-group border border-neutral-30 rounded-10 px-14 py-12 mb-12"><div class="d-flex align-items-center gap-10 mb-10"><span class="gape-student-icon gape-student-soft-blue text-18"><i class="ph ph-users-three"></i></span><div class="min-w-0"><span class="text-13 fw-semibold text-neutral-800 d-block text-line-1"><c:out value="${classGroup.code}"/></span><span class="text-12 text-neutral-500">${classGroup.enrollmentCount} assessment enrollments</span></div></div><div class="row gy-4"><c:forEach var="enrollment" items="${classGroup.assessmentEnrollments}"><c:set var="studentAssessmentEnrollmentModalId" value="studentCompletedAssessmentEnrollment${enrollment.modalKey}"/><%@ include file="/WEB-INF/fragments/student-assessment-enrollment-card.jspf" %></c:forEach></div></div></c:forEach></div>
                                                </div>
                                            </c:forEach>
                                        </div>
                                    </c:forEach>
                                </section>
                            </c:forEach>
                        </div>
                    </c:if>
                    <div class="gape-student-empty py-24 px-20 text-center text-14 text-neutral-500 d-none" data-student-completed-empty>No completed enrollments for this guide.</div>
                    <div class="gape-learning-management-completed-content d-none d-flex flex-column gap-12">
                        <div class="aac-tree">
                            <c:forEach var="courseGroup" items="${studentCompletedEnrollmentGroups}" varStatus="courseLoop">
                                <article class="aac-tree-node aac-tree-node--course gape-student-completed-course">
                                    <div class="aac-tree-row">
                                        <div class="aac-tree-main">
                                            <span class="aac-tree-icon bg-main-50 text-main-600"><i class="ph ph-graduation-cap"></i></span>
                                            <div class="min-w-0">
                                                <span class="aac-tree-title"><c:out value="${courseGroup.courseName}"/></span>
                                                <span class="aac-tree-meta">${courseGroup.enrollmentCount} completed enrollments</span>
                                            </div>
                                        </div>
                                        <div class="aac-tree-count"><span class="aac-tree-count-value">${courseGroup.enrollmentCount}</span></div>
                                        <div class="aac-tree-status"><span class="bg-neutral-20 text-neutral-600 px-12 py-6 border-neutral-30 border rounded-pill text-12">Completed</span></div>
                                        <div class="aac-tree-actions"><button type="button" class="aac-icon-button bg-main-50 text-main-600" title="Hide occurrences" aria-label="Hide completed course occurrences" data-bs-toggle="collapse" data-bs-target="#studentCompletedCourse${courseLoop.index}" aria-expanded="true"><i class="ph ph-caret-up"></i></button></div>
                                    </div>
                                    <div class="collapse show aac-tree-children" id="studentCompletedCourse${courseLoop.index}">
                                        <c:forEach var="occurrenceGroup" items="${courseGroup.occurrences}" varStatus="occurrenceLoop">
                                            <div class="gape-student-completed-occurrence">
                                                <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap">
                                                    <div class="d-flex align-items-center gap-10 min-w-0"><span class="aac-tree-icon bg-info-50 text-info-600"><i class="ph ph-calendar-blank"></i></span><div class="min-w-0"><span class="fw-medium text-14 text-neutral-700 d-block"><c:out value="${occurrenceGroup.label}"/></span><span class="text-12 text-neutral-500">Occurrence</span></div></div>
                                                    <span class="text-12 text-info-600">Newest occurrence first</span>
                                                </div>
                                                <c:forEach var="subjectGroup" items="${occurrenceGroup.subjects}">
                                                    <div class="gape-student-completed-subject">
                                                        <div class="gape-student-completed-subject__heading"><i class="ph ph-book-open-text"></i><span><c:choose><c:when test="${subjectGroup.subjectName eq '-'}">Course enrollment</c:when><c:otherwise><c:out value="${subjectGroup.subjectName}"/></c:otherwise></c:choose></span><span class="text-12 text-neutral-500">${subjectGroup.enrollmentCount} enrollment(s)</span></div>
                                                        <c:forEach var="item" items="${subjectGroup.enrollments}">
                                                            <div class="gape-student-completed-item">
                                                                <span class="min-w-0"><span class="text-12 text-neutral-500 d-block"><c:out value="${item.type}"/></span><strong class="text-14 text-neutral-700"><c:out value="${item.title}"/></strong><c:if test="${item.className ne '-'}"> <span class="text-13 text-neutral-500">&middot; <c:out value="${item.className}"/></span></c:if><c:if test="${item.periodLabel ne '-'}"><span class="text-12 text-neutral-500 d-block gape-student-enrollment-period">Period: <c:out value="${item.periodLabel}"/></span></c:if></span>
                                                                <span class="bg-info-50 text-info-600 px-10 py-5 rounded-pill text-12"><c:out value="${item.stateLabel}"/></span>
                                                            </div>
                                                        </c:forEach>
                                                    </div>
                                                </c:forEach>
                                            </div>
                                        </c:forEach>
                                    </div>
                                </article>
                            </c:forEach>
                        </div>
                    </div>
                </div>
            </article>
        </section>
    </c:if>
    <c:if test="${empty studentCompletedCourseEnrollments and empty studentCompletedClassEnrollments and empty studentCompletedAssessmentEnrollments}"><div class="mt-20 gape-student-empty py-24 px-20 text-center text-14 text-neutral-500">No completed enrollments.</div></c:if>
</section>

<section id="grades" class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30" data-student-tab-panel="grades" role="tabpanel" hidden>
    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-24"><div><h6 class="text-20 fw-semibold text-neutral-800 mb-4">Grades</h6><span class="text-14 text-neutral-500">Published grade sheets and final results.</span></div></div>
    <div class="gape-student-mode-grid gape-student-mode-grid--grades mb-24" data-grade-kind-tabs><button type="button" class="gape-student-mode-card is-active" data-grade-kind="subjects"><span class="gape-student-mode-icon bg-main-50 text-main-600 text-22"><i class="ph ph-book-open"></i></span><span><span class="text-17 fw-semibold d-block">Subjects</span><span class="text-13 text-neutral-500">Subject grade sheets by course and occurrence</span></span></button><button type="button" class="gape-student-mode-card" data-grade-kind="classes"><span class="gape-student-mode-icon bg-info-50 text-info-600 text-22"><i class="ph ph-users-three"></i></span><span><span class="text-17 fw-semibold d-block">Class groups</span><span class="text-13 text-neutral-500">Class grade sheets by course, subject and occurrence</span></span></button></div>
    <div data-grade-kind-panel="subjects" class="d-flex flex-column gap-16">
        <c:forEach var="courseGroup" items="${gradeSheetGroups}"><c:if test="${not empty courseGroup.activeOccurrences}"><section class="gape-student-class-course-group gape-student-grade-course-group"><div class="gape-student-class-course-group__header d-flex align-items-center justify-content-between gap-16 flex-wrap"><div class="d-flex align-items-center gap-12 min-w-0"><span class="gape-student-icon gape-student-soft-blue text-22"><i class="ph ph-books"></i></span><div class="min-w-0"><h4 class="text-18 fw-semibold text-neutral-800 mb-4"><c:out value="${courseGroup.courseLabel}"/></h4><span class="text-13 text-neutral-500">${fn:length(courseGroup.activeOccurrences)} active occurrences</span></div></div></div><c:forEach var="occurrenceGroup" items="${courseGroup.activeOccurrences}"><div class="gape-student-grade-occurrence-group"><div class="gape-student-grade-occurrence-group__header d-flex align-items-center gap-10"><span class="gape-student-icon gape-student-soft-blue text-18"><i class="ph ph-calendar-blank"></i></span><div class="min-w-0"><h5 class="text-15 fw-semibold text-neutral-700 mb-2"><c:out value="${occurrenceGroup.occurrenceLabel}"/></h5><span class="text-12 text-neutral-500"><c:out value="${occurrenceGroup.occurrenceDateRangeLabel}"/> &middot; ${occurrenceGroup.subjectCount} subjects</span></div></div><div class="row gy-3"><c:forEach var="subjectGroup" items="${occurrenceGroup.subjects}"><div class="col-xxl-3 col-xl-4 col-md-6"><article class="gape-student-card gape-student-class-group-card px-14 py-14 h-100"><div class="gape-student-class-group-card__top mb-10"><span class="gape-student-class-group-card__icon gape-student-class-group-card__icon--compact"><i class="ph ph-seal-check"></i></span><div class="min-w-0"><h6 class="text-14 fw-semibold text-neutral-800 mb-4 text-line-1"><c:out value="${subjectGroup.subjectLabel}"/></h6><span class="text-12 text-neutral-500"><c:out value="${subjectGroup.occurrenceLabel}"/></span></div></div><div class="gape-student-class-group-card__badges mb-12"><span class="${subjectGroup.stateBadgeClass} px-12 py-7 rounded-pill text-12"><c:out value="${subjectGroup.stateLabel}"/></span></div><div class="gape-student-class-group-card__meta mb-12"><span>${subjectGroup.sheetCount} class sheets</span><span><c:out value="${subjectGroup.periodLabel}"/></span></div><div class="gape-student-card-actions"><button type="button" class="gape-student-card-icon-button" data-bs-toggle="modal" data-bs-target="#studentSubjectGrade${courseGroup.courseId}_${subjectGroup.subjectId}_${subjectGroup.occurrenceId}" aria-label="View subject grade details"><i class="ph ph-eye"></i></button></div></article></div><div class="modal fade" id="studentSubjectGrade${courseGroup.courseId}_${subjectGroup.subjectId}_${subjectGroup.occurrenceId}" tabindex="-1" aria-hidden="true"><div class="modal-dialog modal-dialog-centered"><div class="modal-content rounded-10 border-0"><div class="modal-header"><h5 class="modal-title text-18 fw-semibold">Subject grade details</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div><div class="modal-body"><div class="gape-student-class-group-card__meta"><span>Course: <c:out value="${courseGroup.courseLabel}"/></span><span>Subject: <c:out value="${subjectGroup.subjectLabel}"/></span><span>Occurrence: <c:out value="${subjectGroup.occurrenceLabel}"/></span><span>State: <c:out value="${subjectGroup.stateLabel}"/></span><span>Sheets: ${subjectGroup.sheetCount}</span></div></div></div></div></div></c:forEach></div></div></c:forEach></section></c:if></c:forEach>
        <div class="gape-student-completed-tool" data-student-completed-subject-occurrences><div class="d-flex align-items-center justify-content-between gap-12 flex-wrap"><div><h5 class="text-16 fw-semibold text-neutral-800 mb-2">Completed Subject Occurrences</h5><span class="text-13 text-neutral-500">Subject grade sheets from occurrences that have already ended.</span></div><button type="button" class="gape-student-card-icon-button" data-grade-completed-subject-toggle aria-expanded="false" aria-label="Show completed subject occurrences" title="Show completed subject occurrences"><i class="ph ph-caret-down"></i></button></div><div class="d-none mt-16" data-grade-completed-subject-panel><c:forEach var="courseGroup" items="${gradeSheetGroups}"><c:forEach var="occurrenceGroup" items="${courseGroup.completedOccurrences}"><section class="gape-student-class-course-group gape-student-grade-course-group mb-16"><div class="gape-student-grade-occurrence-group__header d-flex align-items-center gap-10"><span class="gape-student-icon gape-student-soft-blue text-18"><i class="ph ph-books"></i></span><div class="min-w-0"><h5 class="text-15 fw-semibold text-neutral-700 mb-2"><c:out value="${courseGroup.courseLabel}"/> &middot; <c:out value="${occurrenceGroup.occurrenceLabel}"/></h5><span class="text-12 text-neutral-500"><c:out value="${occurrenceGroup.occurrenceDateRangeLabel}"/></span></div></div><div class="row gy-3"><c:forEach var="subjectGroup" items="${occurrenceGroup.subjects}"><div class="col-xxl-3 col-xl-4 col-md-6"><article class="gape-student-card gape-student-class-group-card px-14 py-14 h-100"><div class="gape-student-class-group-card__top mb-10"><span class="gape-student-class-group-card__icon gape-student-class-group-card__icon--compact"><i class="ph ph-seal-check"></i></span><div class="min-w-0"><h6 class="text-14 fw-semibold text-neutral-800 mb-4 text-line-1"><c:out value="${subjectGroup.subjectLabel}"/></h6><span class="text-12 text-neutral-500"><c:out value="${subjectGroup.occurrenceLabel}"/></span></div></div><div class="gape-student-class-group-card__badges mb-12"><span class="${subjectGroup.stateBadgeClass} px-12 py-7 rounded-pill text-12"><c:out value="${subjectGroup.stateLabel}"/></span></div><div class="gape-student-class-group-card__meta mb-12"><span>${subjectGroup.sheetCount} class sheets</span><span><c:out value="${subjectGroup.periodLabel}"/></span></div><div class="gape-student-card-actions"><button type="button" class="gape-student-card-icon-button" data-bs-toggle="modal" data-bs-target="#studentCompletedSubjectGrade${courseGroup.courseId}_${subjectGroup.subjectId}_${subjectGroup.occurrenceId}" aria-label="View subject grade details"><i class="ph ph-eye"></i></button></div></article></div><div class="modal fade" id="studentCompletedSubjectGrade${courseGroup.courseId}_${subjectGroup.subjectId}_${subjectGroup.occurrenceId}" tabindex="-1" aria-hidden="true"><div class="modal-dialog modal-dialog-centered"><div class="modal-content rounded-10 border-0"><div class="modal-header"><h5 class="modal-title text-18 fw-semibold">Subject grade details</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div><div class="modal-body"><div class="gape-student-class-group-card__meta"><span>Course: <c:out value="${courseGroup.courseLabel}"/></span><span>Subject: <c:out value="${subjectGroup.subjectLabel}"/></span><span>Occurrence: <c:out value="${subjectGroup.occurrenceLabel}"/></span><span>State: <c:out value="${subjectGroup.stateLabel}"/></span><span>Sheets: ${subjectGroup.sheetCount}</span></div></div></div></div></div></c:forEach></div></section></c:forEach></c:forEach></div></div>
    </div>
    <div data-grade-kind-panel="classes" class="d-none d-flex flex-column gap-16">
        <c:forEach var="courseGroup" items="${gradeSheetGroups}"><c:if test="${not empty courseGroup.activeOccurrences}"><section class="gape-student-class-course-group gape-student-grade-course-group"><div class="gape-student-class-course-group__header d-flex align-items-center justify-content-between gap-16 flex-wrap"><div class="d-flex align-items-center gap-12 min-w-0"><span class="gape-student-icon gape-student-soft-blue text-22"><i class="ph ph-books"></i></span><div class="min-w-0"><h4 class="text-18 fw-semibold text-neutral-800 mb-4"><c:out value="${courseGroup.courseLabel}"/></h4><span class="text-13 text-neutral-500">${fn:length(courseGroup.activeOccurrences)} active occurrences</span></div></div></div><c:forEach var="occurrenceGroup" items="${courseGroup.activeOccurrences}"><div class="gape-student-grade-occurrence-group"><div class="gape-student-grade-occurrence-group__header d-flex align-items-center gap-10"><span class="gape-student-icon gape-student-soft-blue text-18"><i class="ph ph-calendar-blank"></i></span><div class="min-w-0"><h5 class="text-15 fw-semibold text-neutral-700 mb-2"><c:out value="${occurrenceGroup.occurrenceLabel}"/></h5><span class="text-12 text-neutral-500"><c:out value="${occurrenceGroup.occurrenceDateRangeLabel}"/></span></div></div><c:forEach var="subjectGroup" items="${occurrenceGroup.subjects}"><h5 class="text-14 fw-semibold text-neutral-700 mt-14 mb-10"><c:out value="${subjectGroup.subjectLabel}"/></h5><div class="row gy-3"><c:forEach var="sheet" items="${subjectGroup.activeClassGroupSheets}"><div class="col-xxl-3 col-xl-4 col-md-6"><article class="gape-student-card gape-student-class-group-card px-14 py-14 h-100"><div class="gape-student-class-group-card__top mb-10"><span class="gape-student-class-group-card__icon gape-student-class-group-card__icon--compact"><i class="ph ph-users-three"></i></span><div class="min-w-0"><h6 class="text-14 fw-semibold text-neutral-800 mb-4 text-line-2"><c:out value="${sheet.classGroupCode}"/></h6><span class="text-12 text-neutral-500"><c:out value="${sheet.title}"/></span></div></div><div class="gape-student-class-group-card__badges mb-12"><span class="${sheet.stateBadgeClass} px-12 py-7 rounded-pill text-12"><c:out value="${sheet.stateLabel}"/></span></div><div class="gape-student-class-group-card__meta mb-12"><span><c:out value="${sheet.recordCount}"/> records</span><span><c:out value="${sheet.typeLabel}"/></span></div><div class="gape-student-card-actions"><button type="button" class="gape-student-card-icon-button" data-bs-toggle="modal" data-bs-target="#studentClassGrade${sheet.id}" aria-label="View class grade details"><i class="ph ph-eye"></i></button></div></article></div><div class="modal fade" id="studentClassGrade${sheet.id}" tabindex="-1" aria-hidden="true"><div class="modal-dialog modal-dialog-centered"><div class="modal-content rounded-10 border-0"><div class="modal-header"><h5 class="modal-title text-18 fw-semibold">Class-group grade details</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div><div class="modal-body"><div class="gape-student-class-group-card__meta"><span>Class: <c:out value="${sheet.classGroupCode}"/></span><span>Title: <c:out value="${sheet.title}"/></span><span>Type: <c:out value="${sheet.typeLabel}"/></span><span>State: <c:out value="${sheet.stateLabel}"/></span><span>Records: ${sheet.recordCount}</span></div></div></div></div></div></c:forEach></div></c:forEach></div></c:forEach></section></c:if></c:forEach>
        <div class="gape-student-completed-tool" data-student-completed-grades><div class="d-flex align-items-center justify-content-between gap-12 flex-wrap"><div><h5 class="text-16 fw-semibold text-neutral-800 mb-2">Completed Grades</h5><span class="text-13 text-neutral-500">Class-group grade sheets in Completed or Inactive state.</span></div><button type="button" class="gape-student-card-icon-button" data-grade-completed-toggle aria-expanded="false" aria-label="Show completed grades" title="Show completed grades"><i class="ph ph-caret-down"></i></button></div><div class="d-none mt-16" data-grade-completed-panel><c:forEach var="courseGroup" items="${gradeSheetGroups}"><c:forEach var="occurrenceGroup" items="${courseGroup.occurrences}"><c:forEach var="subjectGroup" items="${occurrenceGroup.subjects}"><c:if test="${not empty subjectGroup.completedClassGroupSheets}"><section class="gape-student-class-course-group gape-student-grade-course-group mb-16"><div class="gape-student-grade-occurrence-group__header d-flex align-items-center gap-10"><span class="gape-student-icon gape-student-soft-blue text-18"><i class="ph ph-books"></i></span><div class="min-w-0"><h5 class="text-15 fw-semibold text-neutral-700 mb-2"><c:out value="${courseGroup.courseLabel}"/> &middot; <c:out value="${subjectGroup.subjectLabel}"/> &middot; <c:out value="${occurrenceGroup.occurrenceLabel}"/></h5><span class="text-12 text-neutral-500"><c:out value="${occurrenceGroup.occurrenceDateRangeLabel}"/></span></div></div><div class="row gy-3"><c:forEach var="sheet" items="${subjectGroup.completedClassGroupSheets}"><div class="col-xxl-3 col-xl-4 col-md-6"><article class="gape-student-card gape-student-class-group-card px-14 py-14 h-100"><div class="gape-student-class-group-card__top mb-10"><span class="gape-student-class-group-card__icon gape-student-class-group-card__icon--compact"><i class="ph ph-users-three"></i></span><div class="min-w-0"><h6 class="text-14 fw-semibold text-neutral-800 mb-4 text-line-2"><c:out value="${sheet.classGroupCode}"/></h6><span class="text-12 text-neutral-500"><c:out value="${sheet.title}"/></span></div></div><div class="gape-student-class-group-card__badges mb-12"><span class="${sheet.stateBadgeClass} px-12 py-7 rounded-pill text-12"><c:out value="${sheet.stateLabel}"/></span></div><div class="gape-student-class-group-card__meta mb-12"><span><c:out value="${sheet.recordCount}"/> records</span><span><c:out value="${sheet.typeLabel}"/></span></div><div class="gape-student-card-actions"><button type="button" class="gape-student-card-icon-button" data-bs-toggle="modal" data-bs-target="#studentClassGrade${sheet.id}" aria-label="View class grade details"><i class="ph ph-eye"></i></button></div></article></div></c:forEach></div></section></c:if></c:forEach></c:forEach></c:forEach></div></div>
    </div>
    <%-- Full grade-sheet dialogs are rendered once, outside the cards, so
         every Open action uses the same document layout as Class Group Details. --%>
    <div class="d-none" aria-hidden="true">
        <c:forEach var="courseGroup" items="${gradeSheetGroups}">
            <c:forEach var="occurrenceGroup" items="${courseGroup.occurrences}">
                <c:forEach var="subjectGroup" items="${occurrenceGroup.subjects}">
                    <c:set var="studentGradeSheetView" value="${subjectGroup.primarySheet}"/>
                    <c:set var="studentGradeDoc" value="${subjectGroup.subjectDocument}"/>
                    <c:choose><c:when test="${occurrenceGroup.completed}"><c:set var="studentGradeSheetModalId" value="studentCompletedSubjectGrade${courseGroup.courseId}_${subjectGroup.subjectId}_${subjectGroup.occurrenceId}"/></c:when><c:otherwise><c:set var="studentGradeSheetModalId" value="studentSubjectGrade${courseGroup.courseId}_${subjectGroup.subjectId}_${subjectGroup.occurrenceId}"/></c:otherwise></c:choose>
                    <%@ include file="/WEB-INF/fragments/student-grade-sheet-modal.jspf" %>
                    <c:forEach var="sheet" items="${subjectGroup.classGroupSheets}">
                        <c:set var="studentGradeSheetView" value="${sheet}"/>
                        <c:set var="studentGradeDoc" value="${sheet.document}"/>
                        <c:set var="studentGradeSheetModalId" value="studentClassGrade${sheet.id}"/>
                        <%@ include file="/WEB-INF/fragments/student-grade-sheet-modal.jspf" %>
                    </c:forEach>
                </c:forEach>
            </c:forEach>
        </c:forEach>
    </div>
    <c:if test="${empty gradeRecords}"><div class="gape-student-empty py-32 px-20 text-center text-14 text-neutral-500">No published grades available.</div></c:if>
</section>

<section id="certificates" class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30" data-student-tab-panel="certificates" role="tabpanel" hidden>
    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-24"><div><h6 class="text-20 fw-semibold text-neutral-800 mb-4">Certificates</h6><span class="text-14 text-neutral-500">Published certificates and their validation details.</span></div></div>
    <div class="row gy-4 gape-student-card-grid--aligned">
        <c:forEach var="certificate" items="${certificates}"><c:set var="certificateEventHref" value="/student/attendance#studentCertificateDetail${certificate.id}"/><%@ include file="/WEB-INF/fragments/student-certificate-card.jspf" %></c:forEach>
        <c:if test="${empty certificates}"><div class="col-12"><div class="gape-student-empty py-32 px-20 text-center text-14 text-neutral-500">No published certificates available.</div></div></c:if>
    </div>
</section>

<script>
document.addEventListener('DOMContentLoaded', function () {
    /* Bootstrap's modal/backdrop stacking is only reliable when the modal is
       a direct child of <body>.  Enrollment cards render their modal beside
       each card, inside dashboard shells that create stacking contexts; move
       those dialog nodes once before any trigger can open them. */
   document.querySelectorAll('[id^="studentSubjectGrade"], [id^="studentCompletedSubjectGrade"], [id^="studentClassGrade"]').forEach(function (modal) {
        if (!modal.hasAttribute('data-student-grade-sheet-full')) modal.remove();
    });
   document.querySelectorAll('[id^="studentCourseEnrollment"], [id^="studentCompletedCourseEnrollment"], [id^="studentActiveClassEnrollment"], [id^="studentCompletedClassEnrollment"], [id^="studentClassEnrollment"], [id^="studentAssessmentEnrollment"], [id^="studentCompletedAssessmentEnrollment"], [id^="studentCertificateDetail"], [data-student-grade-sheet-full]').forEach(function (modal) {
        if (modal.parentElement !== document.body) document.body.appendChild(modal);
    });
    document.querySelectorAll('[data-enrollment-kind-panel="courses"] .gape-student-class-group-card__meta, [data-student-completed-course-cards] .gape-student-class-group-card__meta').forEach(function (meta) {
        var spans = Array.prototype.slice.call(meta.querySelectorAll(':scope > span'));
        if (spans.length < 2 || !spans[0].textContent.trim().startsWith('From:')) return;
        var shortDate = function (value) {
            var match = value.match(/^(\d{4})-(\d{2})-(\d{2})$/);
            return match ? match[3] + '-' + match[2] + '-' + match[1].slice(-2) : value;
        };
        var start = shortDate(spans[0].textContent.trim().replace(/^From:\s*/, ''));
        var end = shortDate(spans[1].textContent.trim().replace(/^To:\s*/, ''));
        spans[0].textContent = 'Period: ' + start + ' to ' + end;
        spans[0].classList.add('gape-student-enrollment-period');
        spans[1].remove();
    });
    document.querySelectorAll('[data-enrollment-kind-panel="classes"] .gape-student-class-group-card__top > div > span').forEach(function (span) {
        if (span.textContent.indexOf('Period:') >= 0) span.classList.add('gape-student-enrollment-period');
    });
    document.querySelectorAll('button.btn-close[data-bs-dismiss="modal"]:not([aria-label])').forEach(function (button) { button.setAttribute('aria-label', 'Close'); });
    var root=document.querySelector('[data-student-tabs]'); if(!root)return;
    var tabs=[].slice.call(root.querySelectorAll('[data-student-tab]')); var panels=[].slice.call(document.querySelectorAll('[data-student-tab-panel]'));
    function activate(name,hash){ if(!panels.some(function(p){return p.getAttribute('data-student-tab-panel')===name;}))return; tabs.forEach(function(t){var active=t.getAttribute('data-student-tab')===name;t.classList.toggle('is-active',active);t.setAttribute('aria-selected',active?'true':'false');});panels.forEach(function(p){p.hidden=p.getAttribute('data-student-tab-panel')!==name;});if(hash&&window.history&&window.history.replaceState){var stableUrl=window.location.pathname+window.location.search+'#'+name;window.history.replaceState({studentAttendanceTab:name},'',stableUrl);}}
    tabs.forEach(function(t){t.addEventListener('click',function(){activate(t.getAttribute('data-student-tab'),true);});});
    var initialTab=(window.location.hash||'#enrollments').slice(1);
    if(initialTab.indexOf('studentCertificateDetail')===0)initialTab='certificates';
    activate(initialTab,false);
    var kindRoot=document.querySelector('[data-enrollment-kind-tabs]');
    if(kindRoot){
        var kindButtons=[].slice.call(kindRoot.querySelectorAll('[data-enrollment-kind]')); var kindPanels=[].slice.call(document.querySelectorAll('[data-enrollment-kind-panel]'));
        var completedArchive=document.querySelector('[data-student-completed-archive]');
        var completedCourseCards=completedArchive&&completedArchive.querySelector('[data-student-completed-course-cards]');
        var completedClassCards=completedArchive&&completedArchive.querySelector('[data-student-completed-class-cards]');
        var completedAssessmentCards=completedArchive&&completedArchive.querySelector('[data-student-completed-assessment-cards]');
        var completedCount=completedArchive&&completedArchive.querySelector('[data-student-completed-count]');
        var completedSummary=completedArchive&&completedArchive.querySelector('[data-student-completed-summary]');
        var completedEmpty=completedArchive&&completedArchive.querySelector('[data-student-completed-empty]');
        function updateCompletedArchive(name){
            if(!completedArchive)return;
            var showCourses=name==='courses';
            var showClasses=name==='classes';
            var showAssessments=name==='assessments';
            if(completedCourseCards)completedCourseCards.classList.toggle('d-none',!showCourses);
            if(completedClassCards)completedClassCards.classList.toggle('d-none',!showClasses);
            if(completedAssessmentCards)completedAssessmentCards.classList.toggle('d-none',!showAssessments);
            if(completedCount){
                var source=showClasses?completedClassCards:showAssessments?completedAssessmentCards:showCourses?completedCourseCards:null;
                var countAttribute=showClasses?'data-student-completed-class-count':showAssessments?'data-student-completed-assessment-count':'data-student-completed-course-count';
                completedCount.textContent=source?(source.getAttribute(countAttribute)||'0'):'0';
                if(completedEmpty){
                    var numericCount=parseInt(completedCount.textContent,10)||0;
                    completedEmpty.classList.toggle('d-none',!showCourses&&!showClasses&&!showAssessments || numericCount>0);
                    completedEmpty.textContent=showClasses?'No completed class-group enrollments.':showAssessments?'No completed assessment enrollments.':showCourses?'No completed course enrollments.':'Select Courses, Class groups or Assessments.';
                }
            }
            if(completedSummary)completedSummary.textContent=showClasses?'Past class-group enrollments':showAssessments?'Past assessment enrollments':showCourses?'Past course enrollments':'Select Courses, Class groups or Assessments';
        }
        function activateEnrollmentKind(name){
            kindButtons.forEach(function(item){item.classList.toggle('is-active',item.getAttribute('data-enrollment-kind')===name);});
            kindPanels.forEach(function(panel){var legacyClassPanel=name==='classes' && panel.getAttribute('data-enrollment-kind-panel')==='classes' && !panel.hasAttribute('data-student-class-enrollment-modern');var legacyAssessmentPanel=name==='assessments' && panel.getAttribute('data-enrollment-kind-panel')==='assessments' && !panel.hasAttribute('data-student-assessment-enrollment-modern');panel.classList.toggle('d-none',panel.getAttribute('data-enrollment-kind-panel')!==name || legacyClassPanel || legacyAssessmentPanel);});
            updateCompletedArchive(name);
        }
        kindButtons.forEach(function(button){button.addEventListener('click',function(){activateEnrollmentKind(button.getAttribute('data-enrollment-kind'));});});
        var initialKindButton=kindButtons.find(function(button){return button.classList.contains('is-active');})||kindButtons[0];
        activateEnrollmentKind(initialKindButton?initialKindButton.getAttribute('data-enrollment-kind'):'courses');
    }
    function normaliseGradeTree(){
        document.querySelectorAll('[data-grade-kind-panel="subjects"] .gape-student-grade-occurrence-group, [data-grade-kind-panel="classes"] .gape-student-grade-occurrence-group').forEach(function(occurrence){
            occurrence.classList.add('gape-student-class-occurrence-group','border','border-neutral-30','rounded-10','px-16','py-14','mb-16');
            var header=occurrence.querySelector(':scope > .gape-student-grade-occurrence-group__header');
            if(header){
                header.classList.remove('justify-content-between');
                header.classList.add('d-flex','align-items-center','justify-content-start','gap-12','flex-wrap','mb-12');
                var title=header.querySelector('h5');
                if(title){title.classList.remove('text-15');title.classList.add('text-14','fw-semibold','text-neutral-800','d-block');}
            }
            var directRow=occurrence.querySelector(':scope > .row');
            if(directRow){directRow.classList.remove('gy-3');directRow.classList.add('gy-4');}
        });
        document.querySelectorAll('[data-grade-kind-panel="classes"] .gape-student-grade-course-group').forEach(function(courseGroup){
            var occurrenceNodes=Array.prototype.slice.call(courseGroup.children).filter(function(child){
                return child.classList.contains('gape-student-grade-occurrence-group');
            });
            if(!occurrenceNodes.length)return;
            var subjectBuckets=[], bucketByName={};
            occurrenceNodes.forEach(function(occurrence){
                var occurrenceHeader=occurrence.querySelector(':scope > .gape-student-grade-occurrence-group__header');
                var children=Array.prototype.slice.call(occurrence.children);
                for(var index=0;index<children.length-1;index++){
                    var subjectTitle=children[index], row=children[index+1];
                    if(subjectTitle.tagName!=='H5'||!row.classList.contains('row'))continue;
                    if(!row.querySelector('.gape-student-class-group-card')){index++;continue;}
                    var subjectName=subjectTitle.textContent.trim();
                    var bucket=bucketByName[subjectName];
                    if(!bucket){bucket={title:subjectTitle.cloneNode(true),occurrences:[]};bucketByName[subjectName]=bucket;subjectBuckets.push(bucket);}
                    bucket.occurrences.push({header:occurrenceHeader,row:row});
                    index++;
                }
            });
            occurrenceNodes.forEach(function(occurrence){occurrence.remove();});
            var tree=document.createDocumentFragment();
            subjectBuckets.forEach(function(bucket){
                var subjectGroup=document.createElement('section');
                subjectGroup.className='gape-student-class-subject-group';
                var subjectHeading=document.createElement('div');
                subjectHeading.className='d-flex align-items-center gap-12 mb-14';
                var subjectIcon=document.createElement('span');
                subjectIcon.className='gape-student-icon gape-student-soft-violet text-22';
                subjectIcon.innerHTML='<i class="ph ph-book-open-text"></i>';
                bucket.title.className='text-16 fw-semibold text-neutral-800 mb-0';
                subjectHeading.appendChild(subjectIcon);
                subjectHeading.appendChild(bucket.title);
                subjectGroup.appendChild(subjectHeading);
                bucket.occurrences.forEach(function(item){
                    var occurrence=document.createElement('div');
                    occurrence.className='gape-student-grade-occurrence-group gape-student-class-occurrence-group border border-neutral-30 rounded-10 px-16 py-14 mb-16';
                    var header=item.header.cloneNode(true);
                    header.classList.remove('justify-content-between');
                    header.classList.add('d-flex','align-items-center','justify-content-start','gap-12','flex-wrap','mb-12');
                    var title=header.querySelector('h5');
                    if(title){title.classList.remove('text-15');title.classList.add('text-14','fw-semibold','text-neutral-800','d-block');}
                    item.row.classList.remove('gy-3');
                    item.row.classList.add('gy-4');
                    occurrence.appendChild(header);
                    occurrence.appendChild(item.row);
                    subjectGroup.appendChild(occurrence);
                });
                tree.appendChild(subjectGroup);
            });
            courseGroup.appendChild(tree);
        });
    }
    normaliseGradeTree();
    function normaliseCompletedClassGradeTree(){
        var panel=document.querySelector('[data-grade-completed-panel]');
        if(!panel)return;
        var sourceSections=Array.prototype.slice.call(panel.querySelectorAll(':scope > .gape-student-grade-course-group'));
        if(!sourceSections.length)return;
        var courses=[],courseByName={};
        sourceSections.forEach(function(source){
            var header=source.querySelector(':scope > .gape-student-grade-occurrence-group__header');
            var title=header&&header.querySelector('h5');
            var row=source.querySelector(':scope > .row');
            if(!title||!row)return;
            var parts=title.textContent.trim().split(/\s+·\s+/);
            var courseName=parts.shift()||'Course';
            var subjectName=parts.shift()||'Subject';
            var occurrenceName=parts.join(' · ')||'Occurrence';
            var course=courseByName[courseName];
            if(!course){course={name:courseName,subjects:[],subjectByName:{}};courseByName[courseName]=course;courses.push(course);}
            var subject=course.subjectByName[subjectName];
            if(!subject){subject={name:subjectName,occurrences:[]};course.subjectByName[subjectName]=subject;course.subjects.push(subject);}
            var dateLabel=header.querySelector('.text-12');
            subject.occurrences.push({label:occurrenceName,date:dateLabel?dateLabel.textContent.trim():'',row:row});
        });
        sourceSections.forEach(function(source){source.remove();});
        var tree=document.createDocumentFragment();
        courses.forEach(function(course){
            var courseSection=document.createElement('section');
            courseSection.className='gape-student-class-course-group gape-student-grade-course-group mb-16';
            var courseHeader=document.createElement('div');
            courseHeader.className='gape-student-class-course-group__header d-flex align-items-center justify-content-between gap-16 flex-wrap';
            courseHeader.innerHTML='<div class="d-flex align-items-center gap-12 min-w-0"><span class="gape-student-icon gape-student-soft-blue text-22"><i class="ph ph-books"></i></span><div class="min-w-0"><h4 class="text-18 fw-semibold text-neutral-800 mb-4"></h4><span class="text-13 text-neutral-500">Completed class-group grades</span></div></div>';
            courseHeader.querySelector('h4').textContent=course.name;
            courseSection.appendChild(courseHeader);
            course.subjects.forEach(function(subject){
                var subjectSection=document.createElement('section');
                subjectSection.className='gape-student-class-subject-group';
                var subjectHeading=document.createElement('div');
                subjectHeading.className='d-flex align-items-center gap-12 mb-14';
                subjectHeading.innerHTML='<span class="gape-student-icon gape-student-soft-violet text-22"><i class="ph ph-book-open-text"></i></span><h5 class="text-16 fw-semibold text-neutral-800 mb-0"></h5>';
                subjectHeading.querySelector('h5').textContent=subject.name;
                subjectSection.appendChild(subjectHeading);
                subject.occurrences.forEach(function(item){
                    var occurrence=document.createElement('div');
                    occurrence.className='gape-student-grade-occurrence-group gape-student-class-occurrence-group border border-neutral-30 rounded-10 px-16 py-14 mb-16';
                    var occurrenceHeader=document.createElement('div');
                    occurrenceHeader.className='gape-student-grade-occurrence-group__header d-flex align-items-center justify-content-start gap-12 flex-wrap mb-12';
                    occurrenceHeader.innerHTML='<span class="gape-student-icon gape-student-soft-blue text-18"><i class="ph ph-calendar-blank"></i></span><div class="min-w-0"><h5 class="text-14 fw-semibold text-neutral-800 d-block mb-2"></h5><span class="text-12 text-neutral-500"></span></div>';
                    occurrenceHeader.querySelector('h5').textContent=item.label;
                    occurrenceHeader.querySelector('.text-12').textContent=item.date;
                    item.row.classList.remove('gy-3');
                    item.row.classList.add('gy-4');
                    occurrence.appendChild(occurrenceHeader);
                    occurrence.appendChild(item.row);
                    subjectSection.appendChild(occurrence);
                });
                courseSection.appendChild(subjectSection);
            });
            tree.appendChild(courseSection);
        });
        panel.appendChild(tree);
    }
    normaliseCompletedClassGradeTree();
    /* Keep Subjects visually consistent with Class groups: course -> subject ->
       occurrence -> grade cards.  The server model already carries this
       relationship; this only normalises the rendered tree for legacy rows. */
    function normaliseSubjectGradeTree(){
        document.querySelectorAll('[data-grade-kind-panel="subjects"] .gape-student-grade-course-group').forEach(function(courseGroup){
            var occurrenceNodes=Array.prototype.slice.call(courseGroup.children).filter(function(child){
                return child.classList.contains('gape-student-grade-occurrence-group');
            });
            if(!occurrenceNodes.length)return;
            var subjects=[], subjectByName={};
            occurrenceNodes.forEach(function(occurrence){
                var occurrenceHeader=occurrence.querySelector(':scope > .gape-student-grade-occurrence-group__header');
                var row=occurrence.querySelector(':scope > .row');
                if(!occurrenceHeader||!row)return;
                var rows=Array.prototype.slice.call(row.querySelectorAll(':scope > [class*="col-"]'));
                var cardsBySubject={};
                rows.forEach(function(col){
                    var title=col.querySelector('h6');
                    var name=title?title.textContent.trim():'Subject';
                    (cardsBySubject[name]||(cardsBySubject[name]=[])).push(col);
                });
                Object.keys(cardsBySubject).forEach(function(name){
                    var subject=subjectByName[name];
                    if(!subject){subject={name:name,occurrences:[]};subjectByName[name]=subject;subjects.push(subject);}
                    subject.occurrences.push({header:occurrenceHeader.cloneNode(true),cards:cardsBySubject[name]});
                });
            });
            if(!subjects.length)return;
            occurrenceNodes.forEach(function(occurrence){occurrence.remove();});
            var tree=document.createDocumentFragment();
            subjects.forEach(function(subject){
                var subjectGroup=document.createElement('section');
                subjectGroup.className='gape-student-class-subject-group';
                var heading=document.createElement('div');
                heading.className='d-flex align-items-center gap-12 mb-14';
                heading.innerHTML='<span class="gape-student-icon gape-student-soft-violet text-22"><i class="ph ph-book-open-text"></i></span><h5 class="text-16 fw-semibold text-neutral-800 mb-0"></h5>';
                heading.querySelector('h5').textContent=subject.name;
                subjectGroup.appendChild(heading);
                subject.occurrences.forEach(function(item){
                    var occurrence=document.createElement('div');
                    occurrence.className='gape-student-grade-occurrence-group gape-student-class-occurrence-group border border-neutral-30 rounded-10 px-16 py-14 mb-16';
                    item.header.classList.remove('justify-content-between');
                    item.header.classList.add('d-flex','align-items-center','justify-content-start','gap-12','flex-wrap','mb-12');
                    var title=item.header.querySelector('h5');
                    if(title){title.classList.remove('text-15');title.classList.add('text-14','fw-semibold','text-neutral-800','d-block');}
                    var meta=item.header.querySelector('.text-12');
                    if(meta){var metaParts=meta.textContent.split('·');meta.textContent=(metaParts[0]||'').trim()+' · 1 subject';}
                    var row=document.createElement('div');
                    row.className='row gy-4';
                    item.cards.forEach(function(card){row.appendChild(card);});
                    occurrence.appendChild(item.header);
                    occurrence.appendChild(row);
                    subjectGroup.appendChild(occurrence);
                });
                tree.appendChild(subjectGroup);
            });
            courseGroup.appendChild(tree);
        });
    }
    normaliseSubjectGradeTree();
    /* Never leave an active course/subject/class-group container visually
       empty.  Some grade-sheet groups can still have an active occurrence
       while every child sheet has already moved to the completed archive.
       In that case the course heading remains useful context, but the child
       area must explicitly explain why there are no cards to show. */
    function normaliseActiveGradeEmptyStates(){
        [
            {kind:'subjects', courseMessage:'No active subject grade sheets for this course.', panelMessage:'No active subject grade sheets.'},
            {kind:'classes', courseMessage:'No active class-group grade sheets for this course.', panelMessage:'No active class-group grade sheets.'}
        ].forEach(function(config){
            var panel=document.querySelector('[data-grade-kind-panel="'+config.kind+'"]');
            if(!panel)return;
            var courseGroups=Array.prototype.slice.call(panel.children).filter(function(child){
                return child.classList.contains('gape-student-grade-course-group');
            });
            var cardsInPanel=0;
            courseGroups.forEach(function(courseGroup){
                Array.prototype.slice.call(courseGroup.querySelectorAll('.gape-student-grade-occurrence-group')).forEach(function(occurrence){
                    if(!occurrence.querySelector('.gape-student-class-group-card'))occurrence.remove();
                });
                Array.prototype.slice.call(courseGroup.querySelectorAll('.gape-student-class-subject-group')).forEach(function(subjectGroup){
                    if(!subjectGroup.querySelector('.gape-student-class-group-card'))subjectGroup.remove();
                });
                var cards=courseGroup.querySelectorAll('.gape-student-class-group-card');
                cardsInPanel+=cards.length;
                var empty=courseGroup.querySelector(':scope > [data-grade-active-empty]');
                if(!cards.length){
                    if(!empty){
                        empty=document.createElement('div');
                        empty.className='gape-student-empty py-24 px-20 text-center text-14 text-neutral-500';
                        empty.setAttribute('data-grade-active-empty','');
                        courseGroup.appendChild(empty);
                    }
                    empty.textContent=config.courseMessage;
                }else if(empty){
                    empty.remove();
                }
            });
            var panelEmpty=panel.querySelector(':scope > [data-grade-active-empty]');
            if(!courseGroups.length){
                if(!panelEmpty){
                    panelEmpty=document.createElement('div');
                    panelEmpty.className='gape-student-empty py-24 px-20 text-center text-14 text-neutral-500';
                    panelEmpty.setAttribute('data-grade-active-empty','');
                    panel.appendChild(panelEmpty);
                }
                panelEmpty.textContent=config.panelMessage;
            }else if(panelEmpty){
                panelEmpty.remove();
            }
            panel.setAttribute('data-grade-active-card-count',String(cardsInPanel));
        });
    }
    normaliseActiveGradeEmptyStates();
    function normaliseGradeArchiveShell(rootSelector, panelSelector, label, summary, emptyLabel){
        var archive=document.querySelector(rootSelector); if(!archive)return;
        if(archive.querySelector(':scope > .gape-learning-management-completed-divider'))return;
        var panel=archive.querySelector(panelSelector), oldHeader=archive.firstElementChild;
        if(!panel||!oldHeader)return;
        var count=panel.querySelectorAll('.gape-student-class-group-card').length;
        archive.classList.add('gape-learning-management-completed-wrapper','gape-student-completed-enrollments','mt-20');
        var divider=document.createElement('div');
        divider.className='gape-learning-management-completed-divider';
        divider.textContent=label;
        var node=document.createElement('article');
        node.className='gape-learning-management-completed-node border rounded-8 px-18 py-16 bg-white';
        var row=document.createElement('div');
        row.className='gape-deferred-management-archive-row';
        row.innerHTML='<div class="d-flex align-items-center gap-12 min-w-0"><span class="gape-learning-management-group-icon bg-danger-50 text-danger-600"><i class="ph ph-archive" aria-hidden="true"></i></span><div class="min-w-0"><span class="fw-medium text-14 text-neutral-700 d-block"></span><span class="gape-node-meta text-12"></span></div></div><div class="gape-deferred-management-archive-spacer"></div><div><span class="cd-element-count" data-grade-completed-count></span></div><div><span class="bg-neutral-20 text-neutral-600 px-14 py-6 border-neutral-30 border rounded-pill text-13">Completed</span></div><div class="d-flex justify-content-end"><button type="button" class="gape-tree-toggle text-20 text-neutral-500 hover-text-main-600" aria-expanded="false"><i class="ph ph-caret-down" aria-hidden="true"></i></button></div>';
        row.querySelector('.fw-medium').textContent=label;
        row.querySelector('.gape-node-meta').textContent=summary;
        row.querySelector('[data-grade-completed-count]').textContent=String(count);
        var button=row.querySelector('button');
        button.removeAttribute('data-grade-completed-subject-toggle');
        button.removeAttribute('data-grade-completed-toggle');
        if(rootSelector.indexOf('subject')>=0)button.setAttribute('data-grade-completed-subject-toggle','');
        else button.setAttribute('data-grade-completed-toggle','');
        button.setAttribute('aria-label','Show '+label.toLowerCase());
        button.setAttribute('title','Show '+label.toLowerCase());
        oldHeader.remove();
        panel.classList.remove('mt-16');
        panel.classList.add('gape-learning-management-completed-panel');
        node.appendChild(row);
        node.appendChild(panel);
        archive.appendChild(divider);
        archive.appendChild(node);
    }
    normaliseGradeArchiveShell('[data-student-completed-subject-occurrences]','[data-grade-completed-subject-panel]','Completed Subject Occurrences','Past subject grade sheets','No completed subject occurrences.');
    normaliseGradeArchiveShell('[data-student-completed-grades]','[data-grade-completed-panel]','Completed Grades','Past class-group grade sheets','No completed grades.');
    var gradeRoot=document.querySelector('[data-grade-kind-tabs]');
    if(gradeRoot){
        var gradeButtons=[].slice.call(gradeRoot.querySelectorAll('[data-grade-kind]')); var gradePanels=[].slice.call(document.querySelectorAll('[data-grade-kind-panel]'));
        gradeButtons.forEach(function(button){button.addEventListener('click',function(){var name=button.getAttribute('data-grade-kind');gradeButtons.forEach(function(item){item.classList.toggle('is-active',item===button);});gradePanels.forEach(function(panel){panel.classList.toggle('d-none',panel.getAttribute('data-grade-kind-panel')!==name);});});});
    }
    function wireGradeArchive(rootSelector, buttonSelector, panelSelector, openLabel, closeLabel){
        var archive=document.querySelector(rootSelector); if(!archive)return;
        var button=archive.querySelector(buttonSelector), panel=archive.querySelector(panelSelector); if(!button||!panel)return;
        if(!panel.querySelector('.gape-student-grade-course-group')){
            var empty=document.createElement('div');
            empty.className='gape-student-empty py-24 px-20 text-center text-14 text-neutral-500';
            empty.textContent=rootSelector.indexOf('subject')>=0?'No completed subject occurrences.':'No completed grades.';
            panel.appendChild(empty);
        }
        var count=archive.querySelector('[data-grade-completed-count]');
        if(count)count.textContent=String(panel.querySelectorAll('.gape-student-class-group-card').length);
        button.addEventListener('click',function(){
            var open=panel.classList.toggle('d-none')===false;
            button.setAttribute('aria-expanded',open?'true':'false');
            button.setAttribute('aria-label',open?closeLabel:openLabel);
            button.setAttribute('title',open?closeLabel:openLabel);
            var icon=button.querySelector('i'); if(icon){icon.classList.toggle('ph-caret-up',open);icon.classList.toggle('ph-caret-down',!open);}
        });
    }
    wireGradeArchive('[data-student-completed-subject-occurrences]','[data-grade-completed-subject-toggle]','[data-grade-completed-subject-panel]','Show completed subject occurrences','Hide completed subject occurrences');
    wireGradeArchive('[data-student-completed-grades]','[data-grade-completed-toggle]','[data-grade-completed-panel]','Show completed grades','Hide completed grades');
    var completedRoot=document.querySelector('[data-student-completed-enrollments]');
    if(completedRoot){
        var completedButton=completedRoot.querySelector('[data-student-completed-toggle]');
        var completedPanel=completedRoot.querySelector('[data-student-completed-panel]');
        if(completedButton&&completedPanel){
            completedButton.addEventListener('click',function(){
                var open=completedPanel.classList.toggle('d-none')===false;
                completedButton.setAttribute('aria-expanded',open?'true':'false');
                completedButton.setAttribute('title',open?'Hide completed enrollments':'Show completed enrollments');
                completedButton.setAttribute('aria-label',open?'Hide completed enrollments':'Show completed enrollments');
                var icon=completedButton.querySelector('i'); if(icon)icon.classList.toggle('ph-caret-up',open),icon.classList.toggle('ph-caret-down',!open);
            });
        }
    }
});
</script>
<%@ include file="/WEB-INF/fragments/student-dashboard-end.jspf" %>
