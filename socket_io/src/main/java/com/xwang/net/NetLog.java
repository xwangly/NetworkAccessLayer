/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xwang.net;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Logging helper class. */
public class NetLog {
    public interface LogPrinter {
        boolean logable();
        void d(String tag, String msg);
        void e(String tag, String msg);
    }
    private static String TAG = "network";

    private static boolean DEBUG = false;

    private static LogPrinter logPrinter;

    public static void setLogPrinter(LogPrinter printer) {
        logPrinter = printer;
        DEBUG = logPrinter != null ? logPrinter.logable() : false;
    }

    public static void setLogTag(String tag) {
        TAG = tag;
    }
    public static void d(String format) {
        d(TAG, format);
    }
    public static void d(String tag, String format) {
        if (DEBUG) {
            String msg = buildMessage(format);
            if (logPrinter != null) {
                logPrinter.d(tag, msg);
            } else {
                System.out.println(tag +":" + msg);
            }
        }
    }
    public static void e(String format) {
        e(TAG, format);
    }
    public static void e(String tag, String format) {
        String msg = buildMessage(format);
        if (logPrinter != null) {
            logPrinter.e(tag, msg);
        } else {
            System.err.println(tag +":" + msg);
        }
    }

    public static void e(Throwable tr, String msg) {
        e(msg);
        if (tr != null) {
            tr.printStackTrace();
        }
    }

    /**
     * Formats the caller's provided message and prepends useful info like
     * calling thread ID and method name.
     */
    private static String buildMessage(String format) {
        String msg = format;
        StackTraceElement[] trace = new Throwable().fillInStackTrace().getStackTrace();

        String caller = "<unknown>";
        // Walk up the stack looking for the first caller outside of VolleyLog.
        // It will be at least two frames up, so start there.
        for (int i = 2; i < trace.length; i++) {
            Class<?> clazz = trace[i].getClass();
            if (!clazz.equals(NetLog.class)) {
                String callingClass = trace[i].getClassName();
                callingClass = callingClass.substring(callingClass.lastIndexOf('.') + 1);
                callingClass = callingClass.substring(callingClass.lastIndexOf('$') + 1);

                caller = callingClass + "." + trace[i].getMethodName();
                break;
            }
        }
        return String.format(Locale.US, "[%s] %s: %s",
                Thread.currentThread().getName(), caller, msg);
    }
    public static class MarkerLog {
        public static final boolean ENABLED = NetLog.DEBUG;

        /** Minimum duration from first marker to last in an marker log to warrant logging. */
        private static final long MIN_DURATION_FOR_LOGGING_MS = 0;

        private static class Marker {
            public final String name;
            public final long thread;
            public final long time;

            public Marker(String name, long thread, long time) {
                this.name = name;
                this.thread = thread;
                this.time = time;
            }
        }

        private final List<Marker> mMarkers = new ArrayList<Marker>();
        private boolean mFinished = false;

        /** Adds a marker to this log with the specified name. */
        public synchronized void add(String name, long threadId) {
            if (mFinished) {
                throw new IllegalStateException("Marker added to finished log");
            }

            mMarkers.add(new Marker(name, threadId, System.currentTimeMillis()));
        }

        /**
         * Closes the log, dumping it to logcat if the time difference between
         * the first and last markers is greater than {@link #MIN_DURATION_FOR_LOGGING_MS}.
         * @param header Header string to print above the marker log.
         */
        public synchronized void finish(String header) {
            mFinished = true;

            long duration = getTotalDuration();
            if (duration <= MIN_DURATION_FOR_LOGGING_MS) {
                return;
            }

            long prevTime = mMarkers.get(0).time;
            NetLog.d(format("(%-4d ms) %s", duration, header));
            for (Marker marker : mMarkers) {
                long thisTime = marker.time;
                NetLog.d(format("(+%-4d) [%2d] %s", (thisTime - prevTime), marker.thread, marker.name));
                prevTime = thisTime;
            }
        }

        @Override
        protected void finalize() throws Throwable {
            // Catch requests that have been collected (and hence end-of-lifed)
            // but had no debugging output printed for them.
            if (!mFinished) {
                finish("Request on the loose");
                NetLog.e("Marker log finalized without finish() - uncaught exit point for request");
            }
        }

        /** Returns the time difference between the first and last events in this log. */
        private long getTotalDuration() {
            if (mMarkers.size() == 0) {
                return 0;
            }

            long first = mMarkers.get(0).time;
            long last = mMarkers.get(mMarkers.size() - 1).time;
            return last - first;
        }
        private String format(String format, Object... args) {
            return String.format(Locale.US, format, args);
        }
    }
}
