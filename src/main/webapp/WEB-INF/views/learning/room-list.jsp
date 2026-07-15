<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Rooms</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
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
                            <span class="text-14 text-neutral-500">Total Rooms</span>
                            <h2 class="text-32 fw-semibold text-neutral-700 mb-0">${roomCount}</h2>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Active</span>
                            <h2 class="text-32 fw-semibold text-success-600 mb-0">${activeRoomCount}</h2>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <span class="text-14 text-neutral-500">Inactive</span>
                            <h2 class="text-32 fw-semibold text-danger-600 mb-0">${inactiveRoomCount}</h2>
                        </div>
                    </div>
                </div>

                <div class="bg-white rounded-10 px-24 py-24" data-gape-sort-root data-gape-group-item-label="room">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">Room Management</h2>
                            <span class="text-14 text-neutral-500">Rooms available for presential and hybrid lessons.</span>
                        </div>
                        <div class="d-flex align-items-center gap-10 flex-wrap">
                            <div class="gape-management-actions d-flex align-items-center gap-12 flex-wrap">
                                <div class="dropdown">
                                    <button type="button"
                                            class="gape-filter-toggle border-neutral-30 border px-20 py-12 rounded-12 fw-semibold text-neutral-700 hover-bg-main-50 transition-03 bg-white d-flex align-items-center gap-8"
                                            data-bs-toggle="dropdown"
                                            data-bs-auto-close="outside"
                                            data-gape-sort-toggle
                                            aria-expanded="false">
                                        <i class="ph ph-sort-ascending"></i>Sort by
                                    </button>
                                    <ul class="dropdown-menu dropdown-menu-end rounded-12">
                                        <li>
                                            <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-gape-sort-option data-sort-field="code" data-sort-normal="asc" data-sort-state="none" aria-pressed="false">
                                                <span>Code</span>
                                                <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                        <li>
                                            <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-gape-sort-option data-sort-field="date" data-sort-normal="desc" data-sort-type="number" data-sort-state="none" aria-pressed="false">
                                                <span>Date</span>
                                                <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                        <li>
                                            <button type="button" class="dropdown-item gape-sort-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-gape-sort-option data-sort-field="status" data-sort-normal="asc" data-sort-state="none" aria-pressed="false">
                                                <span>State</span>
                                                <span class="gape-sort-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-sort-arrow gape-sort-arrow--normal"></i><i class="ph ph-arrow-down gape-sort-arrow gape-sort-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                    </ul>
                                </div>
                                <div class="dropdown">
                                    <button type="button"
                                            class="gape-filter-toggle border-neutral-30 border px-20 py-12 rounded-12 fw-semibold text-neutral-700 hover-bg-main-50 transition-03 bg-white d-flex align-items-center gap-8"
                                            data-bs-toggle="dropdown"
                                            data-bs-auto-close="outside"
                                            data-gape-group-toggle
                                            aria-expanded="false">
                                        <i class="ph ph-stack"></i>Group by
                                    </button>
                                    <ul class="dropdown-menu dropdown-menu-end rounded-12">
                                        <li>
                                            <button type="button" class="dropdown-item gape-group-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-gape-group-option data-group-field="organization" data-group-normal="asc" data-group-state="none" aria-pressed="false">
                                                <span>Organization</span>
                                                <span class="gape-group-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-group-arrow gape-group-arrow--normal"></i><i class="ph ph-arrow-down gape-group-arrow gape-group-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                        <li>
                                            <button type="button" class="dropdown-item gape-group-option d-flex align-items-center justify-content-between gap-16 px-16 py-10" data-gape-group-option data-group-field="organicUnit" data-group-normal="asc" data-group-state="none" aria-pressed="false">
                                                <span>Organic Unit</span>
                                                <span class="gape-group-arrows d-flex align-items-center justify-content-end gap-4" aria-hidden="true"><i class="ph ph-arrow-up gape-group-arrow gape-group-arrow--normal"></i><i class="ph ph-arrow-down gape-group-arrow gape-group-arrow--reverse"></i></span>
                                            </button>
                                        </li>
                                    </ul>
                                </div>
                            </div>
                            <c:if test="${not empty topActionHref}">
                                <a href="${pageContext.request.contextPath}${topActionHref}" class="bg-main-600 px-20 py-10 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03 d-inline-flex align-items-center" style="min-height: 44px;">
                                    <i class="ph ph-plus-circle me-8"></i>${topActionLabel}
                                </a>
                            </c:if>
                        </div>
                    </div>

                    <div class="overflow-x-auto">
                        <table class="table mb-0">
                            <thead>
                            <tr>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Room</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600" data-gape-group-hide-when-grouped>Organization Context</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Capacity</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Location</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Actions</th>
                            </tr>
                            </thead>
                            <tbody data-gape-sort-list>
                            <c:forEach var="room" items="${rooms}" varStatus="roomLoop">
                                <c:set var="canManageRoomRow" value="${canManageRoomByCode[room.code]}"/>
                                <tr class="hover-bg-neutral-20 border-bottom transition-03"
                                    data-gape-sort-row
                                    data-sort-index="${roomLoop.index}"
                                    data-sort-name="${fn:escapeXml(room.code)} ${fn:escapeXml(room.name)}"
                                    data-sort-code="${fn:escapeXml(room.code)}"
                                    data-sort-date="${roomLoop.index}"
                                    data-sort-capacity="${room.capacity}"
                                    data-sort-location="${fn:escapeXml(room.location)}"
                                    data-sort-status="${fn:escapeXml(room.stateLabel)}"
                                    data-group-organization-id="${room.organizationId}"
                                    data-group-organization="${fn:escapeXml(room.organizationName)}"
                                    data-group-organic-unit-id="${room.organicUnitId}"
                                    data-group-organic-unit="${fn:escapeXml(room.organicUnitName)}"
                                    data-group-status="${fn:escapeXml(room.stateLabel)}">
                                    <td class="py-20 px-20">
                                        <a href="${pageContext.request.contextPath}/learning/rooms/${room.encodedCode}?returnTo=${currentReturnToParam}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                            <c:out value="${room.code}"/>
                                        </a>
                                        <span class="d-block text-12 text-neutral-500"><c:out value="${room.name}"/></span>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500" title="<c:out value='${room.contextTitle}'/>" data-gape-group-hide-when-grouped>
                                        <c:out value="${room.contextHtml}" escapeXml="false"/>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${room.capacityLabel}"/></td>
                                    <td class="py-20 px-20 text-14 text-neutral-500"><c:out value="${room.location}"/></td>
                                    <td class="py-20 px-20">
                                        <span class="${room.stateBadgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13">
                                            <c:out value="${room.stateLabel}"/>
                                        </span>
                                    </td>
                                    <td class="py-20 px-20 text-end">
                                        <div class="d-flex align-items-center gap-12 justify-content-end">
                                            <a href="${pageContext.request.contextPath}/learning/rooms/${room.encodedCode}?returnTo=${currentReturnToParam}" class="text-22 text-neutral-500 hover-text-main-600" title="Detail">
                                                <i class="ph ph-eye"></i>
                                            </a>
                                            <c:if test="${canManageRoomRow}">
                                                <a href="${pageContext.request.contextPath}/learning/rooms/${room.encodedCode}/edit?returnTo=${currentReturnToParam}" class="text-22 text-neutral-500 hover-text-main-600" title="Edit">
                                                    <i class="ph ph-pencil-simple-line"></i>
                                                </a>
                                            </c:if>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty rooms}">
                                <tr>
                                    <td colspan="6" class="py-32 px-20 text-center text-14 text-neutral-500">No rooms available in your context.</td>
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
</body>
</html>
