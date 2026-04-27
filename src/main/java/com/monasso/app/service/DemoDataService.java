package com.monasso.app.service;

import com.monasso.app.model.ContributionStatus;
import com.monasso.app.model.Event;
import com.monasso.app.model.Meeting;
import com.monasso.app.model.Member;
import com.monasso.app.model.PersonType;
import com.monasso.app.model.ScheduleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;

public class DemoDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoDataService.class);

    private final MemberService memberService;
    private final EventService eventService;
    private final MeetingService meetingService;
    private final ContributionService contributionService;

    public DemoDataService(
            MemberService memberService,
            EventService eventService,
            MeetingService meetingService,
            ContributionService contributionService
    ) {
        this.memberService = memberService;
        this.eventService = eventService;
        this.meetingService = meetingService;
        this.contributionService = contributionService;
    }

    public boolean canLoadDemoData() {
        return memberService.countAllMembers() == 0
                && eventService.getEvents("", false).isEmpty()
                && meetingService.getMeetings("", false).isEmpty()
                && contributionService.getContributions("", null, null).isEmpty();
    }

    public DemoDataResult loadDemoData() {
        if (!canLoadDemoData()) {
            throw new IllegalStateException("La base contient deja des donnees. Le jeu de demonstration ne peut etre charge que sur une base vide.");
        }

        LocalDate now = LocalDate.now();
        String currentPeriod = String.valueOf(now.getYear());

        Member marie = memberService.addMember(
                "Marie",
                "Martin",
                PersonType.VOLUNTEER,
                "0611223344",
                "marie.martin@example.org",
                true,
                "12 rue des Lilas, 75012 Paris",
                now.minusMonths(14),
                "Coordination locale",
                "Organisation terrain",
                "Soirs de semaine",
                "Tres active sur les actions locales.",
                "Paul Martin - 0611000000",
                "M",
                "PSC1",
                "",
                ""
        );
        Member luc = memberService.addMember(
                "Luc",
                "Bernard",
                PersonType.MEMBER,
                "0622334455",
                "luc.bernard@example.org",
                true,
                "8 avenue Victor Hugo, 75016 Paris",
                now.minusMonths(9),
                "Tresorier",
                "Gestion budget",
                "Lundi, mercredi",
                "Disponible les soirs.",
                "",
                "L",
                "",
                "",
                ""
        );
        Member sophie = memberService.addMember(
                "Sophie",
                "Dubois",
                PersonType.SPEAKER,
                "0633445566",
                "sophie.dubois@example.org",
                true,
                "4 place de la Mairie, 92100 Boulogne",
                now.minusMonths(3),
                "Intervenante numerique",
                "Animation atelier",
                "Week-end",
                "Interessee par les ateliers.",
                "",
                "S",
                "",
                "",
                ""
        );
        Member hugo = memberService.addMember(
                "Hugo",
                "Petit",
                PersonType.PARTNER,
                "0644556677",
                "hugo.petit@example.org",
                false,
                "22 rue de Verdun, 94000 Creteil",
                now.minusYears(2),
                "Partenariats",
                "",
                "",
                "Ancien membre, a relancer.",
                "",
                "",
                "",
                "",
                ""
        );

        Event collecte = eventService.addEvent(
                "Collecte alimentaire",
                now.plusDays(10),
                LocalTime.of(18, 30),
                "Salle municipale",
                "Collecte solidaire trimestrielle.",
                40
        );
        Event atelier = eventService.addEvent(
                "Atelier numerique",
                now.plusDays(20),
                LocalTime.of(14, 0),
                "Mediatheque",
                "Atelier d'initiation numerique pour seniors.",
                20
        );
        Meeting reunion = meetingService.addMeeting(
                "Reunion bureau",
                now.plusDays(4),
                LocalTime.of(19, 0),
                LocalTime.of(20, 30),
                "Local association",
                "Bureau",
                marie.id(),
                "Preparation du prochain trimestre.",
                "Suivi des actions en cours",
                ScheduleStatus.CONFIRMED,
                "Administratif",
                ""
        );

        eventService.addParticipant(collecte.id(), marie.id());
        eventService.addParticipant(collecte.id(), luc.id());
        eventService.addParticipant(atelier.id(), sophie.id());
        meetingService.addParticipant(reunion.id(), marie.id());
        meetingService.addParticipant(reunion.id(), luc.id());

        contributionService.addContribution(
                marie.id(),
                45.00,
                now.minusMonths(1),
                currentPeriod,
                ContributionStatus.PAID,
                "Virement",
                "Cotisation annuelle complete"
        );
        contributionService.addContribution(
                luc.id(),
                20.00,
                now.minusDays(20),
                currentPeriod,
                ContributionStatus.PARTIAL,
                "Especes",
                "Complement attendu"
        );
        contributionService.addContribution(
                sophie.id(),
                45.00,
                now.minusDays(5),
                currentPeriod,
                ContributionStatus.PAID,
                "Carte",
                ""
        );

        LOGGER.info("Jeu de demonstration charge: membres=4, evenements=2, reunions=1, cotisations=3");
        return new DemoDataResult(4, 2, 1, 3);
    }

    public record DemoDataResult(int members, int events, int meetings, int contributions) {
    }
}
