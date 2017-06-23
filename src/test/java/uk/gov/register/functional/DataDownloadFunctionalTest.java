package uk.gov.register.functional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.register.functional.app.RegisterRule;
import uk.gov.register.views.representations.ExtraMediaType;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertTrue;
import static uk.gov.register.functional.app.TestRegister.address;
import static uk.gov.register.functional.app.TestRegister.local_authority_eng;

public class DataDownloadFunctionalTest {

    @ClassRule
    public static RegisterRule register = new RegisterRule();
    private final String item1 = "{\"street\":\"ellis\",\"address\":\"12345\"}";
    private final String item2 = "{\"street\":\"presley\",\"address\":\"6789\"}";
    private final String item3 = "{\"street\":\"foo\",\"address\":\"12345\"}";
    private final String item4 = "{\"street\":\"ellis\",\"address\":\"145678\"}";

    @Before
    public void publishTestMessages() {
        register.wipe();
//        register.loadRsf(
//                address,
//                "add-item\t{\"address\":\"12345\",\"street\":\"ellis\"}\n" +
//                "append-entry\tuser\t12345\t2017-06-01T10:13:27Z\tsha-256:19205fafe65406b9b27fce1b689abc776df4ddcf150c28b29b73b4ea054af6b9\n" +
//                "add-item\t{\"address\":\"6789\",\"street\":\"presley\"}\n" +
//                "append-entry\tuser\t6789\t2017-06-01T10:13:27Z\tsha-256:bd239db51960376826b937a615f0f3397485f00611d35bb7e951e357bf73b934\n" +
//                "add-item\t{\"address\":\"12345\",\"street\":\"foo\"}\n" +
//                "append-entry\tuser\t12345\t2017-06-01T10:13:27Z\tsha-256:cc8a7c42275c84b94c6e282ae88b3dbcc06319156fc4539a2f39af053bf30592\n" +
//                "add-item\t{\"address\":\"145678\",\"street\":\"ellis\"}\n" +
//                "append-entry\tuser\t145678\t2017-06-01T10:13:27Z\tsha-256:8ac926428ee49fb83c02bdd2556e62e84cfd9e636cd35eb1306ac8cb661e4983\n" +
//                "append-entry\tuser\t12345\t2017-06-01T10:13:27Z\tsha-256:19205fafe65406b9b27fce1b689abc776df4ddcf150c28b29b73b4ea054af6b9");
    }

    @Test
    public void downloadRegister_shouldReturnAZipfile() throws IOException {
        Response response = register.getRequest(address, "/download-register");

        assertThat(response.getHeaderString("Content-Type"), equalTo(MediaType.APPLICATION_OCTET_STREAM));
        assertThat(response.getHeaderString("Content-Disposition"), startsWith("attachment; filename="));
        assertThat(response.getHeaderString("Content-Disposition"), endsWith(".zip"));

        InputStream is = response.readEntity(InputStream.class);
        Set<String> zipEntryNames = getEntries(is).keySet();

        assertThat(zipEntryNames, hasItem("register.json"));
        assertThat(zipEntryNames.stream().filter(e -> e.matches("(entry/)(\\d)(.json)")).count(), is(5L));
        assertThat(zipEntryNames.stream().filter(e -> e.matches("(item/)(\\w+)(.json)")).count(), is(4L));
    }

    @Test
    public void downloadRegister_shouldUseCorrectEntryAndItemJsonFormat() throws IOException {
        Response response = register.getRequest(address, "/download-register");
        InputStream is = response.readEntity(InputStream.class);

        List<String> itemJson = getEntries(is).entrySet().stream()
                .filter(j -> j.getKey().startsWith("item"))
                .map(j -> j.getValue().toString())
                .collect(Collectors.toList());

        assertThat(itemJson, hasItems(item1, item2, item3, item4));

        List<JsonNode> entryJson = getEntries(is).entrySet().stream()
                .filter(j -> j.getKey().startsWith("entry"))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        entryJson.forEach(j -> {
            assertTrue(j.has("entry-number"));
            assertTrue(j.has("entry-timestamp"));
            assertTrue(j.has("item-hash"));
        });
    }

