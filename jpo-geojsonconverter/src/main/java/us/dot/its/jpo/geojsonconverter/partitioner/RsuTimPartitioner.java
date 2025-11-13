package us.dot.its.jpo.geojsonconverter.partitioner;

import org.apache.kafka.common.utils.Utils;
import org.apache.kafka.streams.processor.StreamPartitioner;
import us.dot.its.jpo.geojsonconverter.serialization.serializers.JsonSerializer;

import java.nio.charset.StandardCharsets;

public class RsuTimPartitioner<K, V> implements StreamPartitioner<K, V> {

    private static final JsonSerializer<Object> JSON_SERIALIZER = new JsonSerializer<>();

    @Override
    public Integer partition(String topic, K key, V value, int numPartitions) {
        byte[] partitionBytes = null;

        if (key instanceof RsuTimKey) {
            var rsuTimKey = (RsuTimKey) key;
            if (rsuTimKey.getRsuId() != null && !rsuTimKey.getRsuId().isEmpty()) {
                partitionBytes = rsuTimKey.getRsuId().getBytes(StandardCharsets.UTF_8);
            } else if (rsuTimKey.getPacketId() != null && !rsuTimKey.getPacketId().isEmpty()) {
                partitionBytes = rsuTimKey.getPacketId().getBytes(StandardCharsets.UTF_8);
            }
        }

        if (partitionBytes == null) {
            partitionBytes = JSON_SERIALIZER.serialize(topic, key);
        }

        return Utils.toPositive(Utils.murmur2(partitionBytes)) % numPartitions;
    }
}
