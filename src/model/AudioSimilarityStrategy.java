package model;

import model.SpotifyAPIClient;
import config.Config;
import model.AudioFeatures;
import model.Track;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Recommends tracks based on audio feature similarity
 * Demonstrates Polymorphism and Algorithm Design
 */
public class AudioSimilarityStrategy implements RecommendationStrategy {

    @Override
    public List<Track> recommend(Track seedTrack, SpotifyAPIClient apiClient, int count)
            throws IOException {

        System.out.println("Finding similar tracks to: " + seedTrack.getName());

        // Step 1: Get audio features for the seed track
        AudioFeatures seedFeatures = apiClient.getAudioFeatures(seedTrack.getId());
        seedTrack.setAudioFeatures(seedFeatures);

        // Step 2: Search for candidate tracks from the same artist
        String artistQuery = seedTrack.getArtists().get(0);
        List<Track> candidateTracks = apiClient.searchTracks(artistQuery, Config.TRACK_POOL_SIZE);

        // Step 3: Get audio features for all candidate tracks
        List<String> candidateIds = candidateTracks.stream()
                .map(Track::getId)
                .filter(id -> !id.equals(seedTrack.getId())) // Exclude seed track
                .collect(Collectors.toList());

        List<AudioFeatures> candidateFeatures = apiClient.getBatchAudioFeatures(candidateIds);

        // Step 4: Map features back to tracks
        Map<String, AudioFeatures> featuresMap = new HashMap<>();
        for (AudioFeatures features : candidateFeatures) {
            featuresMap.put(features.getTrackId(), features);
        }

        for (Track track : candidateTracks) {
            if (featuresMap.containsKey(track.getId())) {
                track.setAudioFeatures(featuresMap.get(track.getId()));
            }
        }

        // Step 5: Calculate similarity scores
        List<TrackSimilarity> similarities = new ArrayList<>();
        for (Track candidate : candidateTracks) {
            if (candidate.getId().equals(seedTrack.getId())) continue;
            if (candidate.getAudioFeatures() == null) continue;

            double similarity = seedFeatures.calculateSimilarity(
                    candidate.getAudioFeatures()
            );

            similarities.add(new TrackSimilarity(candidate, similarity));
        }

        // Step 6: Sort by similarity (lower distance = more similar)
        similarities.sort(Comparator.comparingDouble(TrackSimilarity::getSimilarity));

        // Step 7: Return top N recommendations
        return similarities.stream()
                .limit(count)
                .map(TrackSimilarity::getTrack)
                .collect(Collectors.toList());
    }

    @Override
    public String getStrategyName() {
        return "Audio Similarity-Based";
    }

    /**
     * Helper class to pair tracks with their similarity scores
     */
    private static class TrackSimilarity {
        private final Track track;
        private final double similarity;

        public TrackSimilarity(Track track, double similarity) {
            this.track = track;
            this.similarity = similarity;
        }

        public Track getTrack() {
            return track;
        }

        public double getSimilarity() {
            return similarity;
        }
    }
}