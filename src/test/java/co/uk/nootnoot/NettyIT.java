package co.uk.nootnoot;

import co.uk.nootnoot.network.netty.EchoClient;
import co.uk.nootnoot.network.netty.EchoServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NettyIT {
    private final static int serverPort = 8080;
    private EchoServer server;

    @BeforeEach
    public void startServer() throws Exception {
        server = new EchoServer(serverPort);
        new Thread(server).start();
        Thread.sleep(1000);
    }

    @AfterEach
    public void stopServer() throws Exception {
        server.close(); // This closes the future, server will take around 3 seconds to finish stopping gracefully
        Thread.sleep(3000);
    }

    @Test
    public void echoTest() throws Exception {
        try (var client1 = new EchoClient("localhost", serverPort);
             var client2 = new EchoClient("localhost", serverPort)) {
            client1.start();
            client2.start();
        }
    }
}
