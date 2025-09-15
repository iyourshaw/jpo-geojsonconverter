package us.dot.its.jpo.geojsonconverter.converter.tim;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.kafka.streams.kstream.KStream;

import us.dot.its.jpo.geojsonconverter.partitioner.RsuTimKey;
import us.dot.its.jpo.geojsonconverter.partitioner.RsuTimPartitioner;
import us.dot.its.jpo.geojsonconverter.pojos.tim.DeserializedRawTim;
import us.dot.its.jpo.geojsonconverter.pojos.tim.ProcessedTim;
import us.dot.its.jpo.geojsonconverter.pojos.ProcessedValidationMessage;
import us.dot.its.jpo.geojsonconverter.serialization.JsonSerdes;
import us.dot.its.jpo.geojsonconverter.validator.JsonValidatorResult;
import us.dot.its.jpo.geojsonconverter.validator.TimJsonValidator;
import java.util.ArrayList;
import java.util.List;

/**
 * Kafka Streams Topology builder for processing TIM messages from ODE TIM JSON -> TIM GeoJSON
 */
public class TimTopology {

    private static final Logger logger = LoggerFactory.getLogger(TimTopology.class);

    public static Topology build(String timOdeJsonTopic, String timProcessedJsonTopic,
            TimJsonValidator timJsonValidator) {
        StreamsBuilder builder = new StreamsBuilder();

        // Stream for raw TIM messages
        // Raw topic has no key and the values are raw JSON bytes
        KStream<Void, Bytes> rawOdeTimStream =
                builder.stream(timOdeJsonTopic, Consumed.with(Serdes.Void(), Serdes.Bytes()));

        // Validate the JSON and write validation errors to the log at warn level
        // Passes the raw JSON along unchanged, even if there are validation errors.
        KStream<Void, DeserializedRawTim> validatedOdeTimStream = rawOdeTimStream.mapValues((Void key, Bytes value) -> {
            DeserializedRawTim deserializedRawTim = new DeserializedRawTim();
            try {
                JsonValidatorResult validationResults = timJsonValidator.validate(value.get());
                deserializedRawTim.setOdeTimMessageFrameData(
                        JsonSerdes.OdeMessageFrame().deserializer().deserialize(timOdeJsonTopic, value.get()));
                // Convert ValidationMessage to ProcessedValidationMessage
                List<ProcessedValidationMessage> processedValidationMessages = new ArrayList<>();
                for (com.networknt.schema.ValidationMessage vm : validationResults.getValidationMessages()) {
                    ProcessedValidationMessage pvm = new ProcessedValidationMessage();
                    pvm.setMessage(vm.getMessage());
                    pvm.setSchemaPath(vm.getSchemaPath());
                    pvm.setJsonPath(vm.getPath());
                    processedValidationMessages.add(pvm);
                }
                deserializedRawTim.setValidatorResults(processedValidationMessages);
                logger.debug(validationResults.describeResults());
            } catch (Exception e) {
                JsonValidatorResult validatorResult = new JsonValidatorResult();

                validatorResult.addException(e);
                deserializedRawTim.setValidationFailure(true);
                // Convert ValidationMessage to ProcessedValidationMessage for exceptions
                List<ProcessedValidationMessage> processedValidationMessages = new ArrayList<>();
                for (com.networknt.schema.ValidationMessage vm : validatorResult.getValidationMessages()) {
                    ProcessedValidationMessage pvm = new ProcessedValidationMessage();
                    pvm.setMessage(vm.getMessage());
                    pvm.setSchemaPath(vm.getSchemaPath());
                    pvm.setJsonPath(vm.getPath());
                    processedValidationMessages.add(pvm);
                }
                deserializedRawTim.setValidatorResults(processedValidationMessages);
                deserializedRawTim.setFailedMessage(e.getMessage());

                logger.error("Error in timValidation:", e);
            }
            return deserializedRawTim;
        });

        // Convert ODE TIM to ProcessedTim which is not GeoJSON
        KStream<RsuTimKey, ProcessedTim> processedJsonTimStream =
                validatedOdeTimStream.transform(() -> new TimProcessedJsonConverter());

        processedJsonTimStream.to(
                // Push the ProcessedTim to the output topic partitioned by RsuTimKey
                timProcessedJsonTopic, Produced.with(JsonSerdes.RsuTimKey(), JsonSerdes.ProcessedTim(),
                        new RsuTimPartitioner<RsuTimKey, ProcessedTim>()));

        return builder.build();
    }
}
