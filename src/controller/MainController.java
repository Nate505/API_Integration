package controller;

import model.*;
import config.Config;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Main entry point for running the music recommendation demo.
 * Demonstrates Strategy Pattern + Spotify API + Models.
 */
public class MainController {

    public static void main(String[] args) {

        // Create API Client
        SpotifyAPIClient apiClient = new SpotifyAPIClient();

        try {
            // Authenticate once at startup
            apiClient.authenticate();

            // Default strategy
            RecommendationStrategy strategy = new ArtistSimilarityStrategy();

            RecommendationEngine engine = new RecommendationEngine(strategy, apiClient);

            Scanner scanner = new Scanner(System.in);

            System.out.println("🎵 Spotify Music Recommender");
            System.out.println("------------------------------");

            while (true) {
                System.out.print("\nEnter a song name (or 'exit'): ");
                String input = scanner.nextLine();

                if (input.equalsIgnoreCase("exit")) {
                    break;
                }

                // Search for seed track
                List<Track> results = apiClient.searchTracks(input, 5);

                if (results.isEmpty()) {
                    System.out.println("No results found!");
                    continue;
                }

                System.out.println("\nFound Tracks:");
                for (int i = 0; i < results.size(); i++) {
                    Track t = results.get(i);
                    System.out.printf("%d) %s — %s\n", i + 1, t.getName(), t.getArtists());
                }

                System.out.print("\nChoose a track (1-" + results.size() + "): ");
                int choice = Integer.parseInt(scanner.nextLine()) - 1;

                if (choice < 0 || choice >= results.size()) {
                    System.out.println("Invalid choice.");
                    continue;
                }

                Track seedTrack = results.get(choice);

                System.out.println("\nSelected: " + seedTrack.getName());

                // Ask for strategy
                System.out.println("\nChoose Recommendation Strategy:");
                System.out.println("1) Artist's Top Tracks");
                System.out.println("2) Popularity-Based");
                System.out.print("Enter choice: ");

                String strat = scanner.nextLine();

                if (strat.equals("1")) {
                    engine.setStrategy(new ArtistSimilarityStrategy());
                } else if (strat.equals("2")) {
                    engine.setStrategy(new PopularityBasedStrategy());
                } else {
                    System.out.println("Invalid. Using default Artist's Top Tracks.");
                    engine.setStrategy(new ArtistSimilarityStrategy());
                }

                // Get recommendations
                List<Track> recs = engine.getRecommendations(seedTrack, Config.DEFAULT_RECOMMENDATIONS);

                System.out.println("\n🎧 Recommendations:");
                for (Track t : recs) {
                    System.out.printf("- %s — %s (Popularity: %d)\n",
                            t.getName(), t.getArtists(), t.getPopularity());
                }
            }

            System.out.println("Shutting down...");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                apiClient.close();
            } catch (IOException ignored) {}
        }
    }
}
