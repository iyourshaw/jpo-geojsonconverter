package us.dot.its.jpo.geojsonconverter.converter.rtcm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import us.dot.its.jpo.asn.j2735.r2024.Common.Heading;
import us.dot.its.jpo.asn.j2735.r2024.Common.PositionConfidenceSet;
import us.dot.its.jpo.asn.j2735.r2024.Common.PositionalAccuracy;
import us.dot.its.jpo.asn.j2735.r2024.Common.RTCMheader;
import us.dot.its.jpo.asn.j2735.r2024.Common.RegionalExtension;
import us.dot.its.jpo.asn.j2735.r2024.Common.SpeedandHeadingandThrottleConfidence;
import us.dot.its.jpo.asn.j2735.r2024.Common.TimeConfidence;
import us.dot.its.jpo.asn.j2735.r2024.Common.TransmissionAndSpeed;
import us.dot.its.jpo.asn.j2735.r2024.RTCMcorrections.RTCMcorrections;
import us.dot.its.jpo.asn.j2735.r2024.RTCMcorrections.RTCMcorrectionsMessageFrame;
import us.dot.its.jpo.geojsonconverter.GeoJsonConverterProperties;
import us.dot.its.jpo.geojsonconverter.pojos.ProcessedValidationMessage;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.rtcm.ProcessedRTCM;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.rtcm.RTCMProperties;
import us.dot.its.jpo.geojsonconverter.standards.RtcmStandard;
import us.dot.its.jpo.geojsonconverter.validator.JsonValidatorResult;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import com.networknt.schema.Error;

@Slf4j
public class RTCMConverterTest {

    private final static ObjectMapper mapper = new ObjectMapper();



    @Test
    public void testProcessRtcm_Cti4501Valid() throws JsonProcessingException {
        RTCMConverter converter = getConverterV1();
        RTCMcorrectionsMessageFrame messageFrame = buildValidCti4501V1Frame();
        ProcessedRTCM processedRtcm = converter.processRTCM(messageFrame);
        assertThat(processedRtcm, notNullValue());
        RTCMProperties properties = processedRtcm.getProperties();
        assertThat(properties, notNullValue());
        assertThat(properties.getMsgCnt(), equalTo(82));

        assertThat(properties.getRev(), equalTo("rtcmRev3"));
        assertThat(properties.getValidationMessages(), hasSize(equalTo(0)));
        assertThat(properties.isCti4501Conformant(), equalTo(true));
        assertThat(properties.getUtcTime(), equalTo(1753482274168L));

        log.info(mapper.writeValueAsString(processedRtcm));
    }

    @Test
    public void testProcessRtcm_J3258Valid() throws JsonProcessingException {
        RTCMConverter converter = getConverterJ3258();
        RTCMcorrectionsMessageFrame messageFrame = buildValidJ3258Frame();
        ProcessedRTCM processedRtcm = converter.processRTCM(messageFrame);
        assertThat(processedRtcm, notNullValue());
        RTCMProperties properties = processedRtcm.getProperties();
        assertThat(properties, notNullValue());
        assertThat(properties.getMsgCnt(), equalTo(124));
        assertThat(properties.getRev(), equalTo("rtcmRev3"));
        assertThat(properties.getValidationMessages(), hasSize(equalTo(0)));
        assertThat(properties.isCti4501Conformant(), equalTo(true));
        log.info(mapper.writeValueAsString(processedRtcm));
    }

    @Test
    public void testProcessRtcm_InvalidWithMissingDateTime() throws JsonProcessingException {
        final String RTCM = """
        {
            "messageId": 28,
            "value": {
                "RTCMcorrections": {
                    "msgCnt": 82,
                    "rev": "rtcmRev3",
                    "anchorPoint": {
                        "long": -1115094349,
                        "lat": 406603360,
                        "elevation": 20630
                    },
                    "msgs": [
                        "D300133ED980037BDD1A80C6358121DD4E499FFC6712E91F0D",
                        "D3003E409980144144564E554C4C414E54454E4E4120204E4F4E4500000D5452494D424C4520414C4C4F59144E617620352E3434202F20426F6F7420352E3434008C65F9"
                    ]
                }
            }
        }
        """;
        RTCMConverter converter = getConverterV1();
        RTCMcorrectionsMessageFrame messageFrame = mapper.readValue(RTCM, RTCMcorrectionsMessageFrame.class);
        String str = mapper.writeValueAsString(messageFrame);
        log.info(str);
        ProcessedRTCM processedRtcm = converter.processRTCM(messageFrame);
        assertThat(processedRtcm, notNullValue());
        RTCMProperties properties = processedRtcm.getProperties();
        assertThat(properties, notNullValue());
        assertThat(properties.getMsgCnt(), equalTo(82));
        assertThat(properties.getValidationMessages(), hasSize(greaterThanOrEqualTo(1)));

        Set<String> messages = properties.getValidationMessages().stream()
            .map(ProcessedValidationMessage::getMessage)
            .collect(Collectors.toSet());
        assertThat(messages, hasItems(containsString("DDateTime")));
        assertThat(properties.isCti4501Conformant(), equalTo(false));

        log.info(mapper.writeValueAsString(processedRtcm));
    }



