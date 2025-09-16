package com.sirius.bootstrap.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirius.bootstrap.core.tick.TickThread;
import com.sirius.bootstrap.core.io.MsgHandler;
import com.sirius.bootstrap.core.sprite.user.UserObject;
import com.sirius.bootstrap.msg.Msg;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.util.ReferenceCountUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ComponentScan(basePackages = "com.jdt.game.core")
@Configuration
public class CoreConfig {
    @Autowired
    private ConfigurableApplicationContext applicationContext;
    @Value("${game.port}")
    private int port;
    @Value("${game.thread.io.pauseTime}")
    private int ioPauseTime;
    @Value("${game.thread.cpu.pauseTime}")
    private int cpuPauseTime;
    @Value("${game.db.pool.maxNum}")
    private int poolMaxNum;

    @Bean
    public Map<String, UserObject> userObjectMap() {
        return new HashMap<>();
    }

    @Bean("bossGroup")
    public EventLoopGroup bossGroup() {
        return new NioEventLoopGroup(1);
    }

    @Bean("workerGroup")
    public EventLoopGroup workerGroup() {
        return new NioEventLoopGroup();
    }

    @Bean
    public Channel serverChannel(@Qualifier("bossGroup") EventLoopGroup bossGroup, @Qualifier("workerGroup") EventLoopGroup workerGroup, ObjectMapper objectMapper) {
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup);
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.option(ChannelOption.SO_BACKLOG, 1024);
            serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
            serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    ChannelPipeline pipeline = socketChannel.pipeline();
                    // http握手
                    pipeline.addLast(new HttpServerCodec());
                    pipeline.addLast(new HttpObjectAggregator(64 * 1024));
                    // 输入流
                    pipeline.addLast(new WebSocketServerProtocolHandler("/game"));
                    pipeline.addLast(new MessageToMessageDecoder<WebSocketFrame>() {
                        @Override
                        protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> objs) throws Exception {
                            if (frame instanceof BinaryWebSocketFrame) { // 只处理二进制帧
                                ByteBuf byteBuf = frame.content();
                                ReferenceCountUtil.retain(byteBuf);
                                objs.add(byteBuf);
                            } else {
                                // 关闭连接或忽略非二进制帧
                                ctx.close();
                            }
                        }
                    });
                    pipeline.addLast(new ProtobufDecoder(Msg.Message.getDefaultInstance()));
                    // 输出流
                    pipeline.addLast(new MessageToMessageEncoder<ByteBuf>() {
                        @Override
                        protected void encode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
                            ReferenceCountUtil.retain(byteBuf);
                            list.add(new BinaryWebSocketFrame(byteBuf));
                        }
                    });
                    pipeline.addLast(new ProtobufEncoder());
                    // 处理器
                    pipeline.addLast(applicationContext.getBean(MsgHandler.class));
                }
            });
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
            return channelFuture.channel();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean("globalThread")
    public TickThread globalThread() {
        return new TickThread("globalThread", cpuPauseTime);
    }

    @Bean("rpcThread")
    public List<TickThread> rpcThread() {
        List<TickThread> list = new ArrayList<>();
        int processors = Runtime.getRuntime().availableProcessors() * 2;
        for (int i = 0; i < processors; i++) {
            TickThread thread = new TickThread("rpcThread-" + i, ioPauseTime);
            list.add(thread);
        }
        return list;
    }

    @Bean("persistThread")
    public List<TickThread> persistThread() {
        List<TickThread> list = new ArrayList<>();
        for (int i = 0; i < poolMaxNum; i++) {
            TickThread thread = new TickThread("persistThread-" + i, ioPauseTime);
            list.add(thread);
        }
        return list;
    }

    @Bean("sceneThread")
    public List<TickThread> sceneThread() {
        List<TickThread> list = new ArrayList<>();
        int processors = Runtime.getRuntime().availableProcessors() * 2;
        for (int i = 0; i < processors; i++) {
            TickThread thread = new TickThread("sceneThread-" + i, cpuPauseTime);
            list.add(thread);
        }
        return list;
    }

    @Bean
    public List<Class<?>> getClassList() throws IOException, ClassNotFoundException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();
        List<Class<?>> classList = new ArrayList<>();
        for (Resource resource : resolver.getResources("classpath:**/*.class")) {
            MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
            String className = metadataReader.getClassMetadata().getClassName();
            Class<?> clazz = Class.forName(className);
            classList.add(clazz);
        }
        return classList;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
