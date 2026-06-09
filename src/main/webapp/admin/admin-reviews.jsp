<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    request.setAttribute("activeMenu", "reviews");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <base href="${pageContext.request.contextPath}/">
    <!-- Title -->
    <title>GAPE - Deshbord Reviews</title>
    <!-- Favicon -->
    <link rel="shortcut icon" href="assets/images/logo/favicon.png">
    <!-- Bootstrap -->
    <link rel="stylesheet" href="assets/css/bootstrap.min.css">
    <!-- select2 -->
    <link rel="stylesheet" href="assets/css/select2.min.css">
    <!-- Slick -->
    <link rel="stylesheet" href="assets/css/slick.css">
    <!-- Slick -->
    <link rel="stylesheet" href="assets/css/magnific-popup.css">
    <!-- jquery-ui -->
    <link rel="stylesheet" href="assets/css/jquery-ui.css">
    <!-- plyr Css -->
    <link rel="stylesheet" href="assets/css/plyr.css">
    <!-- Editor js Toolbar Start -->
    <link rel="stylesheet" href="assets/css/editor-quill.css">
    <!-- animate -->
    <link rel="stylesheet" href="assets/css/animate.css">
    <!-- dataTables.dataTables -->
    <link rel="stylesheet" href="assets/css/dataTables.dataTables.min.css">
    
    <link rel="stylesheet" href="assets/css/aos.css">
    <!-- Main css -->
    <link rel="stylesheet" href="assets/css/main.css">
</head> 
<body>
    
<!--==================== Preloader Start ====================-->
  <div class="preloader">
    <img src="assets/images/icons/preloader.gif" alt="">
  </div>
<!--==================== Preloader End ====================-->

<!--==================== Overlay Start ====================-->
<div class="overlay"></div>
<!--==================== Overlay End ====================-->

<!--==================== Sidebar Overlay End ====================-->
<div class="side-overlay"></div>
<!--==================== Sidebar Overlay End ====================-->

<!-- ==================== Scroll to Top End Here ==================== -->
<div class="progress-wrap">
  <svg class="progress-circle svg-content" width="100%" height="100%" viewBox="-1 -1 102 102">
      <path d="M50,1 a49,49 0 0,1 0,98 a49,49 0 0,1 0,-98" />
  </svg>
</div>
<!-- ==================== Scroll to Top End Here ==================== -->

<!-- ==================== Mobile Menu Start Here ==================== -->
<div class="mobile-menu scroll-sm d-lg-none d-block">
    <button type="button" class="close-button"><i class="ph ph-x"></i> </button>
    <div class="mobile-menu__inner">
        <a href="index.jsp" class="mobile-menu__logo">
            <img src="assets/images/logo/logo.svg" alt="Logo">
        </a>
        <div class="mobile-menu__menu">
            
