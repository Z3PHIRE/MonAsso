package com.monasso.app.service;

import com.monasso.app.model.CategoryScope;
import com.monasso.app.model.CustomCategory;
import com.monasso.app.model.CustomCategoryValue;
import com.monasso.app.model.CustomFieldType;
import com.monasso.app.model.Member;
import com.monasso.app.support.TestAppFixture;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomCategoryServiceTest {

    @Test
    void shouldCreateCategorySubcategoryAndPersistPersonValue() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("custom-category")) {
            CustomCategory root = fixture.customCategoryService.createCategory(
                    "Logistique",
                    null,
                    CustomFieldType.SHORT_TEXT,
                    null,
                    true,
                    10,
                    List.of(CategoryScope.PERSON, CategoryScope.EVENT)
            );
            CustomCategory subCategory = fixture.customCategoryService.createCategory(
                    "Niveau secourisme",
                    root.id(),
                    CustomFieldType.SHORT_TEXT,
                    null,
                    true,
                    11,
                    List.of(CategoryScope.PERSON)
            );

            Member member = fixture.memberService.addMember(
                    "Alice",
                    "Durand",
                    "alice@example.org",
                    "0611223344",
                    "8 rue des Lilas",
                    LocalDate.now(),
                    true,
                    null
            );

            fixture.customCategoryService.saveValue(subCategory.id(), CategoryScope.PERSON, member.id(), "PSE1");
            Map<Long, CustomCategoryValue> values = fixture.customCategoryService.getValuesByCategoryId(CategoryScope.PERSON, member.id());

            assertTrue(values.containsKey(subCategory.id()));
            assertEquals("PSE1", values.get(subCategory.id()).textValue());
        }
    }
}
