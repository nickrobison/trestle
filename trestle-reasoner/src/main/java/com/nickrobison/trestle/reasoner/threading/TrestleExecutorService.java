package com.nickrobison.trestle.reasoner.threading;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.nickrobison.metrician.Metrician;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Created by nrobison on 7/7/17.
 */
// I hate overriding the null warnings, but I don't really know how to fix them.
@SuppressWarnings({"pmd:DoNotUseThreads", "pmd:LawOfDemeter", "override.return.invalid", "return.type.invalid", "return.type.incompatible"})
public class TrestleExecutorService implements ExecutorService {
    private static final Logger logger = LoggerFactory.getLogger(TrestleExecutorService.class);
    private static final String THIS_CALL_IS_NOT_WRAPPED_BY_STACK_CLEANER_OR_TIMER = "This call is not wrapped by stack cleaner or timer";

    private final ExecutorService target;
    private final Timer queueTimer;
    private final Timer executionTimer;
    private final Meter executionCount;

    @Inject
    public TrestleExecutorService(@Assisted String executorName, Metrician metrician) {
        final Config config = ConfigFactory.load().getConfig("trestle.threading");
//        Try to get the config, otherwise, fallback to the default
        int executorSize;
        try {
            executorSize = config.getInt(executorName + ".size");
        } catch (ConfigException.Missing e) {
            logger.warn("Unable to find configuration for {}. Falling back to defaults", executorName);
            executorSize = config.getInt("default-pool.size");
        }

        //        Setup the thread pool
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(String.format("Trestle-%s-%%d", executorName))
                .setDaemon(false)
                .build();
        final LinkedBlockingQueue<Runnable> backingQueue = new LinkedBlockingQueue<>();
        logger.debug("Creating thread-pool {} with size {}", executorName, executorSize);
        this.target = new ThreadPoolExecutor(executorSize,
                executorSize,
                0L,
                TimeUnit.MILLISECONDS,
                backingQueue,
                threadFactory);

//        Setup Metrician Timers
        queueTimer = metrician.registerTimer(String.format("%s-queue-time", executorName));
        executionTimer = metrician.registerTimer(String.format("%s-execution-time", executorName));
        executionCount = metrician.registerMeter(String.format("%s-execution-count", executorName));
        metrician.registerGauge(String.format("%s-queue-length", executorName), backingQueue::size);
    }


    @Override
    public void shutdown() {
        this.target.shutdown();
    }

    @NonNull
    @Override
    public List<Runnable> shutdownNow() {
        return this.target.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return this.target.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return this.target.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        return this.target.awaitTermination(timeout, unit);
    }

    @NonNull
    @Override
    public <T> Future<T> submit(@NonNull Callable<T> task) {
        final Instant taskSubmit = Instant.now();
        final Timer.Context time = this.queueTimer.time();
        return this.target.submit(() -> {
            time.stop();
            logger.trace("Task took {} ms to start", Duration.between(taskSubmit, Instant.now()).toMillis());
            this.executionCount.mark();
            final Timer.Context execTimer = executionTimer.time();
            try {
                return task.call();
            } catch (Exception e) {
                logger.error("Exception {} in task submitted from thread {}, here:", e, Thread.currentThread().getName(), clientTrace());
                throw e;
            } finally {
                execTimer.stop();
            }
        });
    }

    @NonNull
    @Override
    public <T> Future<T> submit(@NonNull Runnable task, T result) {
        return submit(() -> {
            task.run();
            return result;
        });
    }

    @NonNull
    @Override
    public Future<?> submit(@NonNull Runnable task) {
        return submit((Callable<Void>) () -> {
            task.run();
            return null;
        });
    }

    @NonNull
    @Override
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return tasks.stream().map(this::submit).collect(Collectors.toList());
    }

    @NonNull
    @Override
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        logger.warn(THIS_CALL_IS_NOT_WRAPPED_BY_STACK_CLEANER_OR_TIMER);
        return this.target.invokeAll(tasks, timeout, unit);
    }

    @NonNull
    @Override
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        logger.warn(THIS_CALL_IS_NOT_WRAPPED_BY_STACK_CLEANER_OR_TIMER);
        return this.target.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        logger.warn(THIS_CALL_IS_NOT_WRAPPED_BY_STACK_CLEANER_OR_TIMER);
        return this.target.invokeAny(tasks, timeout, unit);
    }

    @Override
    @SuppressWarnings("FutureReturnValueIgnored") // I think we can ignore this
    public void execute(@NonNull Runnable command) {
        submit((Callable<Void>) () -> {
            command.run();
            return null;
        });
    }

    private Exception clientTrace() {
        return new Exception("Client stack trace");
    }
}
