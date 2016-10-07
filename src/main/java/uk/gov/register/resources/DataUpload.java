package uk.gov.register.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.register.configuration.RegisterNameConfiguration;
import uk.gov.register.core.Item;
import uk.gov.register.core.Register;
import uk.gov.register.service.ItemValidator;
import uk.gov.register.util.ICommandParser;
import uk.gov.register.util.RegisterCommand;
import uk.gov.register.util.ObjectReconstructor;
import uk.gov.register.views.ViewFactory;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import java.util.Arrays;

@Path("/")
public class DataUpload {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final ViewFactory viewFactory;
    private String registerPrimaryKey;
    private ObjectReconstructor objectReconstructor;
    private ItemValidator itemValidator;
    private Register register;
    private ICommandParser commandParser;
    private Iterable<RegisterCommand> registerCommands;

    @Inject
    public DataUpload(ViewFactory viewFactory, RegisterNameConfiguration registerNameConfiguration, ObjectReconstructor objectReconstructor, ItemValidator itemValidator, Register register, ICommandParser commandParser, Iterable<RegisterCommand> registerCommands) {
        this.viewFactory = viewFactory;
        this.objectReconstructor = objectReconstructor;
        this.itemValidator = itemValidator;
        this.register = register;
        this.registerPrimaryKey = registerNameConfiguration.getRegister();
        this.commandParser = commandParser;
        this.registerCommands = registerCommands;
    }

    @Context
    HttpServletRequest httpServletRequest;

    @POST
    @PermitAll
    @Path("/load")
    public void load(String payload) {
        try {
            Iterable<JsonNode> objects = objectReconstructor.reconstruct(payload.split("\n"));
            objects.forEach(singleObject -> itemValidator.validateItem(registerPrimaryKey, singleObject));
            register.mintItems(Iterables.transform(objects, Item::new));
        } catch (Throwable t) {
            logger.error(Throwables.getStackTraceAsString(t));
            throw t;
        }
    }

    @POST
    @PermitAll
    @Path("/load-from-serialization")
    public void loadSerializationFormat(String payload) {
        try {
            if (payload == null || payload.isEmpty()) {
                throw new BadRequestException("Register serialization format has not been specified");
            }

            Iterable<RegisterCommand> registerCommands = commandParser.parseCommands(payload);

            registerCommands.forEach(registerCommand -> {
                if (!registerCommand.TryExecute()) {
                    RollbackTransaction();
                    return false;
                }
            });

            return true;

            String[] commands = payload.split("\n");

            String[] allParameters = payload.split("\t");

            // TODO: use regex to get this part of the string
            String command = allParameters[0];

            String[] parameters = allParameters.length > 1 ? Arrays.copyOfRange(allParameters, 1, allParameters.length);

        } catch (Throwable t) {
            logger.error(Throwables.getStackTraceAsString(t));
        }

    }
}

