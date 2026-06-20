<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "subjects");
    }
%>
<c:if test="${empty subjectBasePath}">
    <c:set var="subjectBasePath" value="/admin/subjects"/>
</c:if>
<c:if test="${empty subjectCourseBasePath}">
    <c:set var="subjectCourseBasePath" value="/admin/courses"/>
</c:if>
<c:set var="subjectPhotoUrl" value=""/>
<c:if test="${subject.hasPhoto}">
    <c:set var="subjectPhotoUrl" value="${pageContext.request.contextPath}/media/${subject.photo}?v=${mediaCacheVersion}"/>
</c:if>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Subject Detail</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        .gape-action-button {
            align-items: center;
            border: 1px solid transparent;
            cursor: pointer;
            display: inline-flex;
            justify-content: center;
            line-height: 1.25;
            min-height: 48px;
            text-decoration: none;
        }

        .gape-action-delete {
            background-color: #dc2626 !important;
            border-color: #dc2626 !important;
            color: #fff !important;
        }

        .gape-action-archive {
            background-color: #f97316 !important;
            border-color: #f97316 !important;
            color: #fff !important;
        }

        .gape-learning-detail-photo {
            border-radius: 16px;
            height: 72px;
            object-fit: cover;
            width: 96px;
        }
    </style>
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

                <div class="bg-white rounded-10 px-24 py-24 mb-24">
                    <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap border-bottom-dashed pb-24 mb-24">
                        <div class="d-flex align-items-center gap-16">
                            <c:choose>
                                <c:when test="${subject.hasPhoto}">
                                    <img src="${subjectPhotoUrl}"
                                         alt=""
                                         class="gape-learning-detail-photo flex-shrink-0"
                                         onerror="this.classList.add('d-none');this.nextElementSibling.classList.remove('d-none');">
                                    <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--learning-detail d-none" aria-label="No subject photo">
                                        <i class="ph ph-image"></i>
                                    </span>
                                </c:when>
                                <c:otherwise>
                                    <span class="gape-photo-placeholder gape-photo-placeholder--image gape-photo-placeholder--learning-detail" aria-label="No subject photo">
                                        <i class="ph ph-image"></i>
                                    </span>
                                </c:otherwise>
                            </c:choose>
                            <div>
                                <h2 class="text-20 fw-semibold text-neutral-700 mb-4"><c:out value="${subject.name}"/></h2>
                                <span class="text-14 text-neutral-500">
                                    <c:out value="${subject.organizationContextHtml}" escapeXml="false"/> |
                                    <span class="gape-acronym-token" tabindex="0" title="<c:out value='${subject.name}'/>"><c:out value="${subject.acronym}"/></span>
                                </span>
                            </div>
                        </div>
                        <div class="d-flex align-items-center gap-12 flex-wrap">
                            <a href="${pageContext.request.contextPath}${subjectBasePath}" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03">Back</a>
                            <c:if test="${not subject.archived and canModifySubject}">
                                <a href="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/edit" class="bg-main-600 px-20 py-10 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">Edit</a>
                            </c:if>
                        </div>
                    </div>
                    <div class="row gy-4">
                        <div class="col-lg-4">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">State</span>
                                <div class="mt-8">
                                    <span class="${subject.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                                        <c:out value="${subject.stateLabel}"/>
                                    </span>
                                </div>
                            </div>
                        </div>
                        <div class="col-lg-4">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">ECTS</span>
                                <p class="text-15 text-neutral-700 mb-0 mt-8"><c:out value="${subject.ectsLabel}"/></p>
                            </div>
                        </div>
                        <div class="col-lg-4">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">Workload</span>
                                <p class="text-15 text-neutral-700 mb-0 mt-8"><c:out value="${subject.workloadHoursLabel}"/></p>
                            </div>
                        </div>
                    </div>
                    <p class="text-14 text-neutral-600 mt-20 mb-0"><c:out value="${subject.description}"/></p>
                </div>

                <div class="row gy-4 mb-24">
                    <c:if test="${canAssignSubjectCoordinators}">
                    <div class="col-xl-6">
                        <div class="bg-white rounded-10 px-24 py-24 h-100">
                            <h3 class="text-18 fw-medium text-neutral-700 mb-4">Coordinator Assignment</h3>
                            <span class="text-14 text-neutral-500 d-block mb-20">Assign an active coordinator to this subject.</span>
                            <form action="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/assign-coordinator" method="post">
                                <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                <div class="mb-20 gape-select-field">
                                    <label for="coordinatorUserId" class="fw-medium text-base text-neutral-800 mb-12">Coordinator</label>
                                    <select id="coordinatorUserId" name="coordinatorUserId" required class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                        <option value="">Select coordinator</option>
                                        <c:forEach var="coordinator" items="${coordinatorOptions}">
                                            <option value="${coordinator.id}"><c:out value="${coordinator.name}"/> - <c:out value="${coordinator.email}"/></option>
                                        </c:forEach>
                                    </select>
                                </div>
                                <div class="row gy-4">
                                    <div class="col-md-6">
                                        <label for="startDate" class="fw-medium text-base text-neutral-800 mb-12">Start Date</label>
                                        <input id="startDate" name="startDate" type="date" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                                    </div>
                                    <div class="col-md-6">
                                        <label for="endDate" class="fw-medium text-base text-neutral-800 mb-12">End Date</label>
                                        <input id="endDate" name="endDate" type="date" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                                    </div>
                                </div>
                                <button type="submit" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03 mt-24">Assign Coordinator</button>
                            </form>
                        </div>
                    </div>
                    </c:if>
                    <div class="col-xl-6">
                        <div class="bg-white rounded-10 px-24 py-24 h-100">
                            <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap mb-20">
                                <div>
                                    <h3 class="text-18 fw-medium text-neutral-700 mb-4">Course Associations</h3>
                                    <span class="text-14 text-neutral-500">Courses linked to this subject.</span>
                                </div>
                                <c:if test="${not subject.archived and canManageSubjectAssociations}">
                                    <a href="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/courses" class="border-main-600 border px-16 py-8 fw-semibold rounded-12 hover-bg-main-50 transition-03">Associate Courses</a>
                                </c:if>
                            </div>
                            <div class="d-flex flex-column gap-12">
                                <c:forEach var="association" items="${subjectCourseAssociations}">
                                    <div class="border border-neutral-30 rounded-12 px-16 py-14">
                                        <div class="d-flex align-items-center justify-content-between gap-12 flex-wrap">
                                            <div>
                                                <a href="${pageContext.request.contextPath}${subjectCourseBasePath}/${association.courseId}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                                    <c:out value="${association.courseName}"/>
                                                </a>
                                                <span class="d-block text-12 text-neutral-500" title="<c:out value='${association.courseContextTitle}'/>"><c:out value="${association.courseContextHtml}" escapeXml="false"/></span>
                                                <span class="d-block text-12 text-neutral-500"><c:out value="${association.curricularPositionLabel}"/> | <c:out value="${association.mandatoryLabel}"/></span>
                                            </div>
                                            <span class="${association.stateBadgeClass} px-14 py-6 border-neutral-30 border rounded-pill text-14">
                                                <c:out value="${association.stateLabel}"/>
                                            </span>
                                        </div>
                                    </div>
                                </c:forEach>
                                <c:if test="${empty subjectCourseAssociations}">
                                    <p class="text-14 text-neutral-500 mb-0">No associated courses found.</p>
                                </c:if>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="bg-white rounded-10 px-24 py-24 mb-24">
                    <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h3 class="text-18 fw-medium text-neutral-700 mb-4">Class Groups</h3>
                            <span class="text-14 text-neutral-500">Groups created for this subject across associated courses.</span>
                        </div>
                        <div class="d-flex align-items-center gap-12 flex-wrap">
                            <a href="${pageContext.request.contextPath}/learning/class-groups?subjectId=${subject.id}" class="border-main-600 border px-16 py-8 fw-semibold rounded-12 hover-bg-main-50 transition-03">Open Groups</a>
                            <c:if test="${not subject.archived and canCreateClassGroupsForSubject}">
                                <a href="${pageContext.request.contextPath}/learning/class-groups/new?subjectId=${subject.id}" class="bg-main-600 px-20 py-10 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">
                                    <i class="ph ph-plus-circle me-8"></i>New Group
                                </a>
                            </c:if>
                        </div>
                    </div>
                    <div class="overflow-x-auto">
                        <table class="table mb-0">
                            <thead>
                            <tr>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Group</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Course</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Capacity</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Blocks</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Action</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="classGroup" items="${classGroups}">
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-20 px-20">
                                        <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                            <c:out value="${classGroup.code}"/>
                                        </a>
                                        <span class="d-block text-12 text-neutral-500"><c:out value="${classGroup.modalityLabel}"/> | <c:out value="${classGroup.shift}"/></span>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500" title="<c:out value='${classGroup.course.subjectManagementContextTitle}'/>"><c:out value="${classGroup.course.subjectManagementContextHtml}" escapeXml="false"/></td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${classGroup.activeEnrollmentCount}"/> enrolled | <c:out value="${classGroup.capacityLabel}"/></td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${classGroup.blockCount}"/></td>
                                    <td class="py-20 px-20">
                                        <span class="${classGroup.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                                            <c:out value="${classGroup.stateLabel}"/>
                                        </span>
                                    </td>
                                    <td class="py-20 px-20 text-end">
                                        <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}" class="text-22 text-neutral-500 hover-text-main-600" title="Detail">
                                            <i class="ph ph-eye"></i>
                                        </a>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty classGroups}">
                                <tr>
                                    <td colspan="6" class="py-32 px-20 text-center text-14 text-neutral-500">No class groups created for this subject.</td>
                                </tr>
                            </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>

                <c:if test="${not subject.archived and canModifySubject}">
                    <div class="bg-white rounded-10 px-24 py-24">
                        <h3 class="text-16 fw-medium text-neutral-700 mb-16">Critical Actions</h3>
                        <div class="d-flex align-items-center gap-16 flex-wrap">
                            <button type="button" class="gape-action-button gape-action-archive px-24 py-12 rounded-12 fw-semibold transition-03" data-bs-toggle="modal" data-bs-target="#archiveSubject">Archive</button>
                            <button type="button" class="gape-action-button gape-action-delete px-24 py-12 rounded-12 fw-semibold transition-03" data-bs-toggle="modal" data-bs-target="#deleteSubject">Delete</button>
                        </div>
                    </div>
                </c:if>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>

<c:if test="${not subject.archived and canModifySubject}">
<div class="modal fade" id="archiveSubject" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-12 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Archive Subject</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">Confirm archiving <strong><c:out value="${subject.name}"/></strong>?</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/archive" method="post" class="m-0">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <button type="submit" class="gape-action-button gape-action-archive px-20 py-10 rounded-12 fw-semibold transition-03">Archive</button>
                </form>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="deleteSubject" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-12 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Delete Subject</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">This action removes the subject if the database allows it.</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}${subjectBasePath}/${subject.id}/delete" method="post" class="m-0">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <button type="submit" class="gape-action-button gape-action-delete px-20 py-10 rounded-12 fw-semibold transition-03">Delete</button>
                </form>
            </div>
        </div>
    </div>
</div>
</c:if>

<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
</body>
</html>
