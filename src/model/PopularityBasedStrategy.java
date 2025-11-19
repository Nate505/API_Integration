package model;

import model.SpotifyAPIClient;
import config.Config;
import model.Track;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Recommends popular tracks from the same artist or genre
 * Simpler strategy that doesn't require audio features
 */
public class PopularityBasedStrategy implements RecommendationStrategy {

    @Override
    public List<Track> recommend(Track seedTrack, SpotifyAPIClient apiClient, int count)
            throws IOException {

        System.out.println("Finding popular tracks similar to: " + seedTrack.getName());

        // Search for tracks from the same artist
        String artistQuery = seedTrack.getArtists().get(0);
        List<Track> candidateTracks = apiClient.searchTracks(artistQuery, Config.TRACK_POOL_SIZE);

        // Sort by popularity and exclude the seed track
        return candidateTracks.stream()
                .filter(track -> !track.getId().equals(seedTrack.getId()))
                .sorted(Comparator.comparingInt(Track::getPopularity).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    @Override
    public String getStrategyName() {
        return "Popularity-Based";
    }
}