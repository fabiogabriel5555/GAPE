<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    request.setAttribute("activeMenu", "subjects");
    request.setAttribute("pageTitle", request.getAttribute("subject") == null ? "Subject Detail" : "Subject Detail");
    request.setAttribute("studentPageTitle", request.getAttribute("subject") == null ? "Subject Detail" : "Subject Detail");
    request.setAttribute("studentPageDescription", "Review subject details, enrollment state and available class groups.");
%>
<%@ include file="/WEB-INF/fragments/student-dashboard-start.jspf" %>

<c:set var="subjectPhotoUrl" value=""/>
<c:if test="${subject.subject.hasPhoto}">
    <c:set var="subjectPhotoUrl" value="${pageContext.request.contextPath}/media/${subject.subject.photo}?v=${mediaCacheVersion}"/>
</c:if>

<section class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30 mb-24"
         data-gape-enrollment-target="subject-${course.id}-${subject.subjectId}">
    <div class="gape-student-course-summary">
        <div class="gape-student-course-visual-stack">
            <div class="gape-student-course-detail-thumb">
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
            <div class="gape-student-course-side-grid">
                <div class="gape-student-course-side-item">
                    <span>Acronym</span>
                    <strong><c:out value="${subject.subjectAcronym}"/></strong>
                </div>
                <div class="gape-student-course-side-item">
                    <span>Course</span>
                    <strong><c:out value="${course.acronym}"/></strong>
                </div>
                <div class="gape-student-course-side-item gape-student-course-side-item--wide">
                    <span>Position</span>
                    <strong><c:out value="${subject.curricularPositionLabel}"/></strong>
                </div>
            </div>
        </div>

        <div>
            <div class="d-flex align-items-center gap-10 flex-wrap mb-16">
                <span class="${subjectEnrollment.badgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14"
                      data-gape-enrollment-badge
                      data-gape-enrollment-badge-fixed="px-16 py-8 border-neutral-30 border rounded-pill text-14">
                    <c:out value="${subjectEnrollment.stateLabel}"/>
                </span>
                <span class="${subject.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                    <c:out value="${subject.stateLabel}"/>
                </span>
            </div>
            <h2 class="text-24 fw-semibold text-neutral-800 mb-10"><c:out value="${subject.subjectName}"/></h2>
            <p class="text-14 text-neutral-500 mb-24"><c:out value="${subject.subject.description}"/></p>

            <div class="gape-student-course-metrics-grid mb-20">
                <div class="gape-student-course-metric">
                    <span class="text-13 text-neutral-500 d-block mb-8">ECTS</span>
                    <strong class="text-16 text-neutral-800"><c:out value="${subject.subjectEctsLabel}"/></strong>
                </div>
                <div class="gape-student-course-metric">
                    <span class="text-13 text-neutral-500 d-block mb-8">Type</span>
                    <strong class="text-16 text-neutral-800"><c:out value="${subject.mandatoryLabel}"/></strong>
                </div>
                <div class="gape-student-course-metric">
                    <span class="text-13 text-neutral-500 d-block mb-8">Class groups</span>
                    <strong class="text-16 text-neutral-800">${fn:length(subjectClassGroups)} visible</strong>
                </div>
            </div>

            <div class="gape-student-course-actions"
                 data-gape-enrollment-actions
                 data-gape-enrollment-actions-kind="subject-leave">
                <a href="${pageContext.request.contextPath}/student/subjects" class="border border-neutral-30 px-16 py-9 rounded-8 text-14 fw-semibold text-neutral-600 hover-bg-neutral-20 transition-03">
                    Back to Subjects
                </a>
                <span data-gape-enrollment-action-items class="d-flex align-items-center gap-10 flex-wrap">
                <c:choose>
                    <c:when test="${subject.activeEnrollment}">
                        <form action="${pageContext.request.contextPath}/student/enrollments/courses/${course.id}/subjects/${subject.subjectId}/withdraw" method="post" class="m-0">
                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                            <input type="hidden" name="returnTo" value="/student/subjects/${course.id}/${subject.subjectId}">
                            <button type="submit" class="gape-student-card-icon-button gape-student-card-icon-button--danger" aria-label="Leave subject" title="Leave subject">
                                <i class="ph ph-sign-out"></i>
                            </button>
                        </form>
                    </c:when>
                    <c:when test="${course.activeEnrollment and not subject.pendingEnrollment}">
                        <form action="${pageContext.request.contextPath}/student/enrollments/courses/${course.id}/subjects/${subject.subjectId}" method="post" class="m-0">
                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                            <input type="hidden" name="returnTo" value="/student/subjects/${course.id}/${subject.subjectId}">
                            <button type="submit" class="gape-student-card-icon-button gape-student-card-icon-button--request" aria-label="Request enrollment" title="Request enrollment">
                                <i class="ph ph-user-plus"></i>
                            </button>
                        </form>
                    </c:when>
                </c:choose>
                </span>
            </div>
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

    <div class="row gy-3">
        <c:forEach var="item" items="${subjectClassGroups}">
            <div class="col-xxl-3 col-xl-4 col-md-6">
                <article id="subject-class-group-${item.classGroup.id}"
                         class="gape-student-card gape-student-class-group-card px-14 py-14 h-100"
                         data-gape-enrollment-target="class-group-${item.classGroup.id}">
                    <div class="gape-student-class-group-card__top mb-10">
                        <span class="gape-student-class-group-card__icon gape-student-class-group-card__icon--compact"><i class="ph ph-users-three"></i></span>
                        <div class="min-w-0">
                            <h6 class="text-14 fw-semibold text-neutral-800 mb-4"><c:out value="${item.classGroup.code}"/></h6>
                            <span class="text-12 text-neutral-500" title="<c:out value='${item.classGroup.contextTitle}'/>">
                                <c:out value="${item.classGroup.contextHtml}" escapeXml="false"/>
                            </span>
                        </div>
                    </div>
                    <div class="gape-student-class-group-card__badges mb-12">
                        <span class="${item.enrollmentBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12"
                              data-gape-enrollment-badge
                              data-gape-enrollment-badge-fixed="px-12 py-7 border-neutral-30 border rounded-pill text-12">
                            <c:out value="${item.enrollmentStateLabel}"/>
                        </span>
                    </div>
                    <div class="gape-student-class-group-card__meta mb-12">
                        <span>Modality: <c:out value="${item.classGroup.modalityLabel}"/></span>
                        <span>Shift: <c:out value="${item.classGroup.shift}"/></span>
                        <span>Occup: <c:out value="${item.classGroup.occupancyLabel}"/></span>
                    </div>
                    <div class="gape-student-class-group-card__actions"
                         data-gape-enrollment-actions
                         data-gape-enrollment-actions-kind="class-group-detail">
                        <c:if test="${item.activeEnrollment}">
                            <a href="${pageContext.request.contextPath}/student/class-groups/${item.classGroup.id}" class="gape-student-card-icon-button" aria-label="Open class group" title="Open class group">
                                <i class="ph ph-eye"></i>
                            </a>
                        </c:if>
                        <c:if test="${item.canEnroll}">
                            <form action="${pageContext.request.contextPath}/student/enrollments/class-groups/${item.classGroup.id}" method="post" class="m-0">
                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                <input type="hidden" name="returnTo" value="/student/subjects/${course.id}/${subject.subjectId}">
                                <button type="submit" class="gape-student-card-icon-button gape-student-card-icon-button--request" aria-label="Request enrollment" title="Request enrollment">
                                    <i class="ph ph-user-plus"></i>
                                </button>
                            </form>
                        </c:if>
                        <c:if test="${item.canWithdraw}">
                            <form action="${pageContext.request.contextPath}/student/enrollments/class-groups/${item.classGroup.id}/withdraw" method="post" class="m-0">
                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                <input type="hidden" name="returnTo" value="/student/subjects/${course.id}/${subject.subjectId}">
                                <button type="submit" class="gape-student-card-icon-button gape-student-card-icon-button--danger" aria-label="Leave class group" title="Leave class group">
                                    <i class="ph ph-sign-out"></i>
                                </button>
                            </form>
                        </c:if>
                        <c:if test="${not item.actionAvailable}">
                            <span class="border border-neutral-30 px-12 py-7 rounded-8 text-12 fw-semibold text-neutral-500 bg-neutral-20"><c:out value="${item.unavailableActionLabel}"/></span>
                        </c:if>
                    </div>
                </article>
            </div>
        </c:forEach>
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

<%@ include file="/WEB-INF/fragments/student-dashboard-end.jspf" %>
