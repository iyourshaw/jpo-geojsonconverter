package us.dot.its.jpo.geojsonconverter.pojos.geojson;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = false)
@JsonInclude(Include.NON_NULL)
public class Point extends Geometry {
    private final double[] coordinates;
    private final double[] bbox;

    public Point(Double longitude, Double latitude) {
        super();
        if (longitude != null && latitude != null) {
            this.coordinates = new double[]{longitude, latitude};
        } else {
            coordinates = null;
        }
        this.bbox = null;
    }

    @JsonCreator
    public Point(@JsonProperty("coordinates") double[] coordinates) {
        super();
        this.coordinates = coordinates;
        this.bbox = null;
    }

    @Override
    public String toString() {
        return "{" + " coordinates='" + Arrays.toString(getCoordinates()) + "'" + ", bbox='" + Arrays.toString(getBbox()) + "'" + "}";
    }

}
