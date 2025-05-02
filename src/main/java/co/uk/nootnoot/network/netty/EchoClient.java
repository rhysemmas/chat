package co.uk.nootnoot.network.netty;

import java.net.InetSocketAddress;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class EchoClient implements Runnable, AutoCloseable {
    private final String host;
    private final int port;
    private ChannelFuture closeFuture;

    public EchoClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws Exception {
        if (closeFuture != null) {
            closeFuture.channel().close();
        }
    }

    public void start() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress(host, port))
                    .handler(new ChannelInitializer<SocketChannel>() {
                       @Override
                       protected void initChannel(SocketChannel socketChannel) {
                           socketChannel.pipeline().addLast(new EchoClientHandler());
                       }
                    });
            ChannelFuture future = bootstrap.connect().sync(); // connects to the remote peer, waits until the connect completes
            closeFuture = future.channel().closeFuture();
            closeFuture.sync();
        } finally {
            group.shutdownGracefully();
            System.out.println("Client closed");
        }
    }
}
