package us.dot.its.jpo.geojsonconverter.converter.rtcm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import us.dot.its.jpo.asn.j2735.r2024.Common.RTCMmessageList;
import us.dot.its.jpo.geojsonconverter.DateJsonMapper;

import java.io.File;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Methods for decoding the RTCM payloads
 */
@Component
@Slf4j
public class RTCMDecoder {

    private final boolean fullDecode;

    public RTCMDecoder(@Value("${rtcm.full.decode}") boolean fullDecode) {
        this.fullDecode = fullDecode;
        File executable = new File(EXECUTABLE);
        this.executableExists = executable.exists();
    }

    private static final HexFormat hexFormat = HexFormat.of();

    private static final String EXECUTABLE = "/usr/bin/gpsdecode";

    private final boolean executableExists;

    public List<byte[]> splitMessages(RTCMmessageList messageList) throws RTCMDecodeException {
        byte[] combinedBytes = combinePartialMessages(messageList);
        return splitCombinedMessages(combinedBytes);
    }

    private byte[] combinePartialMessages(RTCMmessageList messages) throws RTCMDecodeException {
        List<byte[]> messageByteList = messages.stream()
                .map(message -> message != null ? message.getOctets() : null)
                .filter(Objects::nonNull)
                .toList();

        if (messageByteList.isEmpty()) {
            throw new RTCMDecodeException("No rtcm messages are present");
        }

        int totalLength = messageByteList.stream()
                .mapToInt(bytes -> bytes.length)
                .sum();

        if (totalLength == 0) {
            throw new RTCMDecodeException("Total length of all messages is zero.");
        }

        byte[] combinedBytes = new byte[totalLength];
        int offset = 0;
        for (byte[] messageBytes : messageByteList) {
            System.arraycopy(messageBytes, 0, combinedBytes, offset, messageBytes.length);
            offset += messageBytes.length;
        }
        return combinedBytes;
    }

    /**
     * Split concatenated RTCM messages by reading the length information
     * @param combinedMessages The concatenated rtcm messages
     * @return the split messages
     */
    private List<byte[]> splitCombinedMessages(final byte[] combinedMessages) throws RTCMDecodeException {
        List<byte[]> messages = new ArrayList<>();
        messageSplitter(combinedMessages, messages);
        return messages;
    }

    private void messageSplitter(final byte[] combinedMessages, List<byte[]> messages) throws RTCMDecodeException {
        int offset = 0;
        while (offset < combinedMessages.length) {
            int remainingLength = combinedMessages.length - offset;

            // The length from the determinant doesn't include three bytes at the beginning (preamble, zeroes,
            // length det) nor three bytes at the end (CRC). Actual length is length + 6.
            int length = decodeLength(combinedMessages, offset) + 6;

            // verify things that must be true for length to be valid
            if (length <= 0) {
                throw new RTCMDecodeException("Invalid message length: " + length);
            }
            if (length > remainingLength) {
                throw new RTCMDecodeException("Length from determinant is greater than the available bytes: "
                        + length + " > " + remainingLength);
            }

            // Save the message
            byte[] message = new byte[length];
            System.arraycopy(combinedMessages, offset, message, 0, length);
            messages.add(message);
            log.debug("RTCM message length: {}, message: {}", length, hexFormat.formatHex(message));

            offset += length;
        }
    }

    public JsonNode decodeRtcm(byte[] bytes) {
        if (!fullDecode) {
            log.debug("Full decode is disabled by config setting");
            return partialDecode(bytes);
        }

        if (executableExists) {
            return fullDecode(bytes);
        } else {
            log.warn("Executable {} does not exist, or running on Windows. Partially decoding. " +
                    "Install gpsdecode, or configure rtcm.full.decode=false to suppress this warning", EXECUTABLE);
            return partialDecode(bytes);
        }
    }

