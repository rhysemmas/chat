package co.uk.nootnoot;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;
import co.uk.nootnoot.network_blocking.Client;
import co.uk.nootnoot.network_blocking.Message;
import co.uk.nootnoot.network_blocking.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BlockingChatIT {
    private final static Integer serverPort = 8080;
    private Server server;

    @BeforeEach
    public void startServer() throws Exception {
        server = new Server(serverPort);
        new Thread(server).start();
        Thread.sleep(1000);
    }

    @AfterEach
    public void stopServer() {
        server.stop();
    }
    
    @Test
    public void multiClientTest() throws Exception {
        try (var client1 = new Client(1,"localhost", serverPort);
        var client2 = new Client(2,"localhost", serverPort);
        var client3 = new Client(3,"localhost", serverPort)) {
            Assertions.assertTrue(await(client1::isRunning, 1000L));
            Assertions.assertTrue(await(client2::isRunning, 1000L));
            Assertions.assertTrue(await(client3::isRunning, 1000L));

            // TODO: console, read data from console and direct it to the network layer
            client1.send(new Message(1, "Hello"));

            var client2Received = client2.receive();
            Assertions.assertEquals(1, client2Received.clientId());
            Assertions.assertTrue(client2Received.message().contains("Hello"));

            var client3Received = client3.receive();
            Assertions.assertEquals(1, client3Received.clientId());
            Assertions.assertTrue(client3Received.message().contains("Hello"));
        }
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
