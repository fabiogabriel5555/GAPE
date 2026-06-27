<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ include file="/WEB-INF/fragments/student-dashboard-start.jspf" %>

<section class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30">
    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-24">
        <div>
            <h6 class="text-20 fw-semibold text-neutral-800 mb-4">Assessments</h6>
            <span class="text-14 text-neutral-500">Forms, tests, exams, attempts and results available to you.</span>
        </div>
        <span class="bg-main-50 text-main-600 px-14 py-8 rounded-pill text-13 fw-semibold">
            ${assessmentCount} available
        </span>
    </div>

    <div class="row gy-4 mb-24">
        <div class="col-md-3">
            <div class="gape-student-card px-18 py-18 h-100">
                <span class="text-13 text-neutral-500 d-block mb-4">Available</span>
                <strong class="text-28 fw-semibold text-neutral-800">${assessmentCount}</strong>
            </div>
        </div>
        <div class="col-md-3">
            <div class="gape-student-card px-18 py-18 h-100">
                <span class="text-13 text-neutral-500 d-block mb-4">Forms</span>
                <strong class="text-28 fw-semibold text-main-600">${availableForms}</strong>
            </div>
        </div>
        <div class="col-md-3">
            <div class="gape-student-card px-18 py-18 h-100">
                <span class="text-13 text-neutral-500 d-block mb-4">Tests</span>
                <strong class="text-28 fw-semibold text-info-600">${availableTests}</strong>
            </div>
        </div>
        <div class="col-md-3">
            <div class="gape-student-card px-18 py-18 h-100">
                <span class="text-13 text-neutral-500 d-block mb-4">Exams</span>
                <strong class="text-28 fw-semibold text-warning-600">${availableExams}</strong>
            </div>
        </div>
    </div>

    <div class="row gy-4">
        <c:forEach var="assessment" items="${assessments}">
            <c:set var="latestAttempt" value="${latestAttemptByAssessment[assessment.id]}"/>
            <c:set var="assessmentEnrollment" value="${assessmentEnrollmentByAssessment[assessment.id]}"/>
            <c:set var="assessmentEnrollmentState" value="${assessmentEnrollmentStateByAssessment[assessment.id]}"/>
            <c:set var="activeAssessmentEnrollment" value="${assessment.automaticEnrollment or assessmentEnrollmentState eq 'ACTIVE'}"/>
            <c:set var="pendingAssessmentEnrollment" value="${assessmentEnrollmentState eq 'PENDING'}"/>
            <c:set var="attemptLimitReached" value="${not empty latestAttempt and not empty assessment.attemptsLimit and latestAttempt.attemptNumber >= assessment.attemptsLimit}"/>
            <div class="col-xxl-4 col-xl-6">
                <article class="gape-student-card px-18 py-18 h-100">
                    <div class="d-flex align-items-start justify-content-between gap-12 mb-14">
                        <div class="d-flex align-items-start gap-12 min-w-0">
                            <span class="gape-student-class-group-card__icon text-24">
                                <i class="${assessment.iconClass}"></i>
                            </span>
                            <div class="min-w-0">
                                <h6 class="text-17 fw-semibold text-neutral-800 mb-6 text-line-2"><c:out value="${assessment.title}"/></h6>
                                <span class="text-13 text-neutral-500 d-block text-line-1"><c:out value="${assessment.contextLabel}"/></span>
                            </div>
                        </div>
                        <span class="${assessment.softClass} px-10 py-6 rounded-pill text-12 flex-shrink-0">
                            <c:out value="${assessment.typeLabel}"/>
                        </span>
                    </div>

                    <p class="text-13 text-neutral-500 mb-14 text-line-2"><c:out value="${assessment.description}"/></p>

                    <div class="gape-student-class-group-card__meta gape-student-class-group-card__meta--wide mb-16">
                        <span><c:out value="${assessment.questionCountLabel}"/></span>
                        <span>Grade: <c:out value="${assessment.maxGrade}"/></span>
                        <span>Attempts: <c:out value="${assessment.attemptsLimitLabel}"/></span>
                        <span><c:out value="${assessment.enrollmentModeLabel}"/></span>
                        <span><c:out value="${assessment.availabilityLabel}"/></span>
                    </div>

                    <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap">
                        <c:choose>
                            <c:when test="${not empty latestAttempt}">
                                <span class="${latestAttempt.stateBadgeClass} px-12 py-7 rounded-pill text-12">
                                    <c:out value="${latestAttempt.stateLabel}"/>
                                </span>
                            </c:when>
                            <c:otherwise>
                                <span class="bg-neutral-20 text-neutral-500 px-12 py-7 rounded-pill text-12">Not started</span>
                            </c:otherwise>
                        </c:choose>
                        <div class="d-flex align-items-center gap-8">
                            <c:choose>
                                <c:when test="${not empty latestAttempt and latestAttempt.inProgress}">
                                    <a href="${pageContext.request.contextPath}/student/assessments/attempts/${latestAttempt.id}" class="gape-student-card-icon-button" aria-label="Continue attempt" title="Continue attempt">
                                        <i class="ph ph-play"></i>
                                    </a>
                                </c:when>
                                <c:when test="${not empty latestAttempt}">
                                    <a href="${pageContext.request.contextPath}/student/assessments/attempts/${latestAttempt.id}/result" class="gape-student-card-icon-button" aria-label="View result" title="View result">
                                        <i class="ph ph-chart-bar"></i>
                                    </a>
                                </c:when>
                            </c:choose>
                            <c:if test="${pendingAssessmentEnrollment}">
                                <span class="gape-student-card-icon-button" aria-label="Enrollment pending" title="Enrollment pending">
                                    <i class="ph ph-clock"></i>
                                </span>
                            </c:if>
                            <c:if test="${not activeAssessmentEnrollment and not pendingAssessmentEnrollment}">
                                <form action="${pageContext.request.contextPath}/student/assessments/${assessment.id}/enroll" method="post" class="m-0">
                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                    <button type="submit" class="gape-student-card-icon-button gape-student-card-icon-button--request" aria-label="Enroll in assessment" title="Enroll in assessment">
                                        <i class="ph ph-user-plus"></i>
                                    </button>
                                </form>
                            </c:if>
                            <c:if test="${activeAssessmentEnrollment and (not attemptLimitReached or (not empty latestAttempt and latestAttempt.inProgress))}">
                                <form action="${pageContext.request.contextPath}/student/assessments/${assessment.id}/start" method="post" class="m-0">
                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                    <button type="submit" class="gape-student-card-icon-button gape-student-card-icon-button--request" aria-label="Start assessment" title="${not empty latestAttempt and latestAttempt.inProgress ? 'Continue assessment' : 'Start assessment'}">
                                        <i class="ph ph-arrow-right"></i>
                                    </button>
                                </form>
                            </c:if>
                        </div>
                    </div>
                </article>
            </div>
        </c:forEach>

        <c:if test="${empty assessments}">
            <div class="col-12">
                <div class="gape-student-empty text-center px-24 py-40">
                    <span class="gape-student-icon gape-student-soft-blue text-28 mb-16"><i class="ph ph-seal-question"></i></span>
                    <h4 class="text-18 fw-semibold text-neutral-800 mb-8">No assessments available</h4>
                    <p class="text-14 text-neutral-500 mb-0">Assessments appear here when your active subjects or class groups publish them.</p>
                </div>
            </div>
        </c:if>
    </div>
</section>

<%@ include file="/WEB-INF/fragments/student-dashboard-end.jspf" %>
