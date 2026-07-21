<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Assessments</title>
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

                <div class="row gy-4 mb-24">
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30 h-100">
                            <span class="text-14 text-neutral-500">Assessments</span>
                            <h2 class="text-32 fw-semibold text-neutral-700 mb-0">${assessmentCount}</h2>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30 h-100">
                            <span class="text-14 text-neutral-500">Forms</span>
                            <h2 class="text-32 fw-semibold text-main-600 mb-0">${formCount}</h2>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30 h-100">
                            <span class="text-14 text-neutral-500">Tests</span>
                            <h2 class="text-32 fw-semibold text-info-600 mb-0">${testCount}</h2>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30 h-100">
                            <span class="text-14 text-neutral-500">Exams</span>
                            <h2 class="text-32 fw-semibold text-warning-600 mb-0">${examCount}</h2>
                        </div>
                    </div>
                </div>

                <div class="bg-white rounded-10 px-24 py-24">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">Assessments</h2>
                            <span class="text-14 text-neutral-500">Manage questions, options, attempts, results and corrections.</span>
                        </div>
                        <a href="${pageContext.request.contextPath}/learning/assessments/new" class="bg-main-600 px-20 py-10 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03 d-inline-flex align-items-center" style="min-height: 44px;" data-assessment-modal-url="${pageContext.request.contextPath}/learning/assessments/new" data-assessment-modal-title="Create Assessment">
                            <i class="ph ph-plus-circle me-8"></i>New Assessment
                        </a>
                    </div>

                    <div class="overflow-x-auto">
                        <table class="table mb-0">
                            <thead>
                            <tr>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Assessment</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Context</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Availability</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Progress</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="assessment" items="${assessments}">
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-20 px-20">
                                        <div class="d-flex align-items-start gap-12">
                                            <span class="${assessment.softClass} w-40 h-40 rounded-8 d-inline-flex align-items-center justify-content-center text-20 flex-shrink-0">
                                                <i class="${assessment.iconClass}"></i>
                                            </span>
                                            <div class="min-w-0">
                                                <a href="${pageContext.request.contextPath}/learning/assessments/${assessment.id}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                    <c:out value="${assessment.title}"/>
                                                </a>
                                                <span class="d-block text-12 text-neutral-500">
                                                    <c:out value="${assessment.typeLabel}"/> | <c:out value="${assessment.modeLabel}"/> | <c:out value="${assessment.correctionModeLabel}"/> | <c:out value="${assessment.finalGradeWeight}"/>%
                                                </span>
                                            </div>
                                        </div>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500">
                                        <c:out value="${assessment.contextLabel}"/>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500" data-gape-datetime-display>
                                        <c:choose>
                                            <c:when test="${empty assessment.availabilityLabel}">Always available</c:when>
                                            <c:otherwise><c:out value="${assessment.availabilityLabel}"/></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500">
                                        <span class="d-block"><c:out value="${assessment.questionCountLabel}"/></span>
                                        <span class="d-block text-12 text-neutral-500"><c:out value="${assessment.attemptCountLabel}"/></span>
                                    </td>
                                    <td class="py-20 px-20">
                                        <span class="${assessment.stateBadgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13">
                                            <c:out value="${assessment.stateLabel}"/>
                                        </span>
                                    </td>
                                    <td class="py-20 px-20 text-end">
                                        <div class="d-flex align-items-center gap-12 justify-content-end">
                                            <a href="${pageContext.request.contextPath}/learning/assessments/${assessment.id}" class="text-22 text-neutral-500 hover-text-main-600" title="Detail" aria-label="Detail">
                                                <i class="ph ph-eye"></i>
                                            </a>
                                            <a href="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/edit" data-assessment-modal-url="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/edit" data-assessment-modal-title="Edit Assessment" class="text-22 text-neutral-500 hover-text-main-600" title="Edit Assessment" aria-label="Edit Assessment">
                                                <i class="ph ph-pencil-simple-line"></i>
                                            </a>
                                            <a href="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/pdf" class="text-22 text-neutral-500 hover-text-main-600" title="Download" aria-label="Download">
                                                <i class="ph ph-download-simple"></i>
                                            </a>
                                            <form action="${pageContext.request.contextPath}/learning/assessments/${assessment.id}/delete" method="post" class="m-0">
                                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                <input type="hidden" name="returnTo" value="/learning/assessments">
                                                <button type="submit" class="text-22 text-neutral-500 hover-text-danger-600 bg-transparent border-0 p-0" title="Delete" aria-label="Delete">
                                                    <i class="ph ph-trash"></i>
                                                </button>
                                            </form>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty assessments}">
                                <tr>
                                    <td colspan="6" class="py-40 px-20 text-center text-14 text-neutral-500">
                                        No assessments available in your management context.
                                    </td>
                                </tr>
                            </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>

            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
</body>
</html>
