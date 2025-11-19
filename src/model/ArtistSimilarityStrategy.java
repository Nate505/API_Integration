package model;

import config.Config;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ArtistSimilarityStrategy implements RecommendationStrategy{
    @Override
    public List<Track> recommend(Track seedTrack, SpotifyAPIClient apiClient, int count)
            throws IOException {

        System.out.println("Finding similar tracks to: " + seedTrack.getName());

        //Obtaining Artist's ID
        String artistQuery = seedTrack.getArtists().get(0);
        String artistID = apiClient.getArtistID(artistQuery);

        //Search for tracks from Artist's top tracks
        List<Track> candidateTracks = apiClient.getArtistsTopTracks(artistID);

        //Sort byt popularity and exclude the seed track
        return candidateTracks.stream()
                .filter(track -> !track.getId().equals(seedTrack.getId()))
                .sorted(Comparator.comparingInt(Track::getPopularity).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    @Override
    public String getStrategyName() {
        return "Artist's Top Tracks";
    }
}
