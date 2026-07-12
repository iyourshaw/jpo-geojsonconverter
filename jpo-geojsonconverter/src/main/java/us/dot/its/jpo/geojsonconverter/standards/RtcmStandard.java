package us.dot.its.jpo.geojsonconverter.standards;

import lombok.Getter;

@Getter
public enum RtcmStandard {
    CTI4501_V1("CTI-4501v1", "CTI-4501 v01.01, June 2022"),
    J3258_DRAFT("J3258", "SAE J3258, Draft 2026-03-11");
    private final String shortName;
    private final String description;
    private RtcmStandard(String shortName, String description) {
        this.shortName = shortName;
        this.description = description;
    }
}
