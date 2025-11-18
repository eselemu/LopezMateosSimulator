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
        if(lock.tryLock()) {
            if(currentOccupancy < capacity) {
                currentOccupancy++;
                return true;
            } else {
                lock.unlock();
                return false;
            }
        }
        return false;
    }

    public void release() {
        if (lock.isHeldByCurrentThread()) {
            currentOccupancy--;
            lock.unlock();
        }
    }

    public String getSegmentId() { return segmentId; }
    public int getCurrentOccupancy() { return currentOccupancy; }
}
