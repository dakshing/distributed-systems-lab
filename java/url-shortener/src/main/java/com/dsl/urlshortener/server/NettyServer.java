package com.dsl.urlshortener.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public class NettyServer<T extends SimpleChannelInboundHandler<?>> {

    private final int port;
    private final T handler;

    public NettyServer(int port, T handler) {
        this.port = port;
        this.handler = handler;
    }

    public void start() throws Exception {

        try (EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
             EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory())
        ) {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(64 * 1024)); // 64KB max content length

                            // Pass the handler to the pipeline
                            ch.pipeline().addLast(handler);
                        }
                    });

            System.out.println("Server started on port " + port);
            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();
        }
    }

}
