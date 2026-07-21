<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:forEach var="classGroup" items="${classGroups}" varStatus="classGroupLoop">
    <c:set var="canModifyClassGroupRow" value="${canModifyClassGroupById[classGroup.id]}" />
    <c:set var="canManageClassGroupStructureRow" value="${canManageClassGroupStructureById[classGroup.id]}" />
    <c:set var="classGroupEventCount" value="${classGroupEventCountById[classGroup.id]}" />
    <article class="gape-structure-node gape-class-group-node border border-neutral-30 rounded-8 px-16 py-12 bg-white"
             data-class-group-row
             data-sort-index="${classGroupListOffset + classGroupLoop.index}"
             data-sort-code="${fn:escapeXml(classGroup.code)}"
             data-sort-date="${classGroup.id}"
             data-sort-status="${fn:escapeXml(classGroup.stateLabel)}"
             data-class-group-completed="${classGroup.completed}"
             data-class-group-event-count="${classGroupEventCount}"
             data-group-organization-id="${classGroup.course.organizationId}"
             data-group-organization="${fn:escapeXml(classGroup.course.organizationName)}"
             data-group-organic-unit-id="${classGroup.course.organicUnitId}"
             data-group-organic-unit="${fn:escapeXml(classGroup.course.organicUnitLabel)}"
             data-group-course-id="${classGroup.courseId}"
             data-group-course="${fn:escapeXml(classGroup.courseName)}"
             data-group-context="${fn:escapeXml(classGroup.course.subjectManagementContextLabel)}"
             data-group-subject-id="${classGroup.subjectId}"
             data-group-subject="${fn:escapeXml(classGroup.subjectName)}"
             data-group-occurrence-id="${classGroup.courseOccurrenceId}"
             data-group-occurrence="${fn:escapeXml(classGroup.occurrenceLabel)} &mdash; ${fn:escapeXml(classGroup.subjectName)}"
             data-group-occurrence-range="${fn:escapeXml(classGroup.occurrenceDateRangeLabel)}"
             data-group-occurrence-state="${fn:escapeXml(classGroup.occurrenceStateLabel)}"
             data-group-occurrence-state-class="${fn:escapeXml(classGroup.occurrenceStateBadgeClass)}">
        <div class="gape-class-group-structure-row">
            <div class="d-flex align-items-start gap-12 min-w-0">
                <span class="gape-class-group-event-count-anchor">
                    <c:if test="${classGroupEventCount > 0}"><span class="gape-class-group-event-count-badge" title="Pending enrollment requests and uncorrected attempts"><c:out value="${classGroupEventCount}"/></span></c:if>
                    <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--table" role="img" aria-label="Class group" data-class-group-list-icon><i class="ph ph-users-three" aria-hidden="true"></i></span>
                </span>
                <div class="min-w-0">
                    <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}" class="fw-medium text-14 text-neutral-700 hover-text-main-600"><c:out value="${classGroup.code}"/></a>
                    <span class="gape-node-meta text-12"><span><c:out value="${classGroup.modalityLabel}"/></span><span><c:out value="${classGroup.shift}"/></span><span><c:out value="${classGroup.activeEnrollmentCount}"/> enrolled</span></span>
                </div>
            </div>
            <div class="text-13 text-neutral-500" title="<c:out value='${classGroup.contextGroupTitle}'/>"><c:out value="${classGroup.contextGroupHtml}" escapeXml="false"/></div>
            <div><span class="cd-element-count"><c:out value="${classGroup.blockCount}"/></span></div>
            <div><span class="${classGroup.stateBadgeClass} px-14 py-6 border-neutral-30 border rounded-pill text-13"><c:out value="${classGroup.stateLabel}"/></span></div>
            <div class="d-flex align-items-center gap-10 flex-wrap justify-content-end">
                <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}" class="text-20 text-neutral-500 hover-text-main-600" title="Detail" aria-label="Detail"><i class="ph ph-eye" aria-hidden="true"></i></a>
                <c:if test="${canModifyClassGroupRow}"><a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/edit" class="text-20 text-neutral-500 hover-text-main-600" title="Edit" aria-label="Edit"><i class="ph ph-pencil-simple-line" aria-hidden="true"></i></a></c:if>
                <c:if test="${canManageClassGroupStructureRow}"><button type="button" class="text-20 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" aria-label="Delete" data-bs-toggle="modal" data-bs-target="#deleteClassGroup${classGroup.id}"><i class="ph ph-trash" aria-hidden="true"></i></button></c:if>
            </div>
        </div>
        <c:if test="${canManageClassGroupStructureRow}">
            <div class="modal fade" id="deleteClassGroup${classGroup.id}" tabindex="-1" aria-hidden="true"><div class="modal-dialog modal-dialog-centered"><div class="modal-content rounded-12 border-0"><div class="modal-header border-neutral-30"><h5 class="modal-title text-18 fw-semibold">Delete Class Group</h5><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div><div class="modal-body"><p class="text-14 text-neutral-600 mb-0">This action removes <strong><c:out value="${classGroup.code}"/></strong> if it has no dependencies.</p></div><div class="modal-footer border-neutral-30"><button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button><form action="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/delete" method="post" class="m-0"><input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}"><button type="submit" class="gape-action-button gape-action-delete px-20 py-10 rounded-12 fw-semibold transition-03">Delete</button></form></div></div></div></div>
        </c:if>
    </article>
</c:forEach>
<c:if test="${empty classGroups}"><div class="border border-neutral-30 rounded-8 px-20 py-28 text-center text-14 text-neutral-500 bg-white">No class groups available in your context.</div></c:if>
<span hidden data-class-group-load-more-meta data-has-more="${classGroupListHasMore}" data-next-offset="${classGroupListNextOffset}" data-current-page="${classGroupListCurrentPage}" data-load-all="${classGroupListLoadAll}"></span>
