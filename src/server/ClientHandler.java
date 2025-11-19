package server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

/**
 * Handles individual client connections in separate threads
 * Demonstrates Multithreading and Socket communication
 */
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final SpotifyAPIClient apiClient;
    private final RecommendationEngine recommendationEngine;
    private final Gson gson;
    private final int clientId;
    private static int nextClientId = 1;

    public ClientHandler(Socket socket, SpotifyAPIClient apiClient) {
        this.clientSocket = socket;
        this.apiClient = apiClient;
        this.recommendationEngine = new RecommendationEngine(
                new ArtistSimilarityStrategy(), apiClient);
        this.gson = new Gson();
        this.clientId = nextClientId++;
    }

    @Override
    public void run() {
        String clientAddress = clientSocket.getInetAddress().getHostAddress();
        System.out.println(String.format("[Client %d] Connected from %s",
                clientId, clientAddress));

        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String request;
            while ((request = in.readLine()) != null) {
                System.out.println(String.format("[Client %d] Request: %s",
                        clientId, request));

                String response = handleRequest(request);
                out.println(response);
            }
        } catch (IOException e) {
            System.err.println(String.format("[Client %d] Error: %s",
                    clientId, e.getMessage()));
        } finally {
            cleanup();
        }
    }

    private String handleRequest(String request) {
        try {
            JsonObject jsonRequest = gson.fromJson(request, JsonObject.class);
            String action = jsonRequest.get("action").getAsString();

            switch (action) {
                case "SEARCH":
                    return handleSearch(jsonRequest);
                case "RECOMMEND":
                    return handleRecommend(jsonRequest);
                default:
                    return createErrorResponse("Unknown action: " + action);
            }
        } catch (Exception e) {
            return createErrorResponse("Error: " + e.getMessage());
        }
    }

    private String handleSearch(JsonObject request) {
        try {
            String query = request.get("query").getAsString();
            int limit = request.has("limit") ? request.get("limit").getAsInt() : 20;

            List<Track> tracks = apiClient.searchTracks(query, limit);

            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.addProperty("action", "SEARCH");
            response.add("data", gson.toJsonTree(tracks));

            return gson.toJson(response);
        } catch (IOException e) {
            return createErrorResponse("Search failed: " + e.getMessage());
        }
    }

    private String handleRecommend(JsonObject request) {
        try {
            // Get the seed track details
            String trackId = request.get("trackId").getAsString();
            String trackName = request.get("trackName").getAsString();
            String trackArtist = request.get("trackArtist").getAsString();
            String trackAlbum = request.get("trackAlbum").getAsString();

            int count = request.has("count") ? request.get("count").getAsInt() : 10;

            // Create seed track object
            Track seedTrack = new Track(trackId, trackName,
                    List.of(trackArtist), trackAlbum);

            // Get recommendations using our custom algorithm
            List<Track> recommendations = recommendationEngine.getRecommendations(
                    seedTrack, count);

            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.addProperty("action", "RECOMMEND");
            response.add("data", gson.toJsonTree(recommendations));

            return gson.toJson(response);
        } catch (IOException e) {
            return createErrorResponse("Recommendation failed: " + e.getMessage());
        }
    }

    private String createErrorResponse(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "error");
        response.addProperty("message", message);
        return gson.toJson(response);
    }

    private void cleanup() {
        try {
            clientSocket.close();
            System.out.println(String.format("[Client %d] Disconnected", clientId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}