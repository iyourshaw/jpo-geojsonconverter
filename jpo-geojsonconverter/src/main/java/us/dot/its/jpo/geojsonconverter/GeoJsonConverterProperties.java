/*******************************************************************************
 * Copyright 2018 572682
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 ******************************************************************************/
package us.dot.its.jpo.geojsonconverter;

import java.util.Properties;

import jakarta.annotation.PostConstruct;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.processor.LogAndSkipOnInvalidTimestamp;

import us.dot.its.jpo.geojsonconverter.pojos.GeometryOutputMode;
import us.dot.its.jpo.geojsonconverter.standards.MapStandard;
import us.dot.its.jpo.geojsonconverter.standards.RtcmStandard;
import us.dot.its.jpo.geojsonconverter.standards.SpatStandard;

@ConfigurationProperties(prefix = "geojsonconverter")
public class GeoJsonConverterProperties implements EnvironmentAware {

    private static final Logger logger = LoggerFactory.getLogger(GeoJsonConverterProperties.class);

    @Setter
    @Getter
    @Autowired
    private Environment env;

    // General Properties
    @Getter
    private String kafkaBrokers = null;
    private static final String DEFAULT_KAFKA_PORT = "9092";

    // Conluent Properties
    private boolean confluentCloudEnabled = false;
    private String confluentKey = null;
    private String confluentSecret = null;

    // SPAT
    @Setter
    @Getter
    private String kafkaTopicOdeSpatJson = "topic.OdeSpatJson";
    private String kafkaTopicProcessedSpat = "topic.ProcessedSpat";

    // MAP
    @Setter
    @Getter
    private String kafkaTopicOdeMapJson = "topic.OdeMapJson";
    @Setter
    @Getter
    private String kafkaTopicProcessedMap = "topic.ProcessedMap";
    @Setter
    @Getter
    private String kafkaTopicProcessedMapWKT = "topic.ProcessedMapWKT";

    // BSM
    @Setter
    @Getter
    private String kafkaTopicOdeBsmJson = "topic.OdeBsmJson";
    @Setter
    @Getter
    private String kafkaTopicProcessedBsm = "topic.ProcessedBsm";

    // PSM
    @Setter
    @Getter
    private String kafkaTopicOdePsmJson = "topic.OdePsmJson";
    @Setter
    @Getter
    private String kafkaTopicProcessedPsm = "topic.ProcessedPsm";

    // RTCM
    @Getter @Setter private String kafkaTopicOdeRtcmJson;
    @Getter @Setter private String kafkaTopicProcessedRtcm;

    // SRM
    @Getter @Setter private String kafkaTopicOdeSrmJson;
    @Getter @Setter private String kafkaTopicProcessedSrm;

    // SSM
    @Getter @Setter private String kafkaTopicOdeSsmJson;
    @Getter @Setter private String kafkaTopicProcessedSsm;

    private int lingerMs = 0;

    @Getter
    private GeometryOutputMode geometryOutputMode = GeometryOutputMode.GEOJSON_ONLY;

    @Getter
    @Setter
    @Value("${rtcm.full.decode}")
    private boolean rtcmFullDecode;

    @PostConstruct
    public void initialize() {
        if (kafkaBrokers == null) {
            String dockerIp = getEnvironmentVariable("DOCKER_HOST_IP");

            if (dockerIp == null) {
                logger.warn(
                        "Neither spring.kafka.bootstrap-servers property nor DOCKER_HOST_IP environment variable are defined. Defaulting to localhost.");
                dockerIp = "localhost";
            }
            kafkaBrokers = dockerIp + ":" + DEFAULT_KAFKA_PORT;
            logger.warn("spring.kafka.bootstrap-servers property not defined. Will try DOCKER_HOST_IP => {}",
                    kafkaBrokers);
        }

        String kafkaType = getEnvironmentVariable("KAFKA_TYPE");
        if (kafkaType != null) {
            confluentCloudEnabled = kafkaType.equals("CONFLUENT");
            if (confluentCloudEnabled) {
                confluentKey = getEnvironmentVariable("CONFLUENT_KEY");
                confluentSecret = getEnvironmentVariable("CONFLUENT_SECRET");
            }
        }
    }

