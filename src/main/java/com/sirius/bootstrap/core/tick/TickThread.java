package com.sirius.bootstrap.core.tick;

import java.util.HashMap;
import java.util.Map;

public class TickThread extends Thread {

    private final Map<String, TickObject> frameMap = new HashMap<>();

    private boolean alive = true;

    private int pauseTime;

    public TickThread(String name, int pauseTime) {
        super(name);
        this.pauseTime = pauseTime;
        start();
    }

    public void resetPauseTime(int pauseTime) {
        if (pauseTime <= 0) {
            return;
        }
        this.pauseTime = pauseTime;
    }

    public synchronized void register(TickObject tickObject) {
        frameMap.put(tickObject.getId(), tickObject);
    }

    public synchronized void unRegister(String id) {
        frameMap.remove(id);
    }

    @Override
    public void run() {
        while (alive || !frameMap.isEmpty()) {
            try {
                for (TickObject tickObject : frameMap.values()) {
                    try {
                        tickObject.tick();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                Thread.sleep(pauseTime);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void shutdown() {
        alive = false;
    }
}
