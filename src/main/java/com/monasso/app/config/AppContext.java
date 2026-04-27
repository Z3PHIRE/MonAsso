package com.monasso.app.config;

import com.monasso.app.repository.DatabaseManager;
import com.monasso.app.service.BrandingService;
import com.monasso.app.service.CalendarService;
import com.monasso.app.service.ChecklistService;
import com.monasso.app.service.ContributionService;
import com.monasso.app.service.CustomCategoryService;
import com.monasso.app.service.DataSafetyService;
import com.monasso.app.service.DashboardService;
import com.monasso.app.service.DemoDataService;
import com.monasso.app.service.DocumentService;
import com.monasso.app.service.EventService;
import com.monasso.app.service.EventTrackingService;
import com.monasso.app.service.ExportService;
import com.monasso.app.service.GlobalSearchService;
import com.monasso.app.service.MeetingService;
import com.monasso.app.service.MemberService;
import com.monasso.app.service.SettingsService;
import com.monasso.app.service.TaskService;

public class AppContext implements AutoCloseable {

    private final DatabaseManager databaseManager;
    private final BrandingService brandingService;
    private final ThemeManager themeManager;
    private final DashboardService dashboardService;
    private final MemberService memberService;
    private final EventService eventService;
    private final EventTrackingService eventTrackingService;
    private final MeetingService meetingService;
    private final CalendarService calendarService;
    private final ContributionService contributionService;
    private final TaskService taskService;
    private final DocumentService documentService;
    private final GlobalSearchService globalSearchService;
    private final CustomCategoryService customCategoryService;
    private final ChecklistService checklistService;
    private final ExportService exportService;
    private final SettingsService settingsService;
    private final DataSafetyService dataSafetyService;
    private final DemoDataService demoDataService;

    public AppContext(
            DatabaseManager databaseManager,
            BrandingService brandingService,
            ThemeManager themeManager,
            DashboardService dashboardService,
            MemberService memberService,
            EventService eventService,
            EventTrackingService eventTrackingService,
            MeetingService meetingService,
            CalendarService calendarService,
            ContributionService contributionService,
            TaskService taskService,
            DocumentService documentService,
            GlobalSearchService globalSearchService,
            CustomCategoryService customCategoryService,
            ChecklistService checklistService,
            ExportService exportService,
            SettingsService settingsService,
            DataSafetyService dataSafetyService,
            DemoDataService demoDataService
    ) {
        this.databaseManager = databaseManager;
        this.brandingService = brandingService;
        this.themeManager = themeManager;
        this.dashboardService = dashboardService;
        this.memberService = memberService;
        this.eventService = eventService;
        this.eventTrackingService = eventTrackingService;
        this.meetingService = meetingService;
        this.calendarService = calendarService;
        this.contributionService = contributionService;
        this.taskService = taskService;
        this.documentService = documentService;
        this.globalSearchService = globalSearchService;
        this.customCategoryService = customCategoryService;
        this.checklistService = checklistService;
        this.exportService = exportService;
        this.settingsService = settingsService;
        this.dataSafetyService = dataSafetyService;
        this.demoDataService = demoDataService;
    }

    public BrandingService brandingService() {
        return brandingService;
    }

    public ThemeManager themeManager() {
        return themeManager;
    }

    public DashboardService dashboardService() {
        return dashboardService;
    }

    public MemberService memberService() {
        return memberService;
    }

    public EventService eventService() {
        return eventService;
    }

    public EventTrackingService eventTrackingService() {
        return eventTrackingService;
    }

    public MeetingService meetingService() {
        return meetingService;
    }

    public CalendarService calendarService() {
        return calendarService;
    }

    public ContributionService contributionService() {
        return contributionService;
    }

    public TaskService taskService() {
        return taskService;
    }

    public DocumentService documentService() {
        return documentService;
    }

    public GlobalSearchService globalSearchService() {
        return globalSearchService;
    }

    public CustomCategoryService customCategoryService() {
        return customCategoryService;
    }

    public ChecklistService checklistService() {
        return checklistService;
    }

    public ExportService exportService() {
        return exportService;
    }

    public SettingsService settingsService() {
        return settingsService;
    }

    public DataSafetyService dataSafetyService() {
        return dataSafetyService;
    }

    public DemoDataService demoDataService() {
        return demoDataService;
    }

    @Override
    public void close() {
        databaseManager.close();
    }
}
