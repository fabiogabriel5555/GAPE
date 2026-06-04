<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <base href="${pageContext.request.contextPath}/">
    <!-- Title -->
    <title>GAPE - Contact</title>
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
            <img src="assets/images/logo/logo.png" alt="Logo">
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
                <a href="instructor/instructor.jsp" class="nav-submenu__link hover-bg-neutral-30"> Instructor</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-details.jsp" class="nav-submenu__link hover-bg-neutral-30"> Instructor Details</a>
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
                <a href="admin/admin-dashbord.jsp" class="nav-submenu__link hover-bg-neutral-30"> Admin Dashbord</a>
            </li>
            <li class="nav-submenu__item">
                <a href="admin/admin-dashbord-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="admin/admin-dashbord-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="admin/admin-dashbord-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="admin/admin-dashbord-wishlist.jsp" class="nav-submenu__link hover-bg-neutral-30">Wishlist</a>
            </li>
            <li class="nav-submenu__item">
                <a href="admin/admin-dashbord-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="admin/admin-dashbord-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="admin/admin-dashbord-settings.jsp" class="nav-submenu__link hover-bg-neutral-30">Settings</a>
            </li>
            <li class="nav-submenu__item">
                <a href="student/student-dashbord.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Admin Dashbord</a>
            </li>
            <li class="nav-submenu__item">
                <a href="student/student-dashbord-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">Student My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="student/student-dashbord-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="student/student-dashbord-enrolled-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Enrolled Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="student/student-dashbord-wishlist.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Wishlist</a>
            </li>
            <li class="nav-submenu__item">
                <a href="student/student-dashbord-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="student/student-dashbord-my-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Student My Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="student/student-dashbord-assignment.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Assignment</a>
            </li>
            <li class="nav-submenu__item">
                <a href="student/student-dashbord-settings.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Settings</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Dashbord</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-enrolled-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Enrolled Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-wishlist.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Wishlist</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-my-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor My Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-order-history.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Order History</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-my-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor My Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-announcements.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Announcements</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-assignment.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Assignment</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-account-settings.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Account Settings</a>
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
                        <img src="assets/images/logo/logo.png" alt="Logo">
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
                <a href="instructor/instructor.jsp" class="nav-submenu__link hover-bg-neutral-30"> Instructor</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-details.jsp" class="nav-submenu__link hover-bg-neutral-30"> Instructor Details</a>
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
                <a href="admin/admin-dashbord.jsp" class="nav-submenu__link hover-bg-neutral-30"> Admin Dashbord</a>
            </li>
            <li class="nav-submenu__item">
                <a href="admin/admin-dashbord-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="admin/admin-dashbord-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="admin/admin-dashbord-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="admin/admin-dashbord-wishlist.jsp" class="nav-submenu__link hover-bg-neutral-30">Wishlist</a>
            </li>
            <li class="nav-submenu__item">
                <a href="admin/admin-dashbord-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="admin/admin-dashbord-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="admin/admin-dashbord-settings.jsp" class="nav-submenu__link hover-bg-neutral-30">Settings</a>
            </li>
            <li class="nav-submenu__item">
                <a href="student/student-dashbord.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Admin Dashbord</a>
            </li>
            <li class="nav-submenu__item">
                <a href="student/student-dashbord-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">Student My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="student/student-dashbord-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="student/student-dashbord-enrolled-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Enrolled Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="student/student-dashbord-wishlist.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Wishlist</a>
            </li>
            <li class="nav-submenu__item">
                <a href="student/student-dashbord-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="student/student-dashbord-my-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Student My Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="student/student-dashbord-assignment.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Assignment</a>
            </li>
            <li class="nav-submenu__item">
                <a href="student/student-dashbord-settings.jsp" class="nav-submenu__link hover-bg-neutral-30">Student Settings</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Dashbord</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-my-profile.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor My Profile</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-message.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Message</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-enrolled-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Enrolled Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-wishlist.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Wishlist</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-reviews.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Reviews</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-my-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor My Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-order-history.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Order History</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-my-courses.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor My Courses</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-announcements.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Announcements</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-assignment.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Assignment</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-quiz-attempts.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Quiz Attempts</a>
            </li>
            <li class="nav-submenu__item">
                <a href="instructor/instructor-dashboard-account-settings.jsp" class="nav-submenu__link hover-bg-neutral-30">Instructor Account Settings</a>
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
                                <a href="sign-in.jsp" class="info-action gape-user-avatar-trigger w-52 h-52 bg-main-25 hover-bg-main-600 border border-neutral-30 rounded-circle flex-center text-2xl text-neutral-500 hover-text-white hover-border-main-600">
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
                    <h1 class="breadcrumb__title display-4 fw-semibold text-center"> Contact</h1>
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
                            <span class="text-main-two-600"> Contact </span> 
                        </li>
                    </ul>
                </div>
            </div>
        </div>
    </div>
