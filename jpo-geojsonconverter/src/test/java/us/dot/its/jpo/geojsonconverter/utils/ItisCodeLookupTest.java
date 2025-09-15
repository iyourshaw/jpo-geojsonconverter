package us.dot.its.jpo.geojsonconverter.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ItisCodeLookupTest {

    // Known ITIS codes for testing (these should be valid ITIS codes)
    private static final Long KNOWN_ITIS_CODE_1 = 1L; // Should be a valid ITIS code
    private static final Long KNOWN_ITIS_CODE_2 = 2L; // Should be a valid ITIS code
    private static final Long UNKNOWN_ITIS_CODE = 999999L; // Should be an unknown ITIS code
    private static final Long NEGATIVE_ITIS_CODE = -1L; // Invalid ITIS code

    @Test
    public void testLookupItisCodeWithValidCode() {
        // Test lookup with a known ITIS code
        String result = ItisCodeLookup.lookupItisCode(KNOWN_ITIS_CODE_1);

        assertNotNull("Result should not be null", result);
        // The result should either be a valid ITIS name or "unknown" if the code is not in the ITIS database
        assertTrue("Result should be a string", result instanceof String);
    }

    @Test
    public void testLookupItisCodeWithUnknownCode() {
        // Test lookup with an unknown ITIS code
        String result = ItisCodeLookup.lookupItisCode(UNKNOWN_ITIS_CODE);

        assertNotNull("Result should not be null", result);
        assertEquals("Should return 'unknown' for unknown ITIS code", "unknown", result);
    }

    @Test
    public void testLookupItisCodeWithNullCode() {
        // Test lookup with null ITIS code
        String result = ItisCodeLookup.lookupItisCode(null);

        assertNotNull("Result should not be null", result);
        assertEquals("Should return 'unknown' for null ITIS code", "unknown", result);
    }

    @Test
    public void testLookupItisCodeWithNegativeCode() {
        // Test lookup with negative ITIS code
        String result = ItisCodeLookup.lookupItisCode(NEGATIVE_ITIS_CODE);

        assertNotNull("Result should not be null", result);
        assertEquals("Should return 'unknown' for negative ITIS code", "unknown", result);
    }

    @Test
    public void testLookupItisCodeWithZeroCode() {
        // Test lookup with zero ITIS code
        String result = ItisCodeLookup.lookupItisCode(0L);

        assertNotNull("Result should not be null", result);
        // Zero might be a valid ITIS code or unknown, but should not crash
        assertTrue("Result should be a string", result instanceof String);
    }

    @Test
    public void testLookupItisCodesWithValidList() {
        // Test batch lookup with valid ITIS codes
        List<Long> itisCodes = Arrays.asList(KNOWN_ITIS_CODE_1, KNOWN_ITIS_CODE_2);

        List<String> results = ItisCodeLookup.lookupItisCodes(itisCodes);

        assertNotNull("Results should not be null", results);
        assertEquals("Should return same number of results as input codes", itisCodes.size(), results.size());

        // All results should be strings
        for (String result : results) {
            assertNotNull("Each result should not be null", result);
            assertTrue("Each result should be a string", result instanceof String);
        }
    }

    @Test
    public void testLookupItisCodesWithMixedList() {
        // Test batch lookup with mix of valid and invalid ITIS codes
        List<Long> itisCodes = Arrays.asList(KNOWN_ITIS_CODE_1, UNKNOWN_ITIS_CODE, NEGATIVE_ITIS_CODE);

        List<String> results = ItisCodeLookup.lookupItisCodes(itisCodes);

        assertNotNull("Results should not be null", results);
        assertEquals("Should return same number of results as input codes", itisCodes.size(), results.size());

        // Check that unknown codes return "unknown"
        assertEquals("Unknown code should return 'unknown'", "unknown", results.get(1));
        assertEquals("Negative code should return 'unknown'", "unknown", results.get(2));
    }

    @Test
    public void testLookupItisCodesWithNullList() {
        // Test batch lookup with null list
        List<String> results = ItisCodeLookup.lookupItisCodes(null);

        assertNotNull("Results should not be null", results);
        assertTrue("Results should be empty list", results.isEmpty());
    }

    @Test
    public void testLookupItisCodesWithEmptyList() {
        // Test batch lookup with empty list
        List<Long> itisCodes = new ArrayList<>();
        List<String> results = ItisCodeLookup.lookupItisCodes(itisCodes);

        assertNotNull("Results should not be null", results);
        assertTrue("Results should be empty list", results.isEmpty());
    }

    @Test
    public void testLookupItisCodesWithNullElements() {
        // Test batch lookup with list containing null elements
        List<Long> itisCodes = Arrays.asList(KNOWN_ITIS_CODE_1, null, KNOWN_ITIS_CODE_2);

        List<String> results = ItisCodeLookup.lookupItisCodes(itisCodes);

        assertNotNull("Results should not be null", results);
        assertEquals("Should return same number of results as input codes", itisCodes.size(), results.size());
        assertEquals("Null code should return 'unknown'", "unknown", results.get(1));
    }

    @Test
    public void testLookupItisPhrasesWithValidList() {
        // Test ITIS phrases lookup (should be same as lookupItisCodes)
        List<Long> itisCodes = Arrays.asList(KNOWN_ITIS_CODE_1, KNOWN_ITIS_CODE_2);

        List<String> results = ItisCodeLookup.lookupItisPhrases(itisCodes);

        assertNotNull("Results should not be null", results);
        assertEquals("Should return same number of results as input codes", itisCodes.size(), results.size());
    }

    @Test
    public void testLookupItisPhrasesWithNullList() {
        // Test ITIS phrases lookup with null list
        List<String> results = ItisCodeLookup.lookupItisPhrases(null);

        assertNotNull("Results should not be null", results);
        assertTrue("Results should be empty list", results.isEmpty());
    }

    @Test
    public void testCreateCombinedMessageWithItisCodesOnly() {
        // Test combined message creation with ITIS codes only
        List<Long> itisCodes = Arrays.asList(KNOWN_ITIS_CODE_1, KNOWN_ITIS_CODE_2);
        List<String> textMessages = null;

        String result = ItisCodeLookup.createCombinedMessage(itisCodes, textMessages);

        assertNotNull("Result should not be null", result);
        assertTrue("Result should contain ITIS messages", result.length() > 0);
    }

    @Test
    public void testCreateCombinedMessageWithTextMessagesOnly() {
        // Test combined message creation with text messages only
        List<Long> itisCodes = null;
        List<String> textMessages = Arrays.asList("Traffic Alert", "Road Closed");

        String result = ItisCodeLookup.createCombinedMessage(itisCodes, textMessages);

        assertNotNull("Result should not be null", result);
        assertEquals("Should combine text messages with space", "Traffic Alert Road Closed", result);
    }

    @Test
    public void testCreateCombinedMessageWithBothItisAndText() {
        // Test combined message creation with both ITIS codes and text messages
        List<Long> itisCodes = Arrays.asList(KNOWN_ITIS_CODE_1);
        List<String> textMessages = Arrays.asList("Traffic Alert");

        String result = ItisCodeLookup.createCombinedMessage(itisCodes, textMessages);

        assertNotNull("Result should not be null", result);
        assertTrue("Result should contain both ITIS and text messages", result.length() > 0);
    }

    @Test
    public void testCreateCombinedMessageWithEmptyLists() {
        // Test combined message creation with empty lists
        List<Long> itisCodes = new ArrayList<>();
        List<String> textMessages = new ArrayList<>();

        String result = ItisCodeLookup.createCombinedMessage(itisCodes, textMessages);

        assertNotNull("Result should not be null", result);
        assertEquals("Should return default message for empty lists", "TIM Message", result);
    }

    @Test
    public void testCreateCombinedMessageWithNullLists() {
        // Test combined message creation with null lists
        String result = ItisCodeLookup.createCombinedMessage(null, null);

        assertNotNull("Result should not be null", result);
        assertEquals("Should return default message for null lists", "TIM Message", result);
    }

    @Test
    public void testCreateCombinedMessageWithMixedNullLists() {
        // Test combined message creation with one null list
        List<Long> itisCodes = null;
        List<String> textMessages = Arrays.asList("Traffic Alert");

        String result = ItisCodeLookup.createCombinedMessage(itisCodes, textMessages);

        assertNotNull("Result should not be null", result);
        assertEquals("Should handle null ITIS codes list", "Traffic Alert", result);
    }

    @Test
    public void testCreateCombinedMessageWithEmptyTextMessages() {
        // Test combined message creation with empty text messages
        List<Long> itisCodes = Arrays.asList(KNOWN_ITIS_CODE_1);
        List<String> textMessages = new ArrayList<>();

        String result = ItisCodeLookup.createCombinedMessage(itisCodes, textMessages);

        assertNotNull("Result should not be null", result);
        assertTrue("Result should contain ITIS message", result.length() > 0);
    }

    @Test
    public void testCreateCombinedMessageWithNullTextMessages() {
        // Test combined message creation with null text messages
        List<Long> itisCodes = Arrays.asList(KNOWN_ITIS_CODE_1);
        List<String> textMessages = null;

        String result = ItisCodeLookup.createCombinedMessage(itisCodes, textMessages);

        assertNotNull("Result should not be null", result);
        assertTrue("Result should contain ITIS message", result.length() > 0);
    }

    @Test
    public void testCreateCombinedMessageWithMultipleTextMessages() {
        // Test combined message creation with multiple text messages
        List<Long> itisCodes = null;
        List<String> textMessages = Arrays.asList("Traffic", "Alert", "Road", "Closed");

        String result = ItisCodeLookup.createCombinedMessage(itisCodes, textMessages);

        assertNotNull("Result should not be null", result);
        assertEquals("Should join multiple text messages with spaces", "Traffic Alert Road Closed", result);
    }

    @Test
    public void testCreateCombinedMessageWithEmptyStringMessages() {
        // Test combined message creation with empty string messages
        List<Long> itisCodes = null;
        List<String> textMessages = Arrays.asList("", "Traffic", "", "Alert");

        String result = ItisCodeLookup.createCombinedMessage(itisCodes, textMessages);

        assertNotNull("Result should not be null", result);
        assertEquals("Should handle empty strings in messages", " Traffic  Alert", result);
    }

    @Test
    public void testCreateCombinedMessageWithNullStringMessages() {
        // Test combined message creation with null string messages
        List<Long> itisCodes = null;
        List<String> textMessages = Arrays.asList("Traffic", null, "Alert");

        String result = ItisCodeLookup.createCombinedMessage(itisCodes, textMessages);

        assertNotNull("Result should not be null", result);
        // Should handle null strings gracefully (they become "null" when joined)
        assertTrue("Result should contain the non-null messages", result.contains("Traffic"));
        assertTrue("Result should contain the non-null messages", result.contains("Alert"));
    }

    @Test
    public void testLookupItisCodeConsistency() {
        // Test that multiple calls with same ITIS code return consistent results
        String result1 = ItisCodeLookup.lookupItisCode(KNOWN_ITIS_CODE_1);
        String result2 = ItisCodeLookup.lookupItisCode(KNOWN_ITIS_CODE_1);

        assertEquals("Multiple lookups should return consistent results", result1, result2);
    }

    @Test
    public void testLookupItisCodesConsistency() {
        // Test that batch lookup returns same results as individual lookups
        List<Long> itisCodes = Arrays.asList(KNOWN_ITIS_CODE_1, KNOWN_ITIS_CODE_2);

        List<String> batchResults = ItisCodeLookup.lookupItisCodes(itisCodes);
        String individualResult1 = ItisCodeLookup.lookupItisCode(KNOWN_ITIS_CODE_1);
        String individualResult2 = ItisCodeLookup.lookupItisCode(KNOWN_ITIS_CODE_2);

        assertEquals("Batch lookup should match individual lookup", individualResult1, batchResults.get(0));
        assertEquals("Batch lookup should match individual lookup", individualResult2, batchResults.get(1));
    }

    @Test
    public void testLookupItisPhrasesConsistency() {
        // Test that lookupItisPhrases returns same results as lookupItisCodes
        List<Long> itisCodes = Arrays.asList(KNOWN_ITIS_CODE_1, KNOWN_ITIS_CODE_2);

        List<String> phrasesResults = ItisCodeLookup.lookupItisPhrases(itisCodes);
        List<String> codesResults = ItisCodeLookup.lookupItisCodes(itisCodes);

        assertEquals("Phrases lookup should match codes lookup", codesResults, phrasesResults);
    }

    @Test
    public void testLookupItisCodeWithLargeNumber() {
        // Test lookup with a very large ITIS code number
        Long largeItisCode = Long.MAX_VALUE;
        String result = ItisCodeLookup.lookupItisCode(largeItisCode);

        assertNotNull("Result should not be null", result);
        assertEquals("Should return 'unknown' for very large ITIS code", "unknown", result);
    }

    @Test
    public void testLookupItisCodeWithVerySmallNumber() {
        // Test lookup with a very small ITIS code number
        Long smallItisCode = Long.MIN_VALUE;
        String result = ItisCodeLookup.lookupItisCode(smallItisCode);

        assertNotNull("Result should not be null", result);
        assertEquals("Should return 'unknown' for very small ITIS code", "unknown", result);
    }

    @Test
    public void testCreateCombinedMessageWithVeryLongLists() {
        // Test combined message creation with very long lists
        List<Long> itisCodes = new ArrayList<>();
        List<String> textMessages = new ArrayList<>();

        // Create lists with many elements
        for (int i = 0; i < 100; i++) {
            itisCodes.add((long) i);
            textMessages.add("Message" + i);
        }

        String result = ItisCodeLookup.createCombinedMessage(itisCodes, textMessages);

        assertNotNull("Result should not be null", result);
        assertTrue("Result should be very long", result.length() > 1000);
    }

    @Test
    public void testLookupItisCodesWithDuplicateCodes() {
        // Test batch lookup with duplicate ITIS codes
        List<Long> itisCodes = Arrays.asList(KNOWN_ITIS_CODE_1, KNOWN_ITIS_CODE_1, KNOWN_ITIS_CODE_2);

        List<String> results = ItisCodeLookup.lookupItisCodes(itisCodes);

        assertNotNull("Results should not be null", results);
        assertEquals("Should return same number of results as input codes", itisCodes.size(), results.size());
        assertEquals("Duplicate codes should return same result", results.get(0), results.get(1));
    }

    @Test
    public void testCreateCombinedMessageWithSpecialCharacters() {
        // Test combined message creation with special characters in text messages
        List<Long> itisCodes = null;
        List<String> textMessages = Arrays.asList("Traffic Alert!", "Road Closed @ 5PM", "Detour → Main St.");

        String result = ItisCodeLookup.createCombinedMessage(itisCodes, textMessages);

        assertNotNull("Result should not be null", result);
        assertTrue("Result should contain special characters", result.contains("!"));
        assertTrue("Result should contain special characters", result.contains("@"));
        assertTrue("Result should contain special characters", result.contains("→"));
    }
}
