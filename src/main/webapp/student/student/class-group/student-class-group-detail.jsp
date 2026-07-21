<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    request.setAttribute("activeMenu", "class-groups");
    request.setAttribute("pageTitle", request.getAttribute("studentPageTitle"));
%>
<%@ include file="/WEB-INF/fragments/student-dashboard-start.jspf" %>

<section id="student-class-group-overview" class="gape-student-course-visual-stack gape-student-class-group-detail-hero px-24 py-24 mb-24"
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
                        <c:set var="studentGradeSheetEventHref" value="/student/class-groups/${classGroup.id}#student-class-group-grade-sheet" />
                        <c:choose>
                            <c:when test="${not empty studentClassGroupGradeSheet}">
                                <span class="gape-student-content-event-count-anchor">
                                    <button type="button" class="gape-student-card-icon-button" data-bs-toggle="modal" data-bs-target="#studentClassGroupGradeSheetModal" aria-label="View class group grade sheet" title="View class group grade sheet">
                                        <i class="ph ph-table"></i>
                                    </button>
                                    <c:if test="${not empty studentEventUnreadByHref[studentGradeSheetEventHref]}">
                                        <span class="gape-student-pending-corner-badge" data-student-event-marker data-student-event-href="${studentGradeSheetEventHref}" title="Your grade is available">1</span>
                                    </c:if>
                                </span>
                            </c:when>
                            <c:otherwise>
                                <span class="gape-student-card-icon-button gape-student-card-icon-button--muted" aria-label="Class group grade sheet unavailable" title="Class group grade sheet unavailable"><i class="ph ph-table"></i></span>
                            </c:otherwise>
                        </c:choose>
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
                <span class="text-12 text-neutral-500 d-block mb-4">Schedule</span>
                <span class="text-14 fw-semibold text-neutral-800"><c:out value="${classGroup.modalityLabel}"/> &middot; <c:out value="${classGroup.shift}"/></span>
            </div>
            <div class="gape-student-class-group-metric">
                <span class="text-12 text-neutral-500 d-block mb-4">Dates</span>
                <span class="text-14 fw-semibold text-neutral-800" data-gape-datetime-display><c:out value="${classGroup.dateRangeLabel}"/></span>
            </div>
        </div>
</section>

