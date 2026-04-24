package com.monasso.app.config;

import com.monasso.app.repository.DatabaseManager;
import com.monasso.app.service.BrandingService;
import com.monasso.app.service.ContributionService;
import com.monasso.app.service.DataSafetyService;
import com.monasso.app.service.DashboardService;
import com.monasso.app.service.DemoDataService;
import com.monasso.app.service.EventService;
import com.monasso.app.service.ExportService;
import com.monasso.app.service.MemberService;
import com.monasso.app.service.SettingsService;

public class AppContext implements AutoCloseable {

    private final DatabaseManager databaseManager;
    private final BrandingService brandingService;
    private final ThemeManager themeManager;
    private final DashboardService dashboardService;
    private final MemberService memberService;
    private final EventService eventService;
    private final ContributionService contributionService;
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
            ContributionService contributionService,
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
        this.contributionService = contributionService;
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

    public ContributionService contributionService() {
        return contributionService;
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
