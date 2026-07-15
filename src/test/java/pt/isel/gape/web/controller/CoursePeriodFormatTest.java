package pt.isel.gape.web.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import pt.isel.gape.learning.model.CoursePeriodTemplate;
import pt.isel.gape.learning.model.CurricularTerm;
import pt.isel.gape.web.view.CoursePeriodTemplateView;

class CoursePeriodFormatTest {

    @Test
    void parsesPeriodsAsDayThenMonthAndKeepsTheDomainMonthDayOrder() {
        assertArrayEquals(
                new int[]{9, 2},
                CourseManagementServlet.parseMonthDay("02-09", "Course period start date")
        );
    }

    @Test
    void rejectsInvalidOrNonPortuguesePeriodDates() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CourseManagementServlet.parseMonthDay("31-02", "Course period start date")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> CourseManagementServlet.parseMonthDay("09/02", "Course period start date")
        );
    }

    @Test
    void rendersStoredMonthDayValuesAsDayThenMonth() {
        CoursePeriodTemplateView view = CoursePeriodTemplateView.from(new CoursePeriodTemplate(
                1L,
                2L,
                1,
                CurricularTerm.SEMESTER_1,
                9,
                2,
                12,
                31
        ));

        assertEquals("02-09", view.getStartsAt());
        assertEquals("31-12", view.getEndsAt());
        assertEquals("1st Semester", view.getPeriodLabel());
    }

    @Test
    void numbersCoursePeriodsAcrossTheWholeCourse() {
        CoursePeriodTemplateView view = CoursePeriodTemplateView.from(new CoursePeriodTemplate(
                1L,
                2L,
                3,
                CurricularTerm.SEMESTER_2,
                2,
                1,
                6,
                30
        ));

        assertEquals("6th Semester", view.getPeriodLabel());
    }
}
