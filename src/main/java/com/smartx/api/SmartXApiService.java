/**
 Copyright (c) 2017-2018 The SmartX Developers
 <p>
 Distributed under the MIT software license, see the accompanying file
 LICENSE or https://opensource.org/licenses/mit-license.php
 */
package com.smartx.api;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.smartx.Kernel;
import com.smartx.api.http.HttpChannelInitializer;
import com.smartx.api.http.HttpHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * SmartX API launcher
 */
public class SmartXApiService {
    private static final Logger logger = Logger.getLogger(SmartXApiService.class);
    private static final ThreadFactory factory = new ThreadFactory() {
        final AtomicInteger cnt = new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "api-" + cnt.getAndIncrement());
        }
    };
    private Kernel kernel;
    private Channel channel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ApiHandler apiHandler;
    private String ip;
    private int port;
    public SmartXApiService(Kernel kernel) {
        this.kernel = kernel;
        this.apiHandler = new ApiHandlerImpl(kernel);
    }
    /**
     * Starts API server with configured binding address.
     */
    public void start() {
        start(kernel.getConfig().apiListenIp(), kernel.getConfig().apiListenPort());
    }
    /**
     * Starts API server at the given binding IP and port.
     *
     * @param ip
     * @param port
     */
    public void start(String ip, int port) {
        start(ip, port, apiHandler);
    }
    /**
     * Starts API server at the given binding IP and port, with the specified
     * channel initializer.
     *
     * @param ip
     * @param port
     * @param apiHandler
     */
    public void start(String ip, int port, ApiHandler apiHandler) {
        try {
            this.ip = ip;
            this.port = port;
            bossGroup = new NioEventLoopGroup(1, factory);
            workerGroup = new NioEventLoopGroup(0, factory);
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.DEBUG)).childHandler(new HttpChannelInitializer() {
                public HttpHandler initHandler() {
                    return new HttpHandler(kernel, apiHandler);
                }
            });
            logger.info("Starting API server: address = " + ip + " " + port);
            channel = b.bind(ip, port).sync().channel();
            // allow larger messages for local contract calls.
            // contract/call data is 64 * 1024, plus another 1024 for rest of call
            int bufferSize = 65 * 1024;
            channel.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(bufferSize));
            channel.config().setOption(ChannelOption.SO_RCVBUF, bufferSize);
            logger.info(String.format("API server started. Base URL: {%s}, Explorer: {%s}", getApiBaseUrl(), getApiExplorerUrl()));
        } catch (Exception e) {
            logger.error("Failed to start API server", e);
        }
    }
    /**
     * Stops the API server if started.
     */
    public void stop() {
        if (isRunning() && channel.isOpen()) {
            try {
                channel.close().sync();
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
                // workerGroup.terminationFuture().sync();
                // bossGroup.terminationFuture().sync();
                channel = null;
            } catch (Exception e) {
                logger.error("Failed to close channel", e);
            }
            logger.info("API server shut down");
        }
    }
    public String getIp() {
        return ip;
    }
    public int getPort() {
        return port;
    }
    /**
     * Returns whether the API server is running or not.
     *
     * @return
     */
    public boolean isRunning() {
        return channel != null;
    }
    /**
     * Returns the API base URL.
     *
     * @return
     */
    public String getApiBaseUrl() {
        return String.format("http://%s:%d/%s/", ip, port, ApiVersion.DEFAULT.prefix);
    }
    /**
     * Returns the API explorer URL.
     *
     * @return
     */
    public String getApiExplorerUrl() {
        return String.format("http://%s:%d/index.html", ip, port);
    }
}
