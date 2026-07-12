package us.dot.its.jpo.geojsonconverter.standards;

import lombok.Getter;

@Getter
public enum MapStandard{
    CTI4501_V1("CTI-4501v1", "CTI-4501 v01.01, June 2022"),
    CTI4501_V2_DRAFT("CTI-4501v2", "CTI-4501/2, Draft 2026-01-05");
    private final String shortName;
    private final String description;
    private MapStandard(String shortName, String description) {
        this.shortName = shortName;
        this.description = description;
    }
}
