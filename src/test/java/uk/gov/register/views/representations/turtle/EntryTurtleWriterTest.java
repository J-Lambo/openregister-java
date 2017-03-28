package uk.gov.register.views.representations.turtle;

import com.google.common.collect.ImmutableMap;
import org.apache.jena.rdf.model.Model;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.register.core.*;
import uk.gov.register.util.HashValue;
import uk.gov.register.views.ItemView;

import javax.inject.Provider;
import java.io.StringWriter;
import java.net.URI;
import java.time.Instant;

import static java.util.Collections.emptyList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class EntryTurtleWriterTest {

    @Mock
    private Provider<RegisterName> registerNameProvider;
    @Mock
    private RegisterResolver registerResolver;

    private RegisterName registerName = new RegisterName("reg");

    private RegisterName fieldRegister = new RegisterName("field");

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        URI uri = new URI("http://reg.openregister.org");
        URI fieldUri = new URI("http://field.openregister.org");
        when(registerNameProvider.get()).thenReturn(registerName);
        when(registerResolver.baseUriFor(registerName)).thenReturn(uri);
        when(registerResolver.baseUriFor(fieldRegister)).thenReturn(fieldUri);
    }

    @Test
    public void rdfModelFor() throws Exception {
        Entry entry = new Entry(1, new HashValue(HashingAlgorithm.SHA256, "ab"), Instant.ofEpochSecond(1470403440), "b");

        EntryTurtleWriter writer = new EntryTurtleWriter(registerNameProvider, registerResolver);

        Model model = writer.rdfModelFor(entry);

        StringWriter sw = new StringWriter();
        model.write(sw, "TURTLE");

        assertThat(sw.toString(), Matchers.equalToIgnoringWhiteSpace( expectedTurtle ));
    }

    private String expectedTurtle = "@prefix register-metadata:  <https://openregister.github.io/specification/#> . " +
            "<http://reg.openregister.org/entry/1> " +
            "      register-metadata:entry-number-field " +
            "              \"1\" ; " +
            "      register-metadata:entry-timestamp-field " +
            "              \"2016-08-05T13:24:00Z\" ; " +
            "      register-metadata:index-entry-number-field " +
            "              \"1\" ; " +
            "      register-metadata:item-resource " +
            "              register-metadata:item-resource ; " +
            "      register-metadata:key-field " +
            "              \"b\" . " +
            "register-metadata:item-resource " +
            "      a       <http://www.w3.org/1999/02/22-rdf-syntax-ns#Bag> ; " +
            "      <http://www.w3.org/1999/02/22-rdf-syntax-ns#_1> " +
            "              <http://reg.openregister.org/item/sha-256:ab> .";

}
