<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    if (request.getAttribute("activeMenu") == null) {
        request.setAttribute("activeMenu", "organizations");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Organic Unit</title>
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
                <form action="${unitFormAction}" method="post" class="bg-white rounded-10 px-40 py-40">
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap border-bottom-dashed pb-24 mb-24">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">${creating ? 'Create Organic Unit' : 'Edit Organic Unit'}</h2>
                            <span class="text-14 text-neutral-500"><c:out value="${organization.name}"/></span>
                        </div>
                        <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}" class="border-main-600 border px-20 py-10 fw-semibold rounded-12 hover-bg-main-50 transition-03">Back</a>
                    </div>

                    <div class="row gy-4">
                        <c:if test="${not creating and not empty form.code}">
                            <div class="col-lg-3">
                                <span class="fw-medium text-base text-neutral-800 mb-12 d-block">Generated Code</span>
                                <div class="px-24 py-14 text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14">
                                    <c:out value="${form.code}"/>
                                </div>
                            </div>
                        </c:if>
                        <div class="${not creating and not empty form.code ? 'col-lg-6' : 'col-lg-8'}">
                            <label for="name" class="fw-medium text-base text-neutral-800 mb-12">Name</label>
                            <input id="name" name="name" type="text" value="<c:out value='${form.name}'/>" required class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="${not creating and not empty form.code ? 'col-lg-3' : 'col-lg-4'}">
                            <label for="acronym" class="fw-medium text-base text-neutral-800 mb-12">Acronym</label>
                            <input id="acronym" name="acronym" type="text" value="<c:out value='${form.acronym}'/>" class="form-control px-24 py-14 fw-normal text-14 text-neutral-700 bg-neutral-20 border-neutral-30 border rounded-14 focus-visible-outline focus-border-main-600">
                        </div>
                        <div class="col-lg-4 gape-select-field">
                            <label for="type" class="fw-medium text-base text-neutral-800 mb-12">Type</label>
                            <select id="type" name="type" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                <option value="SCHOOL" ${form.type == 'SCHOOL' ? 'selected' : ''}>School</option>
                                <option value="FACULTY" ${form.type == 'FACULTY' ? 'selected' : ''}>Faculty</option>
                                <option value="DEPARTMENT" ${form.type == 'DEPARTMENT' ? 'selected' : ''}>Department</option>
                                <option value="CENTER" ${form.type == 'CENTER' ? 'selected' : ''}>Center</option>
                                <option value="OFFICE" ${form.type == 'OFFICE' ? 'selected' : ''}>Office</option>
                                <option value="SERVICE" ${form.type == 'SERVICE' ? 'selected' : ''}>Service</option>
                                <option value="SECTION" ${form.type == 'SECTION' ? 'selected' : ''}>Section</option>
                                <option value="DIRECTION" ${form.type == 'DIRECTION' ? 'selected' : ''}>Direction</option>
                                <option value="OTHER" ${form.type == 'OTHER' ? 'selected' : ''}>Other</option>
                            </select>
                        </div>
                        <div class="col-lg-4 gape-select-field">
                            <label for="state" class="fw-medium text-base text-neutral-800 mb-12">State</label>
                            <select id="state" name="state" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                <option value="ACTIVE" ${form.state == 'ACTIVE' ? 'selected' : ''}>Active</option>
                                <option value="INACTIVE" ${form.state == 'INACTIVE' ? 'selected' : ''}>Inactive</option>
                            </select>
                        </div>
                        <div class="col-lg-4 gape-select-field">
                            <label for="parentOrganicUnitId" class="fw-medium text-base text-neutral-800 mb-12">Parent Unit</label>
                            <select id="parentOrganicUnitId" name="parentOrganicUnitId" class="form-select px-24 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-14 js-example-basic-single gape-eduall-select">
                                <option value="" ${empty form.parentOrganicUnitId ? 'selected' : ''}>Root</option>
                                <c:forEach var="parent" items="${parentOptions}">
                                    <option value="${parent.id}" ${form.parentOrganicUnitId == parent.id ? 'selected' : ''}>
                                        <c:out value="${parent.code}"/> - <c:out value="${parent.name}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>

                    <div class="d-flex align-items-center gap-16 flex-wrap mt-32">
                        <button type="submit" class="bg-main-600 px-24 py-12 rounded-12 fw-semibold text-white hover-bg-main-700 transition-03">Save Change</button>
                        <a href="${pageContext.request.contextPath}/admin/organizations/${organization.id}" class="border-main-600 border px-24 py-12 fw-semibold rounded-12 hover-bg-main-50 transition-03">Cancel</a>
                    </div>
                </form>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
</body>
</html>
