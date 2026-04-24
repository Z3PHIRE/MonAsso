package com.monasso.app.service;

import com.monasso.app.model.ContributionStatus;
import com.monasso.app.model.Event;
import com.monasso.app.model.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;

public class DemoDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoDataService.class);

    private final MemberService memberService;
    private final EventService eventService;
    private final ContributionService contributionService;

    public DemoDataService(
            MemberService memberService,
            EventService eventService,
            ContributionService contributionService
    ) {
        this.memberService = memberService;
        this.eventService = eventService;
        this.contributionService = contributionService;
    }

    public boolean canLoadDemoData() {
        return memberService.countAllMembers() == 0
                && eventService.getEvents("", false).isEmpty()
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
                "marie.martin@example.org",
                "0611223344",
                "12 rue des Lilas, 75012 Paris",
                now.minusMonths(14),
                true,
                "Tres active sur les actions locales."
        );
        Member luc = memberService.addMember(
                "Luc",
                "Bernard",
                "luc.bernard@example.org",
                "0622334455",
                "8 avenue Victor Hugo, 75016 Paris",
                now.minusMonths(9),
                true,
                "Disponible les soirs."
        );
        Member sophie = memberService.addMember(
                "Sophie",
                "Dubois",
                "sophie.dubois@example.org",
                "0633445566",
                "4 place de la Mairie, 92100 Boulogne",
                now.minusMonths(3),
                true,
                "Interessee par les ateliers."
        );
        Member hugo = memberService.addMember(
                "Hugo",
                "Petit",
                "hugo.petit@example.org",
                "0644556677",
                "22 rue de Verdun, 94000 Creteil",
                now.minusYears(2),
                false,
                "Ancien membre, a relancer."
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
        Event reunion = eventService.addEvent(
                "Reunion bureau",
                now.plusDays(4),
                LocalTime.of(19, 0),
                "Local association",
                "Preparation du prochain trimestre.",
                null
        );

        eventService.addParticipant(collecte.id(), marie.id());
        eventService.addParticipant(collecte.id(), luc.id());
        eventService.addParticipant(atelier.id(), sophie.id());
        eventService.addParticipant(reunion.id(), marie.id());
        eventService.addParticipant(reunion.id(), luc.id());

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

        LOGGER.info("Jeu de demonstration charge: membres=4, evenements=3, cotisations=3");
        return new DemoDataResult(4, 3, 3);
    }

    public record DemoDataResult(int members, int events, int contributions) {
    }
}
