package pt.isel.gape.web.listener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.service.LessonService;

@WebListener
public final class LessonStateSynchronizationListener implements ServletContextListener {

    private ScheduledExecutorService executor;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        LessonService lessonService = new LessonService(
                ConnectionProvider.defaultProvider(),
                ApplicationClock.system()
        );
        executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "gape-lesson-state-sync");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(
                () -> synchronize(lessonService, event),
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

    private static void synchronize(LessonService lessonService, ServletContextEvent event) {
        try {
            lessonService.synchronizeTemporalStates();
        } catch (RuntimeException exception) {
            event.getServletContext().log("Failed to synchronize lesson states", exception);
        }
    }
}
