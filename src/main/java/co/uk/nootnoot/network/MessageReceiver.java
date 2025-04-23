package co.uk.nootnoot.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Queue;
import co.uk.nootnoot.network.Client.ChatLogger;

public class MessageReceiver implements Runnable {
    private final BufferedReader in;
    private final Queue<String> messages;
    private final ChatLogger logger;
    private volatile boolean running = true;

    public MessageReceiver(BufferedReader in, Queue<String> messages, ChatLogger logger) {
        this.in = in;
        this.messages = messages;
        this.logger = logger;
    }

    @Override
    public void run() {
        try {
            while (running) {
                if (in.ready()) {
                    var message = in.readLine();
                    logger.log("MessageReceiver got message: " + message);
                    if (!messages.offer(message)) {
                        logger.log("MessageReceiver can't add message to queue: " + message);
                    }
                }
            }
        } catch (IOException e) {
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