    // Streams configurations
    @Getter @Setter
    private int streamsConfigReplicationFactor;

    @Getter @Setter
    private String streamsConfigAcks;

    @Getter @Setter
    private int streamsConfigNumStreamThreads;

    @Getter @Setter
    private long streamsConfigCacheMaxBytesBuffering;

    @Getter @Setter
    private int streamsConfigCommitIntervalMs;

    @Getter @Setter
    private MapStandard mapStandardVersion = MapStandard.CTI4501_V1;

    @Getter @Setter
    private SpatStandard spatStandardVersion = SpatStandard.CTI4501_V1;

    @Getter @Setter
    private RtcmStandard rtcmStandardVersion = RtcmStandard.CTI4501_V1;

    public Properties createStreamProperties(String name) {
        Properties streamProps = new Properties();
        streamProps.put(StreamsConfig.APPLICATION_ID_CONFIG, name);

        streamProps.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);

        streamProps.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
                LogAndContinueExceptionHandler.class.getName());
        streamProps.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG,
                LogAndSkipOnInvalidTimestamp.class.getName());

        streamProps.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, streamsConfigNumStreamThreads);

        streamProps.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, streamsConfigReplicationFactor);
        streamProps.put(StreamsConfig.producerPrefix(ProducerConfig.ACKS_CONFIG), streamsConfigAcks);

        // Reduce cache buffering per topology to 1MB
        streamProps.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, streamsConfigCacheMaxBytesBuffering);

        // Decrease default commit interval. Default for 'at least once' mode of 30000ms
        // is too slow.
        streamProps.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, streamsConfigCommitIntervalMs);

        // All the keys are Strings in this app
        streamProps.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        // Configure the state store location
        streamProps.put(StreamsConfig.STATE_DIR_CONFIG, "/var/lib/ode/kafka-streams");

        streamProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "zstd");

        streamProps.put(ProducerConfig.LINGER_MS_CONFIG, getKafkaLingerMs());

        if (confluentCloudEnabled) {
            streamProps.put("ssl.endpoint.identification.algorithm", "https");
            streamProps.put("security.protocol", "SASL_SSL");
            streamProps.put("sasl.mechanism", "PLAIN");

            if (confluentKey != null && confluentSecret != null) {
                String auth = "org.apache.kafka.common.security.plain.PlainLoginModule required " + "username=\""
                        + confluentKey + "\" " + "password=\"" + confluentSecret + "\";";
                streamProps.put("sasl.jaas.config", auth);
            } else {
                logger.error(
                        "Environment variables CONFLUENT_KEY and CONFLUENT_SECRET are not set. Set these in the .env file to use Confluent Cloud");
            }
        }

        return streamProps;
    }

    @Value("${spring.kafka.bootstrap-servers}")
    public void setKafkaBrokers(String kafkaBrokers) {
        this.kafkaBrokers = kafkaBrokers;
    }

    @Override
    public void setEnvironment(Environment environment) {
        env = environment;
    }

    public String getKafkaTopicSpatGeoJson() {
        return kafkaTopicProcessedSpat;
    }

    public void setKafkaTopicSpatGeoJson(String kafkaTopicSpatGeoJson) {
        this.kafkaTopicProcessedSpat = kafkaTopicSpatGeoJson;
    }

    public Boolean getConfluentCloudStatus() {
        return confluentCloudEnabled;
    }

    @Value("${geometry.output.mode}")
    public void setGeometryOutputMode(String gomString) {
        if (GeometryOutputMode.findByName(gomString) != null)
            this.geometryOutputMode = GeometryOutputMode.findByName(gomString);
        else
            this.geometryOutputMode = GeometryOutputMode.GEOJSON_ONLY;
    }


    @Value("${kafka.linger_ms}")
    public void setKafkaLingerMs(int lingerMs) {
        this.lingerMs = lingerMs;
    }

    public int getKafkaLingerMs() {
        return lingerMs;
    }

    private static String getEnvironmentVariable(String variableName) {
        String value = System.getenv(variableName);
        if (value == null || value.equals("")) {
            System.out.println("Something went wrong retrieving the environment variable " + variableName);
        }
        return value;
    }
}
