<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <base href="${pageContext.request.contextPath}/">
    <!-- Title -->
    <title>GAPE - Coordinator My Profile</title>
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
                <a href="${pageContext.request.contextPath}/coordinator/coordinator.jsp" class="nav-submenu__link hover-bg-neutral-30"> Coordinator</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-details.jsp" class="nav-submenu__link hover-bg-neutral-30"> Coordinator Details</a>
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
            <li class="nav-submenu__item">
                <a href="privacy-policy.jsp" class="nav-submenu__link hover-bg-neutral-30">Privacy Policy</a>
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
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord-wishlist.jsp" class="nav-submenu__link hover-bg-neutral-30">Wishlist</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord-settings.jsp" class="nav-submenu__link hover-bg-neutral-30">Settings</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Admin Dashbord</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">Student My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-enrolled-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Enrolled Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-wishlist.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Wishlist</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-my-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Student My Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-assignment.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Assignment</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-settings.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Settings</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Dashbord</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-enrolled-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Enrolled Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-wishlist.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Wishlist</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-my-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator My Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-order-history.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Order History</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-my-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator My Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-announcements.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Announcements</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-assignment.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Assignment</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-account-settings.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Account Settings</a>
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


    <!-- ==================== Header Start Here ==================== -->
<header class="header">
    <div class="container container--xl">
        <nav class="header-inner flex-between gap-8">

            <div class="header-content-wrapper flex-align flex-grow-1">
                <!-- Logo Start -->
                <div class="logo">
                    <a href="index.jsp" class="link">
                        <img src="assets/images/logo/logo.svg" alt="Logo">
                    </a>
                </div>
                <!-- Logo End  -->

                <!-- Select Start -->
                <div class="d-sm-block d-none">
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
                <!-- Select End -->

                <!-- Menu Start  -->
                <div class="header-menu d-lg-block d-none">

<ul class="nav-menu flex-align ">
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
                <a href="${pageContext.request.contextPath}/coordinator/coordinator.jsp" class="nav-submenu__link hover-bg-neutral-30"> Coordinator</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-details.jsp" class="nav-submenu__link hover-bg-neutral-30"> Coordinator Details</a>
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
            <li class="nav-submenu__item">
                <a href="privacy-policy.jsp" class="nav-submenu__link hover-bg-neutral-30">Privacy Policy</a>
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
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord-wishlist.jsp" class="nav-submenu__link hover-bg-neutral-30">Wishlist</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/admin/admin-dashbord-settings.jsp" class="nav-submenu__link hover-bg-neutral-30">Settings</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Admin Dashbord</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">Student My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-enrolled-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Enrolled Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-wishlist.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Wishlist</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-my-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Student My Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-assignment.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Assignment</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/student/student-dashbord-settings.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Settings</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Dashbord</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-enrolled-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Enrolled Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-wishlist.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Wishlist</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-my-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator My Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-order-history.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Order History</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-my-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator My Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-announcements.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Announcements</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-assignment.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Assignment</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="${pageContext.request.contextPath}/coordinator/coordinator-dashboard-account-settings.jsp" class="nav-submenu__link hover-bg-neutral-30">Coordinator Account Settings</a>
            </li>
        </ul>
    </li>
    <li class="nav-menu__item">
        <a href="contact.jsp" class="nav-menu__link">Contact</a>
    </li>
</ul>
                </div>
                <!-- Menu End  -->
            </div>

            <!-- Header Right start -->
            <div class="header-right flex-align">
                <form action="#" class="search-form position-relative d-xl-block d-none">
                    <input type="text" class="common-input rounded-pill bg-main-25 pe-48 border-neutral-30" placeholder="Search...">
                    <button type="submit" class="w-36 h-36 bg-main-600 hover-bg-main-700 rounded-circle flex-center text-md text-white position-absolute top-50 translate-middle-y inset-inline-end-0 me-8">
                        <i class="ph-bold ph-magnifying-glass"></i>
                    </button>
                </form>
                                <a href="${pageContext.request.contextPath}${sessionScope['gape.auth.authenticated'] eq true ? '/profile' : '/login.jsp'}" class="info-action gape-user-avatar-trigger w-52 h-52 bg-main-25 hover-bg-main-600 border border-neutral-30 rounded-circle flex-center text-2xl text-neutral-500 hover-text-white hover-border-main-600">
                    <%@ include file="/WEB-INF/fragments/user-avatar-content.jspf" %>
                </a>
                <button type="button" class="toggle-mobileMenu d-lg-none text-neutral-200 flex-center">
                    <i class="ph ph-list"></i>
                </button>
            </div>
            <!-- Header Right End  -->
        </nav>
    </div>