<c:if test="${not empty studentClassGroupGradeSheet}">
    <c:set var="studentGradeDoc" value="${studentClassGroupGradeSheet.document}"/>
    <div class="modal fade gape-student-grade-sheet-modal" id="studentClassGroupGradeSheetModal" tabindex="-1" aria-labelledby="studentClassGroupGradeSheetTitle" aria-hidden="true" data-student-event-read-href="/student/class-groups/${classGroup.id}#student-class-group-grade-sheet">
        <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
            <div class="modal-content rounded-12 border-0">
                <div class="modal-header border-neutral-30">
                    <div>
                        <h5 class="modal-title text-18 fw-semibold" id="studentClassGroupGradeSheetTitle"><c:out value="${studentClassGroupGradeSheet.title}"/></h5>
                        <span class="text-13 text-neutral-500">Class group grade sheet &middot; <c:out value="${studentClassGroupGradeSheet.classGroupCode}"/></span>
                    </div>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <div class="d-flex align-items-center gap-8 flex-wrap mb-18">
                        <span class="${studentClassGroupGradeSheet.stateBadgeClass} px-12 py-7 rounded-pill text-12"><c:out value="${studentClassGroupGradeSheet.stateLabel}"/></span>
                        <span class="text-13 text-neutral-500">Scale: <c:out value="${studentClassGroupGradeSheet.maxGrade}"/></span>
                        <span class="text-13 text-neutral-500">Passing grade: <c:out value="${studentClassGroupGradeSheet.passingGrade}"/></span>
                    </div>
                    <div class="aac-grade-doc cd-grade-sheet-doc">
                        <div class="aac-grade-doc__head">
                            <div class="aac-grade-doc__posted">Published on: ___ / ___ / _____</div>
                            <div class="aac-grade-doc__institution">
                                <div class="aac-grade-doc__media"><span class="aac-grade-doc__placeholder"><i class="${studentGradeDoc.fallbackIconClass}"></i></span></div>
                                <div class="aac-grade-doc__brand" title="<c:out value='${studentGradeDoc.contextTitle}'/>"><c:out value="${studentGradeDoc.contextHtml}" escapeXml="false"/></div>
                            </div>
                            <div class="aac-grade-doc__entity"><strong><c:out value="${studentGradeDoc.entityLabel}"/>:</strong> <c:out value="${studentGradeDoc.entityName}"/></div>
                            <h3><c:out value="${studentGradeDoc.title}"/></h3>
                            <div class="aac-grade-doc__period" data-gape-datetime-display>Academic period: <c:out value="${studentGradeDoc.periodLabel}"/></div>
                        </div>
                        <div class="aac-grade-doc__table-wrap" style="--aac-grade-doc-columns: ${studentGradeDoc.variableColumnCount}; --aac-grade-doc-min-width: ${studentGradeDoc.tableMinWidthPx}px;">
                            <table class="aac-grade-doc__table">
                                <colgroup>
                                    <col class="aac-grade-doc__number-col"><col class="aac-grade-doc__name-col">
                                    <c:choose><c:when test="${empty studentGradeDoc.columns}"><col class="aac-grade-doc__assessment-col"></c:when><c:otherwise><c:forEach var="column" items="${studentGradeDoc.columns}"><col class="aac-grade-doc__assessment-col"></c:forEach></c:otherwise></c:choose>
                                    <col class="aac-grade-doc__final-col">
                                </colgroup>
                                <thead><tr><th>ID</th><th>Student name</th><c:choose><c:when test="${empty studentGradeDoc.columns}"><th>Assessments</th></c:when><c:otherwise><c:forEach var="column" items="${studentGradeDoc.columns}"><th title="<c:out value='${column.headerTitle}'/>"><span class="gape-acronym-token"><c:out value="${column.headerLabel}"/></span></th></c:forEach></c:otherwise></c:choose><th>Final grade</th></tr></thead>
                                <tbody><c:forEach var="row" items="${studentGradeDoc.rows}"><tr><td><c:out value="${row.studentId}"/></td><td class="text-start"><c:out value="${row.studentName}"/></td><c:choose><c:when test="${empty studentGradeDoc.columns}"><td class="aac-grade-doc__grade">-</td></c:when><c:otherwise><c:forEach var="value" items="${row.values}"><td class="aac-grade-doc__grade"><c:out value="${value}"/></td></c:forEach></c:otherwise></c:choose><td class="aac-grade-doc__final"><c:out value="${row.finalGrade}"/></td></tr></c:forEach><c:if test="${empty studentGradeDoc.rows}"><tr><td colspan="${studentGradeDoc.documentColumnCount}" class="py-24 text-center text-neutral-500"><c:out value="${studentGradeDoc.emptyMessage}"/></td></tr></c:if></tbody>
                            </table>
                        </div>
                        <c:if test="${studentGradeDoc.hasAlert}"><div class="aac-grade-doc__alert"><c:out value="${studentGradeDoc.alert}"/></div></c:if>
                    </div>
                </div>
                <div class="modal-footer border-neutral-30 gape-student-grade-sheet-modal__footer"><a class="gape-student-grade-sheet-action gape-student-grade-sheet-action--download" href="${pageContext.request.contextPath}/learning/grades/sheets/${studentClassGroupGradeSheet.id}/download?format=pdf"><i class="ph ph-download-simple" aria-hidden="true"></i><span>Download</span></a><button type="button" class="gape-student-grade-sheet-action gape-student-grade-sheet-action--close" data-bs-dismiss="modal">Close</button></div>
            </div>
        </div>
    </div>
