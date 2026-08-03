package us.dot.its.jpo.geojsonconverter.validator;

import static us.dot.its.jpo.geojsonconverter.validator.LaneType.*;

/**
 * Descriptive info for a lane for use by {@link CTI4501Validator}
 * @param laneId
 * @param laneType
 * @param isIngress
 */
public record LaneDescription(
        Long laneId,
        LaneType laneType,
        boolean isIngress
){
    public String ingressOrEgress() {
        return isIngress ? "ingress" : "egress";
    }
    public boolean isBikeLane(){
        return laneType == BIKE_LANE;
    }
    public boolean isVehicleLane(){
        return laneType == VEHICLE_LANE;
    }
    public boolean isTrackedVehicleLane(){
        return laneType == TRACKED_VEHICLE_LANE;
    }
    public boolean isCrosswalk(){
        return laneType == CROSSWALK;
    }
    public boolean isSidewalk(){
        return laneType == SIDEWALK;
    }
    public boolean isParking(){
        return laneType == PARKING;
    }
    public boolean isStriping(){
        return laneType == STRIPING;
    }
    public boolean isMedian(){
        return laneType == MEDIAN;
    }
}
