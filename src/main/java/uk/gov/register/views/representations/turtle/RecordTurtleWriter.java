package uk.gov.register.views.representations.turtle;

import org.apache.jena.rdf.model.*;
import uk.gov.register.core.Entry;
import uk.gov.register.core.RegisterName;
import uk.gov.register.core.RegisterResolver;
import uk.gov.register.views.ItemView;
import uk.gov.register.views.RecordView;
import uk.gov.register.views.representations.ExtraMediaType;

import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.Provider;
import java.net.URI;
import java.util.Map;

@Provider
@Produces(ExtraMediaType.TEXT_TTL)
public class RecordTurtleWriter extends TurtleRepresentationWriter<RecordView> {

    @Inject
    public RecordTurtleWriter(javax.inject.Provider<RegisterName> registerNameProvider, RegisterResolver registerResolver) {
        super(registerNameProvider, registerResolver);
    }

    @Override
    protected Model rdfModelFor(RecordView view) {
        Entry entry = view.getEntry();

        Model recordModel = ModelFactory.createDefaultModel();
        Model entryModel = new EntryTurtleWriter(registerNameProvider, registerResolver).rdfModelFor(entry);

        Resource recordResource = recordModel.createResource(recordUri(view.getPrimaryKey()).toString());
        addPropertiesToResource(recordResource, entryModel.getResource(entryUri(Integer.toString(entry.getEntryNumber())).toString()));
        Map<String, String> prefixes = entryModel.getNsPrefixMap();

        Bag itemBag = recordModel.createBag(SPEC_PREFIX + "item-resource");

        view.getItemViews().forEach(iv -> {
            Model itemModel = new ItemTurtleWriter(registerNameProvider, registerResolver).rdfModelFor(iv);
            Resource itemResource = itemModel.getResource(itemUri(iv.getItemHash().encode()).toString());
            recordModel.add(itemModel);
            itemBag.add( itemResource );
        });

        recordModel.setNsPrefixes(prefixes);

        return recordModel;
    }

    private void addPropertiesToResource(Resource to, Resource from) {
        StmtIterator iterator = from.listProperties();
        while (iterator.hasNext()) {
            Statement statement = iterator.next();
            to.addProperty(statement.getPredicate(), statement.getObject());
        }
    }

    protected URI recordUri(String primaryKey) {
        return UriBuilder.fromUri(ourBaseUri()).path("record").path(primaryKey).build();
    }
}
