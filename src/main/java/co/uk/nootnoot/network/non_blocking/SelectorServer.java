package co.uk.nootnoot.network.non_blocking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import co.uk.nootnoot.util.Logger;

public class SelectorServer implements Runnable {
    private final SelectorServerLogger logger;
    private final Selector selector;
    private final ServerSocketChannel serverSocket;
    private final Map<SocketAddress, SocketChannel> clientChannels;
    private volatile boolean running = true;

    public SelectorServer(String host, Integer port) throws IOException {
        this.logger = new SelectorServerLogger();
        this.selector = Selector.open();
        this.serverSocket = ServerSocketChannel.open();
        this.clientChannels = new HashMap<>();

        serverSocket.bind(new InetSocketAddress(host, port));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void run() {
        logger.log("Server started");
        while (running) {
            try {
                selector.select();
            } catch (IOException e) {
                logger.log("Selector exception: " + e.getMessage());
            }

            Set<SelectionKey> selectionKeys;
            try {
                selectionKeys = selector.selectedKeys();
            } catch (ClosedSelectorException e) {
                break;
            }

            var iter = selectionKeys.iterator();
            while (iter.hasNext()) {
                var key = iter.next();
                iter.remove(); // next selectedKeys call will return the same key if not removed

                if (key.isAcceptable()) {
                    logger.log("Connection accepted");
                    register(serverSocket, key);
                }

                if (key.isReadable()) {
                    readFromClient(key);
                }
            }
        }
    }

    private void register(ServerSocketChannel serverSocket, SelectionKey key) {
        try {
            SocketChannel client = serverSocket.accept();
            if (client != null) {
                client.configureBlocking(false);
                client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                clientChannels.put(client.getRemoteAddress(), client);
            }
        } catch (IOException e) {
            logger.log("Exception when registering client socket: " + e.getMessage());
        }
    }

    private void readFromClient(SelectionKey key) {
        var buffer = ByteBuffer.allocate(256);
        try {
            var client = (SocketChannel) key.channel();
            // TODO: if we read more than one message from the buffer, we should split them on some character (e.g, '\n')
            client.read(buffer);
            buffer.flip();
            broadcastToClients(buffer, client);
        } catch (IOException e) {
            logger.log("Exception reading from client: " + e.getMessage());
        }
    }

    private void broadcastToClients(ByteBuffer buffer, SocketChannel currentClient) {
        logger.log("Current client: " + getClientAddress(currentClient));
        logger.log("Broadcasting message to clients: " + new String(buffer.array()).trim());
        clientChannels.entrySet().stream()
                .filter(c -> !c.getKey().equals(getClientAddress(currentClient)))
                .forEach(c -> {
                    buffer.mark();
                    writeToClient(buffer, c.getValue());
                    buffer.reset();
                });
    }

    private SocketAddress getClientAddress(SocketChannel client) {
        try {
            return client.getRemoteAddress();
        } catch (IOException e) {
            logger.log("Exception when getting client address: " + e.getMessage());
        }
        return null;
    }

    private void writeToClient(ByteBuffer buffer, SocketChannel client) {
        try {
            if (client.isConnected()) {
                client.write(buffer);
            }
        } catch (IOException e) {
            logger.log("Exception when writing client: " + e.getMessage());
        }
    }

    public void stop() throws IOException {
        running = false;
        serverSocket.close();
        selector.close();
    }

    private static class SelectorServerLogger implements Logger {
        public void log(String message) {
            System.out.println("Selector Server: " + message);
        }
    }
}
