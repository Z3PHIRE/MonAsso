package com.monasso.app.service;

import com.monasso.app.model.Contribution;
import com.monasso.app.model.Event;
import com.monasso.app.model.Member;
import com.monasso.app.repository.ContributionRepository;
import com.monasso.app.repository.EventRepository;
import com.monasso.app.repository.MemberRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExportService {

    private static final DateTimeFormatter FILE_SUFFIX = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final ContributionRepository contributionRepository;

    public ExportService(MemberRepository memberRepository, EventRepository eventRepository, ContributionRepository contributionRepository) {
        this.memberRepository = memberRepository;
        this.eventRepository = eventRepository;
        this.contributionRepository = contributionRepository;
    }

    public Path exportMembers(Path directory) {
        String filename = "members_" + FILE_SUFFIX.format(LocalDateTime.now()) + ".csv";
        List<String> rows = new ArrayList<>();
        rows.add("id;first_name;last_name;email;phone;address;join_date;status;notes");
        for (Member member : memberRepository.findByCriteria("", null)) {
            rows.add(String.join(";",
                    String.valueOf(member.id()),
                    safe(member.firstName()),
                    safe(member.lastName()),
                    safe(member.email()),
                    safe(member.phone()),
                    safe(member.address()),
                    member.joinDate().toString(),
                    member.statusLabel(),
                    safe(member.notes())
            ));
        }
        return writeCsv(directory, filename, rows);
    }

    public Path exportEvents(Path directory) {
        String filename = "events_" + FILE_SUFFIX.format(LocalDateTime.now()) + ".csv";
        List<String> rows = new ArrayList<>();
        rows.add("id;title;event_date;event_time;location;description;capacity;participant_count");
        for (Event event : eventRepository.findByCriteria("", false)) {
            rows.add(String.join(";",
                    String.valueOf(event.id()),
                    safe(event.title()),
                    event.eventDate().toString(),
                    event.eventTime().toString(),
                    safe(event.location()),
                    safe(event.description()),
                    event.capacity() == null ? "" : String.valueOf(event.capacity()),
                    String.valueOf(event.participantCount())
            ));
        }
        return writeCsv(directory, filename, rows);
    }

    public Path exportContributions(Path directory) {
        String filename = "contributions_" + FILE_SUFFIX.format(LocalDateTime.now()) + ".csv";
        List<String> rows = new ArrayList<>();
        rows.add("id;member_id;member_name;amount;contribution_date;period;status;payment_method;notes");
        for (Contribution contribution : contributionRepository.findByCriteria("", null, null)) {
            rows.add(String.join(";",
                    String.valueOf(contribution.id()),
                    String.valueOf(contribution.memberId()),
                    safe(contribution.memberName()),
                    String.format(Locale.US, "%.2f", contribution.amount()),
                    contribution.contributionDate().toString(),
                    safe(contribution.periodLabel()),
                    contribution.status().name(),
                    safe(contribution.paymentMethod()),
                    safe(contribution.notes())
            ));
        }
        return writeCsv(directory, filename, rows);
    }

    private Path writeCsv(Path directory, String fileName, List<String> rows) {
        try {
            Files.createDirectories(directory);
            Path target = directory.resolve(fileName);
            Files.write(target, rows, StandardCharsets.UTF_8);
            return target;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'exporter le fichier " + fileName, e);
        }
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(";", ",").replace("\n", " ").replace("\r", " ");
    }
}
