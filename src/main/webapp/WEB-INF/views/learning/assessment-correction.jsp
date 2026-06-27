<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Correct Attempt</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
</head>
<body>
<div class="preloader">
    <img src="${pageContext.request.contextPath}/assets/images/icons/preloader.gif" alt="">
</div>
<div class="overlay"></div>
<div class="side-overlay"></div>
<div class="dashbord bg-main-25 w-100 overflow-hidden">
    <div class="d-flex">
        <%@ include file="/WEB-INF/fragments/dashboard-sidebar.jspf" %>
        <div class="dashbord-body flex-grow-1 d-flex flex-column min-vh-100">
            <%@ include file="/WEB-INF/fragments/dashboard-topbar.jspf" %>
            <div class="px-24 py-24 flex-grow-1">
                <%@ include file="/WEB-INF/fragments/flash-messages.jspf" %>

                <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30 mb-20">
                    <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap">
                        <div>
                            <h2 class="text-22 fw-semibold text-neutral-800 mb-6"><c:out value="${assessment.title}"/></h2>
                            <span class="text-14 text-neutral-500">Attempt #${attempt.attemptNumber} by <c:out value="${attempt.studentName}"/>.</span>
                        </div>
                        <div class="d-flex align-items-center gap-10 flex-wrap">
                            <a href="${pageContext.request.contextPath}/learning/assessments/${assessment.id}#enrollments-attempts" class="border-main-600 border px-18 py-10 fw-semibold rounded-8 hover-bg-main-50 transition-03">
                                <i class="ph ph-arrow-left me-8"></i>Detail
                            </a>
                            <c:if test="${assessment.automaticCorrectionAllowed and attempt.correctionOpen}">
                                <form action="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/attempts/${attempt.id}/auto-correct" method="post" class="m-0">
                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                    <button type="submit" class="bg-main-600 px-18 py-10 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03">
                                        <i class="ph ph-magic-wand me-8"></i>Auto Correct
                                    </button>
                                </form>
                            </c:if>
                        </div>
                    </div>
                </div>

                <div class="row gy-4 mb-24">
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-20 py-20 border border-neutral-30 h-100">
                            <span class="text-13 text-neutral-500">Student</span>
                            <strong class="text-15 text-neutral-800 d-block mt-4"><c:out value="${attempt.studentName}"/></strong>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-20 py-20 border border-neutral-30 h-100">
                            <span class="text-13 text-neutral-500">State</span>
                            <span class="${attempt.stateBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12 d-inline-block mt-8"><c:out value="${attempt.stateLabel}"/></span>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-20 py-20 border border-neutral-30 h-100">
                            <span class="text-13 text-neutral-500">Score</span>
                            <strong class="text-15 text-neutral-800 d-block mt-4"><c:out value="${attempt.scoreOverMaxLabel}"/></strong>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-20 py-20 border border-neutral-30 h-100">
                            <span class="text-13 text-neutral-500">Correction</span>
                            <strong class="text-15 text-neutral-800 d-block mt-4"><c:out value="${assessment.correctionModeLabel}"/></strong>
                        </div>
                    </div>
                </div>

                <div class="d-flex flex-column gap-16">
                    <c:forEach var="response" items="${responses}">
                        <article class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap mb-18">
                                <div class="d-flex align-items-start gap-12 min-w-0">
                                    <span class="w-44 h-44 rounded-8 bg-main-50 text-main-600 d-inline-flex align-items-center justify-content-center text-22 flex-shrink-0">
                                        <i class="${response.question.iconClass}"></i>
                                    </span>
                                    <div class="min-w-0">
                                        <div class="d-flex align-items-center gap-8 flex-wrap mb-5">
                                            <h3 class="text-17 fw-semibold text-neutral-800 mb-0"><c:out value="${response.question.code}"/></h3>
                                            <span class="bg-neutral-20 text-neutral-600 px-10 py-5 rounded-pill text-12"><c:out value="${response.question.typeLabel}"/></span>
                                            <span class="bg-main-50 text-main-600 px-10 py-5 rounded-pill text-12"><c:out value="${response.question.score}"/> pts</span>
                                        </div>
                                        <p class="text-14 text-neutral-700 mb-0"><c:out value="${response.question.statement}"/></p>
                                    </div>
                                </div>
                                <span class="${response.scored ? 'bg-success-50 text-success-600' : 'bg-warning-30 text-warning-600'} px-12 py-7 rounded-pill text-12">
                                    <c:out value="${response.scoreLabel}"/>
                                </span>
                            </div>

                            <div class="bg-neutral-20 rounded-8 px-16 py-14 mb-16">
                                <span class="text-12 text-neutral-500 d-block mb-6">Student answer</span>
                                <c:choose>
                                    <c:when test="${response.hasAttachment}">
                                        <a class="text-14 text-main-600 fw-semibold hover-text-main-700" href="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/attempts/${attempt.id}/responses/${response.id}/attachment">
                                            <i class="ph ph-download-simple me-6"></i><c:out value="${response.attachmentFileName}"/>
                                        </a>
                                    </c:when>
                                    <c:otherwise>
                                        <p class="text-14 text-neutral-700 mb-0"><c:out value="${response.displayAnswer}"/></p>
                                    </c:otherwise>
                                </c:choose>
                                <c:if test="${not empty response.answeredAt}">
                                    <span class="text-12 text-neutral-500 d-block mt-8">Answered at <c:out value="${response.answeredAt}"/></span>
                                </c:if>
                            </div>

                            <c:if test="${assessment.manualCorrectionAllowed and attempt.correctionOpen}">
                                <form action="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/attempts/${attempt.id}/responses/${response.id}" method="post" class="d-flex align-items-end gap-12 flex-wrap">
                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                    <div>
                                        <label for="responseScore${response.id}" class="text-13 fw-medium text-neutral-700 mb-8">Manual score</label>
                                        <input id="responseScore${response.id}" name="score" type="number" step="0.01" min="0" max="${response.question.score}" required value="<c:out value='${response.score}'/>" class="form-control px-14 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8" style="width: 160px;">
                                    </div>
                                    <button type="submit" class="bg-main-600 px-18 py-10 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03">
                                        Save Score
                                    </button>
                                </form>
                            </c:if>
                        </article>
                    </c:forEach>

                    <c:if test="${empty responses}">
                        <div class="bg-white rounded-10 px-24 py-40 border border-neutral-30 text-center text-14 text-neutral-500">
                            No responses were saved for this attempt.
                        </div>
                    </c:if>
                </div>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
</body>
</html>
