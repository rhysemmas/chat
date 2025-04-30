package co.uk.nootnoot.network_nonblocking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import co.uk.nootnoot.util.Logger;

public class ByteClient implements Runnable, AutoCloseable {
    private final SocketChannel socketChannel;
    private final Logger logger;
    private List<String> receivedMessages = new ArrayList<>();

    public ByteClient(Integer id, String serverHost, Integer serverPort) throws IOException {
        this.logger = new ByteClientLogger(id);
        this.socketChannel = SocketChannel.open(new InetSocketAddress(serverHost, serverPort));
        logger.log("Client with id " + id + " opened on: " + socketChannel.socket().getLocalSocketAddress());
    }

    public void sendMessage(String message) throws IOException {
        var buffer = ByteBuffer.allocate(256);
        buffer = ByteBuffer.wrap(message.getBytes());
        socketChannel.write(buffer);
        logger.log("Sent message: " + message);
    }

    @Override
    public void run() {
        var buffer = ByteBuffer.allocate(256);
        while (socketChannel.isConnected()) {
            buffer.clear();
            try {
                if (socketChannel.read(buffer) > 0) {
                    buffer.flip();
                    String response = new String(buffer.array()).trim();
                    logger.log("Got response: " + response);
                    receivedMessages.add(response);
                }
            } catch (IOException e) {
                logger.log("Exception while reading from socket: " + e);
                break;
            }
        }
    }

    @Override
    public void close() {
        try {
            socketChannel.close();
            socketChannel.socket().close();
            logger.log("Client closed");
        } catch (IOException e) {
            logger.log("Error closing client socket: " + e.getMessage());
        }
    }

    public List<String> getReceivedMessages() {
        return receivedMessages;
    }

    private static class ByteClientLogger implements Logger {
        private final Integer id;

        private ByteClientLogger(Integer id) {
            this.id = id;
        }

        @Override
        public void log(String message) {
            System.out.println("ByteClient " + id + ": "  + message);
        }
    }
}