    @Test
    public void downloadRegister_shouldUseCorrectRegisterJsonFormat() throws IOException {
        Response response = register.getRequest(address, "/download-register");
        InputStream is = response.readEntity(InputStream.class);

        JsonNode registerJson = getEntries(is).get("register.json");

        assertThat(registerJson.get("total-entries").asInt(), is(5));
        assertThat(registerJson.get("total-records").asInt(), is(3));
        assertTrue(registerJson.has("last-updated"));
        assertTrue(registerJson.has("domain"));
        assertTrue(registerJson.has("register-record"));
    }

    @Test
    public void downloadRSF_shouldReturnRegisterAsRsfStream() throws IOException {
        Response response = register.getRequest(address, "/download-rsf");

        assertThat(response.getHeaderString("Content-Type"), equalTo(ExtraMediaType.APPLICATION_RSF));
        assertThat(response.getHeaderString("Content-Disposition"), startsWith("attachment; filename="));
        assertThat(response.getHeaderString("Content-Disposition"), endsWith(".rsf"));

        List<String> rsfLines = getRsfLinesFrom(response);

        assertThat(rsfLines.get(0), equalTo("assert-root-hash\tsha-256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"));

        assertThat(rsfLines, containsInAnyOrder(
                "add-item\t{\"address\":\"12345\",\"street\":\"ellis\"}",
                "add-item\t{\"address\":\"145678\",\"street\":\"ellis\"}",
                "add-item\t{\"address\":\"6789\",\"street\":\"presley\"}",
                "add-item\t{\"address\":\"12345\",\"street\":\"foo\"}"));

        assertFormattedEntry(rsfLines.get(5), "12345","sha-256:19205fafe65406b9b27fce1b689abc776df4ddcf150c28b29b73b4ea054af6b9");
        assertFormattedEntry(rsfLines.get(6), "6789","sha-256:bd239db51960376826b937a615f0f3397485f00611d35bb7e951e357bf73b934");
        assertFormattedEntry(rsfLines.get(7), "12345","sha-256:cc8a7c42275c84b94c6e282ae88b3dbcc06319156fc4539a2f39af053bf30592");
        assertFormattedEntry(rsfLines.get(8), "145678","sha-256:8ac926428ee49fb83c02bdd2556e62e84cfd9e636cd35eb1306ac8cb661e4983");
        assertFormattedEntry(rsfLines.get(9), "12345","sha-256:19205fafe65406b9b27fce1b689abc776df4ddcf150c28b29b73b4ea054af6b9");

        assertThat(rsfLines.get(10), containsString("assert-root-hash\t"));
    }

    @Test
    public void downloadPartialRSF_fromStartEntryNumber_shouldReturnAPartOfRegisterAsRsfStream() throws IOException {
        Response response = register.getRequest(address, "/download-rsf/2");

        assertThat(response.getHeaderString("Content-Type"), equalTo(ExtraMediaType.APPLICATION_RSF));
        assertThat(response.getHeaderString("Content-Disposition"), startsWith("attachment; filename="));
        assertThat(response.getHeaderString("Content-Disposition"), endsWith(".rsf"));

        List<String> rsfLines = getRsfLinesFrom(response);

        assertThat(rsfLines.get(0), equalTo("assert-root-hash\tsha-256:7cdec03e9aba7810340596079a18b495dbc93f22a8d051d3417fb4a79b1f6124"));

        assertThat(rsfLines, hasItems(
                "add-item\t{\"address\":\"12345\",\"street\":\"foo\"}",
                "add-item\t{\"address\":\"12345\",\"street\":\"ellis\"}",
                "add-item\t{\"address\":\"145678\",\"street\":\"ellis\"}"));

        assertFormattedEntry(rsfLines.get(4), "12345","sha-256:cc8a7c42275c84b94c6e282ae88b3dbcc06319156fc4539a2f39af053bf30592");
        assertFormattedEntry(rsfLines.get(5), "145678","sha-256:8ac926428ee49fb83c02bdd2556e62e84cfd9e636cd35eb1306ac8cb661e4983");
        assertFormattedEntry(rsfLines.get(6), "12345","sha-256:19205fafe65406b9b27fce1b689abc776df4ddcf150c28b29b73b4ea054af6b9");

        assertThat(rsfLines.get(7), containsString("assert-root-hash\t"));
    }