<ul class="nav-menu flex-align nav-menu--mobile">
    <li class="nav-menu__item"><a href="index.jsp" class="nav-menu__link">Home</a></li>
    
    <li class="nav-menu__item has-submenu">
        <a href="javascript:void(0)" class="nav-menu__link">Courses</a>
         <ul class="nav-submenu scroll-sm">
            <li class="nav-submenu__item">
                <a href="course.jsp" class="nav-submenu__link hover-bg-neutral-30"> Course Grid View</a>
            </li>
            <li class="nav-submenu__item">
                <a href="course-list-view.jsp" class="nav-submenu__link hover-bg-neutral-30"> Course List View</a>
            </li>
            <li class="nav-submenu__item">
                <a href="course-details.jsp" class="nav-submenu__link hover-bg-neutral-30"> Course Details</a>
            </li>
            <li class="nav-submenu__item">
                <a href="lesson-details.jsp" class="nav-submenu__link hover-bg-neutral-30"> Lesson Details</a>
            </li>
        </ul>
    </li>
    <li class="nav-menu__item has-submenu">
        <a href="javascript:void(0)" class="nav-menu__link">Pages</a>
         <ul class="nav-submenu scroll-sm">
            <li class="nav-submenu__item">
                <a href="about-four.jsp" class="nav-submenu__link hover-bg-neutral-30"> About Four</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor.jsp" class="nav-submenu__link hover-bg-neutral-30"> Instructor</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-details.jsp" class="nav-submenu__link hover-bg-neutral-30"> Instructor Details</a>
            </li>
            <li class="nav-submenu__item">
                <a href="tutor.jsp" class="nav-submenu__link hover-bg-neutral-30"> Premium Tutors</a>
            </li>
            <li class="nav-submenu__item">
                <a href="tutor-details.jsp" class="nav-submenu__link hover-bg-neutral-30"> Premium Tutors Details</a>
            </li>
            <li class="nav-submenu__item">
                <a href="events.jsp" class="nav-submenu__link hover-bg-neutral-30">Events</a>
            </li>
            <li class="nav-submenu__item">
                <a href="event-details.jsp" class="nav-submenu__link hover-bg-neutral-30">Event Details</a>
            </li>
            <li class="nav-submenu__item">
                <a href="apply-admission.jsp" class="nav-submenu__link hover-bg-neutral-30">Apply Admission</a>
            </li>
        </ul>
    </li>
        <li class="nav-menu__item has-submenu">
        <a href="javascript:void(0)" class="nav-menu__link">Dashboard</a>
         <ul class="nav-submenu scroll-sm">
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord.jsp" class="nav-submenu__link hover-bg-neutral-30"> Admin Dashbord</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/profile" class="nav-submenu__link hover-bg-neutral-30">My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-home.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Admin Dashbord</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">Student My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-enrolled-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Enrolled Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-my-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Student My Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-assignment.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Assignment</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-settings.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Settings</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-home.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Dashbord</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-enrolled-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Enrolled Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-my-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor My Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-order-history.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Order History</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-my-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor My Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-announcements.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Announcements</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-assignment.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Assignment</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/instructor/instructor-account-settings.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Account Settings</a>
            </li>
        </ul>
    </li>
    <li class="nav-menu__item">
        <a href="contact.jsp" class="nav-menu__link">Contact</a>
    </li>
</ul>

            <div class="d-sm-none d-block mt-24">
                <div class="header-select border border-neutral-30 bg-main-25 rounded-pill position-relative">
    <span class="select-icon d-xxl-block d-none position-absolute top-50 translate-middle-y inset-inline-start-0 z-1 ms-lg-4 ms-12 text-xl pointer-event-none d-flex">
        <i class="ph-bold ph-squares-four"></i>
    </span>
    <select class="js-example-basic-single border-0" name="state">
        <option value="1" selected disabled>Categories</option>
        <option value="1">Design</option>
        <option value="1">Development</option>
        <option value="1">Architecture</option>
        <option value="1">Life Style</option>
        <option value="1">Data Science</option>
        <option value="1">Marketing</option>
        <option value="1">Music</option>
        <option value="1">Typography</option>
        <option value="1">Finance</option>
        <option value="1">Motivation</option>
    </select>
</div>
            </div>
            
        </div>
    </div>
</div>
<!-- ==================== Mobile Menu End Here ==================== -->


<!-- ==========message dashbord start=========== -->
<div class="dashbord bg-main-25">
    <div class="d-flex">
        
<!-- ========Dashdord Sidebar start======== -->
<%@ include file="/WEB-INF/fragments/dashboard-sidebar.jspf" %>
<!-- ========Dashdord Sidebar end======== -->

         <div class="dashbord-body my-profile flex-grow-1">
            <!-- =====messsage dashbord nab start======== -->
