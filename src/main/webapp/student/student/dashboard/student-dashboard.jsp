<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    request.setAttribute("activeMenu", "dashboard");
    request.setAttribute("pageTitle", "Dashboard");
    request.setAttribute("studentPageTitle", "Dashboard");
    request.setAttribute("studentPageDescription", "A student-first view for courses, class groups, messages and upcoming lessons.");
%>
<%@ include file="/WEB-INF/fragments/student-dashboard-start.jspf" %>

<div class="row gy-4 mb-24">
    <div class="col-xl-3 col-sm-6">
        <a href="${pageContext.request.contextPath}/student/courses" class="gape-student-stat-card d-block h-100">
            <div class="d-flex align-items-center justify-content-between gap-14 mb-18">
                <div>
                    <span class="text-14 text-neutral-500 d-block mb-4">Class Courses</span>
                    <strong class="text-24 text-neutral-800">Open</strong>
                </div>
                <span class="gape-student-stat-icon bg-main-600"><img src="${pageContext.request.contextPath}/assets/images/icons/dashbord-item1.png" alt=""></span>
            </div>
            <span class="gape-student-card-icon-button" aria-hidden="true"><i class="ph ph-arrow-right"></i></span>
        </a>
    </div>
    <div class="col-xl-3 col-sm-6">
        <a href="${pageContext.request.contextPath}/student/subjects" class="gape-student-stat-card d-block h-100">
            <div class="d-flex align-items-center justify-content-between gap-14 mb-18">
                <div>
                    <span class="text-14 text-neutral-500 d-block mb-4">Subjects</span>
                    <strong class="text-24 text-neutral-800">Study</strong>
                </div>
                <span class="gape-student-stat-icon bg-success-600"><img src="${pageContext.request.contextPath}/assets/images/icons/dashbord-item2.png" alt=""></span>
            </div>
            <span class="gape-student-card-icon-button" aria-hidden="true"><i class="ph ph-arrow-right"></i></span>
        </a>
    </div>
    <div class="col-xl-3 col-sm-6">
        <a href="${pageContext.request.contextPath}/student/class-groups" class="gape-student-stat-card d-block h-100">
            <div class="d-flex align-items-center justify-content-between gap-14 mb-18">
                <div>
                    <span class="text-14 text-neutral-500 d-block mb-4">Class Groups</span>
                    <strong class="text-24 text-neutral-800">Join</strong>
                </div>
                <span class="gape-student-stat-icon bg-warning-600"><img src="${pageContext.request.contextPath}/assets/images/icons/dashbord-item3.png" alt=""></span>
            </div>
            <span class="gape-student-card-icon-button" aria-hidden="true"><i class="ph ph-arrow-right"></i></span>
        </a>
    </div>
    <div class="col-xl-3 col-sm-6">
        <a href="${pageContext.request.contextPath}/student/lessons" class="gape-student-stat-card d-block h-100">
            <div class="d-flex align-items-center justify-content-between gap-14 mb-18">
                <div>
                    <span class="text-14 text-neutral-500 d-block mb-4">Lessons</span>
                    <strong class="text-24 text-neutral-800">Attend</strong>
                </div>
                <span class="gape-student-stat-icon bg-neutral-900"><img src="${pageContext.request.contextPath}/assets/images/icons/dashbord-item4.png" alt=""></span>
            </div>
            <span class="gape-student-card-icon-button" aria-hidden="true"><i class="ph ph-arrow-right"></i></span>
        </a>
    </div>
</div>

<div class="row gy-4">
    <div class="col-xl-8">
        <section class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30 h-100">
            <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-20">
                <div>
                    <h3 class="text-20 fw-semibold text-neutral-800 mb-4">In progress</h3>
                    <span class="text-14 text-neutral-500">Start from the academic area that matches your current learning activity.</span>
                </div>
                <a href="${pageContext.request.contextPath}/student/calendar" class="gape-student-card-icon-button" aria-label="Open calendar" title="Open calendar">
                    <i class="ph ph-calendar-dots"></i>
                </a>
            </div>
            <div class="row gy-3">
                <div class="col-md-6">
                    <div class="gape-student-line-card px-20 py-18 h-100">
                        <span class="text-13 text-neutral-500 d-block mb-8">Learning route</span>
                        <strong class="text-16 text-neutral-800">Courses -> Subjects -> Class Groups -> Lessons</strong>
                        <div class="gape-student-progress mt-16"><span style="width: 68%;"></span></div>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="gape-student-line-card px-20 py-18 h-100">
                        <span class="text-13 text-neutral-500 d-block mb-8">Quick message center</span>
                        <strong class="text-16 text-neutral-800">Messages and updates stay separate from class content.</strong>
                        <a href="${pageContext.request.contextPath}/student/student/message/student-message.jsp" class="gape-student-card-icon-button mt-16" aria-label="Open message" title="Open message">
                            <i class="ph ph-arrow-right"></i>
                        </a>
                    </div>
                </div>
            </div>
        </section>
    </div>
    <div class="col-xl-4">
        <section class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30 h-100">
            <h3 class="text-20 fw-semibold text-neutral-800 mb-16">Next actions</h3>
            <div class="d-flex flex-column gap-12">
                <a href="${pageContext.request.contextPath}/student/courses" class="gape-student-line-card px-16 py-14 d-flex align-items-center gap-12 hover-border-main-600 transition-03">
                    <span class="gape-student-icon gape-student-soft-blue text-20"><i class="ph ph-plus-circle"></i></span>
                    <span class="text-14 fw-semibold text-neutral-700">Review available class courses</span>
                </a>
                <a href="${pageContext.request.contextPath}/student/class-groups" class="gape-student-line-card px-16 py-14 d-flex align-items-center gap-12 hover-border-main-600 transition-03">
                    <span class="gape-student-icon gape-student-soft-violet text-20"><i class="ph ph-users-three"></i></span>
                    <span class="text-14 fw-semibold text-neutral-700">Check your class groups</span>
                </a>
                <a href="${pageContext.request.contextPath}/student/lessons" class="gape-student-line-card px-16 py-14 d-flex align-items-center gap-12 hover-border-main-600 transition-03">
                    <span class="gape-student-icon gape-student-soft-amber text-20"><i class="ph ph-play-circle"></i></span>
                    <span class="text-14 fw-semibold text-neutral-700">Open current lessons</span>
                </a>
            </div>
        </section>
    </div>
</div>

<%@ include file="/WEB-INF/fragments/student-dashboard-end.jspf" %>