    @Test
    public void downloadPartialRSF_shouldReturnAPartOfRegisterAsRsfStream() throws IOException {
        Response response = register.getRequest(address, "/download-rsf/0/2");

        assertThat(response.getHeaderString("Content-Type"), equalTo(ExtraMediaType.APPLICATION_RSF));
        assertThat(response.getHeaderString("Content-Disposition"), startsWith("attachment; filename="));
        assertThat(response.getHeaderString("Content-Disposition"), endsWith(".rsf"));

        List<String> rsfLines = getRsfLinesFrom(response);

        assertThat(rsfLines.get(0), equalTo("assert-root-hash\tsha-256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"));

        assertThat(rsfLines, hasItems(
                "add-item\t{\"address\":\"12345\",\"street\":\"ellis\"}",
                "add-item\t{\"address\":\"6789\",\"street\":\"presley\"}"));

        assertFormattedEntry(rsfLines.get(3), "12345","sha-256:19205fafe65406b9b27fce1b689abc776df4ddcf150c28b29b73b4ea054af6b9");
        assertFormattedEntry(rsfLines.get(4), "6789","sha-256:bd239db51960376826b937a615f0f3397485f00611d35bb7e951e357bf73b934");

        assertThat(rsfLines.get(5), containsString("assert-root-hash\t"));
    }

    @Test
    public void downloadPartialRSF_shouldReturn400ForRsfBoundariesOutOfBound() throws IOException {
        Response response = register.getRequest(address, "/download-rsf/666/1000");

        assertThat(response.getStatus(), equalTo(400));
    }

    @Test
    public void downloadPartialRSF_shouldReturn400_whenTotalEntries1OutOfBounds() {
        Response response = register.getRequest(address, "/download-rsf/-1/2");

        assertThat(response.getStatus(), equalTo(400));
    }

    @Test
    public void downloadPartialRSF_shouldReturn400_whenTotalEntries2LessThanTotalEntries1() {
        Response response = register.getRequest(address, "/download-rsf/2/1");

        assertThat(response.getStatus(), equalTo(400));
    }

    @Test
    public void downloadPartialRSF_shouldReturnCurrentRootHash_whenStartEntryNumbersIsCurrentMaximum() {
        Response response = register.getRequest(address, "/download-rsf/5");
        List<String> partialRsfLines = getRsfLinesFrom(response);
        assertThat(partialRsfLines.get(0), equalTo("assert-root-hash\tsha-256:a0aa6dace50e47c68fd4a79e3d94ba525f827e0a44bb64ed2e5f75cfe051b71c"));
    }

    @Test
    public void downloadPartialRSF_shouldReturnRootHash_whenStartAndEndEntryNumbersEqual() {
        Response response = register.getRequest(address, "/download-rsf/0/0");
        List<String> partialRsfLines = getRsfLinesFrom(response);

        assertThat(partialRsfLines.get(0), equalTo("assert-root-hash\tsha-256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"));
    }

    @Test
    public void downloadPartialRSF_shouldReturn400_whenRequestedTotalEntriesExceedsEntriesInRegister() {
        Response response = register.getRequest(address, "/download-rsf/0/6");

        assertThat(response.getStatus(), equalTo(400));
    }

    @Test
    public void downloadPartialRSF_fromStartEntryNumber_shouldReturn400_whenStartEntryNumberOutOfBounds() {
        Response response = register.getRequest(address, "/download-rsf/-1");

        assertThat(response.getStatus(), equalTo(400));
    }

    @Test
    public void downloadPartialRSF_fromStartEntryNumber_shouldReturn400_whenRequestedStartEntryNumberExceedsEntriesInRegister() {
        Response response = register.getRequest(address, "/download-rsf/6");

        assertThat(response.getStatus(), equalTo(400));
    }

    @Test
    public void downloadPartialRSF_fromStartEntryNumber_shouldReturnSameRSFAsFullDownload() {
        Response fullRsfResponse = register.getRequest(address, "/download-rsf");
        Response partialRsfResponse = register.getRequest(address, "/download-rsf/0");

        List<String> fullRsfLines = getRsfLinesFrom(fullRsfResponse);
        List<String> partialRsfLines = getRsfLinesFrom(partialRsfResponse);

        assertThat(partialRsfLines.get(0), is(fullRsfLines.get(0)));
        assertThat(partialRsfLines.subList(1, 5), hasItems(fullRsfLines.subList(1, 5).toArray(new String[4])));
        assertThat(partialRsfLines.subList(5, 9), equalTo(fullRsfLines.subList(5, 9)));
        assertThat(partialRsfLines.get(10), is(fullRsfLines.get(10)));
    }

    @Test
    public void downloadPartialRSF_shouldReturnSameRSFAsFullDownload() {
        Response fullRsfResponse = register.getRequest(address, "/download-rsf");
        Response partialRsfResponse = register.getRequest(address, "/download-rsf/0/5");

        List<String> fullRsfLines = getRsfLinesFrom(fullRsfResponse);
        List<String> partialRsfLines = getRsfLinesFrom(partialRsfResponse);

        assertThat(partialRsfLines.get(0), is(fullRsfLines.get(0)));
        assertThat(partialRsfLines.subList(1, 5), hasItems(fullRsfLines.subList(1, 5).toArray(new String[4])));
        assertThat(partialRsfLines.subList(5, 9), equalTo(fullRsfLines.subList(5, 9)));
        assertThat(partialRsfLines.get(10), is(fullRsfLines.get(10)));
    }

    @Test
    public void downloadIndexRSF_shouldReturnCorrectIndexRSF_whenRegisterRSFContainsEntryWhereItemMovesKey() {
        register.loadRsf(local_authority_eng,
            "add-item\t{\"local-authority-eng\":\"BAS\",\"local-authority-type\":\"UA\",\"name\":\"Bath\",\"official-name\":\"Bath\",\"start-date\":\"1996-04-01\"}\n" +
            "add-item\t{\"local-authority-eng\":\"BAS\",\"local-authority-type\":\"UA\",\"name\":\"New Bath\",\"official-name\":\"Bath\",\"start-date\":\"1996-04-01\"}\n" +
            "add-item\t{\"local-authority-eng\":\"BAS\",\"local-authority-type\":\"MD\",\"name\":\"New Bath\",\"official-name\":\"Bath\",\"start-date\":\"1996-04-01\"}\n" +
            "append-entry\tuser\tBAS\t2016-04-05T13:23:05Z\tsha-256:0957b9d2e02eab7ffeda290dac485dacab54abd0264a5eb789906e091856fbeb\n" +
            "append-entry\tuser\tBAS\t2016-04-05T13:23:05Z\tsha-256:05bc13575c70f73e7660863bb1ce827cb6008b4226c46fb18b2c9274d7990c25\n" +
            "append-entry\tuser\tBAS\t2016-04-05T13:23:05Z\tsha-256:6395e87181af53acd4bf9d96aca5d1d2de9af7927c80f4f8fc415da878f51039"
        );

        Response response = register.getRequest(local_authority_eng, "/index/local-authority-by-type/download-rsf");
        List<String> rsfLines = getRsfLinesFrom(response);

        assertThat(rsfLines.get(0), equalTo("assert-root-hash\tsha-256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"));
        assertThat(rsfLines.subList(1, 4), containsInAnyOrder(
            "add-item\t{\"local-authority-eng\":\"BAS\",\"local-authority-type\":\"UA\",\"name\":\"Bath\",\"official-name\":\"Bath\",\"start-date\":\"1996-04-01\"}",
            "add-item\t{\"local-authority-eng\":\"BAS\",\"local-authority-type\":\"UA\",\"name\":\"New Bath\",\"official-name\":\"Bath\",\"start-date\":\"1996-04-01\"}",
            "add-item\t{\"local-authority-eng\":\"BAS\",\"local-authority-type\":\"MD\",\"name\":\"New Bath\",\"official-name\":\"Bath\",\"start-date\":\"1996-04-01\"}"
        ));

        assertFormattedEntry(rsfLines.get(4), "UA", "sha-256:0957b9d2e02eab7ffeda290dac485dacab54abd0264a5eb789906e091856fbeb");
        assertFormattedEntry(rsfLines.get(5), "UA", "sha-256:05bc13575c70f73e7660863bb1ce827cb6008b4226c46fb18b2c9274d7990c25");
        assertFormattedEntry(rsfLines.get(6), "MD", "sha-256:6395e87181af53acd4bf9d96aca5d1d2de9af7927c80f4f8fc415da878f51039");
        assertFormattedEntry(rsfLines.get(7), "UA", "");
    }

    @Test
    public void downloadIndexRSF_shouldReturnCorrectIndexRSF_whenRegisterRSFContainsItemThatExistsAgainstMultipleEntries() {
        System.setProperty("multi-item-entries-enabled", "true");
        Response res = register.loadRsf(local_authority_eng,
            "add-item\t{\"local-authority-eng\":\"BAS\",\"local-authority-type\":\"R\",\"name\":\"Bath\",\"official-name\":\"Bath\",\"start-date\":\"1996-04-01\"}\n" +
            "add-item\t{\"local-authority-eng\":\"DDE\",\"local-authority-type\":\"P\",\"name\":\"Dundee\",\"official-name\":\"Dundee\",\"start-date\":\"1996-04-01\"}\n" +
            "add-item\t{\"local-authority-eng\":\"CRK\",\"local-authority-type\":\"Q\",\"name\":\"Cork\",\"official-name\":\"Cork\",\"start-date\":\"1996-04-01\"}\n" +
            "append-entry\tuser\tfirstKey\t2016-04-05T13:23:05Z\tsha-256:6ce441486301fadb5527fe9173da7bb3ad176a21459f06c1eb6da70bf417d7fe\n" +
            "append-entry\tuser\tfirstKey\t2016-04-05T13:23:05Z\tsha-256:6ce441486301fadb5527fe9173da7bb3ad176a21459f06c1eb6da70bf417d7fe;sha-256:59516279c591e62cb7ae7defbdd2e638d506056c5a6013e13d1b7926b21259ba\n" +
            "append-entry\tuser\tsecondKey\t2016-04-05T13:23:05Z\tsha-256:59516279c591e62cb7ae7defbdd2e638d506056c5a6013e13d1b7926b21259ba\n" +
            "append-entry\tuser\tsecondKey\t2016-04-05T13:23:05Z\tsha-256:59516279c591e62cb7ae7defbdd2e638d506056c5a6013e13d1b7926b21259ba;sha-256:71d12f0d5fd71b09be0a306902d8d3a17fb7d9896e087a5e0c1904b54746f231\n" +
            "append-entry\tuser\tsecondKey\t2016-04-05T13:23:05Z\tsha-256:71d12f0d5fd71b09be0a306902d8d3a17fb7d9896e087a5e0c1904b54746f231"
        );

        Response response = register.getRequest(local_authority_eng, "/index/local-authority-by-type/download-rsf");
        List<String> rsfLines = getRsfLinesFrom(response);

        assertThat(rsfLines.get(0), equalTo("assert-root-hash\tsha-256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"));
        assertThat(rsfLines.subList(1, 4), containsInAnyOrder(
            "add-item\t{\"local-authority-eng\":\"BAS\",\"local-authority-type\":\"R\",\"name\":\"Bath\",\"official-name\":\"Bath\",\"start-date\":\"1996-04-01\"}",
            "add-item\t{\"local-authority-eng\":\"DDE\",\"local-authority-type\":\"P\",\"name\":\"Dundee\",\"official-name\":\"Dundee\",\"start-date\":\"1996-04-01\"}",
            "add-item\t{\"local-authority-eng\":\"CRK\",\"local-authority-type\":\"Q\",\"name\":\"Cork\",\"official-name\":\"Cork\",\"start-date\":\"1996-04-01\"}"
        ));

        assertFormattedEntry(rsfLines.get(4), "R", "sha-256:6ce441486301fadb5527fe9173da7bb3ad176a21459f06c1eb6da70bf417d7fe");
        assertFormattedEntry(rsfLines.get(5), "P", "sha-256:59516279c591e62cb7ae7defbdd2e638d506056c5a6013e13d1b7926b21259ba");
        assertFormattedEntry(rsfLines.get(6), "Q", "sha-256:71d12f0d5fd71b09be0a306902d8d3a17fb7d9896e087a5e0c1904b54746f231");
    }

