<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<div class="cd-surface px-22 py-22 mb-20 gape-subject-detail-surface" id="subject-coordinators">
    <div class="gape-subject-detail-panel gape-course-structure-panel">
        <div class="px-18 py-18 gape-subject-panel-content" data-subject-detail-sort-root>
            <div class="gape-subject-panel-header">
                <div class="gape-subject-panel-heading">
                    <h3 class="text-18 fw-semibold text-neutral-800 mb-4">Coordinator Assignments</h3>
                    <span class="text-13 text-neutral-500">Coordinators assigned to this subject and their current state.</span>
                </div>
                <div class="gape-subject-panel-actions">
                    <div class="dropdown">
                        <button type="button"
                                class="gape-filter-toggle border-neutral-30 border px-20 py-12 rounded-12 fw-semibold text-neutral-700 hover-bg-main-50 transition-03 bg-white d-flex align-items-center gap-8"
                                data-bs-toggle="dropdown"
                                data-bs-auto-close="outside"
                                data-subject-detail-sort-toggle
                                aria-expanded="false">
                            <i class="ph ph-sort-ascending"></i>Sort by
                        </button>
                        <ul class="dropdown-menu dropdown-menu-end rounded-12">
                            <li><button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-subject-detail-sort-option data-sort-field="name" data-sort-normal="asc" data-sort-state="none" aria-pressed="false"><span>Name</span><span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span></button></li>
                            <li><button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-subject-detail-sort-option data-sort-field="date" data-sort-normal="desc" data-sort-state="none" aria-pressed="false"><span>Date</span><span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--reverse"></i></span></button></li>
                            <li><button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-subject-detail-sort-option data-sort-field="status" data-sort-normal="asc" data-sort-state="none" aria-pressed="false"><span>State</span><span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span></button></li>
                        </ul>
                    </div>
                    <c:if test="${canAssignSubjectCoordinators}">
                        <button type="button" class="cd-primary-button cd-section-create-button" data-bs-toggle="modal" data-bs-target="#newCoordinatorAssignmentModal"><i class="ph ph-plus-circle me-8"></i>Assign Coordinator</button>
                    </c:if>
                </div>
            </div>

            <div class="gape-structure-list-header gape-enrollment-list-header">
                <span>Coordinator</span>
                <span>Email</span>
                <span>State</span>
                <span class="text-end">Actions</span>
            </div>
            <div class="d-flex flex-column gap-12" data-subject-detail-sort-list>
                <c:forEach var="assignment" items="${coordinatorAssignments}" varStatus="assignmentLoop">
                    <article class="gape-structure-node border border-neutral-30 rounded-8 px-18 py-16 bg-white"
                             data-subject-detail-sort-row
                             data-sort-index="${assignmentLoop.index}"
                             data-sort-name="<c:out value='${assignment.coordinatorName}'/>"
                             data-sort-date="${assignment.coordinatorUserId}"
                             data-sort-status="<c:out value='${assignment.stateLabel}'/>">
                        <div class="gape-structure-row gape-enrollment-row">
                            <div class="d-flex align-items-center gap-12 min-w-0">
                                <span class="bg-info-50 text-info-600 cd-mode-icon text-20 line-height-1"><i class="ph ph-user-switch" aria-hidden="true"></i></span>
                                <div class="min-w-0">
                                    <span class="fw-medium text-14 text-neutral-700 d-block"><c:out value="${assignment.coordinatorName}"/></span>
                                    <span class="gape-node-meta text-12"><span>#<c:out value="${assignment.coordinatorUserId}"/></span></span>
                                </div>
                            </div>
                            <div class="min-w-0"><span class="text-14 text-neutral-700 d-block text-truncate"><c:out value="${assignment.coordinatorEmail}"/></span></div>
                            <div><span class="${assignment.stateBadgeClass} px-14 py-6 border-neutral-30 border rounded-pill text-13"><c:out value="${assignment.stateLabel}"/></span></div>
                            <div class="d-flex align-items-center gap-10 flex-wrap justify-content-end">
                                <button type="button" class="text-20 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Detail" aria-label="Coordinator assignment details" data-bs-toggle="modal" data-bs-target="#coordinatorAssignmentDetail${assignment.coordinatorUserId}"><i class="ph ph-eye"></i></button>
                                <c:if test="${canAssignSubjectCoordinators}">
                                    <button type="button" class="text-20 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Edit" aria-label="Edit coordinator assignment" data-bs-toggle="modal" data-bs-target="#editCoordinatorAssignment${assignment.coordinatorUserId}"><i class="ph ph-pencil-simple-line"></i></button>
                                    <button type="button" class="text-20 text-neutral-500 hover-text-danger-600 border-0 bg-transparent p-0" title="Delete" aria-label="Delete coordinator assignment" data-bs-toggle="modal" data-bs-target="#deleteCoordinatorAssignment${assignment.coordinatorUserId}"><i class="ph ph-trash"></i></button>
                                </c:if>
                            </div>
                        </div>
                    </article>

                    <div class="modal fade" id="coordinatorAssignmentDetail${assignment.coordinatorUserId}" tabindex="-1" aria-hidden="true">
                        <div class="modal-dialog modal-dialog-centered gape-subject-form-modal">
                            <div class="modal-content rounded-12 border-0 text-start">
                                <div class="modal-header border-neutral-30"><h5 class="modal-title text-18 fw-semibold">Coordinator Assignment Details</h5><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div>
                                <div class="modal-body"><div class="row g-3"><div class="col-sm-6"><span class="text-12 text-neutral-500 d-block mb-6">Coordinator</span><span class="fw-medium text-14 text-neutral-700">#<c:out value="${assignment.coordinatorUserId}"/> &middot; <c:out value="${assignment.coordinatorName}"/></span></div><div class="col-sm-6"><span class="text-12 text-neutral-500 d-block mb-6">Email</span><span class="fw-medium text-14 text-neutral-700"><c:out value="${assignment.coordinatorEmail}"/></span></div><div class="col-12"><span class="text-12 text-neutral-500 d-block mb-6">State</span><span class="${assignment.stateBadgeClass} px-12 py-5 border-neutral-30 border rounded-pill text-12"><c:out value="${assignment.stateLabel}"/></span></div></div></div>
                                <div class="modal-footer border-neutral-30"><button type="button" class="cd-outline-button" data-bs-dismiss="modal">Close</button></div>
                            </div>
                        </div>
                    </div>

                    <c:if test="${canAssignSubjectCoordinators}">
                        <div class="modal fade" id="editCoordinatorAssignment${assignment.coordinatorUserId}" tabindex="-1" aria-hidden="true">
                            <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable gape-subject-form-modal">
                                <div class="modal-content rounded-12 border-0 text-start">
                                    <div class="modal-header border-neutral-30"><h5 class="modal-title text-18 fw-semibold">Edit Coordinator Assignment</h5><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div>
                                    <form action="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/coordinators/${assignment.coordinatorUserId}/update" method="post" data-subject-live-form data-subject-live-panel="allocations">
                                        <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                        <input type="hidden" name="returnTo" value="${currentReturnTo}#subject-coordinators">
                                        <div class="modal-body"><p class="text-14 text-neutral-600 mb-20"><strong><c:out value="${assignment.coordinatorName}"/></strong><span class="d-block text-12 text-neutral-500"><c:out value="${assignment.coordinatorEmail}"/></span></p><div class="gape-select-field"><label for="coordinatorAssignmentState${assignment.coordinatorUserId}" class="fw-medium text-base text-neutral-800 mb-12">State</label><select id="coordinatorAssignmentState${assignment.coordinatorUserId}" name="state" required class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8"><option value="active" ${assignment.stateValue == 'active' ? 'selected' : ''}>Active</option><option value="inactive" ${assignment.stateValue == 'inactive' ? 'selected' : ''}>Inactive</option></select></div></div>
                                        <div class="modal-footer border-neutral-30"><button type="button" class="cd-outline-button" data-bs-dismiss="modal">Cancel</button><button type="submit" class="cd-primary-button border-0">Save</button></div>
                                    </form>
                                </div>
                            </div>
                        </div>

                        <div class="modal fade" id="deleteCoordinatorAssignment${assignment.coordinatorUserId}" tabindex="-1" aria-hidden="true">
                            <div class="modal-dialog modal-dialog-centered gape-subject-form-modal">
                                <div class="modal-content rounded-12 border-0 text-start">
                                    <div class="modal-header border-neutral-30"><h5 class="modal-title text-18 fw-semibold">Delete Coordinator Assignment</h5><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div>
                                    <div class="modal-body"><p class="text-14 text-neutral-600 mb-0">Remove <strong><c:out value="${assignment.coordinatorName}"/></strong> as a coordinator for this subject?</p></div>
                                    <div class="modal-footer border-neutral-30"><button type="button" class="cd-outline-button" data-bs-dismiss="modal">Cancel</button><form action="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/coordinators/${assignment.coordinatorUserId}/delete" method="post" class="m-0" data-subject-live-form data-subject-live-panel="allocations"><input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}"><input type="hidden" name="returnTo" value="${currentReturnTo}#subject-coordinators"><button type="submit" class="cd-danger-button border-0">Delete</button></form></div>
                                </div>
                            </div>
                        </div>
                    </c:if>
                </c:forEach>
                <c:if test="${empty coordinatorAssignments}"><div class="border border-neutral-30 rounded-8 px-18 py-28 text-center text-14 text-neutral-500">No coordinator assignments are available for this subject.</div></c:if>
            </div>
        </div>
    </div>
