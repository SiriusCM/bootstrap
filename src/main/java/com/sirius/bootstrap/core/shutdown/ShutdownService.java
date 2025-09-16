package com.sirius.bootstrap.core.shutdown;

import com.sirius.bootstrap.core.tick.TickThread;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShutdownService {
    @Autowired
    private Channel serverChannel;
    @Autowired
    @Qualifier("bossGroup")
    private EventLoopGroup bossGroup;
    @Autowired
    @Qualifier("workerGroup")
    private EventLoopGroup workerGroup;
    @Autowired
    @Qualifier("globalThread")
    private TickThread globalThread;
    @Autowired
    @Qualifier("rpcThread")
    private List<TickThread> rpcThreadList;
    @Autowired
    @Qualifier("persistThread")
    private List<TickThread> persistThreadList;
    @Autowired
    @Qualifier("sceneThread")
    private List<TickThread> sceneThreadList;

    @PreDestroy
    public void preDestroy() throws Exception {
        serverChannel.close();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();

        globalThread.shutdown();
        rpcThreadList.forEach(TickThread::shutdown);
        persistThreadList.forEach(TickThread::shutdown);
        sceneThreadList.forEach(TickThread::shutdown);

        globalThread.join();
        for (TickThread tickThread : rpcThreadList) {
            tickThread.join();
        }
        for (TickThread tickThread : persistThreadList) {
            tickThread.join();
        }
        for (TickThread tickThread : sceneThreadList) {
            tickThread.join();
        }
    }
}
