<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
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
                            <h2 class="text-32 fw-semibold text-danger-600 mb-0">${archivedRoomCount}</h2>
                        </div>
                    </div>
                </div>

                <div class="bg-white rounded-10 px-24 py-24">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">Room Management</h2>
                            <span class="text-14 text-neutral-500">Rooms available for presential and hybrid lessons.</span>
                        </div>
                        <div class="d-flex align-items-center gap-10 flex-wrap">
                            <form action="${pageContext.request.contextPath}/learning/rooms" method="get" class="d-flex align-items-center gap-10 flex-wrap mb-0">
                                <select name="scope" class="form-select px-16 py-10 text-14 bg-neutral-20 border-neutral-30 border rounded-8" style="min-width: 280px; min-height: 44px;" aria-label="Room scope filter" onchange="this.form.submit()">
                                    <c:forEach var="option" items="${roomScopeOptions}">
                                        <option value="${option.value}" title="<c:out value='${option.title}'/>" ${option.selected ? 'selected' : ''}>
                                            <c:out value="${option.label}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                            </form>
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
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Organization Context</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Capacity</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">Location</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600">State</th>
                                <th class="py-16 px-20 text-14 fw-medium text-neutral-600 text-end">Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="room" items="${rooms}">
                                <c:set var="canManageRoomRow" value="${canManageRoomByCode[room.code]}"/>
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-20 px-20">
                                        <a href="${pageContext.request.contextPath}/learning/rooms/${room.encodedCode}?returnTo=${currentReturnToParam}" class="fw-medium text-14 text-neutral-700 hover-text-main-600">
                                            <c:out value="${room.code}"/>
                                        </a>
                                        <span class="d-block text-12 text-neutral-500"><c:out value="${room.name}"/></span>
                                    </td>
                                    <td class="py-20 px-20 text-14 text-neutral-500" title="<c:out value='${room.contextTitle}'/>">
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