    @Test
    public void testProcessRtcm_Rev2Invalid() throws JsonProcessingException {
        final String RTCM_REV2_INVALID = """
            {
              "messageId": 28,
              "value": {
                "RTCMcorrections": {
                  "msgCnt": 82,
                  "rev": "rtcmRev2",
                  "timeStamp": 527040,
                  "msgs": [
                    "66300D0A597E7D7C5A7963686F7D4B4278774F4068414565405145404E6A73775F486841406865534164645A40704779467F7E44405E72517B4F7E727F7F4E6E5342544F47404043517F405145406B4D4D47507E437C7F7F7A59467C7C60466E5B75664B406460657F637D624E7F634E60536670715F4F4740604968734352654040594253595F777E7F70584F475068747C5F767159567E7C5C79634C4A5E546F564B7C675D6E776559627C7C7A465C7D71635577784F404060707F534F7C7F714A665D434360465C55775E6A48476C78767F7F7F646F42757F5F4040505A6C7F7F7B5F564C704043534C6A6F5263534F7E7F530D0A"
                  ]
                }
              }
            }
            """;
        RTCMConverter converter = getConverterV1();
        RTCMcorrectionsMessageFrame messageFrame = mapper.readValue(RTCM_REV2_INVALID, RTCMcorrectionsMessageFrame.class);
        String str = mapper.writeValueAsString(messageFrame);
        log.info(str);
        ProcessedRTCM processedRtcm = converter.processRTCM(messageFrame);
        assertThat(processedRtcm, notNullValue());
        RTCMProperties properties = processedRtcm.getProperties();
        assertThat(properties, notNullValue());
        assertThat(properties.getMsgCnt(), equalTo(82));

        assertThat(properties.getValidationMessages(), hasSize(greaterThanOrEqualTo(1)));
        Set<String> messages = properties.getValidationMessages().stream()
            .map(ProcessedValidationMessage::getMessage)
            .collect(Collectors.toSet());
        assertThat(messages, hasItems(containsString("DE_RTCM_Revision")));
        assertThat(properties.isCti4501Conformant(), equalTo(false));

        log.info(mapper.writeValueAsString(processedRtcm));
    }

    @Test
    public void testProcessRtcm_RTCMFrameNull()  {
        RTCMConverter converter = getConverterV1();

        ProcessedRTCM processedRtcm = converter.processRTCM(null);

        assertThat(processedRtcm, notNullValue());
        RTCMProperties properties = processedRtcm.getProperties();
        assertThat(properties, notNullValue());
        assertThat(properties.getMsgCnt(), equalTo(0));
        assertThat(properties.getMessages(), empty());
        assertThat(properties.getValidationMessages(), empty());
        assertThat(properties.isCti4501Conformant(), equalTo(true));
    }

    @Test
    public void testProcessRtcm_RTCMcorrectionsNULL() {
        RTCMConverter converter = getConverterV1();
        RTCMcorrectionsMessageFrame messageFrame = new RTCMcorrectionsMessageFrame();
        messageFrame.setValue(null);

        ProcessedRTCM processedRtcm = converter.processRTCM(messageFrame);

        assertThat(processedRtcm, notNullValue());
        RTCMProperties properties = processedRtcm.getProperties();
        assertThat(properties, notNullValue());
        assertThat(properties.getMsgCnt(), equalTo(0));
        assertThat(properties.getMessages(), empty());
        assertThat(properties.getValidationMessages(), empty());
        assertThat(properties.isCti4501Conformant(), equalTo(true));
    }

