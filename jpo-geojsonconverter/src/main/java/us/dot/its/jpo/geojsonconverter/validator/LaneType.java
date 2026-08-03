package us.dot.its.jpo.geojsonconverter.validator;

/**
 * Lane type for use by {@link CTI4501Validator}
 */
public enum LaneType {
    BIKE_LANE,
    VEHICLE_LANE,
    TRACKED_VEHICLE_LANE,
    CROSSWALK,
    SIDEWALK,
    PARKING,
    MEDIAN,
    STRIPING,
    UNKNOWN;
    public String getDescription() {
        return this.name().toLowerCase().replace('_', ' ');
    }
}
