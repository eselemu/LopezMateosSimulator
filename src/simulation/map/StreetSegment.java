package simulation.map;

import java.util.concurrent.locks.ReentrantLock;

public class StreetSegment {
    private String segmentId;
    private int capacity;
    private int currentOccupancy;
    private ReentrantLock lock;

    public StreetSegment(String segmentId, int capacity) {
        this.segmentId = segmentId;
        this.capacity = capacity;
        this.currentOccupancy = 0;
        this.lock = new ReentrantLock();
    }

    public boolean tryAcquire() {
        System.out.println("[DEBUG] Thread " + Thread.currentThread().getName() +
                " trying to acquire " + segmentId);
        if(lock.tryLock()) {
            System.out.println("[DEBUG] Thread " + Thread.currentThread().getName() +
                    " got lock on " + segmentId + ", occupancy=" + currentOccupancy +
                    ", capacity=" + capacity);
            if(currentOccupancy < capacity) {
                currentOccupancy++;
                System.out.println("[DEBUG] Thread " + Thread.currentThread().getName() +
                        " ACQUIRED " + segmentId + ", new occupancy=" + currentOccupancy);
                return true;
            } else {
                System.out.println("[DEBUG] Thread " + Thread.currentThread().getName() +
                        " FAILED (at capacity) " + segmentId);
                lock.unlock();
                return false;
            }
        }
        System.out.println("[DEBUG] Thread " + Thread.currentThread().getName() +
                " couldn't get lock on " + segmentId + " (already locked)");
        return false;
    }

    public void release() {
        System.out.println("[DEBUG] Thread " + Thread.currentThread().getName() +
                " releasing " + segmentId);
        if (lock.isHeldByCurrentThread()) {
            currentOccupancy--;
            lock.unlock();
            System.out.println("[DEBUG] Thread " + Thread.currentThread().getName() +
                    " RELEASED " + segmentId + ", new occupancy=" + currentOccupancy);
        } else {
            System.out.println("[DEBUG] Thread " + Thread.currentThread().getName() +
                    " tried to release " + segmentId + " but doesn't hold the lock!");
        }
    }

    public String getSegmentId() { return segmentId; }
    public int getCurrentOccupancy() { return currentOccupancy; }
}
