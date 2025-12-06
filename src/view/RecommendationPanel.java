package view;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import model.*;
import service.APIService;
import service.SpotifyAuthService;

import java.io.IOException;
import java.util.List;

public class RecommendationPanel {

    private final APIService apiService;
    private final SpotifyAuthService auth = new SpotifyAuthService();
    private final SpotifyAPIClient apiClient = new SpotifyAPIClient();
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

    private final ComboBox<String> deviceDropdown;
    private final Button playButton;
    private final Button pauseButton;
    private final Label functionLabel;
    private final Button loginButton;

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
        switchStrategyButton = new Button("Switch to Artist Top Track");

        //Device Dropdown
        deviceDropdown = new ComboBox<>();
        playButton = new Button("Play");
        pauseButton = new  Button("Pause");

        deviceDropdown.setVisible(false);
        playButton.setVisible(false);
        pauseButton.setVisible(false);

        loginButton = new Button("Login with Spotify");
        functionLabel = new Label("Login with Spotify to Play Song directly to your device, ONLY if you have Spotify Premium");

        topBox.getChildren().addAll(searchBar, strategyLabel, recommendButton, switchStrategyButton, deviceDropdown, playButton, pauseButton, loginButton, functionLabel);

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

        playButton.setOnAction(e -> playSelectedTrack(deviceDropdown));
        pauseButton.setOnAction(e -> pauseTrack());

        loginButton.setOnAction(e -> {
            try {
                beginLogin();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

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
            RecommendationStrategy artistStrategy = new ArtistSimilarityStrategy();
            recommendationEngine.setStrategy(artistStrategy);
            strategyLabel.setText("Current Strategy: Artist's Top Track");
            switchStrategyButton.setText("Switch to Popularity");
            usingPopularity = false;
        } else {
            RecommendationStrategy popStrategy = new PopularityBasedStrategy();
            recommendationEngine.setStrategy(popStrategy);
            strategyLabel.setText("Current Strategy: Popularity");
            switchStrategyButton.setText("Switch to Artist's Top Track");
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
            System.out.println(recs.get(0).getId());
        });

        task.setOnFailed(ev -> {
            Throwable t = task.getException();
            setStatus("Recommendation failed: " + (t != null ? t.getMessage() : "unknown"));
        });

        new Thread(task).start();
    }

    private void playSelectedTrack(ComboBox<String> dropdown) {
        setStatus("Playing selected track...");

        Track selectedTrack = recommendationsList.getSelectionModel().getSelectedItem();

        if (selectedTrack == null) {
            selectedTrack = searchResultsList.getSelectionModel().getSelectedItem();
        }

        String deviceSelected = dropdown.getSelectionModel().getSelectedItem();

        if (selectedTrack == null) {
            setStatus("Please select a track from the search results.");
            return;
        }

        if(deviceSelected == null) {
            setStatus("Please select a device from the dropdown");
            return;
        }

        String deviceID = apiClient.getDeviceID(deviceSelected);

        try{
            apiClient.playSong(deviceID, selectedTrack.getId(), auth.getAccessToken());
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void pauseTrack() {
        setStatus("Pausing selected track...");

        try{
            apiClient.pauseSong(auth.getAccessToken());
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void beginLogin() {
        new Thread(() -> {
            try {
                auth.login();

                // Wait until token is available
                while (!auth.isLoggedIn()) {
                    Thread.sleep(200);
                }

                Platform.runLater(() -> {
                    try {
                        showPostLoginUI();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                loginButton.setDisable(false);
                loginButton.setVisible(false);

                functionLabel.setVisible(false);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showPostLoginUI() throws IOException {
        try {
            String[] devices = apiClient.getDeviceName(auth.getAccessToken());
            deviceDropdown.getItems().setAll(devices);

            deviceDropdown.setVisible(true);
            playButton.setVisible(true);
            pauseButton.setVisible(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
