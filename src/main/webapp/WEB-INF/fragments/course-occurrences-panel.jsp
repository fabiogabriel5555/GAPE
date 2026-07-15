<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<section class="cd-surface px-22 py-22 mb-20">
    <div class="gape-course-structure-panel">
        <div class="px-18 py-18" data-course-detail-sort-root>
            <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap mb-16">
                <div>
                    <h3 class="text-18 fw-semibold text-neutral-800 mb-4">Occurrences</h3>
                    <span class="text-13 text-neutral-500">Academic years are calculated from the configured course periods.</span>
                </div>
                <div class="d-flex align-items-center gap-12 flex-wrap">
                    <div class="dropdown">
                        <button type="button"
                                class="gape-filter-toggle border-neutral-30 border px-20 py-12 rounded-12 fw-semibold text-neutral-700 hover-bg-main-50 transition-03 bg-white d-flex align-items-center gap-8"
                                data-bs-toggle="dropdown"
                                data-bs-auto-close="outside"
                                data-course-detail-sort-toggle
                                aria-expanded="false">
                            <i class="ph ph-sort-ascending"></i>Sort by
                        </button>
                        <ul class="dropdown-menu dropdown-menu-end rounded-12">
                            <li><button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-course-detail-sort-option data-sort-field="name" data-sort-normal="asc" data-sort-state="none" aria-pressed="false"><span>Name</span><span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span></button></li>
                            <li><button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-course-detail-sort-option data-sort-field="date" data-sort-normal="desc" data-sort-state="none" aria-pressed="false"><span>Date</span><span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--reverse"></i></span></button></li>
                            <li><button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-course-detail-sort-option data-sort-field="status" data-sort-normal="asc" data-sort-state="none" aria-pressed="false"><span>State</span><span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span></button></li>
                        </ul>
                    </div>
                    <c:if test="${canManageCourseChildren}">
                        <button type="button" class="cd-primary-button cd-section-create-button" data-bs-toggle="modal" data-bs-target="#newCourseOccurrenceModal">
                            <i class="ph ph-plus-circle me-8"></i>New Occurrence
                        </button>
                    </c:if>
                </div>
            </div>

            <div class="gape-structure-list-header gape-occurrence-list-header">
                <span>Academic year</span>
                <span>Periods</span>
                <span>State</span>
                <span class="text-end">Actions</span>
            </div>

            <c:set var="completedOccurrenceCount" value="0"/>
            <c:forEach var="occurrence" items="${courseOccurrences}">
                <c:if test="${occurrence.stateValue eq 'completed'}"><c:set var="completedOccurrenceCount" value="${completedOccurrenceCount + 1}"/></c:if>
            </c:forEach>

            <div class="d-flex flex-column gap-12">
                <div class="d-flex flex-column gap-12" data-course-detail-sort-list>
                    <c:forEach var="occurrence" items="${courseOccurrences}" varStatus="occurrenceLoop">
                        <c:if test="${occurrence.stateValue ne 'completed'}">
                            <article class="gape-structure-node border border-neutral-30 rounded-8 px-18 py-16 bg-white"
                                     data-course-detail-sort-row
                                     data-sort-index="${occurrenceLoop.index}"
                                     data-sort-name="<c:out value='${occurrence.academicYearLabel}'/>"
                                     data-sort-date="${fn:replace(occurrence.startsAtValue, '-', '')}"
                                     data-sort-status="<c:out value='${occurrence.stateLabel}'/>">
                                <div class="gape-structure-row gape-occurrence-row">
                                    <div class="d-flex align-items-center gap-12 min-w-0">
                                        <span class="bg-success-50 text-success-600 cd-mode-icon text-20 line-height-1"><i class="ph ph-calendar-dots"></i></span>
                                        <div class="min-w-0"><span class="fw-medium text-14 text-neutral-700 d-block"><c:out value="${occurrence.academicYearLabel}"/></span><span class="gape-node-meta text-12"><span>Course occurrence</span></span></div>
                                    </div>
                                    <div><span class="cd-element-count"><c:out value="${occurrence.periodCount}"/></span></div>
                                    <div><span class="${occurrence.stateBadgeClass} px-14 py-6 border-neutral-30 border rounded-pill text-13"><c:out value="${occurrence.stateLabel}"/></span></div>
                                    <div class="d-flex justify-content-end"><button type="button" class="gape-tree-toggle text-20 text-neutral-500 hover-text-main-600" data-gape-tree-toggle="occurrencePeriods${occurrence.id}" data-gape-open-title="Hide periods" data-gape-closed-title="Show periods" aria-expanded="false" aria-controls="occurrencePeriods${occurrence.id}" aria-label="Show periods" title="Show periods"><i class="ph ph-caret-down" aria-hidden="true"></i></button></div>
                                </div>
                                <div id="occurrencePeriods${occurrence.id}" class="gape-occurrence-periods-panel d-none"><div class="d-flex flex-column"><c:forEach var="period" items="${occurrence.periods}"><article class="gape-occurrence-period-card"><div class="gape-structure-row gape-occurrence-row gape-occurrence-period-row"><div class="fw-medium text-14 text-neutral-700"><c:out value="${period.label}"/></div><div class="text-13 text-neutral-500" data-gape-datetime-display><c:out value="${period.dateRangeLabel}"/></div><div><span class="${period.stateBadgeClass} px-12 py-5 border-neutral-30 border rounded-pill text-12"><c:out value="${period.stateLabel}"/></span></div><div aria-hidden="true"></div></div></article></c:forEach></div></div>
                            </article>
                        </c:if>
                    </c:forEach>
                </div>

                <c:if test="${completedOccurrenceCount gt 0}">
                    <div class="gape-completed-occurrences-divider" aria-hidden="true"><span>Completed occurrences</span></div>
                    <article class="gape-structure-node gape-completed-occurrences-node border rounded-8 px-18 py-16 bg-white">
                        <div class="gape-structure-row gape-occurrence-row">
                            <div class="d-flex align-items-center gap-12 min-w-0"><span class="bg-danger-50 text-danger-600 cd-mode-icon text-20 line-height-1"><i class="ph ph-archive" aria-hidden="true"></i></span><div class="min-w-0"><span class="fw-medium text-14 text-neutral-700 d-block">Completed occurrences</span><span class="gape-node-meta text-12"><span>Past academic years</span></span></div></div>
                            <div><span class="cd-element-count"><c:out value="${completedOccurrenceCount}"/></span></div>
                            <div><span class="bg-neutral-20 text-neutral-600 px-14 py-6 border-neutral-30 border rounded-pill text-13">Completed</span></div>
                            <div class="d-flex justify-content-end"><button type="button" class="gape-tree-toggle text-20 text-neutral-500 hover-text-main-600" data-gape-tree-toggle="completedCourseOccurrences" data-gape-open-title="Hide completed occurrences" data-gape-closed-title="Show completed occurrences" aria-expanded="false" aria-controls="completedCourseOccurrences" aria-label="Show completed occurrences" title="Show completed occurrences"><i class="ph ph-caret-down" aria-hidden="true"></i></button></div>
                        </div>
                        <div id="completedCourseOccurrences" class="gape-completed-occurrences-panel d-none"><div class="gape-completed-occurrences-content d-flex flex-column gap-10" data-course-detail-sort-list><c:forEach var="occurrence" items="${courseOccurrences}" varStatus="completedOccurrenceLoop"><c:if test="${occurrence.stateValue eq 'completed'}"><article class="gape-structure-node border border-neutral-30 rounded-8 px-16 py-12 bg-white" data-course-detail-sort-row data-sort-index="${completedOccurrenceLoop.index}" data-sort-name="<c:out value='${occurrence.academicYearLabel}'/>" data-sort-date="${fn:replace(occurrence.startsAtValue, '-', '')}" data-sort-status="<c:out value='${occurrence.stateLabel}'/>"><div class="gape-structure-row gape-occurrence-row"><div class="d-flex align-items-center gap-10 min-w-0"><span class="bg-neutral-20 text-neutral-600 cd-mode-icon text-18 line-height-1"><i class="ph ph-calendar-check" aria-hidden="true"></i></span><div class="min-w-0"><span class="fw-medium text-14 text-neutral-700 d-block"><c:out value="${occurrence.academicYearLabel}"/></span><span class="gape-node-meta text-12"><span>Course occurrence</span></span></div></div><div><span class="cd-element-count"><c:out value="${occurrence.periodCount}"/></span></div><div><span class="bg-neutral-20 text-neutral-600 px-12 py-5 border-neutral-30 border rounded-pill text-12"><c:out value="${occurrence.stateLabel}"/></span></div><div class="d-flex justify-content-end"><button type="button" class="gape-tree-toggle text-20 text-neutral-500 hover-text-main-600" data-gape-tree-toggle="completedOccurrencePeriods${occurrence.id}" data-gape-open-title="Hide periods" data-gape-closed-title="Show periods" aria-expanded="false" aria-controls="completedOccurrencePeriods${occurrence.id}" aria-label="Show periods" title="Show periods"><i class="ph ph-caret-down" aria-hidden="true"></i></button></div></div><div id="completedOccurrencePeriods${occurrence.id}" class="gape-occurrence-periods-panel d-none"><div class="d-flex flex-column"><c:forEach var="period" items="${occurrence.periods}"><article class="gape-occurrence-period-card"><div class="gape-structure-row gape-occurrence-row gape-occurrence-period-row"><div class="fw-medium text-14 text-neutral-700"><c:out value="${period.label}"/></div><div class="text-13 text-neutral-500" data-gape-datetime-display><c:out value="${period.dateRangeLabel}"/></div><div><span class="${period.stateBadgeClass} px-12 py-5 border-neutral-30 border rounded-pill text-12"><c:out value="${period.stateLabel}"/></span></div><div aria-hidden="true"></div></div></article></c:forEach></div></div></article></c:if></c:forEach></div></div>
                    </article>
                </c:if>

                <c:if test="${empty courseOccurrences}"><div class="border border-neutral-30 rounded-8 px-18 py-28 text-center text-14 text-neutral-500">No occurrences are available for this course yet.</div></c:if>
            </div>
        </div>
    </div>
