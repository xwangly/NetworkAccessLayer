package com.xwang.net.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by xwangly on 2016/8/27.
 */
public class Dispatcher {

    private int maxRequests = 64;
    private int maxRequestsPerHost = 5;

    /** Executes calls. Created lazily. */
    private ExecutorService executorService;

    /** Ready async calls in the order they'll be run. */
    private final Deque<RealCall.AsyncCall> readyAsyncCalls = new ArrayDeque<>();

    /** Running asynchronous calls. Includes canceled calls that haven't finished yet. */
    private final Deque<RealCall.AsyncCall> runningAsyncCalls = new ArrayDeque<>();

    /** Running synchronous calls. Includes canceled calls that haven't finished yet. */
    private final Deque<RealCall> runningSyncCalls = new ArrayDeque<>();

    public Dispatcher(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public Dispatcher() {
    }

    public synchronized ExecutorService executorService() {
        if (executorService == null) {
            executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread result = new Thread(r, "Dispatcher");
                    result.setDaemon(false);
                    return result;
                }
            });
        }
        return executorService;
    }

    /**
     * Set the maximum number of requests to execute concurrently. Above this requests queue in
     * memory, waiting for the running calls to complete.
     *
     * <p>If more than {@code maxRequests} requests are in flight when this is invoked, those requests
     * will remain in flight.
     */
    public synchronized void setMaxRequests(int maxRequests) {
        if (maxRequests < 1) {
            throw new IllegalArgumentException("max < 1: " + maxRequests);
        }
        this.maxRequests = maxRequests;
        promoteCalls();
    }

    public synchronized int getMaxRequests() {
        return maxRequests;
    }

    /**
     * Set the maximum number of requests for each host to execute concurrently. This limits requests
     * by the URL's host name. Note that concurrent requests to a single IP address may still exceed
     * this limit: multiple hostnames may share an IP address or be routed through the same HTTP
     * proxy.
     *
     * <p>If more than {@code maxRequestsPerHost} requests are in flight when this is invoked, those
     * requests will remain in flight.
     */
    public synchronized void setMaxRequestsPerHost(int maxRequestsPerHost) {
        if (maxRequestsPerHost < 1) {
            throw new IllegalArgumentException("max < 1: " + maxRequestsPerHost);
        }
        this.maxRequestsPerHost = maxRequestsPerHost;
        promoteCalls();
    }

    public synchronized int getMaxRequestsPerHost() {
        return maxRequestsPerHost;
    }

    synchronized void enqueue(RealCall.AsyncCall call) {
        if (runningAsyncCalls.size() < maxRequests && runningCallsForHost(call) < maxRequestsPerHost) {
            runningAsyncCalls.add(call);
            executorService().execute(call);
        } else {
            readyAsyncCalls.add(call);
        }
    }

    /**
     * Cancel all calls currently enqueued or executing. Includes calls executed both {@linkplain
     * RealCall#execute() synchronously} and {@linkplain RealCall#enqueue asynchronously}.
     */
    public synchronized void cancelAll() {
        for (RealCall.AsyncCall call : readyAsyncCalls) {
            call.cancel();
        }

        for (RealCall.AsyncCall call : runningAsyncCalls) {
            call.cancel();
        }

        for (RealCall call : runningSyncCalls) {
            call.cancel();
        }
    }

    /** Used by {@code AsyncCall#run} to signal completion. */
    synchronized void finished(RealCall.AsyncCall call) {
        if (!runningAsyncCalls.remove(call)) throw new AssertionError("AsyncCall wasn't running!");
        promoteCalls();
    }

    private void promoteCalls() {
        if (runningAsyncCalls.size() >= maxRequests) return; // Already running max capacity.
        if (readyAsyncCalls.isEmpty()) return; // No ready calls to promote.

        for (Iterator<RealCall.AsyncCall> i = readyAsyncCalls.iterator(); i.hasNext(); ) {
            RealCall.AsyncCall call = i.next();

            if (runningCallsForHost(call) < maxRequestsPerHost) {
                i.remove();
                runningAsyncCalls.add(call);
                executorService().execute(call);
            }

            if (runningAsyncCalls.size() >= maxRequests) return; // Reached max capacity.
        }
    }

    /** Returns the number of running calls that share a host with {@code call}. */
    private int runningCallsForHost(RealCall.AsyncCall call) {
        int result = 0;
        for (RealCall.AsyncCall c : runningAsyncCalls) {
            if (c.host().equals(call.host())) result++;
        }
        return result;
    }

    /** Used by {@code Call#execute} to signal it is in-flight. */
    synchronized void executed(RealCall call) {
        runningSyncCalls.add(call);
    }

    /** Used by {@code Call#execute} to signal completion. */
    synchronized void finished(RealCall call) {
        if (!runningSyncCalls.remove(call)) throw new AssertionError("Call wasn't in-flight!");
    }

    /** Returns a snapshot of the calls currently awaiting execution. */
    public synchronized List<RealCall> queuedCalls() {
        List<RealCall> result = new ArrayList<>();
        for (RealCall.AsyncCall asyncCall : readyAsyncCalls) {
            result.add(asyncCall.get());
        }
        return Collections.unmodifiableList(result);
    }

    /** Returns a snapshot of the calls currently being executed. */
    public synchronized List<RealCall> runningCalls() {
        List<RealCall> result = new ArrayList<>();
        result.addAll(runningSyncCalls);
        for (RealCall.AsyncCall asyncCall : runningAsyncCalls) {
            result.add(asyncCall.get());
        }
        return Collections.unmodifiableList(result);
    }
    public void shutDown() {
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
    }

    public synchronized int queuedCallsCount() {
        return readyAsyncCalls.size();
    }

    public synchronized int runningCallsCount() {
        return runningAsyncCalls.size() + runningSyncCalls.size();
    }
}
