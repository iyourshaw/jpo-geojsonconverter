package us.dot.its.jpo.geojsonconverter.serialization;

import us.dot.its.jpo.geojsonconverter.geojson.map.MapFeatureCollection;
import us.dot.its.jpo.geojsonconverter.geojson.spat.SpatFeatureCollection;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.*;
import us.dot.its.jpo.geojsonconverter.serialization.serializers.*;
import us.dot.its.jpo.ode.model.OdeMapData;
import us.dot.its.jpo.ode.model.OdeSpatData;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;

/**
 * Contains static methods that return a "Serde", a serializer/deserializer for
 * JSON for Kafka, for each POJO type.
 */
public class JsonSerdes {
    public static Serde<OdeMapData> OdeMap() {
        return Serdes.serdeFrom(
            new JsonSerializer<OdeMapData>(), 
            new OdeMapDataJsonDeserializer());
    }

    public static Serde<MapFeatureCollection> MapGeoJson() {
        return Serdes.serdeFrom(
            new JsonSerializer<MapFeatureCollection>(), 
            new JsonDeserializer<>(MapFeatureCollection.class));
    }

    public static Serde<OdeSpatData> OdeSpat() {
        return Serdes.serdeFrom(
            new JsonSerializer<OdeSpatData>(), 
            new OdeSpatDataJsonDeserializer());
    }

    public static Serde<SpatFeatureCollection> SpatGeoJson() {
        return Serdes.serdeFrom(
            new JsonSerializer<SpatFeatureCollection>(), 
            new JsonDeserializer<>(SpatFeatureCollection.class));
    }
}
