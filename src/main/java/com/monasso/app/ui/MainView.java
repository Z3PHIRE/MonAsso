package com.monasso.app.ui;

import com.monasso.app.config.AppContext;
import com.monasso.app.service.BrandingService;
import com.monasso.app.ui.component.PictogramFactory;
import com.monasso.app.ui.navigation.NavigationManager;
import com.monasso.app.ui.navigation.ScreenId;
import com.monasso.app.ui.screen.ContributionsScreen;
import com.monasso.app.ui.screen.DashboardScreen;
import com.monasso.app.ui.screen.EventsScreen;
import com.monasso.app.ui.screen.ExportsScreen;
import com.monasso.app.ui.screen.MembersScreen;
import com.monasso.app.ui.screen.PersonalizationScreen;
import com.monasso.app.ui.screen.SettingsScreen;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.Map;

public class MainView {

    private static final Map<ScreenId, String> SCREEN_PICTOGRAMS = Map.of(
            ScreenId.DASHBOARD, "DB",
            ScreenId.MEMBERS, "MB",
            ScreenId.EVENTS, "EV",
            ScreenId.CONTRIBUTIONS, "CT",
            ScreenId.EXPORTS, "EX",
            ScreenId.SETTINGS, "PR",
            ScreenId.PERSONALIZATION, "TH"
    );

    private final AppContext appContext;
    private final BorderPane root = new BorderPane();
    private final Label appTitleLabel = new Label();
    private final ImageView logoView = new ImageView();
    private final Map<ScreenId, Button> navButtons = new EnumMap<>(ScreenId.class);
    private final NavigationManager navigationManager;

    public MainView(AppContext appContext) {
        this.appContext = appContext;

        root.getStyleClass().add("app-root");

        HBox topBar = createTopBar();
        VBox sidebar = createSidebar();

        StackPane contentPane = new StackPane();
        contentPane.getStyleClass().add("content-host");
        contentPane.setPadding(new Insets(0));
        navigationManager = new NavigationManager(contentPane);
        configureNavigation();
        configureNavigationState();

        root.setTop(topBar);
        root.setLeft(sidebar);
        root.setCenter(contentPane);

        refreshBranding(appContext.brandingService());
        appContext.brandingService().brandingProperty().addListener((obs, oldConfig, newConfig) -> refreshBranding(appContext.brandingService()));

        navigationManager.navigate(ScreenId.DASHBOARD);
    }

    public Parent getRoot() {
        return root;
    }

    public NavigationManager navigationManager() {
        return navigationManager;
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(12);
        topBar.getStyleClass().add("top-bar");

        appTitleLabel.getStyleClass().add("app-title");

        Label offlineTag = new Label("Mode local hors ligne");
        offlineTag.getStyleClass().add("status-pill");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topBar.getChildren().addAll(appTitleLabel, spacer, offlineTag);
        return topBar;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.getStyleClass().add("sidebar");

        VBox logoBlock = new VBox(10);
        logoBlock.getStyleClass().add("sidebar-logo");
        logoBlock.setAlignment(Pos.CENTER_LEFT);

        logoView.setFitWidth(170);
        logoView.setFitHeight(80);
        logoView.setPreserveRatio(true);

        Label navLabel = new Label("Navigation");
        navLabel.getStyleClass().add("sidebar-section-title");

        logoBlock.getChildren().addAll(logoView, navLabel);
        sidebar.getChildren().add(logoBlock);

        for (ScreenId screenId : ScreenId.values()) {
            Button button = createNavButton(screenId);
            navButtons.put(screenId, button);
            sidebar.getChildren().add(button);
        }
        return sidebar;
    }

    private Button createNavButton(ScreenId screenId) {
        Button button = new Button();
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setGraphic(createNavButtonGraphic(screenId));
        button.setOnAction(event -> navigationManager.navigate(screenId));
        return button;
    }

    private HBox createNavButtonGraphic(ScreenId screenId) {
        HBox content = new HBox(10);
        content.setAlignment(Pos.CENTER_LEFT);

        String pictogram = SCREEN_PICTOGRAMS.getOrDefault(screenId, "NA");
        var icon = PictogramFactory.createBadge(pictogram, "nav-icon-badge", "nav-icon-text");

        Label label = new Label(screenId.label());
        label.getStyleClass().add("nav-label");

        content.getChildren().addAll(icon, label);
        return content;
    }

    private void configureNavigation() {
        navigationManager.register(ScreenId.DASHBOARD, () -> new DashboardScreen(appContext.dashboardService()));
        navigationManager.register(ScreenId.MEMBERS, () -> new MembersScreen(appContext.memberService()));
        navigationManager.register(ScreenId.EVENTS, () -> new EventsScreen(appContext.eventService(), appContext.memberService()));
        navigationManager.register(ScreenId.CONTRIBUTIONS, () -> new ContributionsScreen(appContext.contributionService(), appContext.memberService()));
        navigationManager.register(ScreenId.EXPORTS, () -> new ExportsScreen(appContext.exportService(), appContext.settingsService()));
        navigationManager.register(ScreenId.SETTINGS, () -> new SettingsScreen(appContext.settingsService(), appContext.dataSafetyService(), this::reloadDataViews));
        navigationManager.register(ScreenId.PERSONALIZATION, () -> new PersonalizationScreen(appContext.brandingService(), appContext.themeManager()));
    }

    private void configureNavigationState() {
        navigationManager.currentScreenProperty().addListener((obs, oldScreen, newScreen) -> {
            navButtons.forEach((id, button) -> {
                button.getStyleClass().remove("nav-button-active");
                if (id == newScreen) {
                    button.getStyleClass().add("nav-button-active");
                }
            });
        });
    }

    private void refreshBranding(BrandingService brandingService) {
        appTitleLabel.setText(brandingService.getCurrentBranding().appName());
        logoView.setImage(brandingService.loadLogoImage(170, 80));
    }

    private void reloadDataViews() {
        ScreenId current = navigationManager.currentScreen();
        if (current == null) {
            current = ScreenId.DASHBOARD;
        }
        navigationManager.clearCache();
        navigationManager.navigate(current);
    }
}
