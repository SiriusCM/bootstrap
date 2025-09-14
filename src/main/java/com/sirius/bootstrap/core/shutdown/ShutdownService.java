package com.sirius.bootstrap.core.shutdown;

import com.sirius.bootstrap.core.frame.FrameThread;
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
    private FrameThread globalThread;
    @Autowired
    @Qualifier("rpcThread")
    private List<FrameThread> rpcThreadList;
    @Autowired
    @Qualifier("persistThread")
    private List<FrameThread> persistThreadList;
    @Autowired
    @Qualifier("sceneThread")
    private List<FrameThread> sceneThreadList;

    @PreDestroy
    public void preDestroy() throws Exception {
        serverChannel.close();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();

        globalThread.shutdown();
        rpcThreadList.forEach(FrameThread::shutdown);
        persistThreadList.forEach(FrameThread::shutdown);
        sceneThreadList.forEach(FrameThread::shutdown);

        globalThread.join();
        for (FrameThread frameThread : rpcThreadList) {
            frameThread.join();
        }
        for (FrameThread frameThread : persistThreadList) {
            frameThread.join();
        }
        for (FrameThread frameThread : sceneThreadList) {
            frameThread.join();
        }
    }
}
