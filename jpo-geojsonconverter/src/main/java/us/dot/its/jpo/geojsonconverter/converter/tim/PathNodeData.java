package us.dot.its.jpo.geojsonconverter.converter.tim;

import java.util.List;

/**
 * Data structure to hold coordinate and offset information for TIM path processing.
 */
public class PathNodeData {
    private final List<Double> coordinates;
    private final Long dwithOffset;
    private final Long delevationOffset;

    public PathNodeData(List<Double> coordinates, Long dwithOffset, Long delevationOffset) {
        this.coordinates = coordinates;
        this.dwithOffset = dwithOffset;
        this.delevationOffset = delevationOffset;
    }

    public List<Double> getCoordinates() {
        return coordinates;
    }

    public Long getDwithOffset() {
        return dwithOffset;
    }

    public Long getDelevationOffset() {
        return delevationOffset;
    }
}
