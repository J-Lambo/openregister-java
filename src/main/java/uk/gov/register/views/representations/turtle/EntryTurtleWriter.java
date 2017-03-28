package uk.gov.register.views.representations.turtle;

import org.apache.jena.rdf.model.*;
import uk.gov.register.core.Entry;
import uk.gov.register.core.RegisterName;
import uk.gov.register.core.RegisterResolver;
import uk.gov.register.views.ItemView;
import uk.gov.register.views.representations.ExtraMediaType;

import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;
import java.util.Collection;

@Provider
@Produces(ExtraMediaType.TEXT_TTL)
public class EntryTurtleWriter extends TurtleRepresentationWriter<Entry> {

    @Inject
    public EntryTurtleWriter(javax.inject.Provider<RegisterName> registerNameProvider, RegisterResolver registerResolver) {
        super(registerNameProvider, registerResolver);
    }

    @Override
    protected Model rdfModelFor(Entry entry) {
        return rdfModelFor(entry, true);
    }

    protected Model rdfModelFor(Entry entry, boolean includeKey) {
        Model model = ModelFactory.createDefaultModel();
        Property indexEntryNumberProperty = model.createProperty(SPEC_PREFIX + "index-entry-number-field");
        Property entryNumberProperty = model.createProperty(SPEC_PREFIX + "entry-number-field");
        Property entryTimestampProperty = model.createProperty(SPEC_PREFIX + "entry-timestamp-field");

        String entryNumber = Integer.toString(entry.getEntryNumber());

        Resource resource = model.createResource(entryUri(entryNumber).toString())
                .addProperty(entryNumberProperty, entryNumber)
                .addProperty(indexEntryNumberProperty, entryNumber)
                .addProperty(entryTimestampProperty, entry.getTimestampAsISOFormat());

        if (includeKey) {
            Property keyProperty = model.createProperty(SPEC_PREFIX + "key-field");
            resource.addProperty(keyProperty, entry.getKey());
        }

        Bag itemBag = model.createBag(SPEC_PREFIX + "item-resource");
        entry.getItemHashes().forEach(hash -> itemBag.add(model.createResource(itemUri(hash.encode()).toString())));
        Property itemProperty = model.createProperty(SPEC_PREFIX + "item-resource");
        resource.addProperty(itemProperty, itemBag);
        model.setNsPrefix("register-metadata", SPEC_PREFIX);
        return model;
    }

}
