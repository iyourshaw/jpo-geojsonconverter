package us.dot.its.jpo.geojsonconverter.validator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.beans.factory.annotation.Value;


@SpringBootTest( { 
    "valid.map.json=classpath:json/valid.map.json",
    "invalid.map.json=classpath:json/invalid.map.json" })
@RunWith(SpringRunner.class)    
public class MapJsonValidatorTest extends AbstractJsonValidatorTest  {


    @Autowired
    private MapJsonValidator mapJsonValidator;
    
    
    @Test
    public void mapJsonValidatorLoaded() {
        assertThat(mapJsonValidator, notNullValue());
    }

    @Test
    public void jsonSchemaResourceLoaded() {
        testJsonSchemaResourceLoaded(mapJsonValidator);
    }

    @Test
    public void jsonSchemaLoaded() throws IOException {
        testJsonSchemaLoaded(mapJsonValidator);
    }

    
    @Test
    public void validMapJsonTest_String() {
        testJson(mapJsonValidator, validMapJsonResource, true);
    }

    @Test
    public void invalidMapJsonTest_String() {
        testJson(mapJsonValidator, invalidMapJsonResource, false);
    }

    @Test
    public void validMapJsonTest_ByteArray() {
        testJson_ByteArray(mapJsonValidator, validMapJsonResource, true);
    }

    @Test
    public void invalidMapJsonTest_ByteArray() {
        testJson_ByteArray(mapJsonValidator, invalidMapJsonResource, false);
    }

    @Test
    public void testException() {
        MapJsonValidator badValidator = new MapJsonValidator(null);
        var result = badValidator.validate("invalid");
        assertFalse("An exception should have happened", result.isValid());
    }

    @Value("${valid.map.json}")
    private Resource validMapJsonResource;

    @Value("${invalid.map.json}")
    private Resource invalidMapJsonResource;

    
    
}
