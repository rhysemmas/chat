package co.uk.nootnoot.network_blocking;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketException;
import java.util.Queue;
import co.uk.nootnoot.util.Logger;

public class BlockingMessageReceiver implements Runnable {
    private final ObjectInputStream in;
    private final Queue<Message> messages;
    private final Logger logger;
    private volatile boolean running = true;

    public BlockingMessageReceiver(ObjectInputStream in, Queue<Message> messages, Logger logger) {
        this.in = in;
        this.messages = messages;
        this.logger = logger;
    }

    @Override
    public void run() {
        try {
            while (running) {
                Message message = (Message) in.readObject();
                logger.log("MessageReceiver got message: " + message);
                if (!messages.offer(message)) {
                    logger.log("MessageReceiver can't add message to queue: " + message);
                }
            }
        } catch (SocketException | EOFException e) {
            // Socket/stream closed
        } catch (Exception e) {
            logger.log("MessageReceiver got error reading message: " + e.getMessage());
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        try {
            logger.log("MessageReceiver stopping");
            running = false;
            in.close();
        } catch (IOException e) {
            logger.log("MessageReceiver got exception while closing: " + e);
        }
        logger.log("MessageReceiver stopped");
    }
}