</c:if>

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
                    <div class="gape-student-structure-block__header px-22 py-22">
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
                                        <h5 class="text-20 fw-semibold text-neutral-800 mb-0"><c:out value="${block.name}"/></h5>
                                    </div>
                                    <p class="text-14 text-neutral-500 mb-0"><c:out value="${block.description}"/></p>
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
                                    <c:set var="studentLessonEventHref" value="/student/class-groups/${classGroup.id}#student-lesson-${lesson.id}"/>
                                    <div id="student-lesson-${lesson.id}" class="gape-student-activity-row">
                                        <span class="gape-student-content-event-count-anchor">
                                            <c:if test="${not empty studentEventUnreadByHref[studentLessonEventHref]}"><span class="gape-student-pending-corner-badge" data-student-event-marker data-student-event-href="${studentLessonEventHref}" title="Lesson in progress">1</span></c:if>
                                            <span class="gape-student-activity-icon ${lesson.typeBadgeClass} text-22"><i class="${lesson.typeIconClass}"></i></span>
                                        </span>
                                        <div class="min-w-0">
                                            <div class="gape-student-content-meta-row mb-5">
                                                <h6 class="gape-student-content-title text-15 fw-semibold text-neutral-800 mb-0" title="<c:out value='${lesson.title}'/>"><c:out value="${lesson.title}"/></h6>
                                                <div class="gape-student-content-meta-state">
                                                    <span class="${lesson.stateBadgeClass} px-10 py-5 rounded-pill text-12"><c:out value="${lesson.stateLabel}"/></span>
                                                </div>
                                                <div class="gape-student-content-meta-type" aria-hidden="true"></div>
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
                                            <c:choose>
                                            <c:when test="${lesson.active and (lesson.online or lesson.hybrid) and lesson.hasMeetingLink}">
                                                <a href="${pageContext.request.contextPath}/student/lessons/${lesson.id}/access" target="_blank" rel="noopener noreferrer" class="gape-student-card-icon-button gape-student-card-icon-button--meeting" aria-label="Open meeting" title="Open meeting">
                                                    <i class="ph ph-video-camera"></i>
                                                </a>
                                            </c:when>
                                            <c:when test="${lesson.scheduled}">
                                                <span class="gape-student-card-icon-button gape-student-card-icon-button--muted" aria-label="Lesson pending" title="Lesson pending">
                                                    <i class="ph ph-clock"></i>
                                                </span>
                                            </c:when>
                                            </c:choose>
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
                                            <div class="gape-student-content-meta-row mb-5">
                                                <h6 class="gape-student-content-title text-15 fw-semibold text-neutral-800 mb-0" title="<c:out value='${content.title}'/>"><c:out value="${content.title}"/></h6>
                                                <div class="gape-student-content-meta-state">
                                                    <span class="${content.mandatoryBadgeClass} px-10 py-5 rounded-pill text-12"><c:out value="${content.mandatoryLabel}"/></span>
                                                </div>
                                                <div class="gape-student-content-meta-type" aria-hidden="true"></div>
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
                                    <c:set var="studentAssessmentEventHref" value="/student/class-groups/${classGroup.id}#student-assessment-${assessmentItem.id}"/>
                                    <c:set var="studentAssessmentEnrollmentState" value="${studentAssessmentEnrollmentStateByAssessment[assessmentItem.id]}"/>
                                    <c:set var="studentAssessmentEnrollmentActive" value="${assessmentItem.automaticEnrollment or studentAssessmentEnrollmentState eq 'ACTIVE'}"/>
                                    <c:set var="studentAssessmentEnrollmentPending" value="${studentAssessmentEnrollmentState eq 'PENDING'}"/>
                                    <c:set var="studentAssessmentAttempts" value="${studentAssessmentAttemptsByAssessment[assessmentItem.id]}"/>
                                    <c:set var="studentAssessmentLatestSubmittedAttempt" value="${null}"/>
                                    <c:set var="studentAssessmentHasSubmittedAttempt" value="${false}"/>
                                    <c:forEach var="candidateAttempt" items="${studentAssessmentAttempts}">
                                        <c:if test="${not studentAssessmentHasSubmittedAttempt and (candidateAttempt.submitted or candidateAttempt.corrected)}">
                                            <c:set var="studentAssessmentLatestSubmittedAttempt" value="${candidateAttempt}"/>
                                            <c:set var="studentAssessmentHasSubmittedAttempt" value="${true}"/>
                                        </c:if>
                                    </c:forEach>
                                    <c:set var="studentAssessmentCanRetry" value="${studentAssessmentEnrollmentActive and (assessmentItem.attemptsLimit == null or fn:length(studentAssessmentAttempts) lt assessmentItem.attemptsLimit) and not assessmentItem.completed}"/>
                                    <div id="student-assessment-${assessmentItem.id}" class="gape-student-activity-row">
                                        <span class="gape-student-content-event-count-anchor">
                                            <c:if test="${not empty studentEventUnreadByHref[studentAssessmentEventHref]}"><span class="gape-student-pending-corner-badge" data-student-event-marker data-student-event-href="${studentAssessmentEventHref}" title="Assessment requires attention">1</span></c:if>
                                            <span class="gape-student-activity-icon ${assessmentItem.softClass} text-22"><i class="${assessmentItem.iconClass}"></i></span>
                                        </span>
                                        <div class="min-w-0">
                                            <div class="gape-student-content-meta-row mb-5">
                                                <h6 class="gape-student-content-title text-15 fw-semibold text-neutral-800 mb-0" title="<c:out value='${assessmentItem.title}'/>"><c:out value="${assessmentItem.title}"/></h6>
                                                <div class="gape-student-content-meta-state">
                                                    <span class="${assessmentItem.stateBadgeClass} px-10 py-5 rounded-pill text-12"><c:out value="${assessmentItem.stateLabel}"/></span>
                                                </div>
                                                <div class="gape-student-content-meta-type">
                                                    <span class="bg-neutral-20 text-neutral-600 px-10 py-5 rounded-pill text-12"><c:out value="${assessmentItem.typeLabel}"/></span>
                                                </div>
                                            </div>
                                            <div class="d-flex align-items-center gap-8 flex-wrap text-12 text-neutral-500">
                                                <span><i class="ph ph-seal-question me-4"></i><c:out value="${assessmentItem.questionCountLabel}"/></span>
                                                <span><i class="ph ph-repeat me-4"></i><c:out value="${assessmentItem.attemptsLimitLabel}"/> attempts</span>
                                                <span data-gape-datetime-display><i class="ph ph-clock me-4"></i><c:out value="${assessmentItem.availabilityLabel}"/></span>
                                            </div>
                                        </div>
                                        <div class="d-flex align-items-center justify-content-end gap-8">
                                            <c:choose>
                                                <c:when test="${assessmentItem.completed and studentAssessmentHasSubmittedAttempt}">
                                                    <a href="${pageContext.request.contextPath}/student/assessments/attempts/${studentAssessmentLatestSubmittedAttempt.id}/result" class="gape-student-card-icon-button" aria-label="View submitted attempt" title="View submitted attempt">
                                                        <i class="ph ph-eye"></i>
                                                    </a>
                                                </c:when>
                                                <c:when test="${studentAssessmentHasSubmittedAttempt}">
                                                    <a href="${pageContext.request.contextPath}/student/assessments/attempts/${studentAssessmentLatestSubmittedAttempt.id}/result" class="gape-student-card-icon-button" aria-label="View submitted attempt" title="View submitted attempt">
                                                        <i class="ph ph-eye"></i>
                                                    </a>
                                                    <c:if test="${studentAssessmentCanRetry}">
                                                        <form action="${pageContext.request.contextPath}/student/assessments/${assessmentItem.id}/start" method="post" class="m-0">
                                                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                            <button type="submit" class="gape-student-card-icon-button gape-student-card-icon-button--request" aria-label="Start another attempt" title="Start another attempt">
                                                                <i class="ph ph-repeat"></i>
                                                            </button>
                                                        </form>
                                                    </c:if>
                                                </c:when>
                                                <c:when test="${studentAssessmentEnrollmentActive and not assessmentItem.completed}">
                                                    <form action="${pageContext.request.contextPath}/student/assessments/${assessmentItem.id}/start" method="post" class="m-0">
                                                        <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                        <button type="submit" class="gape-student-card-icon-button gape-student-card-icon-button--request" aria-label="Start assessment" title="Start assessment">
                                                            <i class="ph ph-arrow-right"></i>
                                                        </button>
                                                    </form>
                                                </c:when>
                                                <c:when test="${studentAssessmentEnrollmentPending}">
                                                    <span class="gape-student-card-icon-button gape-student-card-icon-button--muted" aria-label="Assessment enrollment pending" title="Assessment enrollment pending">
                                                        <i class="ph ph-clock"></i>
                                                    </span>
                                                </c:when>
                                                <c:when test="${not assessmentItem.completed}">
                                                    <form action="${pageContext.request.contextPath}/student/assessments/${assessmentItem.id}/enroll" method="post" class="m-0">
                                                        <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                        <button type="submit" class="gape-student-card-icon-button gape-student-card-icon-button--request" aria-label="Enroll in assessment" title="Enroll in assessment">
                                                            <i class="ph ph-user-plus"></i>
                                                        </button>
                                                    </form>
                                                </c:when>
                                            </c:choose>
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

