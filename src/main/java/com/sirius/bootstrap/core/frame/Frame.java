package com.sirius.bootstrap.core.frame;

import lombok.Data;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

@Data
public abstract class Frame implements IFrame {

    protected String id;

    protected FrameThread frameThread;

    protected final Queue<Consumer<Frame>> consumerQueue = new LinkedBlockingQueue<>();

    public void bindThread(FrameThread frameThread) {
        if (frameThread == null) {
            return;
        }
        this.frameThread = frameThread;
        this.frameThread.register(this);
    }

    public void unBindThread() {
        frameThread.unRegister(id);
    }

    public void offerQueue(Consumer<Frame> consumer) {
        consumerQueue.offer(consumer);
    }

    @Override
    public void nextFrame() {
        while (!consumerQueue.isEmpty()) {
            try {
                Consumer<Frame> consumer = consumerQueue.poll();
                consumer.accept(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
