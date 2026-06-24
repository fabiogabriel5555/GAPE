<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "class-groups");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Class Groups</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <style>
        .gape-action-button {
            align-items: center;
            border: 1px solid transparent;
            cursor: pointer;
            display: inline-flex;
            justify-content: center;
            line-height: 1.25;
            text-decoration: none;
        }

        .gape-action-delete {
            background-color: #dc2626 !important;
            border-color: #dc2626 !important;
            color: #fff !important;
        }

        .gape-tree-toggle {
            align-items: center;
            background: transparent;
            border: 0;
            border-radius: 8px;
            display: inline-flex;
            height: 32px;
            justify-content: center;
            padding: 0;
            width: 32px;
        }

        .gape-tree-toggle:hover,
        .gape-tree-toggle:focus-visible {
            background-color: var(--main-50);
            text-decoration: none;
        }

        .gape-structure-panel {
            background-color: #f8fbff;
            border: 1px solid #d9e2ef;
            border-radius: 8px;
            margin-block: 14px;
            padding: 14px;
        }

        .gape-class-activities-panel {
            margin-inline-start: 28px;
            position: relative;
        }

        .gape-class-activities-panel::before {
            background-color: #d9e2ef;
            bottom: 12px;
            content: "";
            left: -16px;
            position: absolute;
            top: 12px;
            width: 2px;
        }

        .gape-lesson-node {
            border-inline-start: 3px solid #2563eb;
        }

        .gape-room-node {
            border-inline-start: 3px solid #16a34a;
        }

        .gape-node-meta {
            color: #64748b;
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
        }

        @media (max-width: 575.98px) {
            .gape-class-activities-panel {
                margin-inline-start: 0;
            }

            .gape-class-activities-panel::before {
                display: none;
            }
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

                <div class="row gy-4 mb-24">
                    <div class="col-md-4">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Total</span>
                            <h2 class="text-32 fw-semibold text-neutral-700 mb-0">${classGroupCount}</h2>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Active</span>
                            <h2 class="text-32 fw-semibold text-success-600 mb-0">${activeClassGroups}</h2>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Archived</span>
                            <h2 class="text-32 fw-semibold text-danger-600 mb-0">${archivedClassGroups}</h2>
                        </div>
                    </div>
                </div>

                <div class="bg-white rounded-10 px-24 py-24">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">Class Group Management</h2>
                            <span class="text-14 text-neutral-500">Groups, capacity, enrolled students and content blocks.</span>
                        </div>
                        <c:if test="${canCreateClassGroups}">
                            <a href="${pageContext.request.contextPath}/learning/class-groups/new" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">
                                <i class="ph ph-plus-circle me-8"></i>New Class Group
                            </a>
                        </c:if>
                    </div>
                    <div class="overflow-x-auto">
                        <table class="table mb-0">
                            <thead>
                            <tr>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Class Group</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Context</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Capacity</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Blocks</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="classGroup" items="${classGroups}">
                                <c:set var="canModifyClassGroupRow" value="${canModifyClassGroupById[classGroup.id]}" />
                                <c:set var="canManageClassGroupStructureRow" value="${canManageClassGroupStructureById[classGroup.id]}" />
                                <c:set var="classGroupLessons" value="${classGroupLessonsByClassGroup[classGroup.id]}" />
                                <c:set var="classGroupRooms" value="${classGroupRoomsByClassGroup[classGroup.id]}" />
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-20 px-20">
                                        <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                            <c:out value="${classGroup.code}"/>
                                        </a>
                                        <span class="d-block text-12 text-neutral-500"><c:out value="${classGroup.modalityLabel}"/> | <c:out value="${classGroup.shift}"/></span>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500" title="<c:out value='${classGroup.contextTitle}'/>"><c:out value="${classGroup.contextHtml}" escapeXml="false"/></td>
                                    <td class="py-20 px-20 text-14 text-neutral-500">
                                        <c:out value="${classGroup.activeEnrollmentCount}"/> enrolled
                                        <span class="d-block text-12 text-neutral-500">Min/Max: <c:out value="${classGroup.capacityLabel}"/></span>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${classGroup.blockCount}"/></td>
                                    <td class="py-20 px-20">
                                        <span class="${classGroup.stateBadgeClass} px-16 py-8 border-neutral-30 border rounded-pill text-14">
                                            <c:out value="${classGroup.stateLabel}"/>
                                        </span>
                                    </td>
                                    <td class="py-20 px-20">
                                        <div class="d-flex align-items-center gap-12 justify-content-end">
                                            <button type="button"
                                                    class="gape-tree-toggle text-22 text-neutral-500 hover-text-main-600"
                                                    title="Show Activities"
                                                    aria-label="Show Activities"
                                                    aria-expanded="false"
                                                    aria-controls="classGroupStructure${classGroup.id}"
                                                    data-gape-tree-toggle="classGroupStructure${classGroup.id}"
                                                    data-gape-open-title="Hide Activities"
                                                    data-gape-closed-title="Show Activities">
                                                <i class="ph ph-caret-down"></i>
                                            </button>
                                            <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}" class="text-22 text-neutral-500 hover-text-main-600" title="Detail">
                                                <i class="ph ph-eye"></i>
                                            </a>
                                            <c:if test="${canModifyClassGroupRow}">
                                                <a href="${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/edit" class="text-22 text-neutral-500 hover-text-main-600" title="Edit">
                                                    <i class="ph ph-pencil-simple-line"></i>
                                                </a>
                                            </c:if>
                                            <c:if test="${canManageClassGroupStructureRow}">
                                                <button type="button" class="text-22 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0" title="Delete" data-bs-toggle="modal" data-bs-target="#deleteClassGroup${classGroup.id}">
                                                    <i class="ph ph-trash"></i>
                                                </button>
                                            </c:if>
                                        </div>

                                        <c:if test="${canManageClassGroupStructureRow}">
                                            <div class="modal fade" id="deleteClassGroup${classGroup.id}" tabindex="-1" aria-hidden="true">
                                                <div class="modal-dialog modal-dialog-centered">
                                                    <div class="modal-content rounded-12 border-0">
                                                        <div class="modal-header border-neutral-30">
                                                            <h5 class="modal-title text-18 fw-semibold">Delete Class Group</h5>
                                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                        </div>
                                                        <div class="modal-body">
                                                            <p class="text-14 text-neutral-600 mb-0">This action removes <strong><c:out value="${classGroup.code}"/></strong> if it has no dependencies.</p>
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
                                    </td>
                                </tr>
                                <tr id="classGroupStructure${classGroup.id}" class="d-none">
                                    <td colspan="6" class="py-0 px-20 bg-white">
                                        <c:set var="canModifyClassGroup" value="${canModifyClassGroupRow}" />
                                        <%@ include file="/WEB-INF/fragments/class-group-activities-panel.jspf" %>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty classGroups}">
                                <tr>
                                    <td colspan="6" class="py-32 px-20 text-center text-14 text-neutral-500">No class groups available in your context.</td>
                                </tr>
                            </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
<script>
    document.querySelectorAll('[data-gape-tree-toggle]').forEach(function (button) {
        button.addEventListener('click', function () {
            var target = document.getElementById(button.dataset.gapeTreeToggle);
            if (!target) {
                return;
            }
            var isHidden = target.classList.toggle('d-none');
            var isExpanded = !isHidden;
            var icon = button.querySelector('i');
            button.setAttribute('aria-expanded', String(isExpanded));
            button.setAttribute('title', isExpanded ? button.dataset.gapeOpenTitle : button.dataset.gapeClosedTitle);
            button.setAttribute('aria-label', isExpanded ? button.dataset.gapeOpenTitle : button.dataset.gapeClosedTitle);
            if (icon) {
                icon.classList.toggle('ph-caret-down', !isExpanded);
                icon.classList.toggle('ph-caret-up', isExpanded);
            }
        });
    });
</script>
</body>
</html>
