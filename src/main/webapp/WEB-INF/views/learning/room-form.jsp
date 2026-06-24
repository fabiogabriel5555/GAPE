<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <base href="${pageContext.request.contextPath}/">
    <title>GAPE - Physical Room Form</title>
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

                <form action="${formAction}" method="post" class="bg-white rounded-10 px-32 py-32 border border-neutral-30" data-room-form>
                    <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.csrfToken']}">
                    <c:if test="${not empty formReturnTo}">
                        <input type="hidden" name="returnTo" value="<c:out value='${formReturnTo}'/>">
                    </c:if>
                    <div class="d-flex align-items-center justify-content-between gap-16 flex-wrap border-bottom-dashed pb-24 mb-28">
                        <div>
                            <h2 class="text-18 fw-medium text-neutral-700 mb-4">${creating ? 'Create Physical Room' : 'Edit Physical Room'}</h2>
                            <span class="text-14 text-neutral-500">Set capacity, location and organizational context.</span>
                        </div>
                        <a href="${fn:escapeXml(roomBackHref)}" class="border-main-600 border px-20 py-10 fw-semibold rounded-8 hover-bg-main-50 transition-03">Back</a>
                    </div>

                    <c:if test="${not empty errorMessage}">
                        <div class="alert alert-danger mb-24" role="alert">
                            <c:out value="${errorMessage}"/>
                        </div>
                    </c:if>

                    <div class="row gy-4">
                        <div class="col-lg-4">
                            <label for="code" class="fw-medium text-base text-neutral-800 mb-12">Code</label>
                            <input id="code" name="code" type="text" maxlength="40" required ${creating ? '' : 'readonly'} value="<c:out value='${form.code}'/>" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                        </div>
                        <div class="col-lg-8">
                            <label for="name" class="fw-medium text-base text-neutral-800 mb-12">Name</label>
                            <input id="name" name="name" type="text" maxlength="120" required value="<c:out value='${form.name}'/>" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                        </div>
                        <div class="col-lg-6">
                            <label for="organizationId" class="fw-medium text-base text-neutral-800 mb-12">Organization</label>
                            <select id="organizationId" name="organizationId" required class="form-select px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8" data-room-organization>
                                <option value="">Select organization</option>
                                <c:forEach var="organization" items="${organizationOptions}">
                                    <option value="${organization.id}" ${form.organizationId == organization.id ? 'selected' : ''}><c:out value="${organization.label}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-lg-6">
                            <label for="organicUnitId" class="fw-medium text-base text-neutral-800 mb-12">Organic Unit</label>
                            <select id="organicUnitId" name="organicUnitId" class="form-select px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8" data-room-organic-unit>
                                <option value="">No organic unit</option>
                                <c:forEach var="unit" items="${organicUnitOptions}">
                                    <option value="${unit.id}" data-organization-id="${unit.organizationId}" ${form.organicUnitId == unit.id ? 'selected' : ''}><c:out value="${unit.label}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-lg-4">
                            <label for="capacity" class="fw-medium text-base text-neutral-800 mb-12">Capacity</label>
                            <input id="capacity" name="capacity" type="number" min="1" required value="<c:out value='${form.capacity}'/>" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                        </div>
                        <div class="col-lg-4">
                            <label for="state" class="fw-medium text-base text-neutral-800 mb-12">State</label>
                            <select id="state" name="state" required class="form-select px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                                <c:forEach var="state" items="${roomStates}">
                                    <option value="${state.value}" ${state.selected ? 'selected' : ''}><c:out value="${state.label}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-lg-4">
                            <label for="location" class="fw-medium text-base text-neutral-800 mb-12">Location</label>
                            <input id="location" name="location" type="text" maxlength="255" value="<c:out value='${form.location}'/>" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8">
                        </div>
                        <div class="col-12">
                            <label for="description" class="fw-medium text-base text-neutral-800 mb-12">Description</label>
                            <textarea id="description" name="description" maxlength="500" rows="3" class="form-control px-20 py-14 text-14 bg-neutral-20 border-neutral-30 border rounded-8"><c:out value="${form.description}"/></textarea>
                        </div>
                    </div>

                    <div class="d-flex align-items-center gap-14 flex-wrap border-top-dashed pt-24 mt-28">
                        <button type="submit" class="bg-main-600 px-24 py-12 rounded-8 fw-semibold text-white hover-bg-main-700 transition-03">${creating ? 'Create Room' : 'Save Changes'}</button>
                        <a href="${fn:escapeXml(roomBackHref)}" class="text-neutral-600 fw-semibold hover-text-main-600 transition-03">Cancel</a>
                    </div>
                </form>
            </div>
            <%@ include file="/WEB-INF/fragments/dashboard-footer.jspf" %>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/fragments/template-base-scripts.jspf" %>
<script>
    (function () {
        function ready(callback) {
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', callback);
                return;
            }
            callback();
        }

        ready(function () {
            var form = document.querySelector('[data-room-form]');
            if (!form) {
                return;
            }
            var organization = form.querySelector('[data-room-organization]');
            var unit = form.querySelector('[data-room-organic-unit]');
            var originalUnits = unit ? Array.prototype.slice.call(unit.options).map(function (option) {
                return { value: option.value, text: option.textContent, organizationId: option.dataset.organizationId || '' };
            }) : [];

            function rebuildUnits() {
                if (!organization || !unit) {
                    return;
                }
                var previousValue = unit.value;
                unit.innerHTML = '';
                var empty = document.createElement('option');
                empty.value = '';
                empty.textContent = 'No organic unit';
                unit.appendChild(empty);
                originalUnits.filter(function (optionData) {
                    return optionData.value && optionData.organizationId === organization.value;
                }).forEach(function (optionData) {
                    var option = document.createElement('option');
                    option.value = optionData.value;
                    option.textContent = optionData.text;
                    option.dataset.organizationId = optionData.organizationId;
                    unit.appendChild(option);
                });
                unit.value = previousValue;
                if (unit.value !== previousValue) {
                    unit.value = '';
                }
            }

            if (organization) {
                organization.addEventListener('change', rebuildUnits);
            }
            rebuildUnits();
        });
    })();
</script>
</body>
</html>
