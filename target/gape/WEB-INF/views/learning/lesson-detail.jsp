<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Lesson Detail</title>
    <%@ include file="/WEB-INF/fragments/template-base-head.jspf" %>
    <c:if test="${lessonModal}">
    <style>
        html, body { background: #fff; min-height: 0; }
        .preloader, .overlay, .side-overlay, .sidebar, .dashboard-sidebar, .dashbord-header, .dashboard-header,
        .gape-dashboard-mobile-menu-slot, .gape-dashboard-page-heading,
        .dashbord-body > .dashboard-footer, .dashbord-body > footer,
        .dashbord-body > .bg-neutral-20.border-top { display: none !important; }
        .dashbord, .dashbord > .d-flex, .dashbord-body { background: #fff !important; display: block !important; min-height: 0 !important; }
        .dashbord-body > .px-24.py-24 { padding: 0 !important; }
    </style>
    </c:if>
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
                            <div class="d-flex align-items-center gap-10 flex-wrap mb-12">
                                <span class="${lesson.typeBadgeClass} px-14 py-8 rounded-pill text-13">
                                    <i class="${lesson.typeIconClass} me-6"></i><c:out value="${lesson.typeLabel}"/>
                                </span>
                                <span class="${lesson.stateBadgeClass} px-14 py-8 border-neutral-30 border rounded-pill text-13">
                                    <c:out value="${lesson.stateLabel}"/>
                                </span>
                            </div>
                            <h2 class="text-28 fw-semibold text-neutral-800 mb-8"><c:out value="${lesson.title}"/></h2>
                            <div class="d-flex align-items-stretch gap-12 flex-wrap mt-16">
                                <div class="bg-neutral-20 border border-neutral-30 rounded-8 px-16 py-12">
                                    <span class="text-12 text-neutral-500 d-block mb-4">Class group</span>
                                    <strong class="text-14 text-neutral-700"><c:out value="${classGroup.code}"/></strong>
                                </div>
                                <div class="bg-neutral-20 border border-neutral-30 rounded-8 px-16 py-12 min-w-0" title="<c:out value='${classGroup.contextGroupTitle}'/>">
                                    <span class="text-12 text-neutral-500 d-block mb-4">Context</span>
                                    <strong class="text-14 text-neutral-700"><c:out value="${classGroup.contextGroupHtml}" escapeXml="false"/></strong>
                                </div>
                                <div class="bg-neutral-20 border border-neutral-30 rounded-8 px-16 py-12">
                                    <span class="text-12 text-neutral-500 d-block mb-4">Pedagogical block</span>
                                    <strong class="text-14 text-neutral-700"><c:out value="${contentBlock.name}"/></strong>
                                </div>
                            </div>
                        </div>
                        <div class="d-flex align-items-center gap-10 flex-wrap">
                            <c:choose>
                                <c:when test="${lessonModal}"><button type="button" class="border-main-600 border px-20 py-10 fw-semibold rounded-8 hover-bg-main-50 transition-03 bg-white" data-gape-lesson-modal-close><i class="ph ph-arrow-left me-8"></i>Back</button></c:when>
                                <c:otherwise><a href="${fn:escapeXml(lessonBackHref)}" class="border-main-600 border px-20 py-10 fw-semibold rounded-8 hover-bg-main-50 transition-03"><i class="ph ph-arrow-left me-8"></i>Back</a></c:otherwise>
                            </c:choose>
                            <c:if test="${canManageLesson}">
                                <a href="${lessonEditHref}" class="border-main-600 border px-20 py-10 fw-semibold rounded-8 hover-bg-main-50 transition-03">
                                    <i class="ph ph-pencil-simple-line me-8"></i>Edit
                                </a>
                            </c:if>
                        </div>
                    </div>
                </section>

                <div class="row gy-4">
                    <div class="col-xl-8">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30 h-100">
                            <h3 class="text-18 fw-medium text-neutral-700 mb-16">Lesson Information</h3>
                            <p class="text-14 text-neutral-500 mb-24"><c:out value="${lesson.description}"/></p>
                            <div class="row gy-3">
                                <div class="col-md-6">
                                    <div class="bg-neutral-20 rounded-8 px-16 py-14 h-100">
                                        <span class="text-12 text-neutral-500 d-block mb-6">Starts</span>
                                        <strong class="text-14 text-neutral-700" data-gape-datetime-display><c:out value="${lesson.startsAt}"/></strong>
                                    </div>
                                </div>
                                <div class="col-md-6">
                                    <div class="bg-neutral-20 rounded-8 px-16 py-14 h-100">
                                        <span class="text-12 text-neutral-500 d-block mb-6">Ends</span>
                                        <strong class="text-14 text-neutral-700" data-gape-datetime-display><c:out value="${lesson.endsAt}"/></strong>
                                    </div>
                                </div>
                                <div class="col-md-6">
                                    <div class="bg-neutral-20 rounded-8 px-16 py-14 h-100">
                                        <span class="text-12 text-neutral-500 d-block mb-6">Attendance</span>
                                        <strong class="text-14 text-neutral-700"><c:out value="${lesson.attendanceLabel}"/></strong>
                                    </div>
                                </div>
                                <div class="col-md-6">
                                    <div class="bg-neutral-20 rounded-8 px-16 py-14 h-100">
                                        <span class="text-12 text-neutral-500 d-block mb-6">Provider</span>
                                        <strong class="text-14 text-neutral-700"><c:out value="${lesson.providerLabel}"/></strong>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="col-xl-4">
                        <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30 mb-24">
                            <h3 class="text-18 fw-medium text-neutral-700 mb-16">Access</h3>
                            <c:choose>
                                <c:when test="${lesson.hasMeetingLink}">
                                    <a href="${fn:escapeXml(lesson.accessUrl)}" target="_blank" rel="noopener noreferrer" class="bg-main-600 px-20 py-12 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03 d-inline-flex align-items-center">
                                        <i class="ph ph-video-camera me-8"></i>Open Meeting
                                    </a>
                                </c:when>
                                <c:when test="${lesson.hasRoom}">
                                    <div class="border border-neutral-30 rounded-8 px-16 py-14">
                                        <span class="text-12 text-neutral-500 d-block mb-6">Physical room</span>
                                        <strong class="text-16 text-neutral-700"><c:out value="${lesson.physicalRoomCode}"/></strong>
                                        <c:if test="${not empty physicalRoom}">
                                            <span class="d-block text-13 text-neutral-500 mt-6"><c:out value="${physicalRoom.name}"/> | <c:out value="${physicalRoom.location}"/></span>
                                        </c:if>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <p class="text-14 text-neutral-500 mb-0">No access data configured.</p>
                                </c:otherwise>
                            </c:choose>
                        </div>

                        <c:if test="${canManageLesson}">
                            <div class="bg-white rounded-10 px-24 py-24 border border-neutral-30">
                                <h3 class="text-18 fw-medium text-neutral-700 mb-16">Actions</h3>
                                <div class="d-flex flex-column gap-10">
                                    <form action="${pageContext.request.contextPath}/learning/lessons/${lesson.id}/complete" method="post" class="m-0">
                                        <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                        <c:if test="${lessonModal}"><input type="hidden" name="modal" value="1"></c:if>
                                        <c:if test="${not empty returnTo}"><input type="hidden" name="returnTo" value="<c:out value='${returnTo}'/>"></c:if>
                                        <button type="submit" class="border-main-600 border px-20 py-10 fw-semibold rounded-8 hover-bg-main-50 transition-03 w-100 text-start">
                                            <i class="ph ph-check-circle me-8"></i>Complete
                                        </button>
                                    </form>
                                    <form action="${pageContext.request.contextPath}/learning/lessons/${lesson.id}/cancel" method="post" class="m-0">
                                        <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                        <c:if test="${lessonModal}"><input type="hidden" name="modal" value="1"></c:if>
                                        <c:if test="${not empty returnTo}"><input type="hidden" name="returnTo" value="<c:out value='${returnTo}'/>"></c:if>
                                        <button type="submit" class="border-warning-600 border px-20 py-10 fw-semibold rounded-8 text-warning-600 hover-bg-warning-50 transition-03 w-100 text-start">
                                            <i class="ph ph-prohibit me-8"></i>Cancel
                                        </button>
                                    </form>
                                    <form action="${pageContext.request.contextPath}/learning/lessons/${lesson.id}/delete" method="post" class="m-0">
                                        <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                                        <c:if test="${lessonModal}"><input type="hidden" name="modal" value="1"></c:if>
                                        <c:if test="${not empty returnTo}"><input type="hidden" name="returnTo" value="<c:out value='${returnTo}'/>"></c:if>
                                        <button type="submit" class="bg-danger-600 px-20 py-10 fw-semibold rounded-8 text-white transition-03 w-100 text-start">
                                            <i class="ph ph-trash me-8"></i>Delete
                                        </button>
                                    </form>
                                </div>
                            </div>
                        </c:if>
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
