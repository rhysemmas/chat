package co.uk.nootnoot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import co.uk.nootnoot.network_nonblocking.ByteClient;
import co.uk.nootnoot.network_nonblocking.SelectorServer;
import co.uk.nootnoot.util.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NonBlockingChatIT {
    private final static Integer serverPort = 8080;
    private SelectorServer server;

    @BeforeEach
    public void startServer() throws InterruptedException {
        try {
            this.server = new SelectorServer("localhost", serverPort);
            new Thread(server).start();
            Thread.sleep(1000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    public void stopServer() {
        try {
            this.server.stop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void singleClientTest() {
        try (var client = new ByteClient(1, "localhost", serverPort)) {
            client.sendMessage("Hello World");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void multiClientTest() {
        try {
            var client1 = new ByteClient(1, "localhost", serverPort);
            var client1Message = "Hello World1";

            var client2 = new ByteClient(2, "localhost", serverPort);
            var client2Message = "Hello World2";

            var client3 = new ByteClient(3, "localhost", serverPort);
            var client3Message = "Hello World3";

            new Thread(client1).start();
            new Thread(client2).start();
            new Thread(client3).start();
            Thread.sleep(1000);

            client1.sendMessage(client1Message);
            Thread.sleep(1000);
            client2.sendMessage(client2Message);
            Thread.sleep(1000);
            client3.sendMessage(client3Message);
            Thread.sleep(1000);

            Assertions.assertEquals(List.of(client2Message, client3Message), client1.getReceivedMessages());
            Assertions.assertEquals(List.of(client1Message, client3Message), client2.getReceivedMessages());
            Assertions.assertEquals(List.of(client1Message, client2Message), client3.getReceivedMessages());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
