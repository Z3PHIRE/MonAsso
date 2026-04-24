package com.monasso.app.config;

import com.monasso.app.repository.AppSettingsRepository;
import com.monasso.app.repository.ContributionRepository;
import com.monasso.app.repository.DatabaseManager;
import com.monasso.app.repository.EventParticipantRepository;
import com.monasso.app.repository.EventRepository;
import com.monasso.app.repository.MemberRepository;
import com.monasso.app.repository.SchemaInitializer;
import com.monasso.app.service.BrandingService;
import com.monasso.app.service.ContributionService;
import com.monasso.app.service.DataSafetyService;
import com.monasso.app.service.DashboardService;
import com.monasso.app.service.DemoDataService;
import com.monasso.app.service.EventService;
import com.monasso.app.service.ExportService;
import com.monasso.app.service.MemberService;
import com.monasso.app.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;

public class AppInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppInitializer.class);

    public AppContext initialize() {
        createDirectories();

        BrandingService brandingService = new BrandingService();
        brandingService.initialize();

        ThemeManager themeManager = new ThemeManager(brandingService);

        DatabaseManager databaseManager = new DatabaseManager(AppPaths.databasePath());
        SchemaInitializer schemaInitializer = new SchemaInitializer(databaseManager);
        initializeSchema(schemaInitializer);

        MemberRepository memberRepository = new MemberRepository(databaseManager);
        EventRepository eventRepository = new EventRepository(databaseManager);
        EventParticipantRepository eventParticipantRepository = new EventParticipantRepository(databaseManager);
        ContributionRepository contributionRepository = new ContributionRepository(databaseManager);
        AppSettingsRepository appSettingsRepository = new AppSettingsRepository(databaseManager);

        MemberService memberService = new MemberService(memberRepository);
        EventService eventService = new EventService(eventRepository, eventParticipantRepository, memberRepository);
        ContributionService contributionService = new ContributionService(contributionRepository, memberRepository);
        SettingsService settingsService = new SettingsService(appSettingsRepository);
        DataSafetyService dataSafetyService = new DataSafetyService(settingsService, schemaInitializer);
        DemoDataService demoDataService = new DemoDataService(memberService, eventService, contributionService);
        DashboardService dashboardService = new DashboardService(memberRepository, eventRepository, contributionRepository);
        ExportService exportService = new ExportService(memberRepository, eventRepository, contributionRepository, brandingService);

        return new AppContext(
                databaseManager,
                brandingService,
                themeManager,
                dashboardService,
                memberService,
                eventService,
                contributionService,
                exportService,
                settingsService,
                dataSafetyService,
                demoDataService
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

    private void initializeSchema(SchemaInitializer schemaInitializer) {
        try {
            schemaInitializer.initialize();
        } catch (IllegalStateException e) {
            String rootMessage = rootCauseMessage(e).toLowerCase();
            if (rootMessage.contains("not a database")
                    || rootMessage.contains("database disk image is malformed")
                    || rootMessage.contains("file is encrypted")) {
                LOGGER.error("Base SQLite invalide ou corrompue: {}", AppPaths.databasePath(), e);
                throw new IllegalStateException(
                        "La base locale semble corrompue ou invalide. Restaurez une sauvegarde depuis Parametres > Securite des donnees.",
                        e
                );
            }
            throw e;
        }
    }

    private String rootCauseMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? "" : cause.getMessage();
    }
}
