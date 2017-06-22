package uk.gov.register.functional;

import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.register.functional.app.RegisterRule;
import uk.gov.register.functional.app.RsfRegisterDefinition;
import uk.gov.register.functional.app.TestRegister;

import javax.ws.rs.core.Response;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static uk.gov.register.functional.app.TestRegister.postcode;

public class DeleteRegisterDataFunctionalTest {
    public static final TestRegister REGISTER_WHICH_ALLOWS_DELETING = postcode;
    @ClassRule
    public static final RegisterRule register = new RegisterRule();

    @Test
    public void deleteRegisterData_deletesAllDataFromDb() throws Exception {
        register.loadRsf(postcode, RsfRegisterDefinition.POSTCODE_REGISTER);

        String item1 = "{\"postcode\":\"P1\"}";
        String item2 = "{\"postcode\":\"P2\"}";

        Response mintResponse = register.mintLines(REGISTER_WHICH_ALLOWS_DELETING, item1, item2);
        assertThat(mintResponse.getStatus(), equalTo(204));

        Response entriesResponse1 = register.getRequest(REGISTER_WHICH_ALLOWS_DELETING, "/entries.json");
        List<?> entriesList = entriesResponse1.readEntity(List.class);
        assertThat(entriesList, hasSize(4));

        Response deleteResponse = register.deleteRegisterData(REGISTER_WHICH_ALLOWS_DELETING);
        assertThat(deleteResponse.getStatus(), equalTo(200));

        Response entriesResponse2 = register.getRequest(REGISTER_WHICH_ALLOWS_DELETING, "/entries.json");
        String entriesRawJSON = entriesResponse2.readEntity(String.class);

        assertThat(entriesRawJSON, is("[]"));
    }

    @Test
    public void deleteRegisterData_deletesProofCache() throws Exception {
        register.loadRsf(postcode, RsfRegisterDefinition.POSTCODE_REGISTER);

        String item1 = "{\"postcode\":\"P1\"}";
        String item2 = "{\"postcode\":\"P2\"}";

        register.deleteRegisterData(REGISTER_WHICH_ALLOWS_DELETING);
        register.mintLines(REGISTER_WHICH_ALLOWS_DELETING, item1, item2);

        Response proof1Response = register.getRequest(REGISTER_WHICH_ALLOWS_DELETING, "/proof/register/merkle:sha-256");
        assertThat(proof1Response.getStatus(), equalTo(200));
        String proof1 = proof1Response.readEntity(String.class);

        register.deleteRegisterData(REGISTER_WHICH_ALLOWS_DELETING);
        register.loadRsf(postcode, RsfRegisterDefinition.POSTCODE_REGISTER);
        register.mintLines(REGISTER_WHICH_ALLOWS_DELETING, item2, item1);

        Response proof2Response = register.getRequest(REGISTER_WHICH_ALLOWS_DELETING, "/proof/register/merkle:sha-256");
        assertThat(proof2Response.getStatus(), equalTo(200));
        String proof2 = proof2Response.readEntity(String.class);

        assertThat(proof2, not(equalTo(proof1)));
    }
}