</section>
<!-- ==================== Breadcrumb End Here ==================== -->

    <!-- =============================== Contact Section Start ================================== -->
    <section class="contact py-120">
        <div class="container">
            <div class="section-heading text-center">
            <div class="flex-align d-inline-flex gap-8 mb-16">
                <span class="text-main-600 text-2xl d-flex"><i class="ph-bold ph-book"></i></span>
                <h5 class="text-main-600 mb-0">Get In Touch</h5>
            </div>
            <h2 class="mb-24">Let us help you</h2>
            <p class="">Our platform is built on the principles of innovation, quality, and inclusivity, aiming to provide a seamless learning</p>
        </div>
            <div class="row gy-4">
                <div class="col-xl-4 col-md-6">
                    <div class="contact-item bg-main-25 border border-neutral-30 rounded-12 px-32 py-40 d-flex align-items-start gap-24 hover-bg-main-600 transition-2 hover-border-main-600">
                        <span class="contact-item__icon w-60 h-60 text-32 flex-center rounded-circle bg-main-600 text-white flex-shrink-0">
                            <i class="ph ph-map-pin-line"></i>
                        </span>
                        <div class="flex-grow-1">
                            <h4 class="mb-12">Main Office</h4>
                            <p class="text-neutral-500">2972 Westheimer Rd. Santa Ana, Illinois 85486 </p>
                            <a href="javascript:void(0)" class="text-main-600 fw-semibold text-decoration-underline mt-16">Find Location</a>
                        </div>
                    </div>
                </div>
                <div class="col-xl-4 col-md-6">
                    <div class="contact-item bg-main-25 border border-neutral-30 rounded-12 px-32 py-40 d-flex align-items-start gap-24 hover-bg-main-600 transition-2 hover-border-main-600">
                        <span class="contact-item__icon w-60 h-60 text-32 flex-center rounded-circle bg-main-600 text-white flex-shrink-0">
                            <i class="ph ph-envelope-open"></i>
                        </span>
                        <div class="flex-grow-1">
                            <h4 class="mb-12">Email Address</h4>
                            <p class="text-neutral-500">infoexample@gmail.com</p>
                            <p class="text-neutral-500">example@gmail.com</p>
                            <a href="mailto:infoexample@gmail.com" class="text-main-600 fw-semibold text-decoration-underline mt-16">Get In Touch</a>
                        </div>
                    </div>
                </div>
                <div class="col-xl-4 col-md-6">
                    <div class="contact-item bg-main-25 border border-neutral-30 rounded-12 px-32 py-40 d-flex align-items-start gap-24 hover-bg-main-600 transition-2 hover-border-main-600">
                        <span class="contact-item__icon w-60 h-60 text-32 flex-center rounded-circle bg-main-600 text-white flex-shrink-0">
                            <i class="ph ph-phone-call"></i>
                        </span>
                        <div class="flex-grow-1">
                            <h4 class="mb-12">Phone Number</h4>
                            <p class="text-neutral-500">(505) 555-0125</p>
                            <p class="text-neutral-500">(406) 555-0120</p>
                            <a href="tel:(406)555-0120" class="text-main-600 fw-semibold text-decoration-underline mt-16">Contact Us Today!</a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>
    <!-- =============================== Contact Section End ================================== -->
     
    <!-- ====================== Contact Form Section Start ========================= -->
    <section class="contact-form-section py-240 bg-main-25 position-relative z-1">
        <img src="assets/images/bg/wave-bg.png" alt="" class="position-absolute top-0 start-0 w-100 h-100 z-n1 d-lg-block d-none">
        <div class="container">
            <div class="row gy-5 align-items-center">
                <div class="col-xl-7 col-lg-6 pe-lg-5">
                    <div class="mb-40 md-xl-5">
                        <div class="flex-align d-inline-flex gap-8 mb-16">
                            <span class="text-main-600 text-2xl d-flex"><i class="ph-bold ph-book"></i></span>
                            <h5 class="text-main-600 mb-0">Contact Us</h5>
                        </div>
                        <h2 class="mb-24">Have questions? don't hesitate to contact us</h2>
                        <p class="text-neutral-500 text-line-3 max-w-636">We are passionate about transforming lives through education. Founded with a vision to make learning accessible to all, we believe in the power of knowledge to unlock opportunities and shape the future.</p>
                    </div>
                    <div class="flex-align gap-40 flex-wrap">
                        <div class="enrolled-students mt-12 d-block">
                            <img src="assets/images/thumbs/enroll-student-img1.png" alt="" class="w-48 h-48 rounded-circle object-fit-cover transition-2">
                            <img src="assets/images/thumbs/enroll-student-img2.png" alt="" class="w-48 h-48 rounded-circle object-fit-cover transition-2">
                            <img src="assets/images/thumbs/enroll-student-img3.png" alt="" class="w-48 h-48 rounded-circle object-fit-cover transition-2">
                            <img src="assets/images/thumbs/enroll-student-img4.png" alt="" class="w-48 h-48 rounded-circle object-fit-cover transition-2">
                            <img src="assets/images/thumbs/enroll-student-img5.png" alt="" class="w-48 h-48 rounded-circle object-fit-cover transition-2">
                            <img src="assets/images/thumbs/enroll-student-img6.png" alt="" class="w-48 h-48 rounded-circle object-fit-cover transition-2">
                        </div>
                        <div class="">
                            <ul class="flex-align gap-4 mb-10">
                                <li class="text-warning-600 text-2xl d-flex"><i class="ph-fill ph-star"></i></li>
                                <li class="text-warning-600 text-2xl d-flex"><i class="ph-fill ph-star"></i></li>
                                <li class="text-warning-600 text-2xl d-flex"><i class="ph-fill ph-star"></i></li>
                                <li class="text-warning-600 text-2xl d-flex"><i class="ph-fill ph-star"></i></li>
                                <li class="text-warning-600 text-2xl d-flex"><i class="ph-fill ph-star-half"></i></li>
                            </ul>
                            <span class="text-neutral-700 fw-medium"> 2.5k+ reviews (4.95 of 5)</span>
                        </div>
                    </div>
                </div>
                <div class="col-xl-5 col-lg-6">
                    <div class="p-24 bg-white rounded-12 box-shadow-md">
                        <div class="border border-neutral-30 rounded-8 bg-main-25 p-24">
                            <form action="#" id="commentForm">
                                <h4 class="mb-0">Get In Touch</h4>
                                <span class="d-block border border-neutral-30 my-24 border-dashed"></span>
                                <div class="mb-24">
                                    <label for="name" class="text-neutral-700 text-lg fw-medium mb-12">Name </label>
                                    <input type="text" class="common-input rounded-pill border-transparent focus-border-main-600" id="name" placeholder="Enter Name...">
                                </div>
                                <div class="mb-24">
                                    <label for="email" class="text-neutral-700 text-lg fw-medium mb-12">Email </label>
                                    <input type="email" class="common-input rounded-pill border-transparent focus-border-main-600" id="email" placeholder="Enter Email...">
                                </div>
                                <div class="mb-24">
                                    <label for="phone" class="text-neutral-700 text-lg fw-medium mb-12">Phone </label>
                                    <input type="tel" class="common-input rounded-pill border-transparent focus-border-main-600" id="phone" placeholder="Enter Your Number...">
                                </div>
                                <div class="mb-24">
                                    <label for="desc" class="text-neutral-700 text-lg fw-medium mb-12">Message</label>
                                    <textarea id="desc" class="common-input rounded-24 border-transparent focus-border-main-600 h-110" placeholder="Enter Your Message..."></textarea>
                                </div>
                                <div class="mb-0">
                                    <button type="submit" class="btn btn-main rounded-pill flex-center gap-8 mt-40">
                                        Send Message
                                        <i class="ph-bold ph-arrow-up-right d-flex text-lg"></i>
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>
    <!-- ====================== Contact Form Section End ========================= -->
    
    <!-- ================================= Certificate Section Start ================================= -->