    @Test
    public void testProcessRtcm_rtcmHeaderPresent() throws JsonProcessingException {
        RTCMConverter converter = getConverterV1();
        RTCMcorrectionsMessageFrame messageFrame = buildValidCti4501V1Frame();
        messageFrame.getValue().setRtcmHeader(new RTCMheader());

        ProcessedRTCM processedRtcm = converter.processRTCM(messageFrame);

        assertThat(processedRtcm, notNullValue());
        RTCMProperties properties = processedRtcm.getProperties();
        assertThat(properties, notNullValue());
        assertThat(properties.getValidationMessages(), hasSize(greaterThanOrEqualTo(1)));
        Set<String> messages = properties.getValidationMessages().stream()
            .map(ProcessedValidationMessage::getMessage)
            .collect(Collectors.toSet());
        assertThat(messages, hasItems(containsString("rtcmHeader")));
        assertThat(properties.isCti4501Conformant(), equalTo(false));

    }

    @Test
    public void testProcessRtcm_MessageListNull() throws JsonProcessingException {
        RTCMConverter converter = getConverterV1();
        RTCMcorrectionsMessageFrame messageFrame = buildValidCti4501V1Frame();
        messageFrame.getValue().setMsgs(null);

        ProcessedRTCM processedRtcm = converter.processRTCM(messageFrame);

        assertThat(processedRtcm, notNullValue());
        RTCMProperties properties = processedRtcm.getProperties();
        assertThat(properties, notNullValue());
        assertThat(properties.getMsgCnt(), equalTo(82));
        assertThat(properties.getMessages(), empty());
        assertThat(properties.getValidationMessages(), hasSize(equalTo(0)));
        assertThat(properties.isCti4501Conformant(), equalTo(true));

    }

    @Test
    public void testProcessRtcm_RegionalExtensionsPresent() throws JsonProcessingException {
        RTCMConverter converter = getConverterV1();
        RTCMcorrectionsMessageFrame messageFrame = buildValidCti4501V1Frame();
        RTCMcorrections.SequenceOfRegional regional = new RTCMcorrections.SequenceOfRegional();
        regional.add(new RegionalExtension<>(1, "test") {});
        messageFrame.getValue().setRegional(regional);

        ProcessedRTCM processedRtcm = converter.processRTCM(messageFrame);

        assertThat(processedRtcm, notNullValue());
        RTCMProperties properties = processedRtcm.getProperties();
        assertThat(properties, notNullValue());
        assertThat(properties.getValidationMessages(), hasSize(greaterThanOrEqualTo(1)));
        Set<String> messages = properties.getValidationMessages().stream()
            .map(ProcessedValidationMessage::getMessage)
            .collect(Collectors.toSet());
        assertThat(messages, hasItems(containsString("regional extensions")));
        assertThat(properties.isCti4501Conformant(), equalTo(false));

    }

    @Test
    public void testProcessRtcm_FullPositionVector_LongitudeMissing() throws JsonProcessingException {
        RTCMConverter converter = getConverterV1();
        RTCMcorrectionsMessageFrame messageFrame = buildValidCti4501V1Frame();
        messageFrame.getValue().getAnchorPoint().setLong_(null);

        ProcessedRTCM processedRtcm = converter.processRTCM(messageFrame);

        assertThat(processedRtcm, notNullValue());
        RTCMProperties properties = processedRtcm.getProperties();
        assertThat(properties, notNullValue());
        assertThat(properties.getValidationMessages(), hasSize(greaterThanOrEqualTo(1)));
        Set<String> messages = properties.getValidationMessages().stream()
            .map(ProcessedValidationMessage::getMessage)
            .collect(Collectors.toSet());
        assertThat(messages, hasItems(containsString("'long' field")));
        assertThat(properties.isCti4501Conformant(), equalTo(false));
    }

    @Test
    public void testProcessRtcm_FullPositionVector_LatitudeMissing() throws JsonProcessingException {
        RTCMConverter converter = getConverterV1();
        RTCMcorrectionsMessageFrame messageFrame = buildValidCti4501V1Frame();
        messageFrame.getValue().getAnchorPoint().setLat(null);

        ProcessedRTCM processedRtcm = converter.processRTCM(messageFrame);

        assertThat(processedRtcm, notNullValue());
        RTCMProperties properties = processedRtcm.getProperties();
        assertThat(properties, notNullValue());
        assertThat(properties.getValidationMessages(), hasSize(greaterThanOrEqualTo(1)));
        Set<String> messages = properties.getValidationMessages().stream()
            .map(ProcessedValidationMessage::getMessage)
            .collect(Collectors.toSet());
        assertThat(messages, hasItems(containsString("'lat' field")));
        assertThat(properties.isCti4501Conformant(), equalTo(false));
    }