</section>

<c:if test="${canManageCourseChildren}">
    <div class="modal fade" id="newCourseOccurrenceModal" tabindex="-1" aria-labelledby="newCourseOccurrenceModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered"><div class="modal-content rounded-12 border-0"><div class="modal-header border-neutral-30"><h5 class="modal-title text-18 fw-semibold" id="newCourseOccurrenceModalLabel">New Occurrence</h5><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div>
            <form id="courseOccurrenceForm" action="${pageContext.request.contextPath}${courseBasePath}/${course.id}/occurrences" method="post" novalidate data-course-live-form data-course-live-panel="occurrences">
                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}"><input type="hidden" name="returnTo" value="${currentReturnTo}#course-occurrences">
                <div class="modal-body"><p class="text-14 text-neutral-600 mb-20">Choose the academic year. Its year span, dates and state are calculated automatically from the configured course periods.</p>
                    <c:forEach var="template" items="${coursePeriodTemplates}"><input type="hidden" data-course-period-template data-start-month="${template.startsMonth}" data-start-day="${template.startsDay}" data-end-month="${template.endsMonth}" data-end-day="${template.endsDay}"></c:forEach>
                    <c:forEach var="occurrence" items="${courseOccurrences}"><span hidden data-existing-course-occurrence data-reference-year="${occurrence.referenceYear}" data-start-date="${occurrence.startsAtValue}" data-end-date="${occurrence.endsAtValue}"></span></c:forEach>
                    <input id="courseOccurrenceReferenceYear" name="referenceYear" type="hidden" required>
                    <div class="mb-20"><span class="fw-medium text-base text-neutral-800 d-block mb-12" id="courseOccurrenceAcademicYearLabel">Academic year</span><div class="gape-academic-year-picker" id="courseOccurrenceAcademicYearPicker" aria-labelledby="courseOccurrenceAcademicYearLabel"><div class="gape-academic-year-picker__header"><button type="button" class="aac-icon-button" data-academic-year-previous aria-label="Show previous academic years" title="Show previous academic years"><i class="ph ph-caret-left" aria-hidden="true"></i></button><span class="fw-semibold text-neutral-700" id="courseOccurrenceAcademicYearPickerTitle">Academic years</span><button type="button" class="aac-icon-button" data-academic-year-next aria-label="Show next academic years" title="Show next academic years"><i class="ph ph-caret-right" aria-hidden="true"></i></button></div><div class="gape-academic-year-picker__grid" data-academic-year-grid role="listbox" aria-label="Available academic years"></div></div></div>
                    <div><span class="fw-medium text-base text-neutral-800 d-block mb-12">Selected academic year</span><output id="courseOccurrenceCodePreview" class="gape-academic-year-selection d-block text-14 text-neutral-600">Select an academic year</output></div><div id="courseOccurrenceDateValidation" class="gape-course-occurrence-validation mt-16" role="alert" hidden></div>
                </div>
                <div class="modal-footer border-neutral-30"><button type="button" class="cd-outline-button" data-bs-dismiss="modal">Cancel</button><button type="submit" class="cd-primary-button border-0"><i class="ph ph-plus-circle me-8"></i>Create Occurrence</button></div>
            </form>
        </div></div>
    </div>
</c:if>