<div class="certificate">
    <div class="container container--lg">
        <div class="certificate-box px-16 bg-main-600 rounded-16">
            <div class="container">
                <div class="position-relative py-80">
                    <div class="row align-items-center">
                        <div class="col-xl-6">
                            <div class="certificate__content">
                                <div class="flex-align gap-8 mb-16 wow bounceInDown">
                                    <span class="w-8 h-8 bg-white rounded-circle"></span>
                                    <h5 class="text-white mb-0">Get Certificate</h5>
                                </div>
                                <h2 class="text-white mb-40 fw-medium wow bounceIn">Get Quality Skills Certificate From the GAPE</h2>
                                <a href="" class="btn btn-white rounded-pill flex-align d-inline-flex gap-8 hover-bg-main-800 wow bounceInUp">
                                    Get Started Now
                                    <i class="ph-bold ph-arrow-up-right d-flex text-lg"></i>
                                </a>
                            </div>
                        </div>
                        <div class="col-xl-6 d-xl-block d-none">
                            <div class="certificate__thumb" data-aos="fade-up-left">    
                                <img src="assets/images/thumbs/certificate-img.png" alt="" data-tilt data-tilt-max="8" data-tilt-speed="500" data-tilt-perspective="5000" data-tilt-full-page-listening>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
 </div>
<!-- ================================= Certificate Section End ================================= -->
    
    
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








