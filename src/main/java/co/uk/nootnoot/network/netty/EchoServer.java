package co.uk.nootnoot.network.netty;

import java.net.InetSocketAddress;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class EchoServer implements Runnable, AutoCloseable {
    private final int port;
    private ChannelFuture closeFuture;

    public EchoServer(int port) {
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
    public void close() {
        if (closeFuture != null) {
            closeFuture.channel().close();
        }
    }

//    These are the primary code components of the server:
//            ■ The EchoServerHandler implements the business logic.
//            ■ The main() method bootstraps the server.
//    The following steps are required in bootstrapping:
//            ■ Create a ServerBootstrap instance to bootstrap and bind the server.
//            ■ Create and assign an NioEventLoopGroup instance to handle event processing,
//              such as accepting new connections and reading/writing data.
//            ■ Specify the local InetSocketAddress to which the server binds.
//            ■ Initialize each new Channel with an EchoServerHandler instance.
//            ■ Call ServerBootstrap.bind() to bind the server.

    private void start() throws Exception {
        EchoServerHandler echoServerHandler = new EchoServerHandler();
        EventLoopGroup group  = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(group)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        // When a new connection is accepted, a new child Channel will be created with an instance of
                        // the EchoServerHandler attached to the ChannelPipeline
                        @Override
                        public void initChannel(SocketChannel socketChannel) {
                            socketChannel.pipeline().addLast(echoServerHandler);
                        }
                    });
            ChannelFuture future = bootstrap.bind().sync(); // bind server and sync, which blocks thread until complete
            closeFuture = future.channel().closeFuture();
            closeFuture.sync(); // gets the closeFuture of the server's channel and blocks until complete
        } finally {
            group.shutdownGracefully().sync();
            System.out.println("Server closed");
        }
    }
}
