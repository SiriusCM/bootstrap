package com.sirius.bootstrap.core.tick;

import lombok.Data;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

@Data
public abstract class TickObject implements ITick {

    protected String id;

    protected TickThread tickThread;

    protected final Queue<Consumer<TickObject>> consumerQueue = new LinkedBlockingQueue<>();

    public void bindThread(TickThread tickThread) {
        if (tickThread == null) {
            return;
        }
        this.tickThread = tickThread;
        this.tickThread.register(this);
    }

    public void unBindThread() {
        tickThread.unRegister(id);
    }

    public void offerQueue(Consumer<TickObject> consumer) {
        consumerQueue.offer(consumer);
    }

    @Override
    public void start() {

    }

    @Override
    public void tick() {
        while (!consumerQueue.isEmpty()) {
            try {
                Consumer<TickObject> consumer = consumerQueue.poll();
                consumer.accept(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
