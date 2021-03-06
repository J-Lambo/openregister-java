package uk.gov.register.resources;

import org.junit.Test;
import uk.gov.register.core.RegisterContext;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class DeleteRegisterDataResourceTest {
    @Test
    public void shouldUseFlywayToDeleteData() throws Exception {
        RegisterContext registerMock = mock(RegisterContext.class, RETURNS_DEEP_STUBS);
        DeleteRegisterDataResource sutResource = new DeleteRegisterDataResource(registerMock);

        Response response = sutResource.deleteRegisterData();

        assertThat(response.getStatus(), equalTo(200));
        verify(registerMock, times(1)).resetRegister();
    }
}

