package us.dot.its.jpo.geojsonconverter.converter.tim;

import java.util.List;

/**
 * Data structure to hold elevation and lane width offset information.
 */
public class OffsetInformation {
    private final List<Long> elevationOffsets;
    private final List<Long> laneWidthOffsets;

    public OffsetInformation(List<Long> elevationOffsets, List<Long> laneWidthOffsets) {
        this.elevationOffsets = elevationOffsets;
        this.laneWidthOffsets = laneWidthOffsets;
    }

    public List<Long> getElevationOffsets() {
        return elevationOffsets;
    }

    public List<Long> getLaneWidthOffsets() {
        return laneWidthOffsets;
    }
}
