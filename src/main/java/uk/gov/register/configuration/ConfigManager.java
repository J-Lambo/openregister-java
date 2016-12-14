package uk.gov.register.configuration;

import java.io.IOException;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class ConfigManager {
    private final Optional<String> registersConfigFileUrl;
    private final String registersConfigFilePath;
    private final Optional<String> fieldsConfigFileUrl;
    private final String fieldsConfigFilePath;
    private final boolean refresh;
    private final String externalConfigDirectory;

    public ConfigManager(RegisterConfigConfiguration registerConfigConfiguration, Optional<String> registersConfigFileUrl, Optional<String> fieldsConfigFileUrl) {
        this.registersConfigFileUrl = registersConfigFileUrl;
        this.fieldsConfigFileUrl = fieldsConfigFileUrl;
        this.refresh = registerConfigConfiguration.getDownloadConfigs();
        this.externalConfigDirectory = registerConfigConfiguration.getExternalConfigDirectory();
        registersConfigFilePath = externalConfigDirectory + "/" + "registers.yaml";
        fieldsConfigFilePath = externalConfigDirectory + "/" + "fields.yaml";
    }

    public void refreshConfig() throws IOException {
        if (refresh) {
            if (registersConfigFileUrl.isPresent()) {
                Files.copy(new URL(registersConfigFileUrl.get()).openStream(), Paths.get(externalConfigDirectory + "/" + "registers.yaml"), StandardCopyOption.REPLACE_EXISTING);
            }
            if (fieldsConfigFileUrl.isPresent()) {
                Files.copy(new URL(fieldsConfigFileUrl.get()).openStream(), Paths.get(externalConfigDirectory + "/" + "fields.yaml"), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    public RegistersConfiguration createRegistersConfiguration() throws IOException {
        return new RegistersConfiguration(registersConfigFileUrl.map(s -> registersConfigFilePath));
    }

    public FieldsConfiguration createFieldsConfiguration() {
        return new FieldsConfiguration(fieldsConfigFileUrl.map(s -> fieldsConfigFilePath));
    }
}