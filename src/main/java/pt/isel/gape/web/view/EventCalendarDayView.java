package pt.isel.gape.web.view;

import java.time.LocalDate;
import java.util.List;

public final class EventCalendarDayView {

    private final LocalDate date;
    private final String dayLabel;
    private final boolean currentMonth;
    private final boolean today;
    private final List<LearningNotificationView> events;

    public EventCalendarDayView(
            LocalDate date,
            boolean currentMonth,
            boolean today,
            List<LearningNotificationView> events
    ) {
        this.date = date;
        this.dayLabel = Integer.toString(date.getDayOfMonth());
        this.currentMonth = currentMonth;
        this.today = today;
        this.events = List.copyOf(events);
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDayLabel() {
        return dayLabel;
    }

    public boolean isCurrentMonth() {
        return currentMonth;
    }

    public boolean isToday() {
        return today;
    }

    public List<LearningNotificationView> getEvents() {
        return events;
    }

    public int getEventCount() {
        return events.size();
    }

    public boolean isHasEvents() {
        return !events.isEmpty();
    }
}
