package uk.gov.register.functional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.gov.register.functional.app.RegisterRule;
import uk.gov.register.functional.app.TestRegister;
import uk.gov.register.functional.db.TestEntry;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static uk.gov.register.views.representations.ExtraMediaType.TEXT_HTML;

@RunWith(Parameterized.class)
public class AnalyticsFunctionalTest {

    private static final TestRegister REGISTER_WITH_MISSING_TRACKING_ID = TestRegister.register;
    private static final TestRegister REGISTER_WITH_EMPTY_TRACKING_ID = TestRegister.postcode;
    private static final TestRegister REGISTER_WITH_VALID_TRACKING_ID = TestRegister.address;

    private static final String testEntry1Key = "st1";
    private static final String testEntry2Key = "st2";
    private static final TestEntry testEntry1 = TestEntry.anEntry(1, "{\"street\":\"" + testEntry1Key + "\",\"address\":\"12345\"}", "12345");
    private static final TestEntry testEntry2 = TestEntry.anEntry(2, "{\"street\":\"" + testEntry2Key + "\",\"address\":\"12346\"}", "12346");

    @ClassRule
    public static RegisterRule register = new RegisterRule();

    @Before
    public void setup() {
        register.wipe();
        register.mintLines(REGISTER_WITH_MISSING_TRACKING_ID, testEntry1.itemJson, testEntry2.itemJson);
        register.mintLines(REGISTER_WITH_EMPTY_TRACKING_ID, testEntry1.itemJson, testEntry2.itemJson);
        register.mintLines(REGISTER_WITH_VALID_TRACKING_ID, testEntry1.itemJson, testEntry2.itemJson);
    }

    private final String targetUrl;

    public AnalyticsFunctionalTest(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    @Parameterized.Parameters(name = "{index} - url: {0}")
    public static List data() {
        return Arrays.asList(
                "/",
                "/download",
                "/entries",
                "/entry/9999999999",
                "/entry/1",
                "/entry/2",
                "/records",
                "/records/non-existent-record",
                "/records/non-existent-record/entries",
                "/records/" + testEntry1Key,
                "/records/" + testEntry2Key,
                "/records/" + testEntry1Key + "/entries",
                "/records/" + testEntry2Key + "/entries",
                "/item/sha-256:non-existent-item",
                "/item/" + testEntry1.sha256hex,
                "/item/" + testEntry2.sha256hex,
                "/not-found-page");
    }

    @Test
    public void emptyTrackingId_shouldNotIncludeAnalyticsCode() throws Exception {
        assertUrlHasResponseWithAppropriateAnalytics(REGISTER_WITH_EMPTY_TRACKING_ID, false);
    }

    @Test
    public void missingTrackingId_shouldNotIncludeAnalyticsCode() throws Exception {
        assertUrlHasResponseWithAppropriateAnalytics(REGISTER_WITH_MISSING_TRACKING_ID, false);
    }

    @Test
    public void validTrackingId_shouldIncludeAnalyticsCode() throws Exception {
        assertUrlHasResponseWithAppropriateAnalytics(REGISTER_WITH_VALID_TRACKING_ID, true);
    }

    private void assertUrlHasResponseWithAppropriateAnalytics(TestRegister testRegister, boolean shouldIncludeAnalytics) {
        Response response = register.getRequest(testRegister, targetUrl, TEXT_HTML);

        Document doc = Jsoup.parse(response.readEntity(String.class));
        assertThat(response.getStatus(), lessThan(500));

        Boolean docIncludesAnalyticsId = doc.getElementById("analytics-tracking-id") != null;
        Boolean docIncludesMainAnalytics = doc.getElementById("analytics-main") != null;
        Boolean docIncludesExtLinksAnalytics = doc.getElementById("analytics-external-links") != null;

        assertThat(docIncludesAnalyticsId, equalTo(shouldIncludeAnalytics));
        assertThat(docIncludesMainAnalytics, equalTo(shouldIncludeAnalytics));
        assertThat(docIncludesExtLinksAnalytics, equalTo(shouldIncludeAnalytics));
    }
}
