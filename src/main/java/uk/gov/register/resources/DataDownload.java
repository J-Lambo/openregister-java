package uk.gov.register.resources;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.views.View;
import uk.gov.register.configuration.ResourceConfiguration;
import uk.gov.register.core.*;
import uk.gov.register.serialization.RSFFormatter;
import uk.gov.register.service.RegisterSerialisationFormatService;
import uk.gov.register.util.ArchiveCreator;
import uk.gov.register.views.ViewFactory;
import uk.gov.register.views.representations.ExtraMediaType;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.util.Collection;

@Path("/")
public class DataDownload {

    private final RegisterReadOnly register;
    protected final ViewFactory viewFactory;
    private final RegisterName registerPrimaryKey;
    private final ResourceConfiguration resourceConfiguration;
    private final RegisterSerialisationFormatService rsfService;
    private final RSFFormatter rsfFormatter;


    @Inject
    public DataDownload(RegisterReadOnly register,
                        ViewFactory viewFactory,
                        ResourceConfiguration resourceConfiguration,
                        RegisterSerialisationFormatService rsfService) {
        this.register = register;
        this.viewFactory = viewFactory;
        this.registerPrimaryKey = register.getRegisterName();
        this.resourceConfiguration = resourceConfiguration;
        this.rsfService = rsfService;
        this.rsfFormatter = new RSFFormatter();
    }

    @GET
    @Path("/download-register")
    @Produces({MediaType.APPLICATION_OCTET_STREAM, ExtraMediaType.TEXT_HTML})
    @DownloadNotAvailable
    @Timed
    public Response downloadRegister() {
        Collection<Entry> entries = register.getAllEntries();
        Collection<Item> items = register.getAllItems();

        RegisterDetail registerDetail = viewFactory.registerDetailView(
                register.getTotalRecords(),
                register.getTotalEntries(),
                register.getLastUpdatedTime(),
                register.getCustodianName()).getRegisterDetail();

        return Response
                .ok(new ArchiveCreator().create(registerDetail, entries, items))
                .header("Content-Disposition", String.format("attachment; filename=%s-%d.zip", registerPrimaryKey, System.currentTimeMillis()))
                .header("Content-Transfer-Encoding", "binary")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM)
                .build();
    }

    @GET
    @Path("/download-rsf")
    @Produces({ExtraMediaType.APPLICATION_RSF, ExtraMediaType.TEXT_HTML})
    @DownloadNotAvailable
    @Timed
    public Response downloadRSF() {
        String rsfFileName = String.format("attachment; filename=rsf-%d.%s", System.currentTimeMillis(), rsfFormatter.getFileExtension());
        return Response
                .ok((StreamingOutput) output -> rsfService.writeTo(output, rsfFormatter))
                .header("Content-Disposition", rsfFileName).build();
    }

    @GET
    @Path("/index/{index-name}/download-rsf")
    @Produces({ExtraMediaType.APPLICATION_RSF, ExtraMediaType.TEXT_HTML})
    @DownloadNotAvailable
    @Timed
    public Response downloadIndexRSF(@PathParam("index-name") String indexName) {
        String rsfFileName = String.format("attachment; filename=rsf-%d.%s", System.currentTimeMillis(), rsfFormatter.getFileExtension());
        return Response
                .ok((StreamingOutput) output -> rsfService.writeTo(output, rsfFormatter, indexName))
                .header("Content-Disposition", rsfFileName).build();
    }

    @GET
    @Path("/index/{index-name}/download-rsf/{total-entries-1}/{total-entries-2}")
    @Produces({ExtraMediaType.APPLICATION_RSF, ExtraMediaType.TEXT_HTML})
    @DownloadNotAvailable
    @Timed
    public Response downloadIndexRSF(@PathParam("index-name") String indexName, @PathParam("total-entries-1") int totalEntries1, @PathParam("total-entries-2") int totalEntries2) {
        String rsfFileName = String.format("attachment; filename=rsf-%d.%s", System.currentTimeMillis(), rsfFormatter.getFileExtension());
        return Response
                .ok((StreamingOutput) output -> rsfService.writeTo(output, rsfFormatter, indexName, totalEntries1, totalEntries2))
                .header("Content-Disposition", rsfFileName).build();
    }

    @GET
    @Path("/download-rsf/{start-entry-number}")
    @Produces({ExtraMediaType.APPLICATION_RSF, ExtraMediaType.TEXT_HTML})
    @DownloadNotAvailable
    @Timed
    public Response downloadPartialRSF(@PathParam("start-entry-number") int startEntryNumber) {
        if (startEntryNumber < 0) {
            throw new BadRequestException("start-entry-number must be 0 or greater");
        }

        int totalEntriesInRegister = register.getTotalEntries();

        if (startEntryNumber > totalEntriesInRegister) {
            throw new BadRequestException("start-entry-number must not exceed number of total entries in the register");
        }

        String rsfFileName = String.format("attachment; filename=rsf-%d.%s", System.currentTimeMillis(), rsfFormatter.getFileExtension());
        return Response
                .ok((StreamingOutput) output -> rsfService.writeTo(output, rsfFormatter, startEntryNumber, totalEntriesInRegister))
                .header("Content-Disposition", rsfFileName).build();
    }

    @GET
    @Path("/download-rsf/{total-entries-1}/{total-entries-2}")
    @Produces({ExtraMediaType.APPLICATION_RSF, ExtraMediaType.TEXT_HTML})
    @DownloadNotAvailable
    @Timed
    public Response downloadPartialRSF(@PathParam("total-entries-1") int totalEntries1, @PathParam("total-entries-2") int totalEntries2) {
        if (totalEntries1 < 0) {
            throw new BadRequestException("total-entries-1 must be 0 or greater");
        }

        if (totalEntries2 < totalEntries1) {
            throw new BadRequestException("total-entries-2 must be greater than or equal to total-entries-1");
        }

        int totalEntriesInRegister = register.getTotalEntries();

        if (totalEntries2 > totalEntriesInRegister) {
            throw new BadRequestException("total-entries-2 must not exceed number of total entries in the register");
        }

        String rsfFileName = String.format("attachment; filename=rsf-%d.%s", System.currentTimeMillis(), rsfFormatter.getFileExtension());
        return Response
                .ok((StreamingOutput) output -> rsfService.writeTo(output, rsfFormatter, totalEntries1, totalEntries2))
                .header("Content-Disposition", rsfFileName).build();
    }

    @GET
    @Path("/download")
    @Produces(ExtraMediaType.TEXT_HTML)
    @Timed
    public View download() {
        return viewFactory.downloadPageView(resourceConfiguration.getEnableDownloadResource());
    }
}