<script>
    document.addEventListener('DOMContentLoaded', function () {
        var gradeSheetModal = document.getElementById('studentClassGroupGradeSheetModal');
        if (gradeSheetModal) {
            /* The student shell has a positioned dashboard layout. Bootstrap
               backdrops live at document level, so keeping this modal inside
               that stacking context can leave the dark backdrop above the
               dialog. Promote it before Bootstrap initialises it. */
            if (gradeSheetModal.parentElement !== document.body) {
                document.body.appendChild(gradeSheetModal);
            }
            var clearStudentTransientOverlays = function () {
                document.querySelectorAll('.overlay.show-overlay, .side-overlay.show, .student-overlay-sidebar.show').forEach(function (overlay) {
                    overlay.classList.remove('show-overlay', 'show');
                });
            };
            gradeSheetModal.addEventListener('show.bs.modal', clearStudentTransientOverlays);
            gradeSheetModal.addEventListener('hidden.bs.modal', function () {
                document.querySelectorAll('.modal-backdrop').forEach(function (backdrop) { backdrop.remove(); });
                document.body.classList.remove('modal-open');
                document.body.style.removeProperty('overflow');
                document.body.style.removeProperty('padding-right');
            });
        }
        if (window.location.hash === '#student-class-group-grade-sheet') {
            if (gradeSheetModal && window.bootstrap && window.bootstrap.Modal) {
                window.bootstrap.Modal.getOrCreateInstance(gradeSheetModal).show();
            }
        }
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
