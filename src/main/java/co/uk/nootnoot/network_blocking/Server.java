package co.uk.nootnoot.network_blocking;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import co.uk.nootnoot.util.Logger;

public class Server implements Runnable {
    private final ServerSocket serverSocket;
    private final Map<SocketAddress, ClientHandler> clients = new ConcurrentHashMap<>();
    private final ServerLogger logger;

    public Server(Integer port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.logger = new ServerLogger();
    }

    @Override
    public void run() {
        try (serverSocket) {
            logger.log("Server started on port 8080");

            while (true) {
                var socket = serverSocket.accept();
                var clientHandler = new ClientHandler(socket, clients, logger);
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
        clients.values().forEach(ClientHandler::stop);
        logger.log("Server stopped");
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final BlockingMessageReceiver receiver;
        private final ObjectOutputStream out;
        private final Map<SocketAddress, ClientHandler> clients;
        private final ServerLogger logger;
        private final Queue<Message> messages = new ArrayBlockingQueue<>(1000);
        private volatile boolean running = true;

        ClientHandler(Socket socket, Map<SocketAddress, ClientHandler> clients, ServerLogger logger) throws IOException {
            this.socket = socket;
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.clients = clients;
            this.logger = logger;

            var in = new ObjectInputStream(socket.getInputStream());
            this.receiver = new BlockingMessageReceiver(in, messages, logger);
            new Thread(this.receiver).start();
        }

        @Override
        public void run() {
            while (running) {
                if (messages.peek() != null) {
                    var message = messages.poll();
                    logger.log("ClientHandler received: '" + message + "' from " + socket.getRemoteSocketAddress());
                    clients.entrySet().stream()
                            .filter(entry -> !entry.getKey().equals(socket.getRemoteSocketAddress()))
                            .forEach(entry -> entry.getValue().write(message));
                }
            }
        }

        public synchronized void write(Message message) {
            try {
                logger.log("ClientHandler for socket " + socket.getRemoteSocketAddress() + " sending '" + message + "'");
                out.writeObject(message);
            } catch (IOException e) {
                logger.log("ClientHandler for socket " + socket.getRemoteSocketAddress() + " sending '" + message + "' failed: " + e);
            }
        }

        private void stop() {
            running = false;
            try {
                socket.close();
                receiver.stop();
            } catch (IOException e) {
                logger.log("ClientHandler thread got exception while closing: " + e);
            }
        }
    }

    private static class ServerLogger implements Logger {
        public void log(String message) {
            System.out.println("Server: " + message);
        }
    }
}
