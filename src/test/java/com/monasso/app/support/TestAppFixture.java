package com.monasso.app.support;

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
import com.monasso.app.service.DemoDataService;
import com.monasso.app.service.EventService;
import com.monasso.app.service.ExportService;
import com.monasso.app.service.MemberService;
import com.monasso.app.service.SettingsService;

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
    public final ContributionRepository contributionRepository;
    public final AppSettingsRepository appSettingsRepository;

    public final MemberService memberService;
    public final EventService eventService;
    public final ContributionService contributionService;
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
            ContributionRepository contributionRepository,
            AppSettingsRepository appSettingsRepository,
            MemberService memberService,
            EventService eventService,
            ContributionService contributionService,
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
        this.contributionRepository = contributionRepository;
        this.appSettingsRepository = appSettingsRepository;
        this.memberService = memberService;
        this.eventService = eventService;
        this.contributionService = contributionService;
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
        ContributionRepository contributionRepository = new ContributionRepository(databaseManager);
        AppSettingsRepository appSettingsRepository = new AppSettingsRepository(databaseManager);

        MemberService memberService = new MemberService(memberRepository);
        EventService eventService = new EventService(eventRepository, eventParticipantRepository, memberRepository);
        ContributionService contributionService = new ContributionService(contributionRepository, memberRepository);
        SettingsService settingsService = new SettingsService(appSettingsRepository);
        DataSafetyService dataSafetyService = new DataSafetyService(settingsService, schemaInitializer);
        DemoDataService demoDataService = new DemoDataService(memberService, eventService, contributionService);
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
                contributionRepository,
                appSettingsRepository,
                memberService,
                eventService,
                contributionService,
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
