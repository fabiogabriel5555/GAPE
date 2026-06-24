<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    request.setAttribute("activeMenu", "message");
    request.setAttribute("pageTitle", "Message");
    request.setAttribute("studentPageTitle", "Message");
    request.setAttribute("studentPageDescription", "Messages, announcements and reminders for your student learning activity.");
%>
<%@ include file="/WEB-INF/fragments/student-dashboard-start.jspf" %>

<section class="gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30">
    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap mb-24">
        <div>
            <h3 class="text-20 fw-semibold text-neutral-800 mb-4">Student messages</h3>
            <span class="text-14 text-neutral-500">Academic communication for classes, lessons and support.</span>
        </div>
        <span class="bg-main-50 text-main-600 px-14 py-8 rounded-pill text-13 fw-semibold">
            <i class="ph ph-chat-dots me-6"></i>Inbox
        </span>
    </div>
    <div class="row gy-4 align-items-stretch">
        <div class="col-xl-4">
            <div class="gape-student-card gape-student-card--plain px-20 py-20 h-100">
                <div class="gape-student-course-thumb gape-student-course-thumb--visual mb-18">
                    <img src="${pageContext.request.contextPath}/assets/images/shapes/shape2.png" alt="" class="gape-student-course-thumb__shape">
                    <span class="gape-student-course-thumb__ring"></span>
                    <span class="gape-student-course-thumb__icon"><i class="ph ph-chats-circle"></i></span>
                </div>
                <h4 class="text-18 fw-semibold text-neutral-800 mb-8">Inbox</h4>
                <p class="text-14 text-neutral-500 mb-0">Class updates and instructor notes appear here when the message module sends them.</p>
            </div>
        </div>
        <div class="col-xl-8">
            <div class="d-flex flex-column gap-14 h-100">
                <div class="gape-student-line-card px-20 py-18 d-flex align-items-center gap-14">
                    <span class="gape-student-icon gape-student-soft-blue text-22 flex-shrink-0"><i class="ph ph-megaphone"></i></span>
                    <div class="min-w-0">
                        <h4 class="text-15 fw-semibold text-neutral-800 mb-4">Announcements</h4>
                        <p class="text-13 text-neutral-500 mb-0">Class group and course announcements will appear here.</p>
                    </div>
                </div>
                <div class="gape-student-line-card px-20 py-18 d-flex align-items-center gap-14">
                    <span class="gape-student-icon gape-student-soft-green text-22 flex-shrink-0"><i class="ph ph-note-pencil"></i></span>
                    <div class="min-w-0">
                        <h4 class="text-15 fw-semibold text-neutral-800 mb-4">Instructor notes</h4>
                        <p class="text-13 text-neutral-500 mb-0">Messages from instructors stay connected to the learning context.</p>
                    </div>
                </div>
                <div class="gape-student-line-card px-20 py-18 d-flex align-items-center gap-14">
                    <span class="gape-student-icon gape-student-soft-amber text-22 flex-shrink-0"><i class="ph ph-bell-ringing"></i></span>
                    <div class="min-w-0">
                        <h4 class="text-15 fw-semibold text-neutral-800 mb-4">Reminders</h4>
                        <p class="text-13 text-neutral-500 mb-0">Important lesson and class group reminders are grouped here.</p>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="gape-student-empty text-center px-24 py-40 mt-24">
        <span class="gape-student-icon gape-student-soft-blue text-28 mb-16"><i class="ph ph-envelope-open"></i></span>
        <h4 class="text-18 fw-semibold text-neutral-800 mb-8">No student messages yet</h4>
        <p class="text-14 text-neutral-500 mb-0">When a class or instructor sends an update, it will appear in this workspace.</p>
    </div>
</section>

<%@ include file="/WEB-INF/fragments/student-dashboard-end.jspf" %>
