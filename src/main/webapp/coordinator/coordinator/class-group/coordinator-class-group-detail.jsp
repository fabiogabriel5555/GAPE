<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "class-groups");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Class Group Detail</title>
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

        .gape-block-order {
            align-items: center;
            display: inline-flex;
            height: 40px;
            justify-content: center;
            width: 40px;
        }

        .gape-enrollment-table-wrapper {
            overflow-x: auto;
            -webkit-overflow-scrolling: touch;
        }

        .gape-enrollment-table {
            min-width: 760px;
            table-layout: auto;
        }

        .gape-enrollment-table th,
        .gape-enrollment-table td {
            word-break: normal;
        }

        .gape-enrollment-student {
            min-width: 190px;
        }

        .gape-enrollment-period {
            min-width: 150px;
            white-space: nowrap;
        }

        .gape-enrollment-state {
            min-width: 120px;
        }

        .gape-enrollment-action {
            min-width: 280px;
        }

        .gape-enrollment-withdraw-form {
            flex-wrap: nowrap;
        }

        .gape-enrollment-withdraw-date {
            min-width: 150px;
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
                        <div>
                            <h2 class="text-20 fw-semibold text-neutral-700 mb-4"><c:out value="${classGroup.code}"/></h2>
                            <span class="text-14 text-neutral-500" title="<c:out value='${classGroup.contextTitle}'/>"><c:out value="${classGroup.contextHtml}" escapeXml="false"/></span>
                        </div>
                        <div class="d-flex align-items-center gap-12 flex-wrap">
                            <a href="${pageContext.request.contextPath}/learning/class-groups" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03">Back</a>
                            <c:if test="${canManageClassGroup}">
                                <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/edit" class="bg-main-600 px-20 py-10 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">Edit</a>
                            </c:if>
                        </div>
                    </div>
                    <div class="row gy-4">
                        <div class="col-lg-3">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">State</span>
                                <div class="mt-8">
                                    <span class="${classGroup.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                                        <c:out value="${classGroup.stateLabel}"/>
                                    </span>
                                </div>
                            </div>
                        </div>
                        <div class="col-lg-3">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">Modality</span>
                                <p class="text-15 text-neutral-700 mb-0 mt-8"><c:out value="${classGroup.modalityLabel}"/></p>
                            </div>
                        </div>
                        <div class="col-lg-3">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">Capacity</span>
                                <p class="text-15 text-neutral-700 mb-0 mt-8"><c:out value="${classGroup.activeEnrollmentCount}"/> enrolled | <c:out value="${classGroup.capacityLabel}"/></p>
                            </div>
                        </div>
                        <div class="col-lg-3">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18 h-100">
                                <span class="text-14 text-neutral-500">Dates</span>
                                <p class="text-15 text-neutral-700 mb-0 mt-8"><c:out value="${classGroup.dateRangeLabel}"/></p>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="row gy-4 mb-24">
                    <div class="col-xl-12 col-xxl-7">
                        <div class="bg-white rounded-10 px-24 py-24 h-100">
                            <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap mb-20">
                                <div>
                                    <h3 class="text-18 fw-medium text-neutral-700 mb-4">Enrolled Students</h3>
                                    <span class="text-14 text-neutral-500">Active and withdrawn class group enrollments.</span>
                                </div>
                            </div>
                            <c:if test="${canManageClassGroupEnrollments}">
                                <form action="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/enrollments" method="post" class="border border-neutral-30 rounded-12 px-20 py-20 mb-24">
                                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                    <div class="row gy-4 align-items-end">
                                        <div class="col-lg-6 gape-select-field">
                                            <label for="studentUserId" class="fw-medium text-base text-neutral-800 mb-12">Student</label>
                                            <select id="studentUserId" name="studentUserId" required class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                                <option value="">Select student</option>
                                                <c:forEach var="student" items="${studentOptions}">
                                                    <c:if test="${not activeEnrollmentByStudent[student.id]}">
                                                        <option value="${student.id}"><c:out value="${student.name}"/> - <c:out value="${student.email}"/></option>
                                                    </c:if>
                                                </c:forEach>
                                            </select>
                                        </div>
                                        <div class="col-lg-3">
                                            <label for="enrollmentStartDate" class="fw-medium text-base text-neutral-800 mb-12">Start</label>
                                            <input id="enrollmentStartDate" name="startDate" type="date" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                                        </div>
                                        <div class="col-lg-3">
                                            <label for="enrollmentEndDate" class="fw-medium text-base text-neutral-800 mb-12">End</label>
                                            <input id="enrollmentEndDate" name="endDate" type="date" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                                        </div>
                                        <div class="col-12">
                                            <button type="submit" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">Enroll Student</button>
                                        </div>
                                    </div>
                                </form>
                            </c:if>
                            <div class="gape-enrollment-table-wrapper">
                                <table class="table mb-0 gape-enrollment-table">
                                    <thead>
                                    <tr>
                                        <th class="py-16 px-20 text-14 fw-medium text-neutral-600 gape-enrollment-student">Student</th>
                                        <th class="py-16 px-20 text-14 fw-medium text-neutral-600 gape-enrollment-period">Period</th>
                                        <th class="py-16 px-20 text-14 fw-medium text-neutral-600 gape-enrollment-state">State</th>
                                        <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end gape-enrollment-action">Action</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <c:forEach var="enrollment" items="${classGroupEnrollments}">
                                        <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                            <td class="py-20 px-20 gape-enrollment-student">
                                                <span class="fw-medium text-14 text-neutral-700"><c:out value="${enrollment.studentName}"/></span>
                                                <span class="d-block text-12 text-neutral-500"><c:out value="${enrollment.studentEmail}"/></span>
                                            </td>
                                            <td class="py-20 px-20 text-14 text-neutral-500 gape-enrollment-period"><c:out value="${enrollment.startDate}"/> to <c:out value="${enrollment.endDate}"/></td>
                                            <td class="py-20 px-20 gape-enrollment-state">
                                                <span class="${enrollment.stateBadgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13">
                                                    <c:out value="${enrollment.stateLabel}"/>
                                                </span>
                                            </td>
                                            <td class="py-20 px-20 text-end gape-enrollment-action">
                                                <c:if test="${canManageClassGroupEnrollments and enrollment.active}">
                                                    <form action="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/enrollments/${enrollment.studentUserId}/withdraw" method="post" class="m-0 d-inline-flex align-items-center gap-8 gape-enrollment-withdraw-form">
                                                        <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                        <input name="endDate" type="date" class="form-control px-12 py-8 fw-normal text-13 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-10 focus-visible-outline focus-border-main-600 gape-enrollment-withdraw-date">
                                                        <button type="submit" class="border-main-600 border px-16 py-8 rounded-12 fw-semibold text-main-600 hover-bg-main-50 transition-03">Withdraw</button>
                                                    </form>
                                                </c:if>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    <c:if test="${empty classGroupEnrollments}">
                                        <tr>
                                            <td colspan="4" class="py-32 px-20 text-center text-14 text-neutral-500">No students enrolled in this class group.</td>
                                        </tr>
                                    </c:if>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>

                    <div class="col-xl-12 col-xxl-5">
                        <div class="bg-white rounded-10 px-24 py-24 h-100">
                            <h3 class="text-18 fw-medium text-neutral-700 mb-4">Teachers</h3>
                            <span class="text-14 text-neutral-500 d-block mb-20">Assign active teachers to this class group.</span>
                            <c:choose>
                                <c:when test="${canManageClassGroupStructure}">
                                    <form action="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/teachers" method="post">
                                        <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                        <div class="row gy-4">
                                            <div class="col-12 gape-select-field">
                                                <label for="teacherUserId" class="fw-medium text-base text-neutral-800 mb-12">Teacher</label>
                                                <select id="teacherUserId" name="teacherUserId" required class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                                    <option value="">Select teacher</option>
                                                    <c:forEach var="teacher" items="${teacherOptions}">
                                                        <option value="${teacher.id}"><c:out value="${teacher.name}"/> - <c:out value="${teacher.email}"/></option>
                                                    </c:forEach>
                                                </select>
                                            </div>
                                            <div class="col-md-6">
                                                <label for="teacherStartDate" class="fw-medium text-base text-neutral-800 mb-12">Start Date</label>
                                                <input id="teacherStartDate" name="startDate" type="date" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                                            </div>
                                            <div class="col-md-6">
                                                <label for="teacherEndDate" class="fw-medium text-base text-neutral-800 mb-12">End Date</label>
                                                <input id="teacherEndDate" name="endDate" type="date" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                                            </div>
                                        </div>
                                        <button type="submit" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03 mt-24">Assign Teacher</button>
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    <div class="border border-neutral-30 rounded-12 px-20 py-20">
                                        <span class="text-14 text-neutral-500">Assigned teachers</span>
                                        <p class="text-24 fw-semibold text-neutral-700 mb-0 mt-8"><c:out value="${classGroup.teacherCount}"/></p>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>

                <div class="bg-white rounded-10 px-24 py-24 mb-24">
                    <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h3 class="text-18 fw-medium text-neutral-700 mb-4">Pedagogical Blocks</h3>
                            <span class="text-14 text-neutral-500">Visual order, access mode and availability.</span>
                        </div>
                        <c:if test="${canManageClassGroup}">
                            <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/blocks/new" class="bg-main-600 px-20 py-10 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">
                                <i class="ph ph-plus-circle me-8"></i>New Block
                            </a>
                        </c:if>
                    </div>
                    <div class="d-flex flex-column gap-16">
                        <c:forEach var="block" items="${contentBlocks}">
                            <div class="border border-neutral-30 rounded-12 px-20 py-18">
                                <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap">
                                    <div class="d-flex align-items-start gap-16">
                                        <span class="gape-block-order bg-main-50 text-main-600 rounded-circle fw-semibold"><c:out value="${block.orderNo}"/></span>
                                        <div>
                                            <h4 class="text-16 fw-semibold text-neutral-700 mb-6"><c:out value="${block.name}"/></h4>
                                            <span class="text-13 text-neutral-500 d-block"><c:out value="${block.code}"/> | <c:out value="${block.accessModeLabel}"/> | <c:out value="${block.availabilityLabel}"/></span>
                                            <p class="text-14 text-neutral-600 mb-0 mt-8"><c:out value="${block.description}"/></p>
                                        </div>
                                    </div>
                                    <div class="d-flex align-items-center gap-12 flex-wrap">
                                        <span class="${block.stateBadgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13">
                                            <c:out value="${block.stateLabel}"/>
                                        </span>
                                        <c:if test="${canManageClassGroup and not block.archived}">
                                            <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/blocks/${block.id}/edit" class="text-22 text-neutral-500 hover-text-main-600" title="Edit block">
                                                <i class="ph ph-pencil-simple-line"></i>
                                            </a>
                                            <button type="button" class="text-22 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Archive block" data-bs-toggle="modal" data-bs-target="#archiveBlock${block.id}">
                                                <i class="ph ph-archive-box"></i>
                                            </button>
                                            <button type="button" class="text-22 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete block" data-bs-toggle="modal" data-bs-target="#deleteBlock${block.id}">
                                                <i class="ph ph-trash"></i>
                                            </button>
                                        </c:if>
                                    </div>
                                </div>

                                <c:if test="${canManageClassGroup and not block.archived}">
                                    <div class="modal fade" id="archiveBlock${block.id}" tabindex="-1" aria-hidden="true">
                                        <div class="modal-dialog modal-dialog-centered">
                                            <div class="modal-content rounded-12 border-0">
                                                <div class="modal-header border-neutral-30">
                                                    <h5 class="modal-title text-18 fw-semibold">Archive Content Block</h5>
                                                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                </div>
                                                <div class="modal-body">
                                                    <p class="text-14 text-neutral-600 mb-0">Confirm archiving <strong><c:out value="${block.name}"/></strong>?</p>
                                                </div>
                                                <div class="modal-footer border-neutral-30">
                                                    <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                                                    <form action="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/blocks/${block.id}/archive" method="post" class="m-0">
                                                        <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                        <button type="submit" class="gape-action-button gape-action-archive px-20 py-10 rounded-12 fw-semibold transition-03">Archive</button>
                                                    </form>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="modal fade" id="deleteBlock${block.id}" tabindex="-1" aria-hidden="true">
                                        <div class="modal-dialog modal-dialog-centered">
                                            <div class="modal-content rounded-12 border-0">
                                                <div class="modal-header border-neutral-30">
                                                    <h5 class="modal-title text-18 fw-semibold">Delete Content Block</h5>
                                                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                </div>
                                                <div class="modal-body">
                                                    <p class="text-14 text-neutral-600 mb-0">This action removes the block if it has no dependencies.</p>
                                                </div>
                                                <div class="modal-footer border-neutral-30">
                                                    <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                                                    <form action="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/blocks/${block.id}/delete" method="post" class="m-0">
                                                        <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                                        <button type="submit" class="gape-action-button gape-action-delete px-20 py-10 rounded-12 fw-semibold transition-03">Delete</button>
                                                    </form>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </c:if>
                            </div>
                        </c:forEach>
                        <c:if test="${empty contentBlocks}">
                            <div class="border border-neutral-30 rounded-12 px-20 py-32 text-center text-14 text-neutral-500">No pedagogical blocks created for this class group.</div>
                        </c:if>
                    </div>
                </div>

                <c:if test="${canManageClassGroupStructure}">
                    <div class="bg-white rounded-10 px-24 py-24">
                        <h3 class="text-16 fw-medium text-neutral-700 mb-16">Critical Actions</h3>
                        <div class="d-flex align-items-center gap-16 flex-wrap">
                            <button type="button" class="gape-action-button gape-action-archive px-24 py-12 rounded-12 fw-semibold transition-03" data-bs-toggle="modal" data-bs-target="#archiveClassGroup">Archive</button>
                            <button type="button" class="gape-action-button gape-action-delete px-24 py-12 rounded-12 fw-semibold transition-03" data-bs-toggle="modal" data-bs-target="#deleteClassGroup">Delete</button>
                        </div>
                    </div>
                </c:if>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>

<c:if test="${canManageClassGroupStructure}">
<div class="modal fade" id="archiveClassGroup" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-12 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Archive Class Group</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">Confirm archiving <strong><c:out value="${classGroup.code}"/></strong>?</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/archive" method="post" class="m-0">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <button type="submit" class="gape-action-button gape-action-archive px-20 py-10 rounded-12 fw-semibold transition-03">Archive</button>
                </form>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="deleteClassGroup" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-12 border-0">
            <div class="modal-header border-neutral-30">
                <h5 class="modal-title text-18 fw-semibold">Delete Class Group</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-14 text-neutral-600 mb-0">This action removes the class group if it has no dependencies.</p>
            </div>
            <div class="modal-footer border-neutral-30">
                <button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03" data-bs-dismiss="modal">Cancel</button>
                <form action="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/delete" method="post" class="m-0">
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
