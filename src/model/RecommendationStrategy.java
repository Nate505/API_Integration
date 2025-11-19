package model;

import model.Track;
import model.SpotifyAPIClient;
import java.util.List;
import java.io.IOException;

/**
 * Strategy interface for different recommendation algorithms
 * Demonstrates Abstraction and Strategy Pattern
 */
public interface RecommendationStrategy {
    /**
     * Generate recommendations based on a seed track
     * @param seedTrack The track to base recommendations on
     * @param apiClient API client for fetching additional data
     * @param count Number of recommendations to generate
     * @return List of recommended tracks
     */
    List<Track> recommend(Track seedTrack, SpotifyAPIClient apiClient, int count) throws IOException;

    /**
     * Get the name of this recommendation strategy
     */
    String getStrategyName();
}