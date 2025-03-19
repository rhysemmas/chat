package co.uk.nootnoot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer implements Runnable {
    private final ServerSocket serverSocket;
    private final Map<SocketAddress, ChatClientHandler> clients = new ConcurrentHashMap<>();
    private final ServerLogger logger;

    ChatServer(Integer port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.logger = new ServerLogger();
    }

    public void run() {
        try (serverSocket) {
            logger.log("Server started on port 8080");

            while (true) {
                var socket = serverSocket.accept();
                var clientHandler = new ChatClientHandler(socket, clients, logger);
                clients.put(socket.getRemoteSocketAddress(), clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (SocketException e) {
            // Socket closed
        } catch (IOException e) {
            logger.log("Server crashing with exception: " + e);
        }
    }

    public void stop() {
        logger.log("Server stopping");
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.log("Server stopping with exception: " + e);
        }
        clients.values().forEach(ChatClientHandler::stop);
        logger.log("Server stopped");
    }

    private static class ChatClientHandler implements Runnable {
        private final Socket socket;
        private final PrintWriter writer;
        private final BufferedReader reader;
        private final Map<SocketAddress, ChatClientHandler> clients;
        private final ServerLogger logger;
        private volatile boolean running = true;

        ChatClientHandler(Socket socket, Map<SocketAddress, ChatClientHandler> clients, ServerLogger logger) throws IOException {
            this.socket = socket;
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clients = clients;
            this.logger = logger;
        }

        @Override
        public void run() {
            while (running) {
                try {
                    while (reader.ready()) {
                        var request = reader.readLine();
                        logger.log("ChatClientHandler received: '" + request + "' from " + socket.getRemoteSocketAddress());
                        clients.entrySet().stream()
                                .filter(entry -> !entry.getKey().equals(socket.getRemoteSocketAddress()))
                                .forEach(entry -> entry.getValue().write(request));
                    }
                } catch (IOException e) {
                    logger.log("ChatClientHandler thread got exception while running: " + e);
                }
            }
        }

        public synchronized void write(String message) {
            logger.log("ChatClientHandler for socket " + socket.getRemoteSocketAddress() + " sending '" + message + "'");
            writer.println(message);
        }

        private void stop() {
            running = false;
            try {
                socket.close();
            } catch (IOException e) {
                logger.log("ChatClientHandler thread got exception while closing: " + e);
            }
        }
    }

    private static class ServerLogger {
        public void log(String message) {
            System.out.println("Server: " + message);
        }
    }
}
