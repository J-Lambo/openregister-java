package uk.gov.register.core;

import uk.gov.register.configuration.ConfigManager;
import uk.gov.register.configuration.DatabaseManager;
import uk.gov.register.service.RegisterLinkService;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class AllTheRegistersFactory {
    private RegisterContextFactory defaultRegisterFactory;
    private final Map<RegisterName, RegisterContextFactory> otherRegisters;
    private final RegisterName defaultRegisterName;

    public AllTheRegistersFactory(RegisterContextFactory defaultRegisterFactory, Map<RegisterName, RegisterContextFactory> otherRegisters, RegisterName defaultRegisterName) {
        this.defaultRegisterFactory = defaultRegisterFactory;
        this.otherRegisters = otherRegisters;
        this.defaultRegisterName = defaultRegisterName;
    }

    public AllTheRegisters build(ConfigManager configManager, DatabaseManager databaseManager, RegisterLinkService registerLinkService) {
        Map<RegisterName, RegisterContext> builtRegisters = otherRegisters.entrySet().stream().collect(toMap(Map.Entry::getKey,
                e -> buildRegister(e.getKey(), e.getValue(), configManager, databaseManager, registerLinkService)));
        return new AllTheRegisters(
                defaultRegisterFactory.build(defaultRegisterName, configManager, databaseManager, registerLinkService),
                builtRegisters
        );
    }

    private RegisterContext buildRegister(RegisterName registerName, RegisterContextFactory registerFactory, ConfigManager configManager, DatabaseManager databaseManager, RegisterLinkService registerLinkService) {
        return registerFactory.build(registerName, configManager, databaseManager, registerLinkService);
    }
}