</header>
<!-- ==================== Header End Here ==================== -->

    <!-- ==================== Breadcrumb Start Here ==================== -->
<section class="breadcrumb py-120 bg-main-25 position-relative z-1 overflow-hidden mb-0">
    <img src="assets/images/shapes/shape1.png" alt="" class="shape one animation-rotation d-md-block d-none">
    <img src="assets/images/shapes/shape2.png" alt="" class="shape two animation-scalation d-md-block d-none">
    <img src="assets/images/shapes/shape3.png" alt="" class="shape eight animation-walking d-md-block d-none">
    <img src="assets/images/shapes/shape5.png" alt="" class="shape six animation-walking d-md-block d-none">
    <img src="assets/images/shapes/shape4.png" alt="" class="shape four animation-scalation">
    <img src="assets/images/shapes/shape4.png" alt="" class="shape nine animation-scalation">

    <div class="container">
        <div class="row justify-content-center">
            <div class="col-lg-8">
                <div class="breadcrumb__wrapper">
                    <h1 class="breadcrumb__title display-4 fw-semibold text-center"> coordinator my profile</h1>
                    <ul class="breadcrumb__list d-flex align-items-center justify-content-center gap-4">
                        <li class="breadcrumb__item">
                            <a href="index.jsp" class="breadcrumb__link text-neutral-500 hover-text-main-600 fw-medium">
                                <i class="text-lg d-inline-flex ph-bold ph-house"></i> Home</a>
                         </li>
                        <li class="breadcrumb__item">
                            <i class="text-neutral-500 d-flex ph-bold ph-caret-right"></i>
                        </li>
                        <li class="breadcrumb__item">
                            <a href="course.jsp" class="breadcrumb__link text-neutral-500 hover-text-main-600 fw-medium"> </a>
                        </li>
                        <li class="breadcrumb__item d-none">
                            <i class="text-neutral-500 d-flex ph-bold ph-caret-right"></i>
                        </li>
                        <li class="breadcrumb__item">
                            <span class="text-main-two-600"> Template vazio </span>
                        </li>
                    </ul>
                </div>
            </div>
        </div>
    </div>
</section>
<!-- ==================== Breadcrumb End Here ==================== -->

        <!-- ============================== Placeholder Section Start ============================== -->
    <section class="account py-120 position-relative">
        <div class="container">
            <div class="row justify-content-center">
                <div class="col-lg-8">
                    <div class="bg-main-25 border border-neutral-30 rounded-16 p-40 text-center">
                        <span class="text-main-600 fw-semibold text-xl d-inline-block mb-16">Template vazio</span>
                        <h3 class="mb-16 text-neutral-500">coordinator my profile</h3>
                        <p class="text-neutral-500 mb-32">O ficheiro correspondente no template EduAll encontra-se vazio. Esta pagina foi criada apenas para manter o espelho completo do vendor em JSP.</p>
                        <div class="d-flex justify-content-center gap-16 flex-wrap">
                            <a href="index.jsp" class="btn btn-main rounded-pill">Voltar ao inicio</a>
                            <a href="${pageContext.request.contextPath}/admin/dashboard.jsp" class="btn btn-outline-main rounded-pill">Abrir dashboard</a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>
    <!-- ============================== Placeholder Section End ============================== -->


<!-- ==================== Footer Start Here ==================== -->
<footer class="footer bg-main-25 position-relative z-1">
    <div class="container">
        <!-- bottom Footer -->
        <div class="bottom-footer bg-main-25 border-top border-dashed border-main-100 border-0 py-32">
            <div class="container container-two">
                <div class="bottom-footer__inner flex-between gap-3 flex-wrap">
                    <p class="bottom-footer__text"> Copyright &copy; 2024 <span class="fw-semibold">GAPE</span> All Rights Reserved.</p>
                    <div class="footer-links">
                        <a href="privacy-policy.jsp" class="text-neutral-500 hover-text-main-600 hover-text-decoration-underline">Privacy Policy</a>
                        <a href="#" class="text-neutral-500 hover-text-main-600 hover-text-decoration-underline">Terms & Conditions</a>
                    </div>
                </div>
            </div>
        </div>
    </div>
</footer>
<!-- ==================== Footer End Here ==================== -->


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








