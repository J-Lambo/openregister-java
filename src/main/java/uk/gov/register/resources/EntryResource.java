package uk.gov.register.resources;

import uk.gov.register.configuration.RegisterNameConfiguration;
import uk.gov.register.core.Entry;
import uk.gov.register.core.RegisterReadOnly;
import uk.gov.register.views.AttributionView;
import uk.gov.register.views.EntryListView;
import uk.gov.register.views.ViewFactory;
import uk.gov.register.views.representations.ExtraMediaType;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.Optional;

@Path("/")
public class EntryResource {

    private final RegisterReadOnly register;
    private final ViewFactory viewFactory;
    private final RequestContext requestContext;
    private final HttpServletResponseAdapter httpServletResponseAdapter;
    private final String registerPrimaryKey;

    @Inject
    public EntryResource(RegisterReadOnly register, ViewFactory viewFactory, RequestContext requestContext, RegisterNameConfiguration registerNameConfiguration) {
        this.register = register;
        this.viewFactory = viewFactory;
        this.requestContext = requestContext;
        this.httpServletResponseAdapter = new HttpServletResponseAdapter(requestContext.httpServletResponse);
        this.registerPrimaryKey = registerNameConfiguration.getRegister();
    }

    @GET
    @Path("/entry/{entry-number}")
    @Produces({ExtraMediaType.TEXT_HTML, MediaType.APPLICATION_JSON, ExtraMediaType.TEXT_YAML, ExtraMediaType.TEXT_CSV, ExtraMediaType.TEXT_TSV, ExtraMediaType.TEXT_TTL})
    public AttributionView findByEntryNumber(@PathParam("entry-number") int entryNumber) {
        Optional<Entry> entry = register.getEntry(entryNumber);
        return entry.map(viewFactory::getEntryView).orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("/entries")
    @Produces({ExtraMediaType.TEXT_HTML, MediaType.APPLICATION_JSON, ExtraMediaType.TEXT_YAML, ExtraMediaType.TEXT_CSV, ExtraMediaType.TEXT_TSV, ExtraMediaType.TEXT_TTL})
    public EntryListView entries(@QueryParam("start") Optional<Integer> optionalStart, @QueryParam("limit") Optional<Integer> optionalLimit) {
        int totalEntries = register.getTotalEntries();
        StartLimitPagination startLimitPagination = new StartLimitPagination(optionalStart, optionalLimit, totalEntries);

        Collection<Entry> entries = register.getEntries(startLimitPagination.start, startLimitPagination.limit);

        setHeaders(startLimitPagination);

        return viewFactory.getEntriesView(entries, startLimitPagination);
    }

    @GET
    @Path("/move-head-to/{entry-number}")
    public String moveHeadTo(@PathParam("entry-number") int entryNumber){
        register.moveHeadTo(entryNumber);
        return "all sorted for yah, head moved to: " + entryNumber;
    }

    @GET
    @Path("/clean-rubbish")
    public String cleanRubbish(){
        register.cleanRubbishWhichIsPastHead();
        return "all sorted for yah, all the rubbish past the head cleaned";
    }



    private void setHeaders(StartLimitPagination startLimitPagination) {
        requestContext.resourceExtension().ifPresent(
                ext -> httpServletResponseAdapter.addContentDispositionHeader(registerPrimaryKey + "-entries." + ext)
        );

        if (startLimitPagination.hasNextPage()) {
            httpServletResponseAdapter.addLinkHeader("next", startLimitPagination.getNextPageLink());
        }

        if (startLimitPagination.hasPreviousPage()) {
            httpServletResponseAdapter.addLinkHeader("previous", startLimitPagination.getPreviousPageLink());
        }
    }
}

