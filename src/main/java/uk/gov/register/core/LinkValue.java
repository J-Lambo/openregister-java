package uk.gov.register.core;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A value representing a link. Its "value" is a string representation. It also knows
 * its target register, and its primary key within the target register.
 *
 * For a regular LinkValue, the "value" and "linkKey" are the same. For a CurieValue, the "value"
 * is the Curie as a string, while the "linkKey" is the second half of the Curie (after the colon).
 */
public class LinkValue implements FieldValue {
    private final RegisterName targetRegister;
    private final String value;
    private final String linkKey;

    public LinkValue(RegisterName registerName, String value) {
        this(registerName, value, value);
    }

    private LinkValue(RegisterName registerName, String value, String linkKey){
        this.targetRegister = registerName;
        this.value = value;
        this.linkKey = linkKey;
    }

    @Override
    public boolean isLink() {
        return true;
    }

    @Override
    @JsonValue
    public String getValue() {
        return value;
    }

    public boolean isList() {
        return false;
    }

    public RegisterName getTargetRegister() {
        return targetRegister;
    }

    public String getLinkKey() {
        return linkKey;
    }

    public static class CurieValue extends LinkValue {
        public CurieValue(String curieValue) {
            super(new RegisterName(curieValue.split(":")[0]), curieValue, curieValue.split(":")[1]);
        }
    }
}
