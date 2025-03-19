package co.uk.nootnoot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            executor.submit(new EchoServer());
            Thread.sleep(1000);
            executor.submit(new Client());
        }
    }

    private static class Client implements Runnable {
        @Override
        public void run() {
            System.out.println("Client started");
            try (Socket socket = new Socket("localhost", 8080);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                var request = "hello server";
                System.out.println("Client sending message: " + request);
                out.println(request);
                System.out.println("Client sent message: " + request);
                System.out.println("Client received message: " + in.readLine());
            } catch (Exception e) {
                System.out.println(e);
                throw new RuntimeException(e);
            }
            System.out.println("Client stopped");
        }
    }

    private static class EchoServer implements Runnable {
        @Override
        public void run() {
            System.out.println("Server started");
            try (ServerSocket serverSocket = new ServerSocket(8080);
                 Socket socket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                var line = in.readLine();
                System.out.println("Server received message: " + line);
                var response = "hello client";
                System.out.println("Server sending message: " + response);
                out.println(response);
            } catch (Exception e) {
                System.out.println(e);
                throw new RuntimeException(e);
            }
            System.out.println("Server stopped");
        }
    }
}