package uk.gov.register.presentation.functional;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import io.dropwizard.jackson.Jackson;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.LocalDate;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class EntryResourceFunctionalTest extends FunctionalTestBase {
    static String item1 = "{\"address\":\"6789\",\"name\":\"presley\"}";
    static String item2 = "{\"address\":\"145678\",\"name\":\"ellis\"}";

    @Before
    public void publishTestMessages() throws Throwable {
        cleanDatabaseRule.before();
        dbSupport.publishMessages(ImmutableList.of(
                String.format("{\"hash\":\"hash1\",\"entry\":%s}", item1),
                String.format("{\"hash\":\"hash2\",\"entry\":%s}", item2)
        ));
    }

    @Test
    public void getEntryByEntryNumber() throws JSONException, IOException {
        String sha256Hex = DigestUtils.sha256Hex(item1);

        Response response = getRequest("/entry/1.json");

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.getHeaders().get("cache-control").toString(), equalTo("[no-transform, max-age=31536000]"));

        JsonNode res = Jackson.newObjectMapper().readValue(response.readEntity(String.class), JsonNode.class);
        assertThat(res.get("entry-number").textValue(), equalTo("1"));
        assertThat(res.get("item-hash").textValue(), equalTo("sha-256:" + sha256Hex));
        assertThat(res.get("entry-timestamp").textValue(), containsString(LocalDate.now().toString()));
    }

    @Test
    public void return404ResponseWhenEntryNotExist() {
        assertThat(getRequest("/entry/5001").getStatus(), equalTo(404));
    }

    @Test
    public void return404ResponseWhenEntryNumberIsNotAnIntegerValue() {
        assertThat(getRequest("/entry/a2").getStatus(), equalTo(404));
    }
}