<div class="px-24 py-16 bg-neutral-10 border-bottom border-neutral-40 w-100">
    <div class="d-flex align-items-center justify-content-between gap-24 w-100">
        <div class="d-flex align-items-center gap-24">
            <button type="button" class="toggle-dashbord-button text-neutral-500 text-28 line-height-1 d-lg-none d-block">
                <i class="ph-bold ph-list"></i>
            </button>
            <div class="max-w-357-px position-relative d-sm-block d-none">
                <form action="#">
                    <input type="text" placeholder="Search" class="ps-16 pe-36 py-9 border border-neutral-40 rounded-pill focus-visible-outline focus-border-main-600 text-14 line-height-1">
                    <button type="button" class="w-28 h-28 bg-main-600 text-white text-16 rounded-circle justify-content-center align-items-center d-flex position-absolute top-50-percent translate-middle-y inset-inline-end-0-px me-4">
                        <i class="ph-bold ph-magnifying-glass"></i>
                    </button>
                </form>
            </div>
        </div>

        <div class="d-flex align-items-center gap-16">
            <a href="#" class="px-20 py-10 border-main-600 border rounded-pill text-14 text-main-600 hover-bg-main-600 hover-text-white hover-border-600 d-lg-block d-none line-height-1">Create a New Course</a>
            <div class="w-36 h-36 border-neutral-50 border rounded-pill justify-content-center align-items-center d-flex text-20 text-neutral-500 hover-bg-main-600  transition-03">
                <button type="button" class="hover-text-white transition-03">
                    <i class="ph ph-translate"></i>
                </button>
            </div>
            <div class="position-relative">
                <div class="w-36 h-36 border-neutral-50 border rounded-pill justify-content-center align-items-center d-flex text-20 text-neutral-500 position-relative">
                    <div>
                        <button class="dropdown-toggle w-36 h-36 border-neutral-50 border rounded-pill hover-bg-main-600 hover-text-white transition-03" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                            <i class="ph ph-bell-simple"></i>
                        </button>
                        <ul class="dropdown-menu rounded-12">

                            <li>
                                <a class="dropdown-item d-flex align-items-center gap-12 px-16 py-12" href="javascript:void(0)">
                                <div class="d-flex w-100 justify-content-between gap-12">
                                    <div class="d-flex align-items-center gap-12 ">
                                        <span class="w-36 h-36">
                                            <img src="assets/images/thumbs/reviewer-img1.png" alt="">
                                        </span>
                                        <div>
                                            <span class="text-md fw-semibold text-line-1">Ronald Richards</span>
                                            <p class="text-sm text-line-1 text-neutral-300">You can stitch between artboards</p>
                                        </div>
                                    </div>
                                    <span class="text-sm fw-medium text-neutral-400">23 Mins ago</span>
                                </div>
                                </a>
                            </li>

                            <li>
                                <a class="dropdown-item d-flex align-items-center gap-12 px-16 py-12" href="javascript:void(0)">
                                <div class="d-flex w-100 justify-content-between gap-12">
                                    <div class="d-flex align-items-center gap-12 ">
                                        <span class="w-36 h-36"><img src="assets/images/thumbs/reviewer-img4.png" alt=""></span>
                                        <div>
                                            <span class="text-md fw-semibold text-line-1">Arlene McCoy</span>
                                            <p class="text-sm text-line-1 text-neutral-300">Invite you to prototyping</p>
                                        </div>
                                    </div>
                                    <span class="text-sm fw-medium text-neutral-400">23 Mins ago</span>
                                </div>
                                </a>
                            </li>

                            <li>
                                <a class="dropdown-item d-flex align-items-center gap-12 px-16 py-12" href="javascript:void(0)">
                                <div class="d-flex w-100 justify-content-between gap-12">
                                    <div class="d-flex align-items-center gap-12 ">
                                        <span class="w-36 h-36"><img src="assets/images/thumbs/instructor-details-thumb.png" alt=""></span>
                                        <div>
                                            <span class="text-md fw-semibold text-line-1">Annette Black</span>
                                            <p class="text-sm text-line-1 text-neutral-300">Invite you to prototyping</p>
                                        </div>
                                    </div>
                                    <span class="text-sm fw-medium text-neutral-400">23 Mins ago</span>
                                </div>
                                </a>
                            </li>
                        </ul>
                    </div>
                    <div class="position-absolute inset-inline-start-22px top--20-percent">
                        <span class="bg-main-600 text-white w-16 h-16 rounded-circle text-9 d-flex justify-content-center align-items-center">2</span>
                    </div>
                </div>
            </div>

            <div class="w-36 h-36 border-neutral-50 border rounded-pill justify-content-center align-items-center d-flex text-20 text-neutral-500 hover-bg-main-600 transition-03">
                <button type="button" class=" hover-text-white transition-03">
                    <i class="ph ph-chat-dots"></i>
                </button>
            </div>

            <div class="dropdown d-flex align-items-center gap-12">
                <button class="dropdown-toggle gape-user-avatar-trigger w-36 h-36 border-neutral-50 border rounded-pill" type="button" data-bs-toggle="dropdown" aria-expanded="false" title="${sessionScope['gape.auth.userEmail']}">
                    <%@ include file="/WEB-INF/fragments/user-avatar-content.jspf" %>
                </button>
                <ul class="dropdown-menu rounded-12">
                    <li>
                        <a class="dropdown-item d-flex align-items-center gap-12 hover-text-main-600 transition-03" href="${pageContext.request.contextPath}/profile">
                        <span><i class="ph ph-user-circle"></i></span>
                        <span>My Profile</span>
                        </a>
                    </li>
                    <li>
                        <a class="dropdown-item d-flex align-items-center gap-12 hover-text-main-600 transition-03" href="${pageContext.request.contextPath}/profile">
                        <span><i class="ph ph-gear"></i></span>
                        <span>My Profile</span>
                        </a>
                    </li>
                    <li>
                        <form action="${pageContext.request.contextPath}/auth/logout" method="post" class="m-0">
                            <input type="hidden" name="csrfToken" value="${sessionScope['gape.auth.logoutCsrfToken']}">
                            <button type="submit" class="dropdown-item d-flex align-items-center gap-12 hover-text-main-600 transition-03 border-0 bg-transparent w-100 text-start">
                        <span><i class="ph ph-power"></i></span>
                        <span>Log Out</span>
                            </button>
                        </form>
                    </li>
                </ul>
            </div>

        </div>
    </div>