    public static int decodeLength(byte[] bytes, int offset) throws RTCMDecodeException {
        // Need at least 24 bits (3 bytes) to get the length
        if (bytes.length - offset < 3) {
            throw new RTCMDecodeException("Not enough bytes to get length from RTCM");
        }

        // Preamble: 8 bits
        int preamble = unsigned(bytes[offset]);
        if (preamble != 0xD3) {
            throw new RTCMDecodeException(String.format("Invalid RTCM preamble, can't find length: %02X, should be %02X", preamble, 0xD3));
        }

        // Next 6 bits should be zero
        int zeroBits = unsigned(bytes[offset + 1]) >>> 2;
        if (zeroBits != 0) {
            throw new RTCMDecodeException(String.format("Invalid zero bits, can't find length: %X", zeroBits));
        }

        // Length: 10 bits
        return ((unsigned(bytes[offset + 1]) & 0x03) << 8) | unsigned(bytes[offset + 2]);
    }

    /**
     * Partially decode the RTCM message.  Used when gpsdecode library is not available.
     * Ref. <a href="https://gitlab.com/gpsd/gpsd/-/blob/master/drivers/driver_rtcm3.c">gpsd/driver_rtcm.c</a>
     * @param bytes byte array
     * @return JSON formatted partially decoded message.
     */
    public static JsonNode partialDecode(byte[] bytes) {
        ObjectMapper mapper = DateJsonMapper.getInstance();
        ObjectNode node = mapper.createObjectNode();

        // Need first 6 bytes to get preamble, length, type, station id.
        if (bytes.length < 6) {
            log.error("Not enough bytes to decode RTCM.");
            return node;
        }

        // Preamble: 8 bits
        int preamble = unsigned(bytes[0]);
        if (preamble != 0xD3) {
            log.error(String.format("Invalid RTCM preamble: %02X, should be %20X", preamble, 0xD3));
            return node;
        }
        node.put("class", "RTCM3");

        // Next 6 bits should be zero
        int zeroBits = unsigned(bytes[1]) >>> 2;
        if (zeroBits != 0) {
            log.error(String.format("Invalid zero bits: %X", zeroBits));
            return node;
        }

        // Length: 10 bits
        int length = ((unsigned(bytes[1]) & 0x03) << 8) | unsigned(bytes[2]);
        node.put("length", length);

        //  Type: 12 bits
        int type = (unsigned(bytes[3]) << 4) | (unsigned(bytes[4]) >>> 4);
        node.put("type", type);

        // Station ID: 12 bits
        // Get station ID for types known or guessed to have them per gpsd/driver_rtcm3.c
        if (type <= 1013 || type == 1029 || type == 1033 || (type >= 1071 && type <= 1230)) {
            int stationId = ((unsigned(bytes[4]) & 0x0F) << 8) | unsigned(bytes[5]);
            node.put("station_id", stationId);
        }

        return node;
    }

    /**
     * Call the native gpsdecode command line tool to fully decode RTCMs.
     * <p>Full decode requires gpsd-client to be installed on Linux.  Will not work on Windows.</p>
     */
    public static JsonNode fullDecode(byte[] bytes) {
        var pb = new ProcessBuilder(EXECUTABLE);

        String json = null;
        Process process = null;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (var is = process.getInputStream()) {
            try (var out = process.getOutputStream()) {
                out.write(bytes);
                out.flush();
            }
            byte[] outBytes = is.readAllBytes();
            json = new String(outBytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            int exitCode = process.waitFor();
            log.debug("RTCM process exited with code {}", exitCode);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.debug("decode RTCM json: {}", json);
        ObjectMapper mapper = DateJsonMapper.getInstance();
        try {
            return mapper.readValue(json, JsonNode.class);
        } catch (JsonProcessingException e) {
            log.error("Decode RTCM json failed", e);
            var errNode = mapper.createObjectNode();
            errNode.put("error", e.getMessage());
            return errNode;
        }
    }



    public static int unsigned(byte b) {
        return b & 0xFF;
    }

}
