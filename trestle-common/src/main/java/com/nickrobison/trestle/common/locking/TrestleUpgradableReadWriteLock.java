package com.nickrobison.trestle.common.locking;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nrobison on 2/21/17.
 */
public class TrestleUpgradableReadWriteLock {

    private static final Logger logger = LoggerFactory.getLogger(TrestleUpgradableReadWriteLock.class);
    private static final int waitTimeout = 50000;
    private final Map<Thread, Integer> readingThreads = new HashMap<>();
    private int writeAccesses = 0;
    private int writeRequests = 0;
    private @Nullable Thread writingThread = null;

    /**
     * Take the Read lock
     * If the calling thread already owns the lock, an internal counter is incremented and the thread continues
     *
     * @throws InterruptedException
     */
    public synchronized void lockRead() throws InterruptedException {
        final Thread callingThread = Thread.currentThread();
        while (!canGrantReadAccess(callingThread)) {
            logger.debug("Thread {} waiting {} ms for Read lock", waitTimeout, callingThread);
            long start = System.nanoTime();
            wait(waitTimeout);
            if (System.nanoTime() - start > (waitTimeout * 1000)) {
                throw new InterruptedException("Unable to get read lock");
            }
        }
        logger.debug("{} taking the read lock", callingThread);
        readingThreads.put(callingThread, (getReadAccessCount(callingThread) + 1));
    }

    /**
     * Give up the Read lock
     * If the calling thread has claimed the lock multiple times, the lock is held until all holders have given up their Read locks
     */
    public synchronized void unlockRead() {
        final Thread callingThread = Thread.currentThread();
        if (!isReader(callingThread)) {
            throw new IllegalMonitorStateException(String.format("Calling thread %s does not hold a read lock on this ReadWriteLock", callingThread));
        }
        final int readAccessCount = getReadAccessCount(callingThread);
        if (readAccessCount == 1) {
            logger.debug("Read Unlocking {}", callingThread);
            readingThreads.remove(callingThread);
        } else {
            logger.debug("Decrementing read count for {}. {} read accesses remaining", callingThread, readAccessCount - 1);
            readingThreads.put(callingThread, (readAccessCount - 1));
        }
        notifyAll();
    }

    /**
     * Take a write lock
     * Only a single thread is permitted to hold the write lock
     * If the calling thread holds a read lock, it is permitted to upgrade its lock to a Write lock, provided there are no other threads waiting for a write lock
     *
     * @throws InterruptedException - Throws an exception if the thread is interrupted while trying to take the lock
     */
    public synchronized void lockWrite() throws InterruptedException {
        writeRequests++;
        final Thread callingThread = Thread.currentThread();
        while (!canGrantWriteAccess(callingThread)) {
            logger.debug("{} waiting {} ms for Write lock. {} write requests pending", waitTimeout, callingThread, writeRequests);
            long start = System.nanoTime();
            wait(waitTimeout);
            if (System.nanoTime() - start > (waitTimeout * 1000)) {
                throw new InterruptedException("Unable to get write lock");
            }

        }
        writeRequests--;
        writeAccesses++;
        writingThread = callingThread;
        logger.debug("{} taking the write lock", callingThread);
    }

    /**
     * Relinquish the write lock
     * If the calling thread has multiple write locks, the lock is relinquished only after all the locks are released
     * If the calling thread has upgraded its read lock to a write lock, when it finally releases the write lock, it will still hold a read lock and must call {@link #unlockRead()}
     */
    public synchronized void unlockWrite() {
        final Thread callingThread = Thread.currentThread();
        if (!isWriter(callingThread)) {
            throw new IllegalMonitorStateException(String.format("Calling thread %s does not hold a write lock on this ReadWriteLock", callingThread));
        }
        writeAccesses--;
        logger.debug("Decrementing write count for {}. {} write accesses remaining", callingThread, writeAccesses);
        if (writeAccesses == 0) {
            logger.debug("Write unlocking {}", callingThread);
            writingThread = null;
        }
        notifyAll();
    }

    private boolean canGrantReadAccess(Thread callingThread) {
        if (isWriter(callingThread)) return true;
        if (hasWriter()) return false;
        if (isReader(callingThread)) return true;
        return !hasWriteRequests();
    }

    private boolean canGrantWriteAccess(Thread callingThread) {
        if (isOnlyReader(callingThread)) return true;
        if (hasReaders()) return false;
        if (writingThread == null) return true;
        return isWriter(callingThread);
    }

    private int getReadAccessCount(Thread callingThread) {
        final Integer accessCount = readingThreads.get(callingThread);
        if (accessCount == null) return 0;
        return accessCount;
    }

    private boolean hasReaders() {
        return readingThreads.size() > 0;
    }

    private boolean isReader(Thread callingThread) {
        return readingThreads.get(callingThread) != null;
    }

    private boolean isOnlyReader(Thread callingThread) {
        return readingThreads.size() == 1 && readingThreads.get(callingThread) != null;
    }

    private boolean hasWriter() {
        return writingThread != null;
    }

    private boolean isWriter(Thread callingThread) {
        return callingThread.equals(writingThread);
    }

    private boolean hasWriteRequests() {
        return this.writeRequests > 0;
    }

}
