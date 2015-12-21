package uk.gov.register.datatype;

import com.fasterxml.jackson.databind.JsonNode;

public class StringDatatype implements Datatype {
    private final String datatypeName;

    public StringDatatype(String datatypeName) {
        this.datatypeName = datatypeName;
    }

    @Override
    public boolean isValid(JsonNode value) {
        return value.isTextual();
    }

    public String getName(){
        return datatypeName;
    }
}
