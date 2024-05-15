package uk.gov.di.ipv.stub.cred.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringHelperTest {
    @Test
    void splitCommaDelimitedStringValueShouldCreateList() {
        assertEquals(
                List.of("A", "list", "of", "things"),
                StringHelper.splitCommaDelimitedStringValue("A,list,of,things"));
    }

    @Test
    void splitCommaDelimitedStringValueShouldHandleEmptyString() {
        assertEquals(List.of(), StringHelper.splitCommaDelimitedStringValue(""));
    }

    @Test
    void splitCommaDelimitedStringValueShouldHandleSingleValue() {
        assertEquals(List.of("Only"), StringHelper.splitCommaDelimitedStringValue("Only"));
    }

    @Test
    void splitCommaDelimitedStringValueShouldHandleMultipleCommas() {
        assertEquals(List.of("why?"), StringHelper.splitCommaDelimitedStringValue(",,why?,"));
    }

    @Test
    void splitCommaDelimitedStringValueShouldHandleNull() {
        assertEquals(List.of(), StringHelper.splitCommaDelimitedStringValue(null));
    }
}