    @Test
    public void testProcessRtcm_FullPositionVector_ElevationMissing() throws JsonProcessingException {
        RTCMConverter converter = getConverterV1();
        RTCMcorrectionsMessageFrame messageFrame = buildValidCti4501V1Frame();
        messageFrame.getValue().getAnchorPoint().setElevation(null);

        ProcessedRTCM processedRtcm = converter.processRTCM(messageFrame);

        assertThat(processedRtcm, notNullValue());
        RTCMProperties properties = processedRtcm.getProperties();
        assertThat(properties, notNullValue());
        assertThat(properties.getValidationMessages(), hasSize(greaterThanOrEqualTo(1)));
        Set<String> messages = properties.getValidationMessages().stream()
            .map(ProcessedValidationMessage::getMessage)
            .collect(Collectors.toSet());
        assertThat(messages, hasItems(containsString("'elevation' field")));
        assertThat(properties.isCti4501Conformant(), equalTo(false));
    }

    @Test
    public void testProcessRtcm_FullPositionVector_HeadingPresent() throws JsonProcessingException {
        RTCMcorrectionsMessageFrame messageFrame = buildValidCti4501V1Frame();
        messageFrame.getValue().getAnchorPoint().setHeading(new Heading(0));
        assertForbiddenFullPositionField(messageFrame, "'heading' field");
    }

    @Test
    public void testProcessRtcm_FullPositionVector_SpeedPresent() throws JsonProcessingException {
        RTCMcorrectionsMessageFrame messageFrame = buildValidCti4501V1Frame();
        messageFrame.getValue().getAnchorPoint().setSpeed(new TransmissionAndSpeed());
        assertForbiddenFullPositionField(messageFrame, "'speed' field");
    }

    @Test
    public void testProcessRtcm_FullPositionVector_PosAccuracyPresent() throws JsonProcessingException {
        RTCMcorrectionsMessageFrame messageFrame = buildValidCti4501V1Frame();
        messageFrame.getValue().getAnchorPoint().setPosAccuracy(new PositionalAccuracy());
        assertForbiddenFullPositionField(messageFrame, "'posAccuracy' field");
    }

    @Test
    public void testProcessRtcm_FullPositionVector_TimeConfidencePresent() throws JsonProcessingException {
        RTCMcorrectionsMessageFrame messageFrame = buildValidCti4501V1Frame();
        messageFrame.getValue().getAnchorPoint().setTimeConfidence(TimeConfidence.UNAVAILABLE);
        assertForbiddenFullPositionField(messageFrame, "'timeConfidence' field");
    }

    @Test
    public void testProcessRtcm_FullPositionVector_PosConfidencePresent() throws JsonProcessingException {
        RTCMcorrectionsMessageFrame messageFrame = buildValidCti4501V1Frame();
        messageFrame.getValue().getAnchorPoint().setPosConfidence(new PositionConfidenceSet());
        assertForbiddenFullPositionField(messageFrame, "'posConfidence' field");
    }

    @Test
    public void testProcessRtcm_FullPositionVector_SpeedConfidencePresent() throws JsonProcessingException {
        RTCMcorrectionsMessageFrame messageFrame = buildValidCti4501V1Frame();
        messageFrame.getValue().getAnchorPoint().setSpeedConfidence(new SpeedandHeadingandThrottleConfidence());
        assertForbiddenFullPositionField(messageFrame, "'speedConfidence' field");
    }

    @Test
    public void testJsonValidation() {
        var result = new JsonValidatorResult();
        result.addException(new Exception("test"));
        result.addValidationMessages(List.of(Error.builder().message("test").build()));
        RTCMProperties properties = new RTCMProperties();
        RTCMConverter converter = getConverterV1();

        converter.jsonValidation(properties, result);

        assertThat(properties.getValidationMessages(), hasSize(equalTo(2)));
        assertThat(properties.getValidationMessages().getFirst().getMessage(), equalTo("test"));
        List<String> exceptions = properties.getValidationMessages().stream()
                .map(ProcessedValidationMessage::getException)
                .filter(Objects::nonNull)
                .toList();
        assertThat(exceptions, hasSize(equalTo(1)));
    }


    private RTCMConverter getConverterV1() {
        RTCMDecoder decoder = new RTCMDecoder(false);
        var properties = new GeoJsonConverterProperties();
        properties.setRtcmStandardVersion(RtcmStandard.CTI4501_V1);
        return new RTCMConverter(decoder, properties);
    }

