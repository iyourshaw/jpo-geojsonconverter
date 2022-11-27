package us.dot.its.jpo.geojsonconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class DateJsonMapper {

    private static final ObjectMapper _instance;

    static {
        _instance = new ObjectMapper();
        _instance.registerModule(new JavaTimeModule());
        _instance.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    public static ObjectMapper getInstance() {
        return _instance;
    }
}