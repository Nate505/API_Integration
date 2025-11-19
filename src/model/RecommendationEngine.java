package model;

import model.SpotifyAPIClient;
import model.Track;

import java.io.IOException;
import java.util.List;

/**
 * Main recommendation engine that uses different strategies
 * Demonstrates Composition and Strategy Pattern
 */
public class RecommendationEngine {
    private RecommendationStrategy strategy;
    private final SpotifyAPIClient apiClient;

    public RecommendationEngine(RecommendationStrategy strategy, SpotifyAPIClient apiClient) {
        this.strategy = strategy;
        this.apiClient = apiClient;
    }

    /**
     * Change the recommendation strategy at runtime
     */
    public void setStrategy(RecommendationStrategy strategy) {
        this.strategy = strategy;
        System.out.println("Switched to: " + strategy.getStrategyName());
    }

    /**
     * Get recommendations using the current strategy
     */
    public List<Track> getRecommendations(Track seedTrack, int count) throws IOException {
        System.out.println("Using strategy: " + strategy.getStrategyName());
        return strategy.recommend(seedTrack, apiClient, count);
    }

    public RecommendationStrategy getCurrentStrategy() {
        return strategy;
    }
}