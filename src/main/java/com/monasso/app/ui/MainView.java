package com.monasso.app.ui;

import com.monasso.app.config.AppContext;
import com.monasso.app.model.GlobalSearchResult;
import com.monasso.app.service.BrandingService;
import com.monasso.app.ui.component.PictogramFactory;
import com.monasso.app.ui.navigation.NavigationManager;
import com.monasso.app.ui.navigation.ScreenId;
import com.monasso.app.ui.screen.ContributionsScreen;
import com.monasso.app.ui.screen.CalendarScreen;
import com.monasso.app.ui.screen.DailyUseScreen;
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
import com.monasso.app.ui.screen.WelcomeScreen;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MainView {

    private static final String NAV_ORDER_SETTING_KEY = "ui.nav.order";
    private static final String NAV_HIDDEN_SETTING_KEY = "ui.nav.hidden";
    private static final String NAV_COLLAPSED_SETTING_KEY = "ui.nav.collapsed";
    private static final String NAV_VISIBLE_SETTING_KEY = "ui.nav.visible";
    private static final int MAX_DIRECT_NAV_BUTTONS = 9;

    private static final Map<ScreenId, String> SCREEN_PICTOGRAMS = Map.ofEntries(
            Map.entry(ScreenId.WELCOME, "BI"),
            Map.entry(ScreenId.DAILY_USE, "QD"),
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
    private static final List<ScreenId> DEFAULT_PRIMARY_SCREENS = List.of(
            ScreenId.DAILY_USE,
            ScreenId.DASHBOARD,
            ScreenId.SEARCH,
            ScreenId.CALENDAR,
            ScreenId.MEMBERS,
            ScreenId.EVENTS,
            ScreenId.MEETINGS,
            ScreenId.TASKS,
            ScreenId.DOCUMENTS,
            ScreenId.CONTRIBUTIONS,
            ScreenId.EXPORTS,
            ScreenId.SETTINGS
    );

    private final AppContext appContext;
    private final BorderPane root = new BorderPane();
    private final VBox sidebar = new VBox(10);
    private final VBox navButtonsBox = new VBox(8);
    private final MenuButton overflowMenu = new MenuButton("Autres modules");
    private final MenuButton navConfigMenu = new MenuButton("Personnaliser le menu");
    private final Label appTitleLabel = new Label();
    private final ImageView logoView = new ImageView();
    private final Map<ScreenId, Button> navButtons = new EnumMap<>(ScreenId.class);
    private final List<ScreenId> navOrder = new ArrayList<>();
    private final Set<ScreenId> hiddenScreens = EnumSet.noneOf(ScreenId.class);
    private ToggleButton sidebarToggleButton;
    private boolean sidebarCollapsed;
    private boolean sidebarVisible = true;
    private final NavigationManager navigationManager;

    public MainView(AppContext appContext) {
        this.appContext = appContext;

        root.getStyleClass().add("app-root");
        loadNavigationPreferences();

        StackPane contentPane = new StackPane();
        contentPane.getStyleClass().add("content-host");
        contentPane.setPadding(new Insets(0));
        navigationManager = new NavigationManager(contentPane);

        HBox topBar = createTopBar();
        createSidebar();

        configureNavigation();
        configureNavigationState();

        root.setTop(topBar);
        root.setLeft(sidebar);
        root.setCenter(contentPane);
        applySidebarCollapsedState();
        applySidebarVisibilityState();

        refreshBranding(appContext.brandingService());
        appContext.brandingService().brandingProperty().addListener((obs, oldConfig, newConfig) -> refreshBranding(appContext.brandingService()));

        navigationManager.navigate(ScreenId.DAILY_USE);
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

        sidebarToggleButton = new ToggleButton();
        sidebarToggleButton.getStyleClass().add("ghost-button");
        sidebarToggleButton.setSelected(!sidebarVisible);
        updateSidebarToggleLabel();
        sidebarToggleButton.setOnAction(event -> {
            sidebarVisible = !sidebarToggleButton.isSelected();
            applySidebarVisibilityState();
            persistNavigationPreferences();
        });

        ToggleButton compactModeToggle = new ToggleButton("Mode compact");
        compactModeToggle.getStyleClass().add("ghost-button");
        compactModeToggle.setOnAction(event -> setCompactMode(compactModeToggle.isSelected()));

        MenuButton moreOptions = new MenuButton("Plus d'options");
        moreOptions.getStyleClass().add("ghost-button");
        MenuItem welcomeItem = new MenuItem("Bienvenue / Guide rapide");
        welcomeItem.setOnAction(event -> navigationManager.navigate(ScreenId.WELCOME));
        MenuItem dailyUseItem = new MenuItem("Utilisation quotidienne");
        dailyUseItem.setOnAction(event -> navigationManager.navigate(ScreenId.DAILY_USE));
        MenuItem settingsItem = new MenuItem("Parametres");
        settingsItem.setOnAction(event -> navigationManager.navigate(ScreenId.SETTINGS));
        MenuItem categoriesItem = new MenuItem("Categories personnalisables");
        categoriesItem.setOnAction(event -> openCategoryManager());
        MenuItem personalizationItem = new MenuItem("Personnalisation");
        personalizationItem.setOnAction(event -> navigationManager.navigate(ScreenId.PERSONALIZATION));
        moreOptions.getItems().addAll(welcomeItem, dailyUseItem, settingsItem, categoriesItem, personalizationItem);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topBar.getChildren().addAll(titleBlock, spacer, sidebarToggleButton, compactModeToggle, moreOptions, offlineTag);
        return topBar;
    }

    private void createSidebar() {
        sidebar.getStyleClass().add("sidebar");

        VBox logoBlock = new VBox(10);
        logoBlock.getStyleClass().add("sidebar-logo");
        logoBlock.setAlignment(Pos.CENTER_LEFT);

        logoView.setFitWidth(170);
        logoView.setFitHeight(80);
        logoView.setPreserveRatio(true);

        Label navLabel = new Label("Navigation principale");
        navLabel.getStyleClass().add("sidebar-section-title");

        Button collapseButton = new Button("Reduire");
        collapseButton.getStyleClass().add("ghost-button");
        collapseButton.setOnAction(event -> {
            sidebarCollapsed = !sidebarCollapsed;
            applySidebarCollapsedState();
            persistNavigationPreferences();
        });

        HBox navHeader = new HBox(8, navLabel, collapseButton);
        navHeader.getStyleClass().add("action-row");

        navConfigMenu.getStyleClass().add("ghost-button");
        rebuildNavigationConfigMenu();

        ScrollPane scrollPane = new ScrollPane(navButtonsBox);
        scrollPane.getStyleClass().add("sidebar-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        overflowMenu.getStyleClass().add("ghost-button");
        overflowMenu.setManaged(false);
        overflowMenu.setVisible(false);

        logoBlock.getChildren().addAll(logoView, navHeader);
        sidebar.getChildren().addAll(logoBlock, navConfigMenu, scrollPane, overflowMenu);
        rebuildSidebarButtons();
    }

    private Button createNavButton(ScreenId screenId) {
        Button button = new Button();
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setGraphic(createNavButtonGraphic(screenId));
        button.setOnAction(event -> {
            if (navigationManager.currentScreen() == screenId) {
                return;
            }
            navigationManager.navigate(screenId);
        });
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
        navigationManager.register(ScreenId.WELCOME, () -> new WelcomeScreen(appContext.dashboardService(), this::onWelcomeAction));
        navigationManager.register(
                ScreenId.DAILY_USE,
                () -> new DailyUseScreen(
                        appContext.dashboardService(),
                        appContext.taskService(),
                        appContext.eventService(),
                        this::onDailyUseAction
                )
        );
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
        navigationManager.register(
                ScreenId.MEMBERS,
                () -> new MembersScreen(
                        appContext.memberService(),
                        appContext.customCategoryService(),
                        this::openCategoryManager
                )
        );
        navigationManager.register(
                ScreenId.EVENTS,
                () -> new EventsScreen(
                        appContext.eventService(),
                        appContext.eventTrackingService(),
                        appContext.memberService(),
                        appContext.customCategoryService(),
                        appContext.checklistService(),
                        this::openCategoryManager
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

    private void onWelcomeAction(WelcomeScreen.WelcomeAction action) {
        switch (action) {
            case OPEN_MEMBERS -> navigationManager.navigate(ScreenId.MEMBERS);
            case OPEN_EVENTS -> navigationManager.navigate(ScreenId.EVENTS);
            case OPEN_MEETINGS -> navigationManager.navigate(ScreenId.MEETINGS);
            case OPEN_TASKS -> navigationManager.navigate(ScreenId.TASKS);
            case OPEN_DOCUMENTS -> navigationManager.navigate(ScreenId.DOCUMENTS);
            case OPEN_CALENDAR -> navigationManager.navigate(ScreenId.CALENDAR);
            case OPEN_DASHBOARD -> navigationManager.navigate(ScreenId.DASHBOARD);
        }
    }

    private void onDailyUseAction(DailyUseScreen.DailyAction action) {
        switch (action) {
            case CREATE_EVENT, OPEN_EVENTS -> navigationManager.navigate(ScreenId.EVENTS);
            case CREATE_TASK, OPEN_TASKS -> navigationManager.navigate(ScreenId.TASKS);
            case ADD_PERSON, OPEN_MEMBERS -> navigationManager.navigate(ScreenId.MEMBERS);
            case OPEN_DASHBOARD -> navigationManager.navigate(ScreenId.DASHBOARD);
            case OPEN_CALENDAR -> navigationManager.navigate(ScreenId.CALENDAR);
            case OPEN_MEETINGS -> navigationManager.navigate(ScreenId.MEETINGS);
            case OPEN_DOCUMENTS -> navigationManager.navigate(ScreenId.DOCUMENTS);
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
        navigationManager.clearCache(
                ScreenId.DAILY_USE,
                ScreenId.DASHBOARD,
                ScreenId.SEARCH,
                ScreenId.CALENDAR,
                ScreenId.MEMBERS,
                ScreenId.EVENTS,
                ScreenId.MEETINGS,
                ScreenId.TASKS,
                ScreenId.DOCUMENTS,
                ScreenId.CONTRIBUTIONS,
                ScreenId.EXPORTS,
                ScreenId.SETTINGS
        );
        navigationManager.navigate(current);
    }

    private void openCategoryManager() {
        navigationManager.navigate(ScreenId.SETTINGS);
    }

    private void loadNavigationPreferences() {
        List<ScreenId> defaults = new ArrayList<>(DEFAULT_PRIMARY_SCREENS);
        List<ScreenId> parsedOrder = parseScreenList(appContext.settingsService().getValue(NAV_ORDER_SETTING_KEY).orElse(""));
        navOrder.clear();
        navOrder.addAll(parsedOrder.isEmpty() ? defaults : parsedOrder);
        for (ScreenId defaultScreen : defaults) {
            if (!navOrder.contains(defaultScreen)) {
                navOrder.add(defaultScreen);
            }
        }
        navOrder.removeIf(screenId -> !DEFAULT_PRIMARY_SCREENS.contains(screenId));

        hiddenScreens.clear();
        hiddenScreens.addAll(parseScreenList(appContext.settingsService().getValue(NAV_HIDDEN_SETTING_KEY).orElse("")));
        hiddenScreens.removeIf(screenId -> !navOrder.contains(screenId));

        sidebarCollapsed = Boolean.parseBoolean(appContext.settingsService().getValue(NAV_COLLAPSED_SETTING_KEY).orElse("false"));
        sidebarVisible = !appContext.settingsService().getValue(NAV_VISIBLE_SETTING_KEY).orElse("true").equalsIgnoreCase("false");
    }

    private List<ScreenId> parseScreenList(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        List<ScreenId> screens = new ArrayList<>();
        for (String token : rawValue.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            try {
                ScreenId id = ScreenId.valueOf(trimmed);
                if (!screens.contains(id)) {
                    screens.add(id);
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore unknown screen keys stored by older versions.
            }
        }
        return screens;
    }

    private void rebuildNavigationConfigMenu() {
        navConfigMenu.getItems().clear();

        for (ScreenId screenId : navOrder) {
            CheckMenuItem item = new CheckMenuItem(screenId.label());
            item.setSelected(!hiddenScreens.contains(screenId));
            item.setOnAction(event -> {
                if (!item.isSelected() && visibleScreenCount() <= 1) {
                    item.setSelected(true);
                    return;
                }
                if (item.isSelected()) {
                    hiddenScreens.remove(screenId);
                } else {
                    hiddenScreens.add(screenId);
                }
                rebuildSidebarButtons();
                persistNavigationPreferences();
            });
            navConfigMenu.getItems().add(item);
        }

        navConfigMenu.getItems().add(new SeparatorMenuItem());

        Menu moveMenu = new Menu("Reordonner");
        for (ScreenId screenId : navOrder) {
            Menu entry = new Menu(screenId.label());
            int index = navOrder.indexOf(screenId);
            MenuItem upItem = new MenuItem("Monter");
            upItem.setDisable(index == 0);
            upItem.setOnAction(event -> moveScreen(screenId, -1));
            MenuItem downItem = new MenuItem("Descendre");
            downItem.setDisable(index == navOrder.size() - 1);
            downItem.setOnAction(event -> moveScreen(screenId, 1));
            entry.getItems().addAll(upItem, downItem);
            moveMenu.getItems().add(entry);
        }
        navConfigMenu.getItems().add(moveMenu);

        MenuItem resetItem = new MenuItem("Reinitialiser");
        resetItem.setOnAction(event -> {
            navOrder.clear();
            navOrder.addAll(DEFAULT_PRIMARY_SCREENS);
            hiddenScreens.clear();
            rebuildSidebarButtons();
            rebuildNavigationConfigMenu();
            persistNavigationPreferences();
        });
        navConfigMenu.getItems().add(resetItem);
    }

    private void rebuildSidebarButtons() {
        navButtons.clear();
        navButtonsBox.getChildren().clear();

        List<ScreenId> visibleOrder = navOrder.stream()
                .filter(screenId -> !hiddenScreens.contains(screenId))
                .collect(Collectors.toList());

        int directCount = Math.min(MAX_DIRECT_NAV_BUTTONS, visibleOrder.size());
        for (int i = 0; i < directCount; i++) {
            ScreenId screenId = visibleOrder.get(i);
            Button button = createNavButton(screenId);
            navButtons.put(screenId, button);
            navButtonsBox.getChildren().add(button);
        }

        overflowMenu.getItems().clear();
        for (int i = directCount; i < visibleOrder.size(); i++) {
            ScreenId overflowScreen = visibleOrder.get(i);
            MenuItem item = new MenuItem(overflowScreen.label());
            item.setOnAction(event -> navigationManager.navigate(overflowScreen));
            overflowMenu.getItems().add(item);
        }

        boolean hasOverflow = !overflowMenu.getItems().isEmpty();
        overflowMenu.setManaged(hasOverflow);
        overflowMenu.setVisible(hasOverflow);
        overflowMenu.setText(hasOverflow ? "Autres modules (" + overflowMenu.getItems().size() + ")" : "Autres modules");

        ScreenId current = navigationManager.currentScreen();
        if (current != null && navButtons.containsKey(current)) {
            navButtons.get(current).getStyleClass().add("nav-button-active");
        }
    }

    private int visibleScreenCount() {
        return (int) navOrder.stream().filter(screenId -> !hiddenScreens.contains(screenId)).count();
    }

    private void moveScreen(ScreenId screenId, int delta) {
        int index = navOrder.indexOf(screenId);
        int target = index + delta;
        if (index < 0 || target < 0 || target >= navOrder.size()) {
            return;
        }
        Collections.swap(navOrder, index, target);
        rebuildSidebarButtons();
        rebuildNavigationConfigMenu();
        persistNavigationPreferences();
    }

    private void applySidebarCollapsedState() {
        root.getStyleClass().remove("sidebar-collapsed");
        if (sidebarCollapsed) {
            root.getStyleClass().add("sidebar-collapsed");
            navConfigMenu.setText("Menu");
        } else {
            navConfigMenu.setText("Personnaliser le menu");
        }
    }

    private void applySidebarVisibilityState() {
        sidebar.setVisible(sidebarVisible);
        sidebar.setManaged(sidebarVisible);
        updateSidebarToggleLabel();
    }

    private void updateSidebarToggleLabel() {
        if (sidebarToggleButton == null) {
            return;
        }
        sidebarToggleButton.setText(sidebarVisible ? "Masquer menu" : "Afficher menu");
    }

    private void persistNavigationPreferences() {
        String order = navOrder.stream().map(ScreenId::name).collect(Collectors.joining(","));
        String hidden = hiddenScreens.stream().map(ScreenId::name).collect(Collectors.joining(","));
        try {
            appContext.settingsService().saveSetting(NAV_ORDER_SETTING_KEY, order);
            appContext.settingsService().saveSetting(NAV_HIDDEN_SETTING_KEY, hidden);
            appContext.settingsService().saveSetting(NAV_COLLAPSED_SETTING_KEY, Boolean.toString(sidebarCollapsed));
            appContext.settingsService().saveSetting(NAV_VISIBLE_SETTING_KEY, Boolean.toString(sidebarVisible));
        } catch (Exception ignored) {
            // Keep navigation usable even if settings persistence is temporarily unavailable.
        }
    }
}
