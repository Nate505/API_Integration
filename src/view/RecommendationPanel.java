package view;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import model.RecommendationEngine;
import model.Track;
import model.RecommendationStrategy;
import model.PopularityBasedStrategy;
import model.AudioSimilarityStrategy;
import service.APIService;

import java.io.IOException;
import java.util.List;

public class RecommendationPanel {

    private final APIService apiService;
    private final RecommendationEngine recommendationEngine;
    private final BorderPane root;

    private final Label strategyLabel;
    private final Button recommendButton;
    private final Button switchStrategyButton;

    private final TextField searchField;
    private final Button searchButton;
    private final ListView<Track> searchResultsList;
    private final ListView<Track> recommendationsList;
    private final Label statusLabel;

    private boolean usingPopularity = true;

    public RecommendationPanel(APIService apiService) {
        this.apiService = apiService;
        this.recommendationEngine = apiService.getRecommendationEngine();
        this.root = new BorderPane();

        // --- Top: search bar + strategy controls ---
        VBox topBox = new VBox(8);
        topBox.setPadding(new Insets(10));

        // Search bar
        HBox searchBar = new HBox(8);
        this.searchField = new TextField();
        searchField.setPromptText("Search tracks or artists...");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        this.searchButton = new Button("Search");
        searchBar.getChildren().addAll(searchField, searchButton);

        // Strategy + recommend button
        strategyLabel = new Label("Current Strategy: Popularity");
        recommendButton = new Button("Get Recommendations");
        switchStrategyButton = new Button("Switch to Audio Similarity");

        topBox.getChildren().addAll(searchBar, strategyLabel, recommendButton, switchStrategyButton);

        // --- Center: split view ---
        HBox center = new HBox(10);
        center.setPadding(new Insets(10));

        searchResultsList = new ListView<>();
        searchResultsList.setPlaceholder(new Label("No results"));
        searchResultsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Track item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getName() + " — " + String.join(", ", item.getArtists()));
            }
        });

        recommendationsList = new ListView<>();
        recommendationsList.setPlaceholder(new Label("No recommendations yet"));
        recommendationsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Track item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getName() + " — " + String.join(", ", item.getArtists()));
            }
        });

        center.getChildren().addAll(searchResultsList, recommendationsList);
        HBox.setHgrow(searchResultsList, Priority.ALWAYS);
        HBox.setHgrow(recommendationsList, Priority.ALWAYS);

        // --- Bottom: status ---
        statusLabel = new Label("Ready");
        VBox bottom = new VBox(statusLabel);
        bottom.setPadding(new Insets(8));

        // Layout
        root.setTop(topBox);
        root.setCenter(center);
        root.setBottom(bottom);

        // Wire actions
        searchButton.setOnAction(e -> doSearch());
        searchField.setOnAction(e -> doSearch());
        recommendButton.setOnAction(e -> fetchRecommendations());
        switchStrategyButton.setOnAction(e -> switchStrategy());

        // Enable recommend button only when a track is selected
        recommendButton.setDisable(true);
        searchResultsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            recommendButton.setDisable(newVal == null);
        });

        // Double-click search result to trigger recommendations
        searchResultsList.setOnMouseClicked(click -> {
            if (click.getButton() == MouseButton.PRIMARY && click.getClickCount() == 2) {
                Track selected = searchResultsList.getSelectionModel().getSelectedItem();
                if (selected != null) buildRecommendations(selected);
            }
        });
    }

    public BorderPane getRoot() {
        return root;
    }

    private void setStatus(String text) {
        Platform.runLater(() -> statusLabel.setText(text));
    }

    private void switchStrategy() {
        if (usingPopularity) {
            RecommendationStrategy audioStrategy = new AudioSimilarityStrategy();
            recommendationEngine.setStrategy(audioStrategy);
            strategyLabel.setText("Current Strategy: Audio Similarity");
            switchStrategyButton.setText("Switch to Popularity");
            usingPopularity = false;
        } else {
            RecommendationStrategy popStrategy = new PopularityBasedStrategy();
            recommendationEngine.setStrategy(popStrategy);
            strategyLabel.setText("Current Strategy: Popularity");
            switchStrategyButton.setText("Switch to Audio Similarity");
            usingPopularity = true;
        }
    }

    private void fetchRecommendations() {
        Track seedTrack = searchResultsList.getSelectionModel().getSelectedItem();
        recommendationsList.getItems().clear();

        if (seedTrack == null) {
            setStatus("Please select a track from the search results.");
            return;
        }

        buildRecommendations(seedTrack);
    }

    private void doSearch() {
        String q = searchField.getText();
        if (q == null || q.trim().isEmpty()) return;

        setStatus("Searching...");
        Task<List<Track>> task = new Task<>() {
            @Override
            protected List<Track> call() throws Exception {
                return apiService.searchTracks(q, 30);
            }
        };

        task.setOnSucceeded(ev -> {
            List<Track> results = task.getValue();
            searchResultsList.getItems().setAll(results);
            setStatus("Found " + results.size() + " tracks");
        });

        task.setOnFailed(ev -> {
            Throwable t = task.getException();
            setStatus("Search failed: " + (t != null ? t.getMessage() : "unknown"));
        });

        new Thread(task).start();
    }

    private void buildRecommendations(Track seed) {
        setStatus("Building recommendations for: " + seed.getName());
        recommendationsList.getItems().clear();

        Task<List<Track>> task = new Task<>() {
            @Override
            protected List<Track> call() throws Exception {
                return apiService.recommendForSeed(seed, 15);
            }
        };

        task.setOnSucceeded(ev -> {
            List<Track> recs = task.getValue();
            recommendationsList.getItems().setAll(recs);
            setStatus("Got " + recs.size() + " recommendations for: " + seed.getName());
        });

        task.setOnFailed(ev -> {
            Throwable t = task.getException();
            setStatus("Recommendation failed: " + (t != null ? t.getMessage() : "unknown"));
        });

        new Thread(task).start();
    }
}
