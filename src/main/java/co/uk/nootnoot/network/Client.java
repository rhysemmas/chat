package co.uk.nootnoot.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class Client implements AutoCloseable {
    private final String serverHost;
    private final Integer serverPort;

    private PrintWriter out;
    private MessageReceiver receiver;
    private final Queue<String> messages = new ArrayBlockingQueue<>(1000);
    private final ChatLogger logger;

    public Client(Integer id, String serverHost, Integer serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.logger = new ChatLogger(id);

        try {
            var socket = new Socket(serverHost, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            receiver = new MessageReceiver(in, messages, logger);
            new Thread(receiver).start();
        } catch (IOException e) {
            logger.log("ChatClient got error: " + e.getMessage());
        }

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

    public void send(String message) {
        logger.log("ChatClient sending message: " + message);
        out.println(message);
    }

    // TODO: message structure
    public String receive() {
        if (messages.peek() != null) {
            return messages.poll();
        }
        return "";
    }

    // TODO: UI to display messages from other clients that are disseminated by server
    public List<String> receiveAll() {
        List<String> allMessages = new ArrayList<>();
        while (messages.peek() != null) {
            allMessages.add(messages.poll());
        }
        return allMessages;
    }

    public static class ChatLogger {
        private final Integer id;

        public ChatLogger(Integer id) {
            this.id = id;
        }

        public void log(String message) {
            System.out.println("Client " + id + ": " + message);
        }
    }
}
