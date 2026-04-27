package com.monasso.app.service;

import com.monasso.app.model.AppDocument;
import com.monasso.app.model.DocumentTargetType;
import com.monasso.app.repository.DocumentRepository;
import com.monasso.app.repository.EventRepository;
import com.monasso.app.repository.MeetingRepository;
import com.monasso.app.repository.MemberRepository;
import com.monasso.app.repository.TaskRepository;
import com.monasso.app.util.DesktopUtils;
import com.monasso.app.util.ValidationUtils;

import java.nio.file.Path;
import java.util.List;

public class DocumentService {

    private final DocumentRepository documentRepository;
    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final MeetingRepository meetingRepository;
    private final TaskRepository taskRepository;

    public DocumentService(
            DocumentRepository documentRepository,
            MemberRepository memberRepository,
            EventRepository eventRepository,
            MeetingRepository meetingRepository,
            TaskRepository taskRepository
    ) {
        this.documentRepository = documentRepository;
        this.memberRepository = memberRepository;
        this.eventRepository = eventRepository;
        this.meetingRepository = meetingRepository;
        this.taskRepository = taskRepository;
    }

    public List<AppDocument> getDocuments(DocumentTargetType targetType, String query) {
        return documentRepository.findByCriteria(targetType, query);
    }

    public AppDocument getDocument(long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalStateException("Document introuvable."));
    }

    public AppDocument addDocument(
            DocumentTargetType targetType,
            long targetId,
            String filePath,
            String fileName,
            String fileType,
            String notes
    ) {
        if (targetType == null) {
            throw new IllegalArgumentException("Le type de cible est obligatoire.");
        }
        if (targetId <= 0) {
            throw new IllegalArgumentException("La cible du document est invalide.");
        }
        ensureTargetExists(targetType, targetId);

        String safePath = ValidationUtils.requireText(filePath, "Le chemin du fichier");
        String safeName = resolveFileName(safePath, fileName);
        String safeType = resolveFileType(safeName, fileType);

        AppDocument prepared = new AppDocument(
                0L,
                targetType,
                targetId,
                null,
                safePath,
                safeName,
                safeType,
                ValidationUtils.normalizeOptional(notes),
                null
        );
        return documentRepository.create(prepared);
    }

    public void deleteDocument(long documentId) {
        if (!documentRepository.deleteById(documentId)) {
            throw new IllegalStateException("Le document n'existe plus.");
        }
    }

    public void openDocument(long documentId) {
        AppDocument document = getDocument(documentId);
        DesktopUtils.openFile(Path.of(document.filePath()));
    }

    public void openDocumentDirectory(long documentId) {
        AppDocument document = getDocument(documentId);
        Path filePath = Path.of(document.filePath());
        Path parent = filePath.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("Aucun dossier parent detecte pour ce document.");
        }
        DesktopUtils.openDirectory(parent);
    }

    private void ensureTargetExists(DocumentTargetType targetType, long targetId) {
        switch (targetType) {
            case PERSON -> memberRepository.findById(targetId)
                    .orElseThrow(() -> new IllegalArgumentException("Personne cible introuvable."));
            case EVENT -> eventRepository.findById(targetId)
                    .orElseThrow(() -> new IllegalArgumentException("Evenement cible introuvable."));
            case MEETING -> meetingRepository.findById(targetId)
                    .orElseThrow(() -> new IllegalArgumentException("Reunion cible introuvable."));
            case TASK -> taskRepository.findById(targetId)
                    .orElseThrow(() -> new IllegalArgumentException("Tache cible introuvable."));
        }
    }

    private String resolveFileName(String filePath, String fileName) {
        String provided = ValidationUtils.normalizeOptional(fileName);
        if (provided != null) {
            return provided;
        }
        Path path = Path.of(filePath);
        Path file = path.getFileName();
        if (file == null || file.toString().isBlank()) {
            throw new IllegalArgumentException("Nom de fichier invalide.");
        }
        return file.toString();
    }

    private String resolveFileType(String fileName, String fileType) {
        String provided = ValidationUtils.normalizeOptional(fileType);
        if (provided != null) {
            return provided;
        }
        int index = fileName.lastIndexOf('.');
        if (index <= 0 || index >= fileName.length() - 1) {
            return null;
        }
        return fileName.substring(index + 1).toLowerCase();
    }
}
