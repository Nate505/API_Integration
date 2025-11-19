package server;

import model.SpotifyAPIClient;
import config.Config;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Multi-threaded server that handles multiple client connections
 */
public class MusicRecommendationServer {
    private final int port;
    private final SpotifyAPIClient apiClient;
    private final ExecutorService threadPool;
    private volatile boolean running;

    public MusicRecommendationServer(int port) {
        this.port = port;
        this.apiClient = new SpotifyAPIClient();
        this.threadPool = Executors.newFixedThreadPool(10);
        this.running = false;
    }

    public void start() throws IOException {
        System.out.println("Authenticating with Spotify API...");
        apiClient.authenticate();

        running = true;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Music Recommendation Server started on port " + port);
            System.out.println("Waiting for client connections...");

            while (running) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, apiClient);
                threadPool.execute(handler);
            }
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        running = false;
        threadPool.shutdown();
        try {
            apiClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        MusicRecommendationServer server = new MusicRecommendationServer(Config.SERVER_PORT);
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}