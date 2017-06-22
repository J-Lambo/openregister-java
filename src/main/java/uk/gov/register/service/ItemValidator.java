package uk.gov.register.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import uk.gov.register.configuration.ConfigManager;
import uk.gov.register.core.Cardinality;
import uk.gov.register.core.Field;
import uk.gov.register.core.RegisterMetadata;
import uk.gov.register.core.RegisterName;
import uk.gov.register.core.datatype.Datatype;
import uk.gov.register.exceptions.ItemValidationException;

import java.util.Map;
import java.util.Set;

public class ItemValidator {
    //private final ConfigManager configManager;
    private final RegisterName registerName;

    public ItemValidator(RegisterName registerName) {
        //this.configManager = configManager;
        this.registerName = registerName;
    }

    public void validateItem(JsonNode inputEntry, Map<String, Field> fields, RegisterMetadata registerMetadata) throws ItemValidationException {
       // RegisterMetadata registerMetadata = configManager.getRegistersConfiguration().getRegisterMetadata(registerName);

        validateFields(inputEntry, registerMetadata);

        validatePrimaryKeyExists(inputEntry);

        validateFieldsValue(inputEntry, fields);
    }

    private void validatePrimaryKeyExists(JsonNode inputEntry) throws ItemValidationException {
        JsonNode primaryKeyNode = inputEntry.get(registerName.value());
        throwEntryValidationExceptionIfConditionIsFalse(primaryKeyNode == null, inputEntry, "Entry does not contain primary key field '" + registerName + "'");
        validatePrimaryKeyIsNotBlankAssumingItWillAlwaysBeAStringNode(StringUtils.isBlank(primaryKeyNode.textValue()), inputEntry, "Primary key field '" + registerName + "' must have a valid value");
    }

    private void validateFields(JsonNode inputEntry, RegisterMetadata registerMetadata) throws ItemValidationException {
        Set<String> inputFieldNames = Sets.newHashSet(inputEntry.fieldNames());
        Set<String> expectedFieldNames = Sets.newHashSet(registerMetadata.getFields());
        Set<String> unknownFields = Sets.difference(inputFieldNames, expectedFieldNames);

        throwEntryValidationExceptionIfConditionIsFalse(!unknownFields.isEmpty(), inputEntry, "Entry contains invalid fields: " + unknownFields.toString());
    }

    private void validateFieldsValue(JsonNode inputEntry, Map<String, Field> fields) throws ItemValidationException {
        inputEntry.fieldNames().forEachRemaining(fieldName -> {
            //Field field = configManager.getFieldsConfiguration().getField(fieldName);
            Field field = fields.get(fieldName);

            Datatype datatype = field.getDatatype();

            JsonNode fieldValue = inputEntry.get(fieldName);

            if (field.getCardinality().equals(Cardinality.MANY)) {

                throwEntryValidationExceptionIfConditionIsFalse(!fieldValue.isArray(), inputEntry, String.format("Field '%s' has cardinality 'n' so the value must be an array of '%s'", fieldName, datatype.getName()));

                fieldValue.elements().forEachRemaining(element -> throwEntryValidationExceptionIfConditionIsFalse(!datatype.isValid(element), inputEntry, String.format("Field '%s' values must be of type '%s'", fieldName, datatype.getName())));

            } else {
                throwEntryValidationExceptionIfConditionIsFalse(!datatype.isValid(fieldValue), inputEntry, String.format("Field '%s' value must be of type '%s'", fieldName, datatype.getName()));
            }

        });
    }

    private void validatePrimaryKeyIsNotBlankAssumingItWillAlwaysBeAStringNode(boolean condition, JsonNode inputJsonEntry, String errorMessage) {
        throwEntryValidationExceptionIfConditionIsFalse(condition, inputJsonEntry, errorMessage);
    }

    private void throwEntryValidationExceptionIfConditionIsFalse(boolean condition, JsonNode inputJsonEntry, String errorMessage) {
        if (condition) {
            throw new ItemValidationException(errorMessage, inputJsonEntry);
        }
    }
}
