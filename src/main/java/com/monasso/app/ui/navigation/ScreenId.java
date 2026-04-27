package com.monasso.app.ui.navigation;

public enum ScreenId {
    WELCOME("Bienvenue"),
    DAILY_USE("Utilisation quotidienne"),
    DASHBOARD("Tableau de bord"),
    SEARCH("Recherche globale"),
    CALENDAR("Calendrier"),
    MEMBERS("Personnes"),
    EVENTS("Evenements"),
    MEETINGS("Reunions"),
    TASKS("Taches"),
    DOCUMENTS("Documents"),
    CONTRIBUTIONS("Cotisations"),
    EXPORTS("Exports"),
    SETTINGS("Parametres"),
    PERSONALIZATION("Personnalisation");

    private final String label;

    ScreenId(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
