package com.sirius.bootstrap.core.tick;

import java.util.HashMap;
import java.util.Map;

public class TickThread extends Thread {

    private final Map<String, TickObject> tickObjectMap = new HashMap<>();

    private boolean alive = true;

    private int tickTime;

    public TickThread(String name, int tickTime) {
        super(name);
        this.tickTime = tickTime;
        start();
    }

    public void resetPauseTime(int tickTime) {
        if (tickTime <= 0) {
            return;
        }
        this.tickTime = tickTime;
    }

    public synchronized void register(TickObject tickObject) {
        tickObjectMap.put(tickObject.getId(), tickObject);
    }

    public synchronized void unRegister(String id) {
        tickObjectMap.remove(id);
    }

    @Override
    public void run() {
        while (alive || !tickObjectMap.isEmpty()) {
            try {
                for (TickObject tickObject : tickObjectMap.values()) {
                    try {
                        tickObject.tick();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                Thread.sleep(tickTime);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void shutdown() {
        alive = false;
    }
}
