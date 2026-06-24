<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="pt.isel.gape.web.view.DashboardMyProfilePageData" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    if (!DashboardMyProfilePageData.prepare(request, response)) {
        return;
    }
    request.setAttribute("activeMenu", "profile");
    request.setAttribute("pageTitle", "My Profile");
    request.setAttribute("studentPageTitle", "My Profile");
    request.setAttribute("returnTo", "/student/student/profile/student-profile.jsp");
    request.setAttribute("profileContentFlush", Boolean.TRUE);
%>
<%@ include file="/WEB-INF/fragments/student-dashboard-start.jspf" %>

<%@ include file="/WEB-INF/fragments/dashboard-my-profile-content.jspf" %>

<%@ include file="/WEB-INF/fragments/student-dashboard-end.jspf" %>
