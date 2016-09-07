package com.xwang.net.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingHashMap<K, V> {

    /**
     * The queued items
     */
    final HashMap<K, V> items;
    final Set<K> interruptSet;

    /**
     * Main lock guarding all access
     */
    final ReentrantLock lock;
    /**
     * Condition for waiting takes
     */
    private final Condition notEmpty;
    private boolean interruptGetter = false;

    public BlockingHashMap() {
        items = new HashMap<K, V>();
        interruptSet = new HashSet<>();
        lock = new ReentrantLock(true);
        notEmpty = lock.newCondition();
    }

    /**
     * Put Socket response and signal all Dispatcher
     *
     * @param k
     * @param v
     * @return
     * @throws InterruptedException
     */
    public V put(K k, V v) {
        final ReentrantLock lock = this.lock;
        try {
            lock.lockInterruptibly();
            try {
                V v1 = items.put(k, v);
                notEmpty.signalAll();
                return v1;
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public V get(K k, long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        try {
            V v;
            while ((v = items.remove(k)) == null) {
                if (nanos <= 0) // timeout
                    return null;
                if (interruptSet.remove(k))
                    throw new InterruptedException();
                nanos = notEmpty.awaitNanos(nanos);
            }
            return v;
        } finally {
            lock.unlock();
        }
    }

    public void interrutp(K k) {
        final ReentrantLock lock = this.lock;
        try {
            lock.lockInterruptibly();
            try {
                interruptSet.clear();
                interruptSet.add(k);
                notEmpty.signalAll();
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void interrutpAll() {
        final ReentrantLock lock = this.lock;
        try {
            lock.lockInterruptibly();
            try {
                interruptSet.clear();
                interruptSet.addAll(items.keySet());
                notEmpty.signalAll();
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * @param k
     * @return
     */
    public boolean contains(K k) {
        if (k == null) return false;
        final ReentrantLock lock = this.lock;
        try {
            lock.lockInterruptibly();
            try {
                return items.containsKey(k);
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }


}
