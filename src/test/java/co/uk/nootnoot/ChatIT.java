package co.uk.nootnoot;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ChatIT {
    private final static Integer serverPort = 8080;
    private ChatServer server;

    @BeforeEach
    public void startServer() throws Exception {
        server = new ChatServer(serverPort);
        new Thread(server).start();
        Thread.sleep(1000);
    }

    @AfterEach
    public void stopServer() throws Exception {
        server.stop();
    }

    @Test
    public void singleClientTest() throws Exception {
        var client = new ChatClient(1,"localhost", serverPort);
        new Thread(client).start();
        Assertions.assertTrue(await(client::isRunning, 1000L));

        // TODO: should be possible to assert that server received correct message
        client.send("Hello");
        Thread.sleep(1000);

        client.send("Hello");
        Thread.sleep(1000);

        client.stop();
    }

    @Test
    public void multiClientTest() throws Exception {
        var client1 = new ChatClient(1,"localhost", serverPort);
        new Thread(client1).start();
        var client2 = new ChatClient(2,"localhost", serverPort);
        new Thread(client2).start();
        var client3 = new ChatClient(3,"localhost", serverPort);
        new Thread(client3).start();
        Assertions.assertTrue(await(client1::isRunning, 1000L));
        Assertions.assertTrue(await(client2::isRunning, 1000L));
        Assertions.assertTrue(await(client3::isRunning, 1000L));

        client1.send("Hello");
        Thread.sleep(1000);

        Assertions.assertTrue(client2.receive().contains("Hello"));
        Assertions.assertTrue(client3.receive().contains("Hello"));

        client1.stop();
        client2.stop();
        client3.stop();
    }

    private boolean await(Callable<Boolean> callable, Long timeoutMillis) throws Exception {
        var timeNow = Instant.now();
        while (!callable.call()) {
            var elapsed = ChronoUnit.MILLIS.between(timeNow, Instant.now());
            if (elapsed > timeoutMillis) {
                break;
            }
        }
        return callable.call();
    }
}
