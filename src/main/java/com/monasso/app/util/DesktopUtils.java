package com.monasso.app.util;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;

public final class DesktopUtils {

    private DesktopUtils() {
    }

    public static void openDirectory(Path directory) {
        if (directory == null) {
            throw new IllegalArgumentException("Le dossier a ouvrir est obligatoire.");
        }
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            throw new IllegalStateException("L'ouverture de dossier n'est pas supportee sur ce systeme.");
        }
        try {
            Desktop.getDesktop().open(directory.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'ouvrir le dossier: " + directory, e);
        }
    }
}
