package com.sirius.bootstrap.core.frame;

import java.util.HashMap;
import java.util.Map;

public class FrameThread extends Thread {

    private final Map<String, Frame> frameMap = new HashMap<>();

    private boolean alive = true;

    private int pauseTime;

    public FrameThread(String name, int pauseTime) {
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

    public synchronized void register(Frame frame) {
        frameMap.put(frame.getId(), frame);
    }

    public synchronized void unRegister(String id) {
        frameMap.remove(id);
    }

    @Override
    public void run() {
        while (alive || !frameMap.isEmpty()) {
            try {
                for (Frame frame : frameMap.values()) {
                    try {
                        frame.nextFrame();
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
