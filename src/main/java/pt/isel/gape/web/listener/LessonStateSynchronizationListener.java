package pt.isel.gape.web.listener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.service.AcademicLifecycleSynchronizationService;

@WebListener
public final class LessonStateSynchronizationListener implements ServletContextListener {

    private ScheduledExecutorService executor;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ConnectionProvider connectionProvider = ConnectionProvider.defaultProvider();
        java.time.Clock clock = ApplicationClock.system();
        AcademicLifecycleSynchronizationService lifecycleService =
                new AcademicLifecycleSynchronizationService(connectionProvider, clock);
        executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "gape-temporal-state-sync");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(
                () -> synchronize(lifecycleService, event),
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
            AcademicLifecycleSynchronizationService lifecycleService,
            ServletContextEvent event
    ) {
        runSafely("academic lifecycle data", lifecycleService::synchronize, event);
    }

    static void runSafely(String target, Runnable synchronization, ServletContextEvent event) {
        try {
            synchronization.run();
        } catch (RuntimeException exception) {
            event.getServletContext().log("Failed to synchronize temporal states for " + target, exception);
        }
    }
}
