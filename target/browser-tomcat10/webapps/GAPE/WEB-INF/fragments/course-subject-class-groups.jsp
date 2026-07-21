<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<div class="d-flex align-items-center justify-content-between gap-12 flex-wrap mb-12">
    <div>
        <h4 class="text-16 fw-medium text-neutral-700 mb-4">Class Groups</h4>
        <span class="text-13 text-neutral-500"><c:out value="${association.subjectName}"/> &middot; <c:out value="${fn:length(subjectClassGroups)}"/> class groups</span>
    </div>
    <c:if test="${canManageCourseChildren}">
        <a href="${pageContext.request.contextPath}/learning/class-groups/new?courseId=${course.id}&subjectId=${association.subjectId}" class="text-20 text-neutral-500 hover-text-main-600" title="New Group" aria-label="New Group"><i class="ph ph-plus-circle"></i></a>
    </c:if>
</div>
<div class="d-flex flex-column gap-10">
    <c:forEach var="classGroup" items="${subjectClassGroups}">
        <c:set var="canModifyClassGroup" value="${canModifyClassGroupById[classGroup.id]}" />
        <c:set var="canManageClassGroupStructure" value="${canManageClassGroupStructureById[classGroup.id]}" />
        <c:set var="activityPanelId" value="courseDetailClassGroupActivities${association.subjectId}_${classGroup.id}" />
        <article class="gape-structure-node gape-class-group-node border border-neutral-30 rounded-8 px-16 py-12 bg-white">
            <div class="gape-structure-row">
                <div class="d-flex align-items-start gap-10 min-w-0">
                    <span class="bg-warning-50 text-warning-600 cd-mode-icon text-20 line-height-1"><i class="ph ph-users-three"></i></span>
                    <div class="min-w-0"><a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}" class="fw-medium text-14 text-neutral-700 hover-text-main-600"><c:out value="${classGroup.code}"/></a><span class="gape-node-meta text-12"><span><c:out value="${classGroup.modalityLabel}"/></span><span><c:out value="${classGroup.shift}"/></span><span data-gape-datetime-display><c:out value="${classGroup.dateRangeLabel}"/></span><span><c:out value="${classGroup.activeEnrollmentCount}"/> enrolled</span></span></div>
                </div>
                <div class="text-13 text-neutral-500"><c:out value="${classGroup.modalityLabel}"/></div>
                <div><span class="cd-element-count">${classGroupActivityCountByClassGroup[classGroup.id]}</span></div>
                <div><span class="${classGroup.stateBadgeClass} px-14 py-6 border-neutral-30 border rounded-pill text-13"><c:out value="${classGroup.stateLabel}"/></span></div>
                <div class="d-flex align-items-center gap-10 flex-wrap justify-content-end">
                    <button type="button" class="gape-tree-toggle text-20 text-neutral-500 hover-text-main-600" title="Show Activities" aria-label="Show Activities" aria-expanded="false" aria-controls="${activityPanelId}" data-gape-tree-toggle="${activityPanelId}" data-gape-open-title="Hide Activities" data-gape-closed-title="Show Activities"><i class="ph ph-caret-down"></i></button>
                    <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}" class="text-20 text-neutral-500 hover-text-main-600" title="Detail" aria-label="Detail"><i class="ph ph-eye"></i></a>
                    <c:if test="${canModifyClassGroup}"><a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/edit" class="text-20 text-neutral-500 hover-text-main-600" title="Edit" aria-label="Edit"><i class="ph ph-pencil-simple-line"></i></a></c:if>
                    <c:if test="${canManageClassGroupStructure}"><button type="button" class="text-20 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" aria-label="Delete" data-bs-toggle="modal" data-bs-target="#deleteCourseDetailClassGroup${classGroup.id}"><i class="ph ph-trash"></i></button></c:if>
                </div>
            </div>
            <div id="${activityPanelId}" class="d-none" data-course-class-group-activities data-course-activity-url="${pageContext.request.contextPath}${courseBasePath}/${course.id}/class-groups/${classGroup.id}/activities" aria-busy="false"><div class="border-top border-neutral-30 mt-12 pt-12 text-13 text-neutral-500">Open activities to load their details.</div></div>
        </article>
        <c:if test="${canManageClassGroupStructure}">
            <div class="modal fade" id="deleteCourseDetailClassGroup${classGroup.id}" tabindex="-1" aria-hidden="true"><div class="modal-dialog modal-dialog-centered"><div class="modal-content rounded-8 border-0"><div class="modal-header border-neutral-30"><h5 class="modal-title text-18 fw-semibold">Delete Class Group</h5><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div><div class="modal-body"><p class="text-14 text-neutral-600 mb-0">This action removes <strong><c:out value="${classGroup.code}"/></strong> if it has no dependencies.</p></div><div class="modal-footer border-neutral-30"><button type="button" class="cd-outline-button" data-bs-dismiss="modal">Cancel</button><form action="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/delete" method="post" class="m-0" data-course-live-form data-course-live-panel="structure"><input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}"><button type="submit" class="cd-danger-button">Delete</button></form></div></div></div></div>
        </c:if>
    </c:forEach>
    <c:if test="${empty subjectClassGroups}"><div class="border border-neutral-30 rounded-8 px-16 py-18 text-center text-13 text-neutral-500 bg-white">No class groups registered for this subject in this course.</div></c:if>
</div>
