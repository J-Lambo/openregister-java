package uk.gov.register.service;

import uk.gov.register.core.Register;
import uk.gov.register.core.RegisterContext;
import uk.gov.register.serialization.RSFCreator;
import uk.gov.register.serialization.RSFExecutor;
import uk.gov.register.serialization.RSFFormatter;
import uk.gov.register.serialization.RegisterCommand;
import uk.gov.register.serialization.RegisterResult;
import uk.gov.register.serialization.RegisterSerialisationFormat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.function.Function;

import javax.inject.Inject;

public class RegisterSerialisationFormatService {
    private final RegisterContext registerContext;
    private final RSFExecutor rsfExecutor;
    private final RSFCreator rsfCreator;

    @Inject
    public RegisterSerialisationFormatService(RegisterContext registerContext, RSFExecutor rsfExecutor, RSFCreator rsfCreator) {
        this.registerContext = registerContext;
        this.rsfExecutor = rsfExecutor;
        this.rsfCreator = rsfCreator;
    }

    public void writeTo(OutputStream output, RSFFormatter rsfFormatter) {
        writeTo(output, rsfFormatter, rsfCreator::create);
    }

    public void writeTo(OutputStream output, RSFFormatter RSFFormatter, int totalEntries1, int totalEntries2) {
        writeTo(output, RSFFormatter, register -> rsfCreator.create(register, totalEntries1, totalEntries2));
    }

    public void writeTo(OutputStream output, RSFFormatter RSFFormatter, String indexName) {
        writeTo(output, RSFFormatter, register -> rsfCreator.create(register, indexName));
    }

    public void writeTo(OutputStream output, RSFFormatter RSFFormatter, String indexName, int totalEntries1, int totalEntries2) {
        writeTo(output, RSFFormatter, register -> rsfCreator.create(register, indexName, totalEntries1, totalEntries2));
    }

    public RegisterResult process(RegisterSerialisationFormat rsf) {
        return registerContext.transactionalRegisterOperation(register -> {
            return rsfExecutor.execute(rsf, register);
        });
    }

    public RegisterSerialisationFormat readFrom(InputStream commandStream, RSFFormatter rsfFormatter) {
        BufferedReader buffer = new BufferedReader(new InputStreamReader(commandStream));
        Iterator<RegisterCommand> commandsIterator = buffer.lines()
                .map(rsfFormatter::parse)
                .iterator();
        return new RegisterSerialisationFormat(commandsIterator);
    }

    private void writeTo(OutputStream output, RSFFormatter rsfFormatter, Function<Register, RegisterSerialisationFormat> rsfCreatorFunc) {
        registerContext.transactionalRegisterOperation(register -> {
            Iterator<RegisterCommand> commands = rsfCreatorFunc.apply(register).getCommands();

            int commandCount = 0;
            try {
                while (commands.hasNext()) {
                    output.write(rsfFormatter.format(commands.next()).getBytes());

                    // TODO: is flushing every 10000 commands ok?
                    if (++commandCount >= 10000) {
                        output.flush();
                        commandCount = 0;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
