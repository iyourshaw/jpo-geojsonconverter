package us.dot.its.jpo.ode.plugin.j2735.builders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.json.JSONObject;
import org.json.XML;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import us.dot.its.jpo.ode.util.JsonUtils;
import us.dot.its.jpo.ode.util.JsonUtils.JsonUtilsException;

public class TravelerMessageFromHumanToAsnConverterTest {

   @Test
   public void testRealJson() throws JsonProcessingException, IOException, JsonUtilsException {
      ObjectNode inputTID = JsonUtils.toObjectNode(
            "{ \"tim\": { \"packetID\": \"2\", \"msgCnt\": \"1\", \"timeStamp\": \"2017-12-01T17:47:11-05:00\", \"urlB\": \"null\", \"dataframes\": [ { \"startDateTime\": \"2017-08-02T22:25:00.000Z\", \"durationTime\": 1, \"frameType\": \"1\", \"sspTimRights\": \"0\", \"msgID\": \"RoadSignID\", \"position\": { \"latitude\": \"41.678473\", \"longitude\": \"-108.782775\", \"elevation\": \"917.1432\" }, \"viewAngle\": \"1010101010101010\", \"mutcd\": \"5\", \"crc\": \"0000000000000000\", \"priority\": \"0\", \"sspLocationRights\": \"3\", \"regions\": [ { \"name\": \"Testing TIM\", \"regulatorID\": \"0\", \"segmentID\": \"33\", \"anchorPosition\": { \"latitude\": \"41.2500807\", \"longitude\": \"-111.0093847\", \"elevation\": \"2020.6969900289998\" }, \"laneWidth\": \"7\", \"directionality\": \"3\", \"closedPath\": \"false\", \"description\": \"path\", \"path\": { \"scale\": \"0\", \"type\": \"ll\", \"nodes\": [ { \"nodeLong\": \"0.0031024\", \"nodeLat\": \"0.0014506\", \"delta\": \"node-LL3\" }, { \"nodeLong\": \"0.0030974\", \"nodeLat\": \"0.0014568\", \"delta\": \"node-LL3\" }, { \"nodeLong\": \"0.0030983\", \"nodeLat\": \"0.0014559\", \"delta\": \"node-LL3\" }, { \"nodeLong\": \"0.0030980\", \"nodeLat\": \"0.0014563\", \"delta\": \"node-LL3\" }, { \"nodeLong\": \"0.0030982\", \"nodeLat\": \"0.0014562\", \"delta\": \"node-LL3\" } ] }, \"direction\": \"0000000000001010\" } ], \"sspMsgTypes\": \"2\", \"sspMsgContent\": \"3\", \"content\": \"Advisory\", \"items\": [\"513\",\"some text\", \"'98765\"], \"url\": \"null\" } ] }, \"rsus\": [ { \"rsuIndex\": \"1\", \"rsuTarget\": \"192.168.1.1\", \"rsuUsername\": \"user\", \"rsuPassword\": \"password\", \"rsuRetries\": \"1\", \"rsuTimeout\": \"2000\" } ], \"snmp\": { \"rsuid\": \"00000083\", \"msgid\": \"31\", \"mode\": \"1\", \"channel\": \"178\", \"interval\": \"2\", \"deliverystart\": \"2017-06-01T17:47:11-05:00\", \"deliverystop\": \"2018-01-01T17:47:11-05:15\", \"enable\": \"1\", \"status\": \"4\" } }");
      TravelerMessageFromHumanToAsnConverter
            .convertTravelerInputDataToEncodableTim(inputTID);

      ObjectNode expectedTID = JsonUtils.toObjectNode(
          "{\"tim\":{\"packetID\":\"2\",\"msgCnt\":\"1\",\"timeStamp\":482327,\"urlB\":\"null\",\"dataFrames\":[{\"TravelerDataFrame\":{\"frameType\":{\"1\":\"EMPTY_TAG\"},\"sspTimRights\":\"0\",\"msgID\":\"RoadSignID\",\"position\":{\"latitude\":\"41.678473\",\"longitude\":\"-108.782775\",\"elevation\":\"917.1432\"},\"viewAngle\":\"1010101010101010\",\"mutcd\":\"5\",\"crc\":\"0000000000000000\",\"priority\":\"0\",\"sspLocationRights\":\"3\",\"regions\":[{\"GeographicalPath\":{\"name\":\"Testing TIM\",\"laneWidth\":0,\"directionality\":{\"both\":\"EMPTY_TAG\"},\"closedPath\":\"BOOLEAN_OBJECT_FALSE\",\"description\":{\"path\":{\"scale\":\"0\",\"offset\":{\"ll\":{\"nodes\":{\"NodeLL\":[{\"delta\":{\"node-LL3\":{\"lat\":0,\"lon\":0}}},{\"delta\":{\"node-LL3\":{\"lat\":0,\"lon\":0}}},{\"delta\":{\"node-LL3\":{\"lat\":0,\"lon\":0}}},{\"delta\":{\"node-LL3\":{\"lat\":0,\"lon\":0}}},{\"delta\":{\"node-LL3\":{\"lat\":0,\"lon\":0}}}]}}}}},\"direction\":\"0000000000001010\",\"id\":{\"region\":0,\"id\":33},\"anchor\":{\"lat\":0,\"long\":0,\"elevation\":0}}}],\"url\":\"null\",\"sspMsgRights2\":3,\"sspMsgRights1\":2,\"duratonTime\":1,\"startYear\":2017,\"startTime\":308065,\"tcontent\":{\"advisory\":{\"SEQUENCE\":[{\"item\":{\"itis\":513}},{\"item\":{\"text\":\"some text\"}},{\"item\":{\"text\":\"98765\"}}]}}}}]},\"rsus\":[{ \"rsuIndex\": \"1\",\"rsuTarget\": \"192.168.1.1\",\"rsuUsername\":\"user\",\"rsuPassword\":\"password\",\"rsuRetries\":\"1\",\"rsuTimeout\":\"2000\"}],\"snmp\":{\"rsuid\":\"00000083\",\"msgid\":\"31\",\"mode\":\"1\",\"channel\":\"178\",\"interval\":\"2\",\"deliverystart\":\"2017-06-01T17:47:11-05:00\",\"deliverystop\":\"2018-01-01T17:47:11-05:15\",\"enable\":\"1\",\"status\":\"4\"}}");
      assertEquals(expectedTID.toString() , inputTID.toString());
      JSONObject timObject = new JSONObject();
      timObject.put("TravelerInformation", JsonUtils.toJSONObject(inputTID.toString()));
      assertNotNull(XML.toString(timObject));
      //assertEquals("string", XML.toString(timObject));
   }
   
