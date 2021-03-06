package uk.gov.register.filters;

import com.google.common.collect.ImmutableMap;
import org.glassfish.jersey.server.filter.UriConnegFilter;
import uk.gov.register.views.representations.ExtraMediaType;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Map;

@PreMatching
@Priority(Priorities.HEADER_DECORATOR)
@Provider
public class UriDataFormatFilter implements ContainerRequestFilter {

    private final UriConnegFilter uriConnegFilter;

    public UriDataFormatFilter() {
        this(ImmutableMap.of(
                "csv", ExtraMediaType.TEXT_CSV_TYPE,
                "tsv", ExtraMediaType.TEXT_TSV_TYPE,
                "ttl", ExtraMediaType.TEXT_TTL_TYPE,
                "json", MediaType.APPLICATION_JSON_TYPE,
                "yaml", ExtraMediaType.TEXT_YAML_TYPE
        ));
    }

    public UriDataFormatFilter(Map<String, MediaType> mediaTypeMappings) {
        uriConnegFilter = new UriConnegFilter(mediaTypeMappings, null);
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        uriConnegFilter.filter(requestContext);
    }
}
