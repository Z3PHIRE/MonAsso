package com.monasso.app.support;

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

import java.nio.file.Files;
import java.nio.file.Path;

public final class TestAppFixture implements AutoCloseable {

    private final String previousHomeProperty;

    public final Path appHome;
    public final Path databasePath;
    public final Path brandingDir;
    public final Path brandingFile;

    public final DatabaseManager databaseManager;
    public final SchemaInitializer schemaInitializer;
    public final BrandingService brandingService;

    public final MemberRepository memberRepository;
    public final EventRepository eventRepository;
    public final EventParticipantRepository eventParticipantRepository;
    public final EventTrackingRepository eventTrackingRepository;
    public final MeetingRepository meetingRepository;
    public final MeetingParticipantRepository meetingParticipantRepository;
    public final ContributionRepository contributionRepository;
    public final TaskRepository taskRepository;
    public final DocumentRepository documentRepository;
    public final AppSettingsRepository appSettingsRepository;
    public final CustomCategoryRepository customCategoryRepository;
    public final ChecklistRepository checklistRepository;

    public final MemberService memberService;
    public final EventService eventService;
    public final EventTrackingService eventTrackingService;
    public final MeetingService meetingService;
    public final CalendarService calendarService;
    public final ContributionService contributionService;
    public final TaskService taskService;
    public final DocumentService documentService;
    public final GlobalSearchService globalSearchService;
    public final CustomCategoryService customCategoryService;
    public final ChecklistService checklistService;
    public final SettingsService settingsService;
    public final DataSafetyService dataSafetyService;
    public final DemoDataService demoDataService;
    public final ExportService exportService;

    private TestAppFixture(
            String previousHomeProperty,
            Path appHome,
            Path databasePath,
            Path brandingDir,
            Path brandingFile,
            DatabaseManager databaseManager,
            SchemaInitializer schemaInitializer,
            BrandingService brandingService,
            MemberRepository memberRepository,
            EventRepository eventRepository,
            EventParticipantRepository eventParticipantRepository,
            EventTrackingRepository eventTrackingRepository,
            MeetingRepository meetingRepository,
            MeetingParticipantRepository meetingParticipantRepository,
            ContributionRepository contributionRepository,
            TaskRepository taskRepository,
            DocumentRepository documentRepository,
            AppSettingsRepository appSettingsRepository,
            CustomCategoryRepository customCategoryRepository,
            ChecklistRepository checklistRepository,
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
            SettingsService settingsService,
            DataSafetyService dataSafetyService,
            DemoDataService demoDataService,
            ExportService exportService
    ) {
        this.previousHomeProperty = previousHomeProperty;
        this.appHome = appHome;
        this.databasePath = databasePath;
        this.brandingDir = brandingDir;
        this.brandingFile = brandingFile;
        this.databaseManager = databaseManager;
        this.schemaInitializer = schemaInitializer;
        this.brandingService = brandingService;
        this.memberRepository = memberRepository;
        this.eventRepository = eventRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.eventTrackingRepository = eventTrackingRepository;
        this.meetingRepository = meetingRepository;
        this.meetingParticipantRepository = meetingParticipantRepository;
        this.contributionRepository = contributionRepository;
        this.taskRepository = taskRepository;
        this.documentRepository = documentRepository;
        this.appSettingsRepository = appSettingsRepository;
        this.customCategoryRepository = customCategoryRepository;
        this.checklistRepository = checklistRepository;
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
        this.settingsService = settingsService;
        this.dataSafetyService = dataSafetyService;
        this.demoDataService = demoDataService;
        this.exportService = exportService;
    }

    public static TestAppFixture create(String name) throws Exception {
        Path home = Files.createTempDirectory("monasso-" + name + "-");
        String previousHomeProperty = System.getProperty("monasso.home");
        System.setProperty("monasso.home", home.toString());
        Path dbPath = home.resolve("data").resolve("monasso.db");
        Path brandingDir = home.resolve("assets").resolve("branding");
        Path brandingFile = brandingDir.resolve("branding.json");

        DatabaseManager databaseManager = new DatabaseManager(dbPath);
        SchemaInitializer schemaInitializer = new SchemaInitializer(databaseManager);
        schemaInitializer.initialize();

        BrandingService brandingService = new BrandingService(brandingDir, brandingFile);
        brandingService.initialize();

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
        ExportService exportService = new ExportService(memberRepository, eventRepository, contributionRepository, brandingService);

        return new TestAppFixture(
                previousHomeProperty,
                home,
                dbPath,
                brandingDir,
                brandingFile,
                databaseManager,
                schemaInitializer,
                brandingService,
                memberRepository,
                eventRepository,
                eventParticipantRepository,
                eventTrackingRepository,
                meetingRepository,
                meetingParticipantRepository,
                contributionRepository,
                taskRepository,
                documentRepository,
                appSettingsRepository,
                customCategoryRepository,
                checklistRepository,
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
                settingsService,
                dataSafetyService,
                demoDataService,
                exportService
        );
    }

    @Override
    public void close() {
        databaseManager.close();
        if (previousHomeProperty == null) {
            System.clearProperty("monasso.home");
        } else {
            System.setProperty("monasso.home", previousHomeProperty);
        }
    }
}
