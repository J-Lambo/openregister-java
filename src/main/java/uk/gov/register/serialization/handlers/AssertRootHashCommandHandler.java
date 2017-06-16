package uk.gov.register.serialization.handlers;

import uk.gov.register.core.Entry;
import uk.gov.register.core.HashingAlgorithm;
import uk.gov.register.core.Register;
import uk.gov.register.serialization.RSFFormatter;
import uk.gov.register.serialization.RegisterCommand;
import uk.gov.register.serialization.RegisterCommandHandler;
import uk.gov.register.serialization.RegisterResult;
import uk.gov.register.util.HashValue;

import java.util.ArrayList;
import java.util.List;

public class AssertRootHashCommandHandler extends RegisterCommandHandler {
    @Override
    protected RegisterResult executeCommand(RegisterCommand command, Register register) {
        try {
            // Get staged entries before they're pushed to the DB
            List<Entry> stagedEntries = new ArrayList<>(register.getStagedEntries());

            HashValue expectedHash = HashValue.decode(HashingAlgorithm.SHA256, command.getCommandArguments().get(RSFFormatter.RSF_ASSERT_ROOT_HASH_ARGUMENT_POSITION));
            HashValue actualHash = register.getRegisterProof().getRootHash();
            if (!actualHash.equals(expectedHash)) {
                String message = String.format("Root hashes don't match. Expected: %s actual: %s", expectedHash.toString(), actualHash.toString());
                return RegisterResult.createFailResult(message);
            }

            // Update indexes now that everything has been committed
            register.updateIndexes(stagedEntries);

            return RegisterResult.createSuccessResult();
        } catch (Exception e) {
            return RegisterResult.createFailResult("Exception when executing command: " + command, e);
        }
    }

    @Override
    public String getCommandName() {
        return "assert-root-hash";
    }
}
