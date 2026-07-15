<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Room Detail</title>
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

                <section class="bg-white rounded-10 px-24 py-24 mb-24 border border-neutral-30">
                    <div class="d-flex align-items-start justify-content-between gap-16 flex-wrap">
                        <div class="min-w-0">
                            <span class="${room.stateBadgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13 d-inline-block mb-12">
                                <c:out value="${room.stateLabel}"/>
                            </span>
                            <h2 class="text-28 fw-semibold text-neutral-800 mb-8"><c:out value="${room.code}"/> - <c:out value="${room.name}"/></h2>
                            <p class="text-14 text-neutral-500 mb-0"><c:out value="${room.organizationName}"/> | <c:out value="${room.organicUnitName}"/></p>
                        </div>
                        <div class="d-flex align-items-center gap-10 flex-wrap">
                            <a href="${fn:escapeXml(roomBackHref)}" class="border-main-600 border px-20 py-10 fw-semibold rounded-8 hover-bg-main-50 transition-03">
                                <i class="ph ph-arrow-left me-8"></i>Back
                            </a>
                            <c:if test="${canManageRoom}">
                                <a href="${roomEditHref}" class="border-main-600 border px-20 py-10 fw-semibold rounded-8 hover-bg-main-50 transition-03">
                                    <i class="ph ph-pencil-simple-line me-8"></i>Edit
                                </a>
                            </c:if>
                        </div>
                    </div>
                </section>

                <div class="row gy-4">
                    <div class="col-xl-4">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30 mb-24">
                            <h3 class="text-18 fw-medium text-neutral-700 mb-16">Room Information</h3>
                            <div class="d-flex flex-column gap-12">
                                <div class="bg-neutral-20 rounded-8 px-16 py-14">
                                    <span class="text-12 text-neutral-500 d-block mb-6">Capacity</span>
                                    <strong class="text-14 text-neutral-700"><c:out value="${room.capacityLabel}"/></strong>
                                </div>
                                <div class="bg-neutral-20 rounded-8 px-16 py-14">
                                    <span class="text-12 text-neutral-500 d-block mb-6">Location</span>
                                    <strong class="text-14 text-neutral-700"><c:out value="${room.location}"/></strong>
                                </div>
                                <div class="bg-neutral-20 rounded-8 px-16 py-14">
                                    <span class="text-12 text-neutral-500 d-block mb-6">Description</span>
                                    <strong class="text-14 text-neutral-700"><c:out value="${room.description}"/></strong>
                                </div>
                            </div>
                        </div>
                        <c:if test="${canManageRoom}">
                            <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                                <h3 class="text-18 fw-medium text-neutral-700 mb-16">Actions</h3>
                                <div class="d-flex flex-column gap-10">
                                    <form action="${pageContext.request.contextPath}/learning/rooms/${room.encodedCode}/archive" method="post" class="m-0">
                                        <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                        <c:if test="${not empty returnTo}"><input type="hidden" name="returnTo" value="<c:out value='${returnTo}'/>"></c:if>
                                        <button type="submit" class="border-warning-600 border px-20 py-10 fw-semibold rounded-8 text-warning-600 hover-bg-warning-50 transition-03 w-100 text-start">
                                            <i class="ph ph-archive-box me-8"></i>Deactivate
                                        </button>
                                    </form>
                                    <form action="${pageContext.request.contextPath}/learning/rooms/${room.encodedCode}/delete" method="post" class="m-0">
                                        <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                        <c:if test="${not empty returnTo}"><input type="hidden" name="returnTo" value="<c:out value='${returnTo}'/>"></c:if>
                                        <button type="submit" class="bg-danger-600 px-20 py-10 fw-semibold rounded-8 text-white transition-03 w-100 text-start">
                                            <i class="ph ph-trash me-8"></i>Delete
                                        </button>
                                    </form>
                                </div>
                            </div>
                        </c:if>
                    </div>
                    <div class="col-xl-8">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                            <h3 class="text-18 fw-medium text-neutral-700 mb-20">Scheduled Lessons</h3>
                            <div class="d-flex flex-column gap-12">
                                <c:forEach var="lesson" items="${roomLessons}">
                                    <div class="border border-neutral-30 rounded-8 px-16 py-14 d-flex align-items-start justify-content-between gap-16 flex-wrap">
                                        <div class="d-flex align-items-start gap-12">
                                            <span class="${lesson.typeBadgeClass} w-40 h-40 rounded-8 d-inline-flex align-items-center justify-content-center text-20 flex-shrink-0">
                                                <i class="${lesson.typeIconClass}"></i>
                                            </span>
                                            <div>
                                                <a href="${pageContext.request.contextPath}/learning/lessons/${lesson.id}?returnTo=${currentReturnToParam}" class="text-14 fw-semibold text-neutral-700 hover-text-main-600"><c:out value="${lesson.title}"/></a>
                                                <span class="d-block text-12 text-neutral-500" data-gape-datetime-display><c:out value="${lesson.dateRangeLabel}"/></span>
                                            </div>
                                        </div>
                                        <span class="${lesson.stateBadgeClass} px-12 py-7 border-neutral-30 border rounded-pill text-12"><c:out value="${lesson.stateLabel}"/></span>
                                    </div>
                                </c:forEach>
                                <c:if test="${empty roomLessons}">
                                    <div class="text-14 text-neutral-500 py-24 text-center">No lessons scheduled for this room.</div>
                                </c:if>
                            </div>
                        </div>
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
