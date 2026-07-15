package pt.isel.gape.web.view;

import java.time.YearMonth;
import java.util.List;

public final class EventCalendarView {

    private final String title;
    private final String monthValue;
    private final String previousMonthValue;
    private final String nextMonthValue;
    private final List<EventCalendarDayView> days;
    private final int visibleMonthEventCount;
    private final int totalEventCount;

    public EventCalendarView(
            YearMonth month,
            String title,
            List<EventCalendarDayView> days,
            int visibleMonthEventCount,
            int totalEventCount
    ) {
        this.title = title;
        this.monthValue = month.toString();
        this.previousMonthValue = month.minusMonths(1).toString();
        this.nextMonthValue = month.plusMonths(1).toString();
        this.days = List.copyOf(days);
        this.visibleMonthEventCount = visibleMonthEventCount;
        this.totalEventCount = totalEventCount;
    }

    public String getTitle() {
        return title;
    }

    public String getMonthValue() {
        return monthValue;
    }

    public String getPreviousMonthValue() {
        return previousMonthValue;
    }

    public String getNextMonthValue() {
        return nextMonthValue;
    }

    public List<EventCalendarDayView> getDays() {
        return days;
    }

    public int getVisibleMonthEventCount() {
        return visibleMonthEventCount;
    }

    public int getTotalEventCount() {
        return totalEventCount;
    }
}
