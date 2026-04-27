package com.monasso.app.ui;

import com.monasso.app.config.AppContext;
import com.monasso.app.model.GlobalSearchResult;
import com.monasso.app.service.BrandingService;
import com.monasso.app.ui.component.PictogramFactory;
import com.monasso.app.ui.navigation.NavigationManager;
import com.monasso.app.ui.navigation.ScreenId;
import com.monasso.app.ui.screen.ContributionsScreen;
import com.monasso.app.ui.screen.CalendarScreen;
import com.monasso.app.ui.screen.DashboardScreen;
import com.monasso.app.ui.screen.DocumentsScreen;
import com.monasso.app.ui.screen.EventsScreen;
import com.monasso.app.ui.screen.ExportsScreen;
import com.monasso.app.ui.screen.GlobalSearchScreen;
import com.monasso.app.ui.screen.MeetingsScreen;
import com.monasso.app.ui.screen.MembersScreen;
import com.monasso.app.ui.screen.PersonalizationScreen;
import com.monasso.app.ui.screen.SettingsScreen;
import com.monasso.app.ui.screen.TasksScreen;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MainView {

    private static final Map<ScreenId, String> SCREEN_PICTOGRAMS = Map.ofEntries(
            Map.entry(ScreenId.DASHBOARD, "DB"),
            Map.entry(ScreenId.SEARCH, "SR"),
            Map.entry(ScreenId.CALENDAR, "CL"),
            Map.entry(ScreenId.MEMBERS, "PS"),
            Map.entry(ScreenId.EVENTS, "EV"),
            Map.entry(ScreenId.MEETINGS, "RE"),
            Map.entry(ScreenId.TASKS, "TK"),
            Map.entry(ScreenId.DOCUMENTS, "DC"),
            Map.entry(ScreenId.CONTRIBUTIONS, "CT"),
            Map.entry(ScreenId.EXPORTS, "EX"),
            Map.entry(ScreenId.SETTINGS, "PR"),
            Map.entry(ScreenId.PERSONALIZATION, "TH")
    );
    private static final List<ScreenId> PRIMARY_SCREENS = Arrays.asList(
            ScreenId.DASHBOARD,
            ScreenId.SEARCH,
            ScreenId.CALENDAR,
            ScreenId.MEMBERS,
            ScreenId.EVENTS,
            ScreenId.MEETINGS,
            ScreenId.TASKS,
            ScreenId.DOCUMENTS,
            ScreenId.CONTRIBUTIONS,
            ScreenId.EXPORTS
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
        Label contextLabel = new Label("Gestion locale d'association");
        contextLabel.getStyleClass().add("top-context-label");

        VBox titleBlock = new VBox(2, appTitleLabel, contextLabel);
        titleBlock.setAlignment(Pos.CENTER_LEFT);

        Label offlineTag = new Label("Mode local hors ligne");
        offlineTag.getStyleClass().add("status-pill");

        ToggleButton compactModeToggle = new ToggleButton("Mode compact");
        compactModeToggle.getStyleClass().add("ghost-button");
        compactModeToggle.setOnAction(event -> setCompactMode(compactModeToggle.isSelected()));

        MenuButton moreOptions = new MenuButton("Plus d'options");
        moreOptions.getStyleClass().add("ghost-button");
        MenuItem settingsItem = new MenuItem("Parametres");
        settingsItem.setOnAction(event -> navigationManager.navigate(ScreenId.SETTINGS));
        MenuItem personalizationItem = new MenuItem("Personnalisation");
        personalizationItem.setOnAction(event -> navigationManager.navigate(ScreenId.PERSONALIZATION));
        moreOptions.getItems().addAll(settingsItem, personalizationItem);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topBar.getChildren().addAll(titleBlock, spacer, compactModeToggle, moreOptions, offlineTag);
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

        for (ScreenId screenId : PRIMARY_SCREENS) {
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
        navigationManager.register(ScreenId.DASHBOARD, () -> new DashboardScreen(appContext.dashboardService(), this::onDashboardAction));
        navigationManager.register(ScreenId.SEARCH, () -> new GlobalSearchScreen(appContext.globalSearchService(), this::onSearchOpen));
        navigationManager.register(
                ScreenId.CALENDAR,
                () -> new CalendarScreen(
                        appContext.calendarService(),
                        appContext.eventService(),
                        appContext.meetingService(),
                        appContext.memberService()
                )
        );
        navigationManager.register(ScreenId.MEMBERS, () -> new MembersScreen(appContext.memberService(), appContext.customCategoryService()));
        navigationManager.register(
                ScreenId.EVENTS,
                () -> new EventsScreen(
                        appContext.eventService(),
                        appContext.eventTrackingService(),
                        appContext.memberService(),
                        appContext.customCategoryService(),
                        appContext.checklistService()
                )
        );
        navigationManager.register(ScreenId.MEETINGS, () -> new MeetingsScreen(appContext.meetingService(), appContext.memberService(), appContext.checklistService()));
        navigationManager.register(
                ScreenId.TASKS,
                () -> new TasksScreen(
                        appContext.taskService(),
                        appContext.eventService(),
                        appContext.meetingService(),
                        appContext.memberService()
                )
        );
        navigationManager.register(
                ScreenId.DOCUMENTS,
                () -> new DocumentsScreen(
                        appContext.documentService(),
                        appContext.memberService(),
                        appContext.eventService(),
                        appContext.meetingService(),
                        appContext.taskService()
                )
        );
        navigationManager.register(ScreenId.CONTRIBUTIONS, () -> new ContributionsScreen(appContext.contributionService(), appContext.memberService()));
        navigationManager.register(ScreenId.EXPORTS, () -> new ExportsScreen(appContext.exportService(), appContext.settingsService()));
        navigationManager.register(
                ScreenId.SETTINGS,
                () -> new SettingsScreen(
                        appContext.settingsService(),
                        appContext.dataSafetyService(),
                        appContext.demoDataService(),
                        appContext.customCategoryService(),
                        this::reloadDataViews
                )
        );
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

    private void setCompactMode(boolean compactMode) {
        root.getStyleClass().remove("compact-mode");
        if (compactMode) {
            root.getStyleClass().add("compact-mode");
        }
    }

    private void onDashboardAction(DashboardScreen.QuickAction action) {
        switch (action) {
            case ADD_PERSON -> navigationManager.navigate(ScreenId.MEMBERS);
            case CREATE_EVENT -> navigationManager.navigate(ScreenId.EVENTS);
            case CREATE_MEETING -> navigationManager.navigate(ScreenId.MEETINGS);
            case RECORD_CONTRIBUTION -> navigationManager.navigate(ScreenId.CONTRIBUTIONS);
            case EXPORT_DATA -> navigationManager.navigate(ScreenId.EXPORTS);
        }
    }

    private void onSearchOpen(GlobalSearchResult result) {
        if (result == null || result.type() == null) {
            return;
        }
        switch (result.type()) {
            case PERSON -> navigationManager.navigate(ScreenId.MEMBERS);
            case EVENT -> navigationManager.navigate(ScreenId.EVENTS);
            case MEETING -> navigationManager.navigate(ScreenId.MEETINGS);
            case TASK -> navigationManager.navigate(ScreenId.TASKS);
        }
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
