package model;

/**
 * Represents audio analysis features from Spotify API
 * These are used for computing similarity between tracks
 */
public class AudioFeatures {
    private String trackId;
    private double danceability;    // 0.0 to 1.0 - How suitable for dancing
    private double energy;          // 0.0 to 1.0 - Intensity and activity
    private double valence;         // 0.0 to 1.0 - Musical positivity/happiness
    private double tempo;           // BPM (typically 50-200)
    private double acousticness;    // 0.0 to 1.0 - Acoustic vs electronic
    private double instrumentalness; // 0.0 to 1.0 - Contains vocals or not
    private double liveness;        // 0.0 to 1.0 - Performed live
    private double speechiness;     // 0.0 to 1.0 - Contains spoken words
    private int key;                // 0-11 (C, C#, D, etc.)
    private int mode;               // 0 = minor, 1 = major

    public AudioFeatures(String trackId) {
        this.trackId = trackId;
    }

    // Getters and Setters
    public String getTrackId() {
        return trackId;
    }

    public double getDanceability() {
        return danceability;
    }

    public void setDanceability(double danceability) {
        this.danceability = danceability;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        this.energy = energy;
    }

    public double getValence() {
        return valence;
    }

    public void setValence(double valence) {
        this.valence = valence;
    }

    public double getTempo() {
        return tempo;
    }

    public void setTempo(double tempo) {
        this.tempo = tempo;
    }

    public double getAcousticness() {
        return acousticness;
    }

    public void setAcousticness(double acousticness) {
        this.acousticness = acousticness;
    }

    public double getInstrumentalness() {
        return instrumentalness;
    }

    public void setInstrumentalness(double instrumentalness) {
        this.instrumentalness = instrumentalness;
    }

    public double getLiveness() {
        return liveness;
    }

    public void setLiveness(double liveness) {
        this.liveness = liveness;
    }

    public double getSpeechiness() {
        return speechiness;
    }

    public void setSpeechiness(double speechiness) {
        this.speechiness = speechiness;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    /**
     * Calculate similarity to another track's audio features
     * Uses Euclidean distance (lower = more similar)
     */
    public double calculateSimilarity(AudioFeatures other) {
        if (other == null) return Double.MAX_VALUE;

        // Normalize tempo to 0-1 scale (assume tempo range 50-200 BPM)
        double normalizedTempo1 = (this.tempo - 50) / 150.0;
        double normalizedTempo2 = (other.tempo - 50) / 150.0;

        // Calculate Euclidean distance across all features
        double sum = 0;
        sum += Math.pow(this.danceability - other.danceability, 2);
        sum += Math.pow(this.energy - other.energy, 2);
        sum += Math.pow(this.valence - other.valence, 2);
        sum += Math.pow(normalizedTempo1 - normalizedTempo2, 2);
        sum += Math.pow(this.acousticness - other.acousticness, 2);
        sum += Math.pow(this.instrumentalness - other.instrumentalness, 2);

        return Math.sqrt(sum);
    }

    @Override
    public String toString() {
        return String.format(
                "AudioFeatures[dance=%.2f, energy=%.2f, valence=%.2f, tempo=%.1f BPM]",
                danceability, energy, valence, tempo
        );
    }
}