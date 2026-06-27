<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ include file="/WEB-INF/fragments/student-dashboard-start.jspf" %>

<section class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30">
    <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap mb-24">
        <div class="d-flex align-items-start gap-14 min-w-0">
            <span class="gape-student-class-group-card__icon text-26"><i class="${assessment.iconClass}"></i></span>
            <div class="min-w-0">
                <div class="d-flex align-items-center gap-8 flex-wrap mb-8">
                    <h3 class="text-24 fw-semibold text-neutral-800 mb-0"><c:out value="${assessment.title}"/></h3>
                    <span class="${attempt.stateBadgeClass} px-12 py-7 rounded-pill text-12"><c:out value="${attempt.stateLabel}"/></span>
                </div>
                <p class="text-14 text-neutral-500 mb-0">Attempt #${attempt.attemptNumber} | <c:out value="${assessment.contextLabel}"/></p>
            </div>
        </div>
        <a href="${pageContext.request.contextPath}/student/assessments" class="gape-student-card-icon-button" aria-label="Back to assessments" title="Back to assessments">
            <i class="ph ph-arrow-left"></i>
        </a>
    </div>

    <div class="row gy-4 mb-24">
        <div class="col-md-3">
            <div class="gape-student-card px-18 py-18 h-100">
                <span class="text-13 text-neutral-500 d-block mb-4">Score</span>
                <strong class="text-22 fw-semibold text-neutral-800"><c:out value="${attempt.scoreOverMaxLabel}"/></strong>
            </div>
        </div>
        <div class="col-md-3">
            <div class="gape-student-card px-18 py-18 h-100">
                <span class="text-13 text-neutral-500 d-block mb-4">Result</span>
                <strong class="text-16 fw-semibold text-neutral-800"><c:out value="${attempt.resultLabel}"/></strong>
            </div>
        </div>
        <div class="col-md-3">
            <div class="gape-student-card px-18 py-18 h-100">
                <span class="text-13 text-neutral-500 d-block mb-4">Submitted</span>
                <strong class="text-16 fw-semibold text-neutral-800"><c:out value="${attempt.submittedAt}"/></strong>
            </div>
        </div>
        <div class="col-md-3">
            <div class="gape-student-card px-18 py-18 h-100">
                <span class="text-13 text-neutral-500 d-block mb-4">Correction</span>
                <strong class="text-16 fw-semibold text-neutral-800"><c:out value="${assessment.correctionModeLabel}"/></strong>
            </div>
        </div>
    </div>

    <div class="d-flex flex-column gap-16">
        <c:forEach var="response" items="${responses}">
            <article class="border border-neutral-30 rounded-10 px-20 py-20">
                <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap mb-14">
                    <div class="d-flex align-items-start gap-12 min-w-0">
                        <span class="w-44 h-44 rounded-8 bg-main-50 text-main-600 d-inline-flex align-items-center justify-content-center text-22 flex-shrink-0">
                            <i class="${response.question.iconClass}"></i>
                        </span>
                        <div class="min-w-0">
                            <div class="d-flex align-items-center gap-8 flex-wrap mb-5">
                                <h4 class="text-16 fw-semibold text-neutral-800 mb-0"><c:out value="${response.question.code}"/></h4>
                                <span class="bg-neutral-20 text-neutral-600 px-10 py-5 rounded-pill text-12"><c:out value="${response.question.typeLabel}"/></span>
                            </div>
                            <p class="text-14 text-neutral-700 mb-0"><c:out value="${response.question.statement}"/></p>
                        </div>
                    </div>
                    <span class="${response.scored ? 'bg-success-50 text-success-600' : 'bg-warning-30 text-warning-600'} px-12 py-7 rounded-pill text-12">
                        <c:out value="${response.scoreLabel}"/>
                    </span>
                </div>
                <div class="bg-neutral-20 rounded-8 px-16 py-14">
                    <span class="text-12 text-neutral-500 d-block mb-6">Your answer</span>
                    <c:choose>
                        <c:when test="${response.hasAttachment}">
                            <a class="text-14 text-main-600 fw-semibold hover-text-main-700" href="${pageContext.request.contextPath}/student/assessments/responses/${response.id}/attachment">
                                <i class="ph ph-download-simple me-6"></i><c:out value="${response.attachmentFileName}"/>
                            </a>
                        </c:when>
                        <c:otherwise>
                            <p class="text-14 text-neutral-700 mb-0"><c:out value="${response.displayAnswer}"/></p>
                        </c:otherwise>
                    </c:choose>
                </div>
            </article>
        </c:forEach>

        <c:if test="${empty responses}">
            <div class="gape-student-empty text-center px-24 py-40">
                <span class="gape-student-icon gape-student-soft-blue text-28 mb-16"><i class="ph ph-note-pencil"></i></span>
                <h4 class="text-18 fw-semibold text-neutral-800 mb-8">No answers saved</h4>
                <p class="text-14 text-neutral-500 mb-0">This attempt has no recorded responses.</p>
            </div>
        </c:if>
    </div>
</section>

<%@ include file="/WEB-INF/fragments/student-dashboard-end.jspf" %>
