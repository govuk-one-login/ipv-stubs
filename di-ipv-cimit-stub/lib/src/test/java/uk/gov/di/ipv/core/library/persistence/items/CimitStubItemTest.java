package uk.gov.di.ipv.core.library.persistence.items;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CimitStubItemTest {
    @Test
    void addMitigationsShouldDeduplicate() {
        CimitStubItem item =
                CimitStubItem.builder().mitigations(List.of("X01", "X02", "X03")).build();

        item.addMitigations(List.of("X01", "X03", "Z11"));

        assertEquals(List.of("X01", "X02", "X03", "Z11"), item.getMitigations());
    }

    @Test
    void addMitigationsShouldHandleNoExistingMitigations() {
        CimitStubItem item = CimitStubItem.builder().build();

        item.addMitigations(List.of("X01", "X03", "Z11"));

        assertEquals(List.of("X01", "X03", "Z11"), item.getMitigations());
    }

    @Test
    void addMitigationsShouldHandleNullNewMitigations() {
        CimitStubItem item =
                CimitStubItem.builder().mitigations(List.of("X01", "X02", "X03")).build();

        item.addMitigations(null);

        assertEquals(List.of("X01", "X02", "X03"), item.getMitigations());
    }
}
