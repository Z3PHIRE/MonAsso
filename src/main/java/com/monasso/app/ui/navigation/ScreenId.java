package com.monasso.app.ui.navigation;

public enum ScreenId {
    DASHBOARD("Tableau de bord"),
    MEMBERS("Membres"),
    EVENTS("Evenements"),
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
