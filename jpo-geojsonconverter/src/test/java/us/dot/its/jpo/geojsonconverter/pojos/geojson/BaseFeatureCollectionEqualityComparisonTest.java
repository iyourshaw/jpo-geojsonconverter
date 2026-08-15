package us.dot.its.jpo.geojsonconverter.pojos.geojson;

import org.junit.Test;

import us.dot.its.jpo.geojsonconverter.pojos.geojson.bsm.ProcessedBsm;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.bsm.ProcessedBsmCollection;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.connectinglanes.ConnectingLanesFeature;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.connectinglanes.ConnectingLanesFeatureCollection;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.map.MapFeature;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.map.MapFeatureCollection;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.psm.ProcessedPsm;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.psm.ProcessedPsmCollection;

import static org.junit.Assert.*;

@SuppressWarnings({"unchecked", "rawtypes"})
public class BaseFeatureCollectionEqualityComparisonTest {

    @Test
    public void testEquals_distinguishesBsmCollectionFromPsmCollection() {
        ProcessedBsmCollection<Point> bsmCollection = new ProcessedBsmCollection<>(new ProcessedBsm[0]);
        ProcessedPsmCollection<Point> psmCollection = new ProcessedPsmCollection<>(new ProcessedPsm[0]);

        assertNotEquals(bsmCollection, psmCollection);
    }

    @Test
    public void testEquals_distinguishesPsmCollectionFromMapFeatureCollection() {
        ProcessedPsmCollection<Point> psmCollection = new ProcessedPsmCollection<>(new ProcessedPsm[0]);
        MapFeatureCollection<Point> mapFeatureCollection = new MapFeatureCollection<>(new MapFeature[0]);

        assertNotEquals(psmCollection, mapFeatureCollection);
    }

    @Test
    public void testEquals_distinguishesMapFeatureCollectionFromConnectingLanesFeatureCollection() {
        MapFeatureCollection<Point> mapFeatureCollection = new MapFeatureCollection<>(new MapFeature[0]);
        ConnectingLanesFeatureCollection<Point> connectingLanesFeatureCollection =
                new ConnectingLanesFeatureCollection<>(new ConnectingLanesFeature[0]);

        assertNotEquals(mapFeatureCollection, connectingLanesFeatureCollection);
    }
}
