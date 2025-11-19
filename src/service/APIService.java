package service;

import model.Track;
import model.SpotifyAPIClient;
import model.RecommendationEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class APIService {
    private final SpotifyAPIClient spotifyClient;
    private final RecommendationEngine recommendationEngine;

    public APIService(SpotifyAPIClient spotifyClient, RecommendationEngine recommendationEngine) {
        this.spotifyClient = spotifyClient;
        this.recommendationEngine = recommendationEngine;
    }

    public RecommendationEngine getRecommendationEngine() {
        return recommendationEngine;
    }

    public List<Track> searchTracks(String query, int limit) throws IOException {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return spotifyClient.searchTracks(query, limit);
    }

    public List<Track> recommendForSeed(Track seed, int resultLimit) throws IOException {
        if (seed == null) return new ArrayList<>();
        if (recommendationEngine == null) {
            throw new IllegalStateException("RecommendationEngine is not initialized");
        }
        if (resultLimit <= 0) {
            return new ArrayList<>();
        }

        // Delegate to the RecommendationEngine you provided
        return recommendationEngine.getRecommendations(seed, resultLimit);
    }
}