</div>

<c:if test="${canAssignSubjectCoordinators}">
    <div class="modal fade gape-subject-form-modal-root" id="newCoordinatorAssignmentModal" tabindex="-1" aria-hidden="true" aria-labelledby="newCoordinatorAssignmentModalTitle">
        <div class="modal-dialog modal-dialog-centered gape-subject-form-modal gape-subject-form-modal--coordinator">
            <div class="modal-content rounded-12 border-0">
                <div class="modal-header border-neutral-30">
                    <h5 class="modal-title text-18 fw-semibold" id="newCoordinatorAssignmentModalTitle">Assign Coordinator</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <form action="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/assign-coordinator" method="post" data-subject-live-form data-subject-live-panel="allocations">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <input type="hidden" name="returnTo" value="${currentReturnTo}#subject-coordinators">
                    <div class="modal-body">
                        <p class="text-14 text-neutral-600 mb-20">Search and select a coordinator by ID, name or email.</p>
                        <div class="gape-select-field">
                            <label for="coordinatorUserId" class="fw-medium text-base text-neutral-800 mb-12">Select coordinator</label>
                            <select id="coordinatorUserId" name="coordinatorUserId" required data-subject-coordinator-select class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8 js-example-basic-single gape-eduall-select">
                                <option value="">Select coordinator</option>
                                <c:forEach var="coordinator" items="${coordinatorOptions}">
                                    <option value="${coordinator.id}">#<c:out value="${coordinator.id}"/> &middot; <c:out value="${coordinator.name}"/> &middot; <c:out value="${coordinator.email}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>
                    <div class="modal-footer border-neutral-30"><button type="button" class="cd-outline-button" data-bs-dismiss="modal">Cancel</button><button type="submit" class="cd-primary-button border-0">Assign Coordinator</button></div>
                </form>
            </div>
        </div>
    </div>
</c:if>
