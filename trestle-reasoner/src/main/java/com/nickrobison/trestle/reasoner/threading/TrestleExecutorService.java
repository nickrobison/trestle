package com.nickrobison.trestle.reasoner.threading;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.nickrobison.metrician.Metrician;
import org.jetbrains.annotations.NotNull;
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
public class TrestleExecutorService implements ExecutorService {
    private static final Logger logger = LoggerFactory.getLogger(TrestleExecutorService.class);
    private static final String THIS_CALL_IS_NOT_WRAPPED_BY_STACK_CLEANER_OR_TIMER = "This call is not wrapped by stack cleaner or timer";

    private final ExecutorService target;
    private final Metrician metrician;
    private final Timer queueTimer;
    private final Timer executionTimer;
    private final Meter executionCount;
    private final LinkedBlockingQueue<Runnable> backingQueue;

    private TrestleExecutorService(String executorName, int executorSize, Metrician metrician) {
//        Setup the thread pool
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(String.format("Trestle-%s-%%d", executorName))
                .setDaemon(false)
                .build();
        backingQueue = new LinkedBlockingQueue<>();
        logger.debug("Creating thread-pool {} with size {}", executorName, executorSize);
        this.target = new ThreadPoolExecutor(executorSize,
                executorSize,
                0L,
                TimeUnit.MILLISECONDS,
                backingQueue,
                threadFactory);
        this.metrician = metrician;

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

    @NotNull
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
    public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        return this.target.awaitTermination(timeout, unit);
    }

    @NotNull
    @Override
    public <T> Future<T> submit(@NotNull Callable<T> task) {
        final Instant taskSubmit = Instant.now();
        final Timer.Context time = this.queueTimer.time();
        return this.target.submit(() -> {
            time.stop();
            logger.trace("Task took {} ms to start", Duration.between(Instant.now(), taskSubmit).toMillis());
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

    @NotNull
    @Override
    public <T> Future<T> submit(@NotNull Runnable task, T result) {
        return submit(() -> {
            task.run();
            return result;
        });
    }

    @NotNull
    @Override
    public Future<?> submit(@NotNull Runnable task) {
        return submit((Callable<Void>) () -> {
            task.run();
            return null;
        });
    }

    @NotNull
    @Override
    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return tasks.stream().map(this::submit).collect(Collectors.toList());
    }

    @NotNull
    @Override
    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        logger.warn(THIS_CALL_IS_NOT_WRAPPED_BY_STACK_CLEANER_OR_TIMER);
        return this.target.invokeAll(tasks, timeout, unit);
    }

    @NotNull
    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        logger.warn(THIS_CALL_IS_NOT_WRAPPED_BY_STACK_CLEANER_OR_TIMER);
        return this.target.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        logger.warn(THIS_CALL_IS_NOT_WRAPPED_BY_STACK_CLEANER_OR_TIMER);
        return this.target.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(@NotNull Runnable command) {
        submit((Callable<Void>) () -> {
            command.run();
            return null;
        });
    }

    private Exception clientTrace() {
        return new Exception("Client stack trace");
    }


    /**
     * Returns a {@link TrestleExecutorService} with the specified parameters
     *
     * @param executorName - Name of Executor (will be propagated down to the individual threads)
     * @param executorSize - Number of threads to spawn
     * @param metrician    - {@link Metrician} instance to metric Executor performance
     * @return - {@link TrestleExecutorService}
     */
    public static TrestleExecutorService executorFactory(String executorName, int executorSize, Metrician metrician) {
        return new TrestleExecutorService(executorName, executorSize, metrician);
    }
}
