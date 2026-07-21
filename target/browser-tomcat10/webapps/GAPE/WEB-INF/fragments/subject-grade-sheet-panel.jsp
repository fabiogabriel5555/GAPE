<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<section class="cd-surface px-22 py-22 mb-20 gape-subject-detail-surface" id="subject-grade-sheets">
    <div class="gape-subject-detail-panel gape-course-structure-panel">
        <div class="px-18 py-18 gape-subject-panel-content" data-subject-detail-sort-root>
            <div class="gape-subject-panel-header">
                <div class="gape-subject-panel-heading">
                    <h3 class="text-18 fw-semibold text-neutral-800 mb-4">Grade Sheets</h3>
                    <span class="text-13 text-neutral-500">Grade sheets grouped by the occurrence of each associated course.</span>
                </div>
                <div class="gape-subject-panel-actions">
                    <div class="dropdown">
                        <button type="button"
                                class="gape-filter-toggle border-neutral-30 border px-20 py-12 rounded-12 fw-semibold text-neutral-700 hover-bg-main-50 transition-03 bg-white d-flex align-items-center gap-8"
                                data-bs-toggle="dropdown"
                                data-bs-auto-close="outside"
                                data-subject-detail-sort-toggle
                                aria-expanded="false"><i class="ph ph-sort-ascending"></i>Sort by</button>
                        <ul class="dropdown-menu dropdown-menu-end rounded-12">
                            <li><button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-subject-detail-sort-option data-sort-field="name" data-sort-normal="asc" data-sort-state="none" aria-pressed="false"><span>Name</span><span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span></button></li>
                            <li><button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-subject-detail-sort-option data-sort-field="date" data-sort-normal="desc" data-sort-state="none" aria-pressed="false"><span>Date</span><span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--reverse"></i></span></button></li>
                            <li><button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-subject-detail-sort-option data-sort-field="status" data-sort-normal="asc" data-sort-state="none" aria-pressed="false"><span>State</span><span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span></button></li>
                        </ul>
                    </div>
                </div>
            </div>

            <div class="gape-structure-list-header gape-subject-grade-sheet-list-header">
                <span>Course Occurrence</span><span>Course</span><span>Grade Sheets</span><span>State</span><span class="text-end">Actions</span>
            </div>

            <div class="d-flex flex-column gap-12">
                <div class="d-flex flex-column gap-12" data-subject-detail-sort-list>
                    <c:forEach var="subjectGradeGroup" items="${activeSubjectGradeSheetOccurrenceGroups}" varStatus="gradeSheetLoop">
                        <c:set var="subjectGradeSheetRowPrefix" value="subjectGradeSheetActive"/>
                        <c:set var="subjectGradeSheetRowIndex" value="${gradeSheetLoop.index}"/>
                        <%@ include file="/WEB-INF/fragments/subject-grade-sheet-occurrence-row.jspf" %>
                    </c:forEach>
                </div>

                <c:if test="${publishedSubjectGradeSheetCount gt 0}">
                    <div class="gape-published-grade-sheets-divider" aria-hidden="true"><span>Published Grade Sheets</span></div>
                    <article class="gape-structure-node gape-published-grade-sheets-node border rounded-8 px-18 py-16 bg-white">
                        <div class="gape-structure-row">
                            <div class="d-flex align-items-center gap-12 min-w-0"><span class="bg-success-50 text-success-600 cd-mode-icon text-20 line-height-1"><i class="ph ph-check-circle" aria-hidden="true"></i></span><div class="min-w-0"><span class="fw-medium text-14 text-neutral-700 d-block">Published Grade Sheets</span><span class="gape-node-meta text-12"><span>Published grade sheets</span></span></div></div>
                            <div aria-hidden="true"></div>
                            <div><span class="cd-element-count"><c:out value="${publishedSubjectGradeSheetCount}"/></span></div>
                            <div><span class="bg-success-50 text-success-600 px-14 py-6 border-neutral-30 border rounded-pill text-13">Published</span></div>
                            <div class="d-flex justify-content-end"><button type="button" class="gape-tree-toggle text-20 text-neutral-500 hover-text-main-600" data-gape-tree-toggle="publishedSubjectGradeSheets${subject.id}" data-gape-open-title="Hide published grade sheets" data-gape-closed-title="Show published grade sheets" aria-expanded="false" aria-controls="publishedSubjectGradeSheets${subject.id}" aria-label="Show published grade sheets" title="Show published grade sheets"><i class="ph ph-caret-down" aria-hidden="true"></i></button></div>
                        </div>
                        <div id="publishedSubjectGradeSheets${subject.id}" class="gape-published-grade-sheets-panel d-none">
                            <div class="gape-published-grade-sheets-content d-flex flex-column gap-12">
                                <div class="d-flex flex-column gap-12" data-subject-detail-sort-list>
                                    <c:forEach var="subjectGradeGroup" items="${publishedSubjectGradeSheetOccurrenceGroups}" varStatus="gradeSheetLoop">
                                        <c:set var="subjectGradeSheetRowPrefix" value="subjectGradeSheetPublished"/>
                                        <c:set var="subjectGradeSheetRowIndex" value="${gradeSheetLoop.index}"/>
                                        <%@ include file="/WEB-INF/fragments/subject-grade-sheet-occurrence-row.jspf" %>
                                    </c:forEach>
                                </div>
                            </div>
                        </div>
                    </article>
                </c:if>

                <c:if test="${empty subjectGradeSheetOccurrenceGroups}">
                    <div class="border border-neutral-30 rounded-8 px-18 py-28 text-center text-14 text-neutral-500">No grade sheets found.</div>
                </c:if>
            </div>
        </div>
    </div>
</section>
