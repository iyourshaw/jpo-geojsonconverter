package us.dot.its.jpo.geojsonconverter.pojos.geojson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Arrays;

@Getter
@EqualsAndHashCode(callSuper = false)
@JsonInclude(Include.NON_NULL)
public class LineString extends Geometry {
    private final double[][] coordinates;
    private final double[] bbox;

    @JsonCreator
    public LineString(@JsonProperty("coordinates") double [][] coordinates) {
        super();
        this.coordinates = coordinates;
        this.bbox = null;
    }

    @Override
    public String toString() {
        return "{" +
            " coordinates='" + Arrays.deepToString(getCoordinates()) + "'" +
            ", bbox='" + Arrays.toString(getBbox()) + "'" +
            "}";
    }
}
