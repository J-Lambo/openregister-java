package uk.gov.register.presentation.view;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import uk.gov.organisation.client.GovukOrganisation;
import uk.gov.register.presentation.EntryConverter;
import uk.gov.register.presentation.FieldValue;
import uk.gov.register.presentation.config.PublicBody;
import uk.gov.register.presentation.dao.Item;
import uk.gov.register.presentation.representations.RepresentationView;
import uk.gov.register.presentation.resource.RequestContext;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ItemView extends AttributionView implements RepresentationView {
    private EntryConverter itemConverter;
    private Item item;

    public ItemView(RequestContext requestContext, PublicBody custodian, Optional<GovukOrganisation.Details> branding, EntryConverter itemConverter, Item item) {
        super(requestContext, custodian, branding, "item.html");
        this.itemConverter = itemConverter;
        this.item = item;
    }

    @JsonValue
    public Map<String, FieldValue> getContent() {
        return item.getFieldsStream().collect(Collectors.toMap(Map.Entry::getKey, itemConverter::convert));
    }

    @Override
    public CsvSchema csvSchema() {
        CsvSchema.Builder schemaBuilder = new CsvSchema.Builder() ;
        for (String value : getContent().keySet()) {
            schemaBuilder.addColumn(value, CsvSchema.ColumnType.STRING);
        }
        return schemaBuilder.build();
    }
}
