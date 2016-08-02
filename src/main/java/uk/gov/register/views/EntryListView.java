package uk.gov.register.views;

import com.fasterxml.jackson.annotation.JsonValue;
import uk.gov.organisation.client.GovukOrganisation;
import uk.gov.register.core.RegisterData;
import uk.gov.register.configuration.PublicBody;
import uk.gov.register.configuration.RegisterDomainConfiguration;
import uk.gov.register.core.Entry;
import uk.gov.register.api.representations.CsvRepresentation;
import uk.gov.register.resources.IPagination;
import uk.gov.register.resources.RequestContext;

import java.util.Collection;
import java.util.Optional;

public class EntryListView extends CsvRepresentationView {
    private IPagination pagination;
    private Collection<Entry> entries;
    private final Optional<String> recordKey;

    public EntryListView(RequestContext requestContext, IPagination pagination, PublicBody custodian, Optional<GovukOrganisation.Details> custodianBranding, Collection<Entry> entries, RegisterDomainConfiguration registerDomainConfiguration, RegisterData registerData) {
        super(requestContext, custodian, custodianBranding, "entries.html", registerDomainConfiguration, registerData);
        this.pagination = pagination;
        this.entries = entries;
        this.recordKey = Optional.empty();
    }

    public EntryListView(RequestContext requestContext, IPagination pagination, PublicBody custodian, Optional<GovukOrganisation.Details> custodianBranding, Collection<Entry> entries, String recordKey, RegisterDomainConfiguration registerDomainConfiguration, RegisterData registerData) {
        super(requestContext, custodian, custodianBranding, "entries.html", registerDomainConfiguration, registerData);
        this.pagination = pagination;
        this.entries = entries;
        this.recordKey = Optional.of(recordKey);
    }

    @JsonValue
    public Collection<Entry> getEntries() {
        return entries;
    }

    @SuppressWarnings("unused, used from templates")
    public IPagination getPagination() {
        return pagination;
    }

    @SuppressWarnings("unused, used from templates")
    public Optional<String> getRecordKey() { return recordKey; }

    @Override
    public CsvRepresentation<Collection<Entry>> csvRepresentation() {
        return new CsvRepresentation<>(Entry.csvSchema(), getEntries());
    }
}