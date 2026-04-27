package com.monasso.app.service;

import com.monasso.app.model.AppDocument;
import com.monasso.app.model.DocumentTargetType;
import com.monasso.app.model.Member;
import com.monasso.app.support.TestAppFixture;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentServiceTest {

    @Test
    void shouldAttachAndDeleteDocumentsForPerson() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("document-service")) {
            Member member = fixture.memberService.addMember("Paul", "Riviere", null, null, null, LocalDate.now(), true, null);

            AppDocument created = fixture.documentService.addDocument(
                    DocumentTargetType.PERSON,
                    member.id(),
                    "C:/tmp/justificatif.pdf",
                    null,
                    null,
                    "Document test"
            );

            List<AppDocument> personDocs = fixture.documentService.getDocuments(DocumentTargetType.PERSON, "justificatif");
            assertEquals(1, personDocs.size());
            assertEquals("justificatif.pdf", created.fileName());

            fixture.documentService.deleteDocument(created.id());
            List<AppDocument> afterDelete = fixture.documentService.getDocuments(DocumentTargetType.PERSON, "justificatif");
            assertEquals(0, afterDelete.size());
        }
    }

    @Test
    void shouldFailClearlyWhenDocumentFileIsMissing() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("document-service-missing-file")) {
            Member member = fixture.memberService.addMember("Luc", "Bernard", null, null, null, LocalDate.now(), true, null);
            Path missingFile = fixture.appHome.resolve("documents").resolve("missing-file.pdf");

            AppDocument created = fixture.documentService.addDocument(
                    DocumentTargetType.PERSON,
                    member.id(),
                    missingFile.toString(),
                    null,
                    null,
                    null
            );

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> fixture.documentService.openDocument(created.id())
            );
            assertTrue(exception.getMessage().toLowerCase().contains("introuvable"));
        }
    }
}
