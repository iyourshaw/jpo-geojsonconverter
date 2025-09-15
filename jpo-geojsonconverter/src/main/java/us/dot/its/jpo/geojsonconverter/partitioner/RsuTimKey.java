package us.dot.its.jpo.geojsonconverter.partitioner;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka key for TIM messages. Partition on RSU ID, with TIM-specific fields for better message organization.
 */
@Data
@NoArgsConstructor
public class RsuTimKey implements RsuIdKey {

    private String rsuId;
    private String packetId;
    private Integer msgCnt;

    public RsuTimKey(String rsuId, String packetId, Integer msgCnt) {
        this.rsuId = rsuId;
        this.packetId = packetId;
        this.msgCnt = msgCnt;
    }

    /**
     * Create a key from RSU ID and packet ID only
     */
    public RsuTimKey(String rsuId, String packetId) {
        this.rsuId = rsuId;
        this.packetId = packetId;
        this.msgCnt = null;
    }

    /**
     * Create a key from RSU ID only (for error cases)
     */
    public RsuTimKey(String rsuId) {
        this.rsuId = rsuId;
        this.packetId = null;
        this.msgCnt = null;
    }

    @Override
    public String toString() {
        return "{" + " rsuId='" + getRsuId() + "'" + ", packetId='" + getPacketId() + "'" + ", msgCnt='" + getMsgCnt()
                + "'" + "}";
    }
}