</div>
<!-- =====message nab end======== -->
        
            <!-- ============Feedbacks start============ -->
            <div class="px-24 py-24">
        
            <div class="px-24 py-24 bg-white rounded-10">
                <div class="d-flex align-items-center justify-content-between mb-24">
                    <h6 class="mb-0 fw-medium">Reviews</h6>
                </div>
                <div class="mb-24">
                    
                    <div class="overflow-y-auto">
                        <table id="example-five" class="display min-w-max w-100">
                            <thead>
                                <tr class="bg-main-25 border-bottom border-neutral-30">
                                    <th class="text-12 fw-medium text-neutral-500 py-16 px-20">Course</th>
                                    <th class="text-12 fw-medium text-neutral-500 py-16 px-20">Reviews</th>
                                    <th class="text-12 fw-medium text-neutral-500 py-16 px-20">Feedback</th>
                                    <th class="text-12 fw-medium text-neutral-500 py-16 px-20">Action</th>
                                </tr>
                            </thead>
                            <tbody>
        
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-28 px-20 shadow-none">
                                        <span class="fw-normal text-12 text-neutral-500">Automation System</span>
                                    </td>
                                    <td class="text-14 fw-normal py-28 px-20 shadow-none">
                                        <div>
                                            <ul class="d-flex align-items-center gap-6">
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star-half"></i></li>
                                            </ul>
                                        </div>
                                    </td>
                                    <td class="py-28 px-20 shadow-none">(3 Reviews)</td>
                                    </td>
                                    <td class="py-28 px-20 shadow-none">
                                        <div class="d-flex align-items-center gap-12  justify-content-end">
                                             <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                            <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                        </div>
                                    </td>
                                </tr>
        
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-28 px-20 shadow-none">
                                        <span class="fw-normal text-12 text-neutral-500">Laboratory Expansion</span>
                                    </td>
                                    <td class="text-14 fw-normal py-28 px-20 shadow-none">
                                        <div>
                                            <ul class="d-flex align-items-center gap-6">
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star-half"></i></li>
                                            </ul>
                                        </div>
                                    </td>
                                    <td class="py-28 px-20 shadow-none">(3 Reviews)</td>
                                    <td class="py-28 px-20 shadow-none">
                                        <div class="d-flex align-items-center gap-12  justify-content-end">
                                             <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                            <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                        </div>
                                    </td>
                                </tr>
        
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-28 px-20 shadow-none">
                                        <span class="fw-normal text-12 text-neutral-500">Cleanroom Upgrade</span>
                                    </td>
                                    <td class="text-14 fw-normal py-28 px-20 shadow-none">
                                        <div>
                                            <ul class="d-flex align-items-center gap-6">
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star-half"></i></li>
                                            </ul>
                                        </div>
                                    </td>
                                    <td class="py-28 px-20 shadow-none">(3 Reviews)</td>
                                    <td class="py-28 px-20 shadow-none">
                                        <div class="d-flex align-items-center gap-12  justify-content-end">
                                             <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                            <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                        </div>
                                    </td>
                                </tr>
        
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-28 px-20 shadow-none">
                                        <span class="fw-normal text-12 text-neutral-500">Quality Control</span>
                                    </td>
                                    <td class="text-14 fw-normal py-28 px-20 shadow-none">
                                        <div>
                                            <ul class="d-flex align-items-center gap-6">
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star-half"></i></li>
                                            </ul>
                                        </div>
                                    </td>
                                    <td class="py-28 px-20 shadow-none">(3 Reviews)</td>
                                    <td class="py-28 px-20 shadow-none">
                                        <div class="d-flex align-items-center gap-12  justify-content-end">
                                             <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                            <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                        </div>
                                    </td>
                                </tr>
        
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-28 px-20 shadow-none">
                                        <span class="fw-normal text-12 text-neutral-500">Analytical Equipment</span>
                                    </td>
                                    <td class="text-14 fw-normal py-28 px-20 shadow-none">
                                        <div>
                                            <ul class="d-flex align-items-center gap-6">
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star-half"></i></li>
                                            </ul>
                                        </div>
                                    </td>
                                    <td class="py-28 px-20 shadow-none">(3 Reviews)</td>
                                    <td class="py-28 px-20 shadow-none">
                                        <div class="d-flex align-items-center gap-12  justify-content-end">
                                             <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                            <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                        </div>
                                    </td>
                                </tr>
        
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-28 px-20 shadow-none">
                                        <span class="fw-normal text-12 text-neutral-500">Vaccine Development</span>
                                    </td>
                                    <td class="text-14 fw-normal py-28 px-20 shadow-none">
                                        <div>
                                            <ul class="d-flex align-items-center gap-6">
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star-half"></i></li>
                                            </ul>
                                        </div>
                                    </td>
                                    <td class="py-28 px-20 shadow-none">(3 Reviews)</td>
                                    <td class="py-28 px-20 shadow-none">
                                        <div class="d-flex align-items-center gap-12  justify-content-end">
                                             <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                            <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                        </div>
                                    </td>
                                </tr>
        
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-28 px-20 shadow-none">
                                        <span class="fw-normal text-12 text-neutral-500">Packaging Line</span>
                                    </td>
                                    <td class="text-14 fw-normal py-28 px-20 shadow-none">
                                        <div>
                                            <ul class="d-flex align-items-center gap-6">
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star-half"></i></li>
                                            </ul>
                                        </div>
                                    </td>
                                    <td class="py-28 px-20 shadow-none">(3 Reviews)</td>
                                    <td class="py-28 px-20 shadow-none">
                                        <div class="d-flex align-items-center gap-12  justify-content-end">
                                         <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                        <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                    </div>
                                    </td>
                                </tr>
        
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-28 px-20 shadow-none">
                                        <span class="fw-normal text-12 text-neutral-500">IT Infrastructure</span>
                                    </td>
                                    <td class="text-14 fw-normal py-28 px-20 shadow-none">
                                        <div>
                                            <ul class="d-flex align-items-center gap-6">
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star-half"></i></li>
                                            </ul>
                                        </div>
                                    </td>
                                    <td class="py-28 px-20 shadow-none">(3 Reviews)</td>
                                    <td class="py-28 px-20 shadow-none">
                                        <div class="d-flex align-items-center gap-12  justify-content-end">
                                             <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                            <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                        </div>
                                    </td>
                                </tr>
        
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-28 px-20 shadow-none">
                                        <span class="fw-normal text-12 text-neutral-500">Clinical Trials</span>
                                    </td>
                                    <td class="text-14 fw-normal py-28 px-20 shadow-none">
                                        <div>
                                            <ul class="d-flex align-items-center gap-6">
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star-half"></i></li>
                                            </ul>
                                        </div>
                                    </td>
                                    <td class="py-28 px-20 shadow-none">(3 Reviews)</td>
                                    <td class="py-28 px-20 shadow-none">
                                        <div class="d-flex align-items-center gap-12  justify-content-end">
                                             <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                            <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                        </div>
                                    </td>
                                </tr>
        
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-28 px-20 shadow-none">
                                        <span class="fw-normal text-12 text-neutral-500">Clinical Trials</span>
                                    </td>
                                    <td class="text-14 fw-normal py-28 px-20 shadow-none">
                                        <div>
                                            <ul class="d-flex align-items-center gap-6">
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star-half"></i></li>
                                            </ul>
                                        </div>
                                    </td>
                                    <td class="py-28 px-20 shadow-none">(3 Reviews)</td>
                                    <td class="py-28 px-20 shadow-none">
                                        <div class="d-flex align-items-center gap-12  justify-content-end">
                                             <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                            <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                        </div>
                                    </td>
                                </tr>
        
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-28 px-20 shadow-none">
                                        <span class="fw-normal text-12 text-neutral-500">Cold Chain</span>
                                    </td>
                                    <td class="text-14 fw-normal py-28 px-20 shadow-none">
                                        <div>
                                            <ul class="d-flex align-items-center gap-6">
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star-half"></i></li>
                                            </ul>
                                        </div>
                                    </td>
                                    <td class="py-28 px-20 shadow-none">(3 Reviews)</td>
                                    <td class="py-28 px-20 shadow-none">
                                        <div class="d-flex align-items-center gap-12  justify-content-end">
                                             <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                            <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                        </div>
                                    </td>
                                </tr>
        
                                <tr class="hover-bg-neutral-20 border-bottom transition-03">
                                    <td class="py-28 px-20 shadow-none">
                                        <span class="fw-normal text-12 text-neutral-500">IT Infrastructure</span>
                                    </td>
                                    <td class="text-14 fw-normal py-28 px-20 shadow-none">
                                        <div>
                                            <ul class="d-flex align-items-center gap-6">
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star"></i></li>
                                                <li class="text-20 text-warning-600"><i class="ph-fill ph-star-half"></i></li>
                                            </ul>
                                        </div>
                                    </td>
                                    <td class="py-28 px-20 shadow-none">(3 Reviews)</td>
                                    <td class="py-28 px-20 shadow-none">
                                        <div class="d-flex align-items-center gap-12  justify-content-end">
                                             <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-pencil-simple-line"></i></button>
                                            <button type="button" class="text-24 text-neutral-500"><i class="ph-bold ph-trash"></i></button>
                                        </div>
                                    </td>
                                </tr>
        
                            </tbody>
                        </table>
                    </div>

                </div>
    
                <div class="d-flex align-items-center gap-24 justify-content-between flex-wrap">
                    <div class="form-check form-switch">
                        <input class="form-check-input focus-box-shadow" type="checkbox" role="switch" id="flexSwitchCheckDefault">
                        <label class="form-check-label text-14 fw-normal text-neutral-500" for="flexSwitchCheckDefault">Dense</label>
                    </div>
                    <div class="d-flex align-items-center gap-40 flex-wrap">
    
                        <div class="d-flex align-items-center gap-16">
                            <span class="fw-normal text-14 text-neutral-500">Rows per page:</span>
                            <select class="form-select w-auto pe-32 text-14 fw-normal text-neutral-500 bg-transparent border-0">
                                <option value="1">12</option>
                                <option value="1">13</option>
                                <option value="1">11</option>
                                <option value="1">15</option>
                            </select>
                        </div>
    
                        <div class="d-flex align-items-center gap-16">
                            <span class="fw-normal text-14 text-neutral-500">1-12 of 100</span>
                            <div class="d-flex align-items-center gpa-8">
                                <span class="text-20 text-neutral-500">
                                    <i class="ph-bold ph-caret-left"></i>
                                </span>
                                <span class="text-20 text-neutral-500">
                                    <i class="ph-bold ph-caret-right"></i>
                                </span>
                            </div>
                        </div>
    
                    </div>
    
                </div>
    
            </div>
            </div>
            <!-- ============Feedbacks end============ -->

          <!-- =========message profile footer start============== -->
