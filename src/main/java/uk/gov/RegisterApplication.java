package uk.gov;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.java8.jdbi.DBIFactory;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jetty.MutableServletContextHandler;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ServerProperties;
import org.skife.jdbi.v2.DBI;
import uk.gov.mint.*;
import uk.gov.mint.monitoring.CloudWatchHeartbeater;
import uk.gov.organisation.client.GovukOrganisationClient;
import uk.gov.register.FieldsConfiguration;
import uk.gov.register.presentation.AssetsBundleCustomErrorHandler;
import uk.gov.register.presentation.ItemConverter;
import uk.gov.register.presentation.config.PublicBodiesConfiguration;
import uk.gov.register.presentation.config.RegistersConfiguration;
import uk.gov.register.presentation.dao.EntryQueryDAO;
import uk.gov.register.presentation.dao.ItemQueryDAO;
import uk.gov.register.presentation.dao.RecordQueryDAO;
import uk.gov.register.presentation.representations.ExtraMediaType;
import uk.gov.register.presentation.resource.RequestContext;
import uk.gov.register.presentation.view.ViewFactory;
import uk.gov.register.thymeleaf.ThymeleafViewRenderer;
import uk.gov.store.EntryStore;
import uk.gov.verifiablelog.store.memoization.InMemoryPowOfTwo;
import uk.gov.verifiablelog.store.memoization.MemoizationStore;

import javax.inject.Singleton;
import javax.servlet.DispatcherType;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RegisterApplication extends Application<RegisterConfiguration> {
    public static void main(String[] args) {
        try {
            new RegisterApplication().run(args);
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    public static ObjectMapper customObjectMapper() {
        ObjectMapper objectMapper = Jackson.newObjectMapper();
        objectMapper.registerModules(new Jdk8Module(), new JavaTimeModule());
        return objectMapper;
    }

    @Override
    public String getName() {
        return "openregister";
    }

    @Override
    public void initialize(Bootstrap<RegisterConfiguration> bootstrap) {
        bootstrap.addBundle(new ViewBundle<>(ImmutableList.of(new ThymeleafViewRenderer("HTML5", "/templates/", ".html", false))));
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                ));
        bootstrap.addBundle(new AssetsBundle("/assets"));
        bootstrap.addBundle(new Java8Bundle());
        bootstrap.setObjectMapper(customObjectMapper());
    }

    @Override
    public void run(RegisterConfiguration configuration, Environment environment) throws Exception {
        DBIFactory dbiFactory = new DBIFactory();
        DBI jdbi = dbiFactory.build(environment, configuration.getDatabase(), "postgres");

        EntryStore entryStore = jdbi.open().attach(EntryStore.class);
        ItemQueryDAO itemDAO = jdbi.onDemand(ItemQueryDAO.class);
        EntryQueryDAO entryDAO = jdbi.onDemand(EntryQueryDAO.class);
        RecordQueryDAO recordDAO = jdbi.onDemand(RecordQueryDAO.class);

        RegistersConfiguration registersConfiguration = new RegistersConfiguration(Optional.ofNullable(System.getProperty("registersYaml")));
        FieldsConfiguration mintFieldsConfiguration = new FieldsConfiguration(Optional.ofNullable(System.getProperty("fieldsYaml")));

        JerseyEnvironment jersey = environment.jersey();
        DropwizardResourceConfig resourceConfig = jersey.getResourceConfig();

        ImmutableMap<String, MediaType> representations = ImmutableMap.of(
                "csv", ExtraMediaType.TEXT_CSV_TYPE,
                "tsv", ExtraMediaType.TEXT_TSV_TYPE,
                "ttl", ExtraMediaType.TEXT_TTL_TYPE,
                "json", MediaType.APPLICATION_JSON_TYPE,
                "yaml", ExtraMediaType.TEXT_YAML_TYPE
        );
        resourceConfig.property(ServerProperties.MEDIA_TYPE_MAPPINGS, representations);

        Client client = new JerseyClientBuilder(environment).using(configuration.getJerseyClientConfiguration())
                .build("http-client");

        jersey.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(entryStore).to(EntryStore.class);
                bind(itemDAO).to(ItemQueryDAO.class);
                bind(entryDAO).to(EntryQueryDAO.class);
                bind(recordDAO).to(RecordQueryDAO.class);
                bind(mintFieldsConfiguration).to(FieldsConfiguration.class);
                bind(registersConfiguration).to(RegistersConfiguration.class);
                bind(new PublicBodiesConfiguration(Optional.ofNullable(System.getProperty("publicBodiesYaml")))).to(PublicBodiesConfiguration.class);

                bind(ItemValidator.class).to(ItemValidator.class);
                bind(ObjectReconstructor.class).to(ObjectReconstructor.class);

                bind(RequestContext.class).to(RequestContext.class);
                bind(ViewFactory.class).to(ViewFactory.class).in(Singleton.class);
                bind(ItemConverter.class).to(ItemConverter.class).in(Singleton.class);
                bind(GovukOrganisationClient.class).to(GovukOrganisationClient.class).in(Singleton.class);
                bind(InMemoryPowOfTwo.class).to(MemoizationStore.class).in(Singleton.class);
                bind(configuration);
                bind(client).to(Client.class);
            }
        });

        resourceConfig.packages(
                "uk.gov.register.presentation.filter",
                "uk.gov.register.presentation.representations",
                "uk.gov.register.presentation.resource");

        jersey.register(ItemValidationExceptionMapper.class);
        jersey.register(JsonParseExceptionMapper.class);
        jersey.register(MintService.class);

        configuration.getAuthenticator().build()
                .ifPresent(authenticator ->
                        jersey.register(new AuthDynamicFeature(
                                new BasicCredentialAuthFilter.Builder<User>()
                                        .setAuthenticator(authenticator)
                                        .buildAuthFilter()
                        ))
                );

        if (configuration.cloudWatchEnvironmentName().isPresent()) {
            ScheduledExecutorService cloudwatch = environment.lifecycle().scheduledExecutorService("cloudwatch").threads(1).build();
            cloudwatch.scheduleAtFixedRate(new CloudWatchHeartbeater(configuration.cloudWatchEnvironmentName().get(), configuration.getRegister()), 0, 10000, TimeUnit.MILLISECONDS);
        }

        setCorsPreflight(environment.getApplicationContext());

        environment.getApplicationContext().setErrorHandler(new AssetsBundleCustomErrorHandler(environment));
    }

    private void setCorsPreflight(MutableServletContextHandler applicationContext) {
        FilterHolder filterHolder = applicationContext
                .addFilter(CrossOriginFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
        filterHolder.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        filterHolder.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin");
        filterHolder.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,HEAD");

        filterHolder.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "false");
    }
}


