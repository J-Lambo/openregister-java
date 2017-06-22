package uk.gov.register.core;

import org.flywaydb.core.Flyway;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import uk.gov.register.auth.RegisterAuthenticator;
import uk.gov.register.configuration.ConfigManager;
import uk.gov.register.configuration.RegistersConfiguration;
import uk.gov.register.exceptions.NoSuchConfigException;
import uk.gov.register.service.RegisterLinkService;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.*;

public class RegisterContextTest {
    private RegisterName registerName;
    private ConfigManager configManager;
    private RegisterLinkService registerLinkService;
    private DBI dbi;
    private Flyway flyway;
    private String schema;

    @Before
    public void setup() {
        registerName = new RegisterName("register");
        schema = "register";
        configManager = mock(ConfigManager.class, RETURNS_DEEP_STUBS);
        registerLinkService = mock(RegisterLinkService.class);
        dbi = mock(DBI.class);
        flyway = mock(Flyway.class);
    }

    @Test
    public void resetRegister_shouldNotResetRegister_whenEnableRegisterDataDeleteIsDisabled() throws IOException, NoSuchConfigException {
        RegisterContext context = new RegisterContext(registerName, configManager, registerLinkService, dbi, flyway, schema, Optional.empty(), false, false, Optional.empty(), emptyList(), emptyList(), new RegisterAuthenticator("", ""));
        context.resetRegister();

        verify(flyway, never()).clean();
        verify(configManager, never()).refreshConfig();
        verify(flyway, never()).migrate();
    }

    @Test
    public void resetRegister_shouldResetRegister_whenEnableRegisterDataDeleteIsEnabled() throws IOException, NoSuchConfigException {
        RegisterContext context = new RegisterContext(registerName, configManager, registerLinkService, dbi, flyway, schema, Optional.empty(), true, false, Optional.empty(), emptyList(), emptyList(), new RegisterAuthenticator("", ""));
        context.resetRegister();

        verify(flyway, times(1)).clean();
        verify(configManager, times(1)).refreshConfig();
        verify(flyway, times(1)).migrate();
    }

    @Test
    public void getRegisterMetadata_returnsUpToDateConfigProvidedByConfigManager() {
        RegisterMetadata expectedInitialMetadata = new RegisterMetadata(
                new RegisterName("test-register-1"),
                Collections.emptyList(),
                "copyright-1",
                "registry-1",
                "text-1",
                "phase-1");

        RegisterMetadata expectedUpdatedMetadata = new RegisterMetadata(
                new RegisterName("test-register-2"),
                Collections.emptyList(),
                "copyright-2",
                "registry-2",
                "text-2",
                "phase-2");


        RegistersConfiguration rcMock = mock(RegistersConfiguration.class);
        when(configManager.getRegistersConfiguration()).thenReturn(rcMock);
        when(rcMock.getRegisterMetadata(registerName))
                .thenReturn(expectedInitialMetadata)
                .thenReturn(expectedUpdatedMetadata);

        RegisterContext context = new RegisterContext(registerName, configManager, registerLinkService, dbi, flyway, schema, Optional.empty(), true, false, Optional.empty(), emptyList(), emptyList(), new RegisterAuthenticator("", ""));

        RegisterMetadata actualInitialMetadata = context.getRegisterMetadata();
        assertThat(actualInitialMetadata, equalTo(expectedInitialMetadata));

        RegisterMetadata actualUpdatedMetadata = context.getRegisterMetadata();
        assertThat(actualUpdatedMetadata, equalTo(expectedUpdatedMetadata));
    }
}