   @Test
   public void test2() throws JsonUtilsException {
      ObjectNode inputTID = JsonUtils.toObjectNode(
         "{\"tim\":{\"msgCnt\":\"1\",\"timeStamp\":\"2017-10-27T18:04:43.045Z\",\"packetID\":\"3\",\"urlB\":\"null\",\"dataframes\":[{\"startDateTime\":\"2017-10-20T05:22:33.985Z\",\"durationTime\":100,\"frameType\":\"1\",\"sspTimRights\":\"1\",\"msgID\":\"RoadSignID\",\"position\":{\"latitude\":\"40.573068\",\"longitude\":\"-105.049016\",\"elevation\":\"1500.8999999999999\"},\"viewAngle\":\"1111111111111111\",\"mutcd\":\"2\",\"crc\":\"0000000000000000\",\"priority\":\"5\",\"sspLocationRights\":\"1\",\"regions\":[{\"name\":\"Testing TIM\",\"regulatorID\":\"0\",\"segmentID\":\"33\",\"anchorPosition\":{\"latitude\":\"40.573068\",\"longitude\":\"-105.049016\",\"elevation\":\"1500.8999999999999\"},\"laneWidth\":\"327\",\"directionality\":\"3\",\"closedPath\":\"false\",\"description\":\"path\",\"path\":{\"scale\":\"0\",\"type\":\"xy\",\"nodes\":[{\"nodeLong\":\"-105.047355\",\"nodeLat\":\"40.572429\",\"delta\":\"node-LatLon\"},{\"nodeLong\":\"-105.046844\",\"nodeLat\":\"40.572228\",\"delta\":\"node-LatLon\"},{\"nodeLong\":\"-105.04659\",\"nodeLat\":\"40.572113\",\"delta\":\"node-LatLon\"},{\"nodeLong\":\"-105.046243\",\"nodeLat\":\"40.57191\",\"delta\":\"node-LatLon\"},{\"nodeLong\":\"-105.045936\",\"nodeLat\":\"40.571675\",\"delta\":\"node-LatLon\"},{\"nodeLong\":\"-105.045674\",\"nodeLat\":\"40.571422\",\"delta\":\"node-LatLon\"},{\"nodeLong\":\"-105.04545\",\"nodeLat\":\"40.571131\",\"delta\":\"node-LatLon\"},{\"nodeLong\":\"-105.045235\",\"nodeLat\":\"40.570724\",\"delta\":\"node-LatLon\"},{\"nodeLong\":\"-105.045113\",\"nodeLat\":\"40.570293\",\"delta\":\"node-LatLon\"},{\"nodeLong\":\"-105.045087\",\"nodeLat\":\"40.569848\",\"delta\":\"node-LatLon\"}]},\"direction\":\"1111111111111111\"}],\"sspMsgTypes\":\"1\",\"sspMsgContent\":\"1\",\"content\":\"Advisory\",\"items\":[\"513\",\"some text\", \"'98765\"],\"url\":\"null\"}]},\"rsus\":[{\"rsuRetries\":\"3\",\"rsuTimeout\":\"5000\",\"rsuUsername\":\"v3user\",\"rsuPassword\":\"password\", \"rsuIndex\": \"1\",\"rsuTarget\": \"192.168.0.145\",\"indicies\":[1,2]}],\"snmp\":{\"rsuid\":\"00000083\",\"msgid\":\"31\",\"mode\":\"1\",\"channel\":\"178\",\"interval\":\"2\",\"deliverystart\":\"2017-06-01T17:47:11-05:00\",\"deliverystop\":\"2018-01-01T17:47:11-05:15\",\"enable\":\"1\",\"status\":\"4\"},\"sdw\":{\"ttl\":\"oneday\",\"serviceRegion\":{\"nwCorner\":{\"latitude\":\"45.035685245316394\",\"longitude\":\"-110.95195770263672\"},\"seCorner\":{\"latitude\":\"40.96538194577477\",\"longitude\":\"-104.15382385253906\"}}}}");
      TravelerMessageFromHumanToAsnConverter
            .convertTravelerInputDataToEncodableTim(inputTID);

      ObjectNode expectedTID = JsonUtils.toObjectNode(
         "{\"tim\":{\"msgCnt\":\"1\",\"timeStamp\":431644,\"packetID\":\"3\",\"urlB\":\"null\",\"dataFrames\":[{\"TravelerDataFrame\":{\"frameType\":{\"1\":\"EMPTY_TAG\"},\"sspTimRights\":\"1\",\"msgID\":\"RoadSignID\",\"position\":{\"latitude\":\"40.573068\",\"longitude\":\"-105.049016\",\"elevation\":\"1500.8999999999999\"},\"viewAngle\":\"1111111111111111\",\"mutcd\":\"2\",\"crc\":\"0000000000000000\",\"priority\":\"5\",\"sspLocationRights\":\"1\",\"regions\":[{\"GeographicalPath\":{\"name\":\"Testing TIM\",\"laneWidth\":0,\"directionality\":{\"both\":\"EMPTY_TAG\"},\"closedPath\":\"BOOLEAN_OBJECT_FALSE\",\"description\":{\"path\":{\"scale\":\"0\",\"offset\":{\"xy\":{\"nodes\":{\"NodeXY\":[{\"delta\":{\"node-LatLon\":{\"lat\":0,\"lon\":0}}},{\"delta\":{\"node-LatLon\":{\"lat\":0,\"lon\":0}}},{\"delta\":{\"node-LatLon\":{\"lat\":0,\"lon\":0}}},{\"delta\":{\"node-LatLon\":{\"lat\":0,\"lon\":0}}},{\"delta\":{\"node-LatLon\":{\"lat\":0,\"lon\":0}}},{\"delta\":{\"node-LatLon\":{\"lat\":0,\"lon\":0}}},{\"delta\":{\"node-LatLon\":{\"lat\":0,\"lon\":0}}},{\"delta\":{\"node-LatLon\":{\"lat\":0,\"lon\":0}}},{\"delta\":{\"node-LatLon\":{\"lat\":0,\"lon\":0}}},{\"delta\":{\"node-LatLon\":{\"lat\":0,\"lon\":0}}}]}}}}},\"direction\":\"1111111111111111\",\"id\":{\"region\":0,\"id\":33},\"anchor\":{\"lat\":0,\"long\":0,\"elevation\":0}}}],\"url\":\"null\",\"sspMsgRights2\":1,\"sspMsgRights1\":1,\"duratonTime\":100,\"startYear\":2017,\"startTime\":420802,\"tcontent\":{\"advisory\":{\"SEQUENCE\":[{\"item\":{\"itis\":513}},{\"item\":{\"text\":\"some text\"}},{\"item\":{\"text\":\"98765\"}}]}}}}]},\"rsus\":[{\"rsuRetries\":\"3\",\"rsuTimeout\":\"5000\",\"rsuUsername\":\"v3user\",\"rsuPassword\":\"password\", \"rsuIndex\": \"1\",\"rsuTarget\": \"192.168.0.145\",\"indicies\":[1,2]}],\"snmp\":{\"rsuid\":\"00000083\",\"msgid\":\"31\",\"mode\":\"1\",\"channel\":\"178\",\"interval\":\"2\",\"deliverystart\":\"2017-06-01T17:47:11-05:00\",\"deliverystop\":\"2018-01-01T17:47:11-05:15\",\"enable\":\"1\",\"status\":\"4\"},\"sdw\":{\"ttl\":\"oneday\",\"serviceRegion\":{\"nwCorner\":{\"latitude\":\"45.035685245316394\",\"longitude\":\"-110.95195770263672\"},\"seCorner\":{\"latitude\":\"40.96538194577477\",\"longitude\":\"-104.15382385253906\"}}}}");
      assertEquals(expectedTID.toString() , inputTID.toString());
      JSONObject timObject = new JSONObject();
      timObject.put("TravelerInformation", JsonUtils.toJSONObject(inputTID.toString()));
      assertNotNull(XML.toString(timObject));
      //assertEquals("string", XML.toString(timObject));
   }

}
