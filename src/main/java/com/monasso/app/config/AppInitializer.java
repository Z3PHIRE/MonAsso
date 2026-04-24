package com.monasso.app.config;

import com.monasso.app.repository.AppSettingsRepository;
import com.monasso.app.repository.ContributionRepository;
import com.monasso.app.repository.DatabaseManager;
import com.monasso.app.repository.EventRepository;
import com.monasso.app.repository.MemberRepository;
import com.monasso.app.repository.SchemaInitializer;
import com.monasso.app.service.BrandingService;
import com.monasso.app.service.ContributionService;
import com.monasso.app.service.DashboardService;
import com.monasso.app.service.EventService;
import com.monasso.app.service.ExportService;
import com.monasso.app.service.MemberService;
import com.monasso.app.service.SettingsService;

import java.io.IOException;
import java.nio.file.Files;

public class AppInitializer {

    public AppContext initialize() {
        createDirectories();

        BrandingService brandingService = new BrandingService();
        brandingService.initialize();

        ThemeManager themeManager = new ThemeManager(brandingService);

        DatabaseManager databaseManager = new DatabaseManager(AppPaths.databasePath());
        new SchemaInitializer(databaseManager).initialize();

        MemberRepository memberRepository = new MemberRepository(databaseManager);
        EventRepository eventRepository = new EventRepository(databaseManager);
        ContributionRepository contributionRepository = new ContributionRepository(databaseManager);
        AppSettingsRepository appSettingsRepository = new AppSettingsRepository(databaseManager);

        MemberService memberService = new MemberService(memberRepository);
        EventService eventService = new EventService(eventRepository);
        ContributionService contributionService = new ContributionService(contributionRepository);
        SettingsService settingsService = new SettingsService(appSettingsRepository);
        DashboardService dashboardService = new DashboardService(memberRepository, eventRepository, contributionRepository);
        ExportService exportService = new ExportService(memberRepository, eventRepository, contributionRepository);

        return new AppContext(
                databaseManager,
                brandingService,
                themeManager,
                dashboardService,
                memberService,
                eventService,
                contributionService,
                exportService,
                settingsService
        );
    }

    private void createDirectories() {
        try {
            Files.createDirectories(AppPaths.assetsBrandingDir());
            Files.createDirectories(AppPaths.dataDir());
            Files.createDirectories(AppPaths.exportsDir());
            Files.createDirectories(AppPaths.backupsDir());
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de creer les dossiers applicatifs.", e);
        }
    }
}
