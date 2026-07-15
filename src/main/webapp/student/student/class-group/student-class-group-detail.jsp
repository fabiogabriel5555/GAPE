<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    request.setAttribute("activeMenu", "class-groups");
    request.setAttribute("pageTitle", request.getAttribute("studentPageTitle"));
%>
<%@ include file="/WEB-INF/fragments/student-dashboard-start.jspf" %>

<section class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30">
    <div class="gape-student-class-group-detail-hero px-24 py-24 mb-24"
         data-gape-enrollment-target="class-group-${classGroup.id}">
        <div class="d-flex align-items-start justify-content-between gap-18 flex-wrap mb-22">
            <div class="d-flex align-items-start gap-14 min-w-0">
                <span class="gape-student-class-group-card__icon text-26"><i class="ph ph-users-three"></i></span>
                <div class="min-w-0">
                    <div class="d-flex align-items-center gap-8 flex-wrap mb-8">
                        <h3 class="text-24 fw-semibold text-neutral-800 mb-0"><c:out value="${classGroup.code}"/></h3>
                        <span class="${classGroup.stateBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12">
                            <c:out value="${classGroup.stateLabel}"/>
                        </span>
                        <span class="${classGroupItem.enrollmentBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12"
                              data-gape-enrollment-badge
                              data-gape-enrollment-badge-fixed="px-12 py-7 border-neutral-30 border rounded-pill text-12">
                            <c:out value="${classGroupItem.enrollmentStateLabel}"/>
                        </span>
                    </div>
                    <p class="text-14 text-neutral-500 mb-0" title="<c:out value='${classGroup.contextTitle}'/>">
                        <c:out value="${classGroup.contextHtml}" escapeXml="false"/>
                    </p>
                </div>
            </div>
            <div class="d-flex align-items-center gap-8 flex-wrap"
                 data-gape-enrollment-actions
                 data-gape-enrollment-actions-kind="class-group-detail-page">
                <c:choose>
                    <c:when test="${classGroupItem.activeEnrollment}">
                        <a href="#student-class-group-structure" class="gape-student-card-icon-button" aria-label="Open structure" title="Open structure">
                            <i class="ph ph-stack"></i>
                        </a>
                        <form action="${pageContext.request.contextPath}/student/enrollments/class-groups/${classGroup.id}/withdraw" method="post" class="m-0">
                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                            <input type="hidden" name="returnTo" value="/student/class-groups/${classGroup.id}">
                            <button type="submit" class="gape-student-card-icon-button gape-student-card-icon-button--danger" aria-label="Leave class group" title="Leave class group">
                                <i class="ph ph-sign-out"></i>
                            </button>
                        </form>
                    </c:when>
                    <c:when test="${classGroupItem.canEnroll}">
                        <form action="${pageContext.request.contextPath}/student/enrollments/class-groups/${classGroup.id}" method="post" class="m-0">
                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                            <input type="hidden" name="returnTo" value="/student/class-groups/${classGroup.id}">
                            <button type="submit" class="gape-student-card-icon-button gape-student-card-icon-button--request" aria-label="Request enrollment" title="Request enrollment">
                                <i class="ph ph-user-plus"></i>
                            </button>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <span class="border border-neutral-30 px-14 py-8 rounded-8 text-13 fw-semibold text-neutral-500 bg-white"><c:out value="${classGroupItem.unavailableActionLabel}"/></span>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <div class="gape-student-class-group-metrics">
            <div class="gape-student-class-group-metric">
                <span class="text-12 text-neutral-500 d-block mb-4">Course</span>
                <a href="${pageContext.request.contextPath}/student/courses/${course.id}" class="text-14 fw-semibold text-neutral-800 hover-text-main-600"><c:out value="${course.name}"/></a>
            </div>
            <div class="gape-student-class-group-metric">
                <span class="text-12 text-neutral-500 d-block mb-4">Subject</span>
                <a href="${pageContext.request.contextPath}/student/subjects/${course.id}/${subject.subjectId}" class="text-14 fw-semibold text-neutral-800 hover-text-main-600"><c:out value="${subject.subjectName}"/></a>
            </div>
            <div class="gape-student-class-group-metric">
                <span class="text-12 text-neutral-500 d-block mb-4">Modality</span>
                <span class="text-14 fw-semibold text-neutral-800"><c:out value="${classGroup.modalityLabel}"/></span>
            </div>
            <div class="gape-student-class-group-metric">
                <span class="text-12 text-neutral-500 d-block mb-4">Shift</span>
                <span class="text-14 fw-semibold text-neutral-800"><c:out value="${classGroup.shift}"/></span>
            </div>
            <div class="gape-student-class-group-metric">
                <span class="text-12 text-neutral-500 d-block mb-4">Capacity</span>
                <span class="text-14 fw-semibold text-neutral-800"><c:out value="${classGroup.capacityLabel}"/></span>
            </div>
            <div class="gape-student-class-group-metric">
                <span class="text-12 text-neutral-500 d-block mb-4">Occupancy</span>
                <span class="text-14 fw-semibold text-neutral-800"><c:out value="${classGroup.occupancyLabel}"/></span>
            </div>
            <div class="gape-student-class-group-metric">
                <span class="text-12 text-neutral-500 d-block mb-4">Dates</span>
                <span class="text-14 fw-semibold text-neutral-800" data-gape-datetime-display><c:out value="${classGroup.dateRangeLabel}"/></span>
            </div>
            <div class="gape-student-class-group-metric">
                <span class="text-12 text-neutral-500 d-block mb-4">Blocks</span>
                <span class="text-14 fw-semibold text-neutral-800">${fn:length(contentBlocks)}</span>
            </div>
        </div>
    </div>

    <section id="student-class-group-structure" class="gape-student-structure-board">
        <div class="gape-student-structure-board__header px-22 py-22 d-flex align-items-start justify-content-between gap-16 flex-wrap">
            <div>
                <h4 class="text-22 fw-semibold text-neutral-800 mb-6">Study Path</h4>
                <span class="text-14 text-neutral-500">Lessons, materials and assessments released for this class group, organized by pedagogical blocks.</span>
            </div>
            <div class="d-flex align-items-center gap-8 flex-wrap">
                <span class="bg-main-50 text-main-600 px-12 py-7 rounded-pill text-12 fw-semibold">
                    <i class="ph ph-stack me-6"></i>${fn:length(contentBlocks)} blocks
                </span>
                <span class="bg-success-50 text-success-600 px-12 py-7 rounded-pill text-12 fw-semibold">
                    <i class="ph ph-calendar-check me-6"></i>${fn:length(lessons)} lessons
                </span>
            </div>
        </div>

        <div class="px-18 py-18">
            <c:forEach var="block" items="${contentBlocks}" varStatus="blockLoop">
                <article class="gape-student-structure-block">
                    <div class="gape-student-structure-block__header px-18 py-18">
                        <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap">
                            <div class="d-flex align-items-start gap-14 min-w-0">
                                <span class="gape-student-structure-number">
                                    <c:choose>
                                        <c:when test="${blockLoop.count lt 10}">0<c:out value="${blockLoop.count}"/></c:when>
                                        <c:otherwise><c:out value="${blockLoop.count}"/></c:otherwise>
                                    </c:choose>
                                </span>
                                <div class="min-w-0">
                                    <div class="d-flex align-items-center gap-8 flex-wrap mb-6">
                                        <h5 class="text-18 fw-semibold text-neutral-800 mb-0"><c:out value="${block.name}"/></h5>
                                    </div>
                                    <p class="text-13 text-neutral-500 mb-0"><c:out value="${block.description}"/></p>
                                </div>
                            </div>
                            <span class="${block.stateBadgeClass} px-12 py-7 rounded-pill text-12">
                                <c:out value="${block.stateLabel}"/>
                            </span>
                        </div>
                    </div>

                    <c:set var="blockActivities" value="${blockActivitiesByBlock[block.id]}"/>
                    <div>
                        <c:forEach var="activity" items="${blockActivities}">
                            <c:choose>
                                <c:when test="${activity.lessonActivity}">
                                    <c:set var="lesson" value="${activity.lesson}"/>
                                    <div class="gape-student-activity-row">
                                        <span class="gape-student-activity-icon ${lesson.typeBadgeClass} text-22">
                                            <i class="${lesson.typeIconClass}"></i>
                                        </span>
                                        <div class="min-w-0">
                                            <div class="d-flex align-items-center gap-8 flex-wrap mb-5">
                                                <h6 class="text-15 fw-semibold text-neutral-800 mb-0"><c:out value="${lesson.title}"/></h6>
                                                <span class="${lesson.stateBadgeClass} px-10 py-5 rounded-pill text-12"><c:out value="${lesson.stateLabel}"/></span>
                                            </div>
                                            <div class="d-flex align-items-center gap-8 flex-wrap text-12 text-neutral-500">
                                                <span data-gape-datetime-display><i class="ph ph-clock me-4"></i><c:out value="${lesson.compactDateRangeLabel}"/></span>
                                                <span><i class="ph ph-timer me-4"></i><c:out value="${lesson.durationLabel}"/></span>
                                                <span><i class="${lesson.typeIconClass} me-4"></i><c:out value="${lesson.typeLabel}"/></span>
                                                <span><i class="ph ph-user-check me-4"></i><c:out value="${lesson.attendanceLabel}"/></span>
                                                <c:if test="${lesson.hasRoom}">
                                                    <span><i class="ph ph-map-pin me-4"></i><c:out value="${lesson.physicalRoomCode}"/></span>
                                                </c:if>
                                                <c:if test="${(lesson.online or lesson.hybrid) and lesson.hasMeetingLink}">
                                                    <span><i class="ph ph-video-camera me-4"></i><c:out value="${lesson.providerLabel}"/></span>
                                                </c:if>
                                            </div>
                                        </div>
                                        <div class="d-flex align-items-center justify-content-end gap-8">
                                            <c:if test="${(lesson.online or lesson.hybrid) and lesson.hasMeetingLink}">
                                                <a href="${pageContext.request.contextPath}/student/lessons/${lesson.id}/access" target="_blank" rel="noopener noreferrer" class="gape-student-card-icon-button gape-student-card-icon-button--meeting" aria-label="Open meeting" title="Open meeting">
                                                    <i class="ph ph-video-camera"></i>
                                                </a>
                                            </c:if>
                                        </div>
                                    </div>
                                </c:when>
                                <c:when test="${activity.contentActivity}">
                                    <c:set var="content" value="${activity.content}"/>
                                    <c:url var="studentContentThumbnailUrl" value="/contents/download/${content.id}">
                                        <c:param name="variant" value="thumbnail"/>
                                    </c:url>
                                    <c:url var="studentContentInlineUrl" value="/contents/download/${content.id}">
                                        <c:param name="disposition" value="inline"/>
                                    </c:url>
                                    <c:url var="studentContentDownloadUrl" value="/contents/download/${content.id}"/>
                                    <div class="gape-student-activity-row">
                                        <c:choose>
                                            <c:when test="${classGroup.showContentThumbnails and content.visualThumbnailAvailable}">
                                                <span class="gape-student-content-thumb-wrap" data-content-thumbnail>
                                                    <img src="${studentContentThumbnailUrl}" alt="" class="gape-student-content-thumb" loading="eager" onerror="this.dataset.thumbnailFailed='true';">
                                                </span>
                                                <span class="gape-student-activity-icon ${content.formatBadgeClass} text-22 d-none" data-content-thumbnail-fallback>
                                                    <i class="${content.formatIconClass}"></i>
                                                </span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="gape-student-activity-icon ${content.formatBadgeClass} text-22">
                                                    <i class="${content.formatIconClass}"></i>
                                                </span>
                                            </c:otherwise>
                                        </c:choose>
                                        <div class="min-w-0">
                                            <div class="d-flex align-items-center gap-8 flex-wrap mb-5">
                                                <h6 class="text-15 fw-semibold text-neutral-800 mb-0"><c:out value="${content.title}"/></h6>
                                                <span class="${content.mandatoryBadgeClass} px-10 py-5 rounded-pill text-12"><c:out value="${content.mandatoryLabel}"/></span>
                                            </div>
                                            <p class="text-12 text-neutral-500 mb-0"><c:out value="${content.description}"/></p>
                                        </div>
                                        <div class="d-flex align-items-center justify-content-end gap-8 flex-wrap">
                                            <c:choose>
                                                <c:when test="${content.downloadable}">
                                                    <a href="${studentContentInlineUrl}" target="_blank" rel="noopener noreferrer" class="gape-student-card-icon-button" aria-label="Open content" title="Open content">
                                                        <i class="ph ph-eye"></i>
                                                    </a>
                                                    <a href="${studentContentDownloadUrl}" class="gape-student-card-icon-button gape-student-card-icon-button--download" aria-label="Download content" title="Download content">
                                                        <i class="ph ph-download-simple"></i>
                                                    </a>
                                                </c:when>
                                                <c:when test="${content.linkable}">
                                                    <a href="${fn:escapeXml(content.source)}" target="_blank" rel="noopener noreferrer" class="gape-student-card-icon-button" aria-label="Open content" title="Open content">
                                                        <i class="ph ph-eye"></i>
                                                    </a>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="border border-neutral-30 px-12 py-7 rounded-8 text-12 fw-semibold text-neutral-400 bg-neutral-20">Preview unavailable</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </div>
                                </c:when>
                                <c:when test="${activity.assessmentActivity}">
                                    <c:set var="assessmentItem" value="${activity.assessment}"/>
                                    <div class="gape-student-activity-row">
                                        <span class="gape-student-activity-icon ${assessmentItem.softClass} text-22">
                                            <i class="${assessmentItem.iconClass}"></i>
                                        </span>
                                        <div class="min-w-0">
                                            <div class="d-flex align-items-center gap-8 flex-wrap mb-5">
                                                <h6 class="text-15 fw-semibold text-neutral-800 mb-0"><c:out value="${assessmentItem.title}"/></h6>
                                                <span class="${assessmentItem.stateBadgeClass} px-10 py-5 rounded-pill text-12"><c:out value="${assessmentItem.stateLabel}"/></span>
                                                <span class="bg-neutral-20 text-neutral-600 px-10 py-5 rounded-pill text-12"><c:out value="${assessmentItem.typeLabel}"/></span>
                                            </div>
                                            <div class="d-flex align-items-center gap-8 flex-wrap text-12 text-neutral-500">
                                                <span><i class="ph ph-seal-question me-4"></i><c:out value="${assessmentItem.questionCountLabel}"/></span>
                                                <span><i class="ph ph-repeat me-4"></i><c:out value="${assessmentItem.attemptsLimitLabel}"/> attempts</span>
                                                <span data-gape-datetime-display><i class="ph ph-clock me-4"></i><c:out value="${assessmentItem.availabilityLabel}"/></span>
                                            </div>
                                        </div>
                                        <div class="d-flex align-items-center justify-content-end gap-8">
                                            <form action="${pageContext.request.contextPath}/student/assessments/${assessmentItem.id}/start" method="post" class="m-0">
                                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                <button type="submit" class="gape-student-card-icon-button gape-student-card-icon-button--request" aria-label="Open assessment" title="Open assessment">
                                                    <i class="ph ph-arrow-right"></i>
                                                </button>
                                            </form>
                                        </div>
                                    </div>
                                </c:when>
                            </c:choose>
                        </c:forEach>
                        <c:if test="${empty blockActivities}">
                            <div class="gape-student-activity-row">
                                <span class="gape-student-activity-icon bg-neutral-30 text-neutral-500 text-22"><i class="ph ph-folder-open"></i></span>
                                <div class="min-w-0">
                                    <span class="text-13 text-neutral-500">No visible items in this block.</span>
                                </div>
                                <span></span>
                            </div>
                        </c:if>
                    </div>
                </article>
            </c:forEach>

            <c:if test="${empty contentBlocks}">
                <div class="gape-student-empty text-center px-20 py-32">
                    <span class="gape-student-icon gape-student-soft-blue text-26 mb-14"><i class="ph ph-stack"></i></span>
                    <h5 class="text-16 fw-semibold text-neutral-800 mb-6">No visible structure</h5>
                    <p class="text-13 text-neutral-500 mb-0">
                        <c:choose>
                            <c:when test="${classGroupItem.activeEnrollment}">Learning blocks appear here when published.</c:when>
                            <c:otherwise>Enroll in this class group to see its learning structure.</c:otherwise>
                        </c:choose>
                    </p>
                </div>
            </c:if>
        </div>
    </section>
</section>

<script>
    document.addEventListener('DOMContentLoaded', function () {
        function showContentThumbnailFallback(image) {
            if (!image) {
                return;
            }
            var thumbnail = image.closest('[data-content-thumbnail]');
            var fallback = thumbnail ? thumbnail.nextElementSibling : null;
            if (thumbnail) {
                thumbnail.classList.add('d-none');
            }
            if (fallback && fallback.hasAttribute('data-content-thumbnail-fallback')) {
                fallback.classList.remove('d-none');
            }
        }

        document.querySelectorAll('[data-content-thumbnail] img').forEach(function (image) {
            image.addEventListener('error', function () {
                showContentThumbnailFallback(image);
            }, { once: true });
            if (image.dataset.thumbnailFailed === 'true') {
                showContentThumbnailFallback(image);
            }
        });
    });
</script>
<%@ include file="/WEB-INF/fragments/student-dashboard-end.jspf" %>
