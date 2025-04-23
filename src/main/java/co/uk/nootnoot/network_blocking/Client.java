package co.uk.nootnoot.network_blocking;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import co.uk.nootnoot.util.Logger;

public class Client implements AutoCloseable {
    private final ObjectOutputStream out;
    private final BlockingMessageReceiver receiver;
    private final Queue<Message> messages = new ArrayBlockingQueue<>(1000);
    private final ClientLogger logger;

    public Client(Integer id, String serverHost, Integer serverPort) throws IOException {
        this.logger = new ClientLogger(id);

        var socket = new Socket(serverHost, serverPort);
        out = new ObjectOutputStream(socket.getOutputStream());
        var in = new ObjectInputStream(socket.getInputStream());
        receiver = new BlockingMessageReceiver(in, messages, logger);
        new Thread(receiver).start();
    }

    public void close() {
        logger.log("ChatClient stopping");
        receiver.stop();
        logger.log("ChatClient stopped");
    }

    public boolean isRunning() {
        if (receiver != null) {
            return receiver.isRunning();
        }
        return false;
    }

    public void send(Message message) throws IOException {
        logger.log("ChatClient sending message: " + message);
        out.writeObject(message);
    }

    public Message receive() {
        while (true) {
            if (messages.peek() != null) {
                return messages.poll();
            }
        }
    }

    public static class ClientLogger implements Logger {
        private final Integer id;

        public ClientLogger(Integer id) {
            this.id = id;
        }

        public void log(String message) {
            System.out.println("Client " + id + ": " + message);
        }
    }
}
