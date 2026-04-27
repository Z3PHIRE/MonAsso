package com.monasso.app.config;

import com.monasso.app.repository.AppSettingsRepository;
import com.monasso.app.repository.ChecklistRepository;
import com.monasso.app.repository.ContributionRepository;
import com.monasso.app.repository.CustomCategoryRepository;
import com.monasso.app.repository.DatabaseManager;
import com.monasso.app.repository.DocumentRepository;
import com.monasso.app.repository.EventParticipantRepository;
import com.monasso.app.repository.EventRepository;
import com.monasso.app.repository.EventTrackingRepository;
import com.monasso.app.repository.MeetingParticipantRepository;
import com.monasso.app.repository.MeetingRepository;
import com.monasso.app.repository.MemberRepository;
import com.monasso.app.repository.SchemaInitializer;
import com.monasso.app.repository.TaskRepository;
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
        EventTrackingRepository eventTrackingRepository = new EventTrackingRepository(databaseManager);
        MeetingRepository meetingRepository = new MeetingRepository(databaseManager);
        MeetingParticipantRepository meetingParticipantRepository = new MeetingParticipantRepository(databaseManager);
        ContributionRepository contributionRepository = new ContributionRepository(databaseManager);
        TaskRepository taskRepository = new TaskRepository(databaseManager);
        DocumentRepository documentRepository = new DocumentRepository(databaseManager);
        AppSettingsRepository appSettingsRepository = new AppSettingsRepository(databaseManager);
        CustomCategoryRepository customCategoryRepository = new CustomCategoryRepository(databaseManager);
        ChecklistRepository checklistRepository = new ChecklistRepository(databaseManager);

        MemberService memberService = new MemberService(memberRepository);
        EventService eventService = new EventService(eventRepository, eventParticipantRepository, memberRepository);
        EventTrackingService eventTrackingService = new EventTrackingService(eventRepository, memberRepository, eventTrackingRepository);
        MeetingService meetingService = new MeetingService(meetingRepository, meetingParticipantRepository, memberRepository);
        CalendarService calendarService = new CalendarService(eventRepository, meetingRepository);
        ContributionService contributionService = new ContributionService(contributionRepository, memberRepository);
        TaskService taskService = new TaskService(taskRepository, memberRepository, eventRepository, meetingRepository);
        DocumentService documentService = new DocumentService(documentRepository, memberRepository, eventRepository, meetingRepository, taskRepository);
        GlobalSearchService globalSearchService = new GlobalSearchService(memberRepository, eventRepository, meetingRepository, taskRepository);
        CustomCategoryService customCategoryService = new CustomCategoryService(customCategoryRepository);
        ChecklistService checklistService = new ChecklistService(checklistRepository);
        SettingsService settingsService = new SettingsService(appSettingsRepository);
        DataSafetyService dataSafetyService = new DataSafetyService(settingsService, schemaInitializer);
        DemoDataService demoDataService = new DemoDataService(memberService, eventService, meetingService, contributionService);
        DashboardService dashboardService = new DashboardService(memberRepository, eventRepository, meetingRepository, contributionRepository, taskRepository, calendarService);
        ExportService exportService = new ExportService(memberRepository, eventRepository, contributionRepository, brandingService);

        return new AppContext(
                databaseManager,
                brandingService,
                themeManager,
                dashboardService,
                memberService,
                eventService,
                eventTrackingService,
                meetingService,
                calendarService,
                contributionService,
                taskService,
                documentService,
                globalSearchService,
                customCategoryService,
                checklistService,
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