    @Test
    public void downloadIndexRSF_shouldReturnCorrectIndexRSF_whenRegisterRSFAddsTwoItemsAgainstDifferentKeysThenRemovesTheFirstInstance() throws IOException {
        System.setProperty("multi-item-entries-enabled", "true");
        Response res = register.loadRsf(local_authority_eng,
            "add-item\t{\"local-authority-eng\":\"BAS\",\"local-authority-type\":\"R\",\"name\":\"Bath\",\"official-name\":\"Bath\",\"start-date\":\"1996-04-01\"}\n" +
            "add-item\t{\"local-authority-eng\":\"DDE\",\"local-authority-type\":\"P\",\"name\":\"Dundee\",\"official-name\":\"Dundee\",\"start-date\":\"1996-04-01\"}\n" +
            "append-entry\tuser\tfirstKey\t2016-04-05T13:23:05Z\tsha-256:6ce441486301fadb5527fe9173da7bb3ad176a21459f06c1eb6da70bf417d7fe\n" +
            "append-entry\tuser\tfirstKey\t2016-04-05T13:23:05Z\tsha-256:6ce441486301fadb5527fe9173da7bb3ad176a21459f06c1eb6da70bf417d7fe;sha-256:59516279c591e62cb7ae7defbdd2e638d506056c5a6013e13d1b7926b21259ba\n" +
            "append-entry\tuser\tsecondKey\t2016-04-05T13:23:05Z\tsha-256:59516279c591e62cb7ae7defbdd2e638d506056c5a6013e13d1b7926b21259ba\n" +
            "append-entry\tuser\tfirstKey\t2016-04-05T13:23:05Z\tsha-256:6ce441486301fadb5527fe9173da7bb3ad176a21459f06c1eb6da70bf417d7fe\n"
        );

        Response response = register.getRequest(local_authority_eng, "/index/local-authority-by-type/download-rsf");
        List<String> rsfLines = getRsfLinesFrom(response);

        assertThat(rsfLines.get(0), equalTo("assert-root-hash\tsha-256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"));
        assertThat(rsfLines.subList(1, 3), containsInAnyOrder(
            "add-item\t{\"local-authority-eng\":\"BAS\",\"local-authority-type\":\"R\",\"name\":\"Bath\",\"official-name\":\"Bath\",\"start-date\":\"1996-04-01\"}",
            "add-item\t{\"local-authority-eng\":\"DDE\",\"local-authority-type\":\"P\",\"name\":\"Dundee\",\"official-name\":\"Dundee\",\"start-date\":\"1996-04-01\"}"
        ));

        assertFormattedEntry(rsfLines.get(3), "R", "sha-256:6ce441486301fadb5527fe9173da7bb3ad176a21459f06c1eb6da70bf417d7fe");
        assertFormattedEntry(rsfLines.get(4), "P", "sha-256:59516279c591e62cb7ae7defbdd2e638d506056c5a6013e13d1b7926b21259ba");

        Response indexRecordsResponse = register.getRequest(local_authority_eng, "/index/local-authority-by-type/record/P.json");
        assertThat(indexRecordsResponse.getStatus(), is(200));

        JsonNode result = Jackson.newObjectMapper().readValue(indexRecordsResponse.readEntity(String.class), JsonNode.class).get("P");
        assertThat(result.get("entry-number").textValue(), is("3"));
        assertThat(result.get("index-entry-number").textValue(), is("2"));
    }

    private List<String> getRsfLinesFrom(Response response) {
        InputStream is = response.readEntity(InputStream.class);
        return new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.toList());
    }

    private void assertFormattedEntry(String actualEntry, String expectedKey, String expectedHash) {
        String[] parts = actualEntry.split("\t");
        List<String> expectedHashes = expectedHash == "" ? Arrays.asList() : Arrays.asList(expectedHash);

        if (expectedHashes.isEmpty()) {
            assertThat(parts.length, is(4));
        }
        else {
            assertThat(parts.length, is(5));
            assertThat(expectedHashes.contains(parts[4]), is(true));
        }

        assertThat(parts[0], is("append-entry"));
        assertThat(parts[1], is("user"));
        assertThat(parts[2], is(expectedKey));
    }

    private Map<String, JsonNode> getEntries(InputStream inputStream) throws IOException {
        ZipInputStream zis = new ZipInputStream(inputStream);

        Map<String, JsonNode> entries = new HashMap<>();
        byte[] buffer = new byte[1024];
        int read = 0;
        for (ZipEntry entry; (entry = zis.getNextEntry()) != null; ) {
            StringBuilder sb = new StringBuilder();
            while ((read = zis.read(buffer, 0, 1024)) >= 0) {
                sb.append(new String(buffer, 0, read));
            }

            entries.put(entry.getName(), new ObjectMapper().readTree(sb.toString()));
        }

        return entries;
    }
}