<div class="bg-neutral-20 border-neutral-40 border-top px-24 py-16 mt-auto">
    <div class="d-flex align-items-center gap-24 justify-content-between flex-wrap">
        <p class="fw-medium text-14 text-neutral-500">
            Copyright &copy; 2026 
            <a href="${pageContext.request.contextPath}/admin/admin-dashbord.jsp" class="text-main-600 fw-medium">GAPE</a>.
            All Rights Reserved
        </p>
        <div class="d-flex align-items-center gap-24">
            <a href="#" class="fw-medium text-14 text-neutral-500 hover-text-main-600">Terms & Conditions</a>
        </div>
    </div>
</div>
<!-- =========message profile footer end============== -->
        </div>
        <!-- ==========message dashbord end============ -->
    </div>
</div>



    <!-- Jquery js -->
    <script src="assets/js/jquery-3.7.1.min.js"></script>
    <!-- Bootstrap Bundle Js -->
    <script src="assets/js/boostrap.bundle.min.js"></script>
    <!-- select2 Js -->
    <script src="assets/js/select2.min.js"></script>
    <!-- Phosphor Icon Js -->
    <script src="assets/js/phosphor-icon.js"></script>
    <!-- Slick js -->
    <script src="assets/js/slick.min.js"></script>
    <!-- Slick js -->
    <script src="assets/js/counter.min.js"></script>
    <!-- magnific popup -->
    <script src="assets/js/magnific-popup.min.js"></script>
    <!-- Jquery Ui js -->
    <script src="assets/js/jquery-ui.js"></script>
    <!-- marquee js -->
    <script src="assets/js/marquee.min.js"></script>
    <!-- react charts-->
     <script src="assets/js/apexcharts.js"></script>
    <!-- plyr Js -->
    <script src="assets/js/plyr.js"></script>
    <!-- vanilla Tilt -->
    <!-- Editor js Toolbar Start -->
    <script src="assets/js/editor-quill.js"></script>
    <!-- dataTables -->
    <script src="assets/js/dataTables.min.js"></script>
    <!-- Tilt -->
    <script src="assets/js/vanilla-tilt.min.js"></script>
    <!-- wow -->
    <script src="assets/js/wow.min.js"></script>

    <script src="assets/js/aos.js"></script>
    
    <!-- main js -->
    <script src="assets/js/main.js"></script>


    



</body>
</html>










