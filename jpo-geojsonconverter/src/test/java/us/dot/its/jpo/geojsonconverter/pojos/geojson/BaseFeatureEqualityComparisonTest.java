package us.dot.its.jpo.geojsonconverter.pojos.geojson;

import org.junit.Test;

import us.dot.its.jpo.geojsonconverter.pojos.geojson.bsm.ProcessedBsm;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.connectinglanes.ConnectingLanesFeature;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.map.MapFeature;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.psm.ProcessedPsm;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.rtcm.ProcessedRTCM;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.srm.ProcessedSrm;

import static org.junit.Assert.*;

public class BaseFeatureEqualityComparisonTest {

    @Test
    public void testEquals_distinguishesRtcmFromSrm() {
        ProcessedRTCM rtcm = new ProcessedRTCM(null, null);
        ProcessedSrm srm = new ProcessedSrm(null, null);

        assertNotEquals(rtcm, srm);
    }

    @Test
    public void testEquals_distinguishesSrmFromConnectingLanesFeature() {
        ProcessedSrm srm = new ProcessedSrm(null, null);
        ConnectingLanesFeature<Point> connectingLanesFeature = new ConnectingLanesFeature<>(null, null, null);

        assertNotEquals(srm, connectingLanesFeature);
    }

    @Test
    public void testEquals_distinguishesConnectingLanesFeatureFromProcessedBsm() {
        ConnectingLanesFeature<Point> connectingLanesFeature = new ConnectingLanesFeature<>(null, null, null);
        ProcessedBsm<Point> bsm = new ProcessedBsm<>(null, null, null);

        assertNotEquals(connectingLanesFeature, bsm);
    }

    @Test
    public void testEquals_distinguishesProcessedBsmFromMapFeature() {
        ProcessedBsm<Point> bsm = new ProcessedBsm<>(null, null, null);
        MapFeature<Point> mapFeature = new MapFeature<>(null, null, null);

        assertNotEquals(bsm, mapFeature);
    }

    @Test
    public void testEquals_distinguishesMapFeatureFromProcessedPsm() {
        MapFeature<Point> mapFeature = new MapFeature<>(null, null, null);
        ProcessedPsm<Point> psm = new ProcessedPsm<>(null, null, null);

        assertNotEquals(mapFeature, psm);
    }
}