    private RTCMConverter getConverterJ3258() {
        RTCMDecoder decoder = new RTCMDecoder(false);
        var properties = new GeoJsonConverterProperties();
        properties.setRtcmStandardVersion(RtcmStandard.J3258_DRAFT);
        return new RTCMConverter(decoder, properties);
    }

    private RTCMcorrectionsMessageFrame buildValidCti4501V1Frame() throws JsonProcessingException {
        return mapper.readValue(RTCM_CTI4501_V1_VALID, RTCMcorrectionsMessageFrame.class);
    }

    private RTCMcorrectionsMessageFrame buildValidJ3258Frame() throws JsonProcessingException {
        return mapper.readValue(RTCM_J3258_VALID, RTCMcorrectionsMessageFrame.class);
    }

    private void assertForbiddenFullPositionField(RTCMcorrectionsMessageFrame messageFrame, String expectedMessageFragment) {
        RTCMConverter converter = getConverterV1();

        ProcessedRTCM processedRtcm = converter.processRTCM(messageFrame);

        assertThat(processedRtcm, notNullValue());
        RTCMProperties properties = processedRtcm.getProperties();
        assertThat(properties, notNullValue());
        assertThat(properties.getValidationMessages(), hasSize(greaterThanOrEqualTo(1)));
        Set<String> messages = properties.getValidationMessages().stream()
            .map(ProcessedValidationMessage::getMessage)
            .collect(Collectors.toSet());
        assertThat(messages, hasItems(containsString(expectedMessageFragment)));
        assertThat(properties.isCti4501Conformant(), equalTo(false));
    }


    private final static String RTCM_CTI4501_V1_VALID = """
        {
            "messageId": 28,
            "value": {
                "RTCMcorrections": {
                    "msgCnt": 82,
                    "rev": "rtcmRev3",
                    "anchorPoint": {
                        "utcTime": {
                            "year": 2025,
                            "month": 7,
                            "day": 25,
                            "hour": 16,
                            "minute": 24,
                            "second": 34168,
                            "offset": -360
                        },
                        "long": -1115094349,
                        "lat": 406603360,
                        "elevation": 20630
                    },
                    "msgs": [
                        "D300133ED980037BDD1A80C6358121DD4E499FFC6712E91F0D",
                        "D3003E409980144144564E554C4C414E54454E4E4120204E4F4E4500000D5452494D424C4520414C4C4F59144E617620352E3434202F20426F6F7420352E3434008C65F9"
                    ]
                }
            }
        }
        """;

    private final static String RTCM_J3258_VALID = """
            {
                "messageId": 28,
                "value": {
                    "RTCMcorrections": {
                        "msgCnt": 124,
                        "rev": "rtcmRev3",
                        "msgs": [
                            "D300153EE00103012E3ADE9E350FDE024F89F13209210000393065D300DA43200175A4B302002084012D060000000020210100777FFFF7A223A622A528A229B0CD89FB6831753E7D27E61DCC07988CFD19FC23FC229E85638AD817B82BC05938B3917EFCC5399AF347E8C07810F759F084047A07B406682CFA57F9DFF547EE0F4CE33D79CFF5E74846D201324FC4C94005C87C1977F065E081F84B0817E8281A90A06A82BE56FA5FDBEC675FB19D3F06230F23083EF3A0FBCFC42E82F46F57D1F6FF47DBFFAB42FF73C3FDCF0FFB84FFFFFFFFFFFFFFFFFFFBBBBFFFFFFC0000001A69AE38614C93D98E5B7375D9692454DB9E7A4B25170071F5D9D300EC44600175A4B30000209185052000000000200111007FFFFFFFACAAA8A72D2DAC28FF92E67FCF01D0C42BC9ED46DCADBAC3789617CC5218B5319914D12A425588ADF005201DE04FC0CB8BB81A6036086ED6862D385A86353F175630906248C78621EC4B78A7717D7B848BF08177C306FF140A792DCDE99A7FA8543EA96C85CF5C1A9EF86E8521B9F580711A02ECF00D5B2039C88275820A18E82804E0A9827987A9E6355F98A8FE6DC585C47217E46062A3018A04791363E69A8F9D111E7A0D7FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF80000000577616DD8E9867AF1AEBBF5BD55E175155D5D96E1663A6F98044DBA6"
                        ]
                    }
                }
            }
            """;
}
