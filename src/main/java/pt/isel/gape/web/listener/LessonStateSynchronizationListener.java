package pt.isel.gape.web.listener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.service.AssessmentService;
import pt.isel.gape.learning.service.ClassGroupService;
import pt.isel.gape.learning.service.LessonService;

@WebListener
public final class LessonStateSynchronizationListener implements ServletContextListener {

    private ScheduledExecutorService executor;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ConnectionProvider connectionProvider = ConnectionProvider.defaultProvider();
        java.time.Clock clock = ApplicationClock.system();
        ClassGroupService classGroupService = new ClassGroupService(connectionProvider, clock);
        LessonService lessonService = new LessonService(connectionProvider, clock);
        AssessmentService assessmentService = new AssessmentService(connectionProvider, clock);
        executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "gape-temporal-state-sync");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(
                () -> synchronize(classGroupService, lessonService, assessmentService, event),
                30,
                60,
                TimeUnit.SECONDS
        );
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private static void synchronize(
            ClassGroupService classGroupService,
            LessonService lessonService,
            AssessmentService assessmentService,
            ServletContextEvent event
    ) {
        try {
            classGroupService.synchronizeTemporalStates();
            lessonService.synchronizeTemporalStates();
            assessmentService.synchronizeTemporalStates();
        } catch (RuntimeException exception) {
            event.getServletContext().log("Failed to synchronize temporal states", exception);
        }
    }
}
