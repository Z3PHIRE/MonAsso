package com.monasso.app.service;

import com.monasso.app.model.AppDocument;
import com.monasso.app.model.DocumentTargetType;
import com.monasso.app.model.Member;
import com.monasso.app.support.TestAppFixture;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
