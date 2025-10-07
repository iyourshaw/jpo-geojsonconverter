package us.dot.its.jpo.geojsonconverter.partitioner;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Utils;
import org.apache.kafka.streams.processor.StreamPartitioner;

import us.dot.its.jpo.geojsonconverter.serialization.serializers.JsonSerializer;

public class RsuTimPartitioner<K, V> implements StreamPartitioner<K, V> {
    @Override
    public Integer partition(String topic, K key, V value, int numPartitions) {
        byte[] partitionBytes = null;

        if (key instanceof RsuTimKey) {
            // If the key is a TIM key, partition on RSU ID first, then packet ID for better distribution
            var rsuTimKey = (RsuTimKey) key;
            if (rsuTimKey.getRsuId() != null && !rsuTimKey.getRsuId().isEmpty()) {
                // Primary partitioning on RSU ID
                partitionBytes = serializeString(topic, rsuTimKey.getRsuId());
            } else if (rsuTimKey.getPacketId() != null && !rsuTimKey.getPacketId().isEmpty()) {
                // Fallback to packet ID if RSU ID is not available
                partitionBytes = serializeString(topic, rsuTimKey.getPacketId());
            }
        }

        // If the key is not a TIM key or doesn't have valid RSU ID/packet ID, partition on the full key object
        if (partitionBytes == null) {
            partitionBytes = serializeObj(topic, key);
        }

        return Utils.toPositive(Utils.murmur2(partitionBytes)) % numPartitions;
    }

    private byte[] serializeString(String topic, String str) {
        byte[] partitionBytes;
        try (var serializer = Serdes.String().serializer()) {
            partitionBytes = serializer.serialize(topic, str);
        }
        return partitionBytes;
    }

    private byte[] serializeObj(String topic, K obj) {
        byte[] partitionBytes;
        try (var serializer = new JsonSerializer<K>()) {
            partitionBytes = serializer.serialize(topic, obj);
        }
        return partitionBytes;
    }
}
