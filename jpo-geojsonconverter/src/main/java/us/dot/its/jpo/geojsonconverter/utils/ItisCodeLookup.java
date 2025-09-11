package us.dot.its.jpo.geojsonconverter.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import us.dot.its.jpo.asn.j2735.r2024.J2540ITIS.ITIScodes;

/**
 * Utility class for looking up ITIS codes and converting them to human-readable messages. Uses the ASN.1 ITIScodes
 * class with its internal NamedValues hashMap for translations.
 */
@Slf4j
public class ItisCodeLookup {

    /**
     * Look up a single ITIS code and return its human-readable message.
     *
     * @param itisCode The ITIS code to look up
     * @return The human-readable message for the ITIS code, or "unknown" if not found
     */
    public static String lookupItisCode(Long itisCode) {
        if (itisCode == null) {
            return "unknown";
        }

        try {
            ITIScodes itisCodes = new ITIScodes(itisCode);
            return itisCodes.name().orElse("unknown");
        } catch (Exception e) {
            log.debug("Error looking up ITIS code {}: {}", itisCode, e.getMessage());
        }

        return "unknown";
    }

    /**
     * Look up multiple ITIS codes and return their human-readable messages.
     *
     * @param itisCodes List of ITIS codes to look up
     * @return List of human-readable messages for the ITIS codes
     */
    public static List<String> lookupItisCodes(List<Long> itisCodes) {
        List<String> messages = new ArrayList<>();

        if (itisCodes == null || itisCodes.isEmpty()) {
            return messages;
        }

        for (Long itisCode : itisCodes) {
            String message = lookupItisCode(itisCode);
            messages.add(message); // Always add the message (will be "unknown" if not found)
        }

        return messages;
    }

    /**
     * Look up ITIS phrases for a list of ITIS codes. This method populates the itisPhraseList field in
     * ProcessedTimContent.
     *
     * @param itisCodes List of ITIS codes to look up
     * @return List of ITIS phrases (human-readable messages)
     */
    public static List<String> lookupItisPhrases(List<Long> itisCodes) {
        return lookupItisCodes(itisCodes);
    }

    /**
     * Create a combined message from ITIS codes and text messages.
     *
     * @param itisCodes List of ITIS codes
     * @param textMessages List of text messages
     * @return Combined message string
     */
    public static String createCombinedMessage(List<Long> itisCodes, List<String> textMessages) {
        List<String> allMessages = new ArrayList<>();

        // Add ITIS code messages
        if (itisCodes != null && !itisCodes.isEmpty()) {
            List<String> itisMessages = lookupItisCodes(itisCodes);
            allMessages.addAll(itisMessages);
        }

        // Add text messages
        if (textMessages != null && !textMessages.isEmpty()) {
            allMessages.addAll(textMessages);
        }

        if (allMessages.isEmpty()) {
            return "TIM Message"; // Default message
        }

        return String.join(" ", allMessages);
    }
}
