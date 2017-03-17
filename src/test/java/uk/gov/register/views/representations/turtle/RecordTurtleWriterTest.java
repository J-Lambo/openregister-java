package uk.gov.register.views.representations.turtle;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.register.core.*;
import uk.gov.register.util.HashValue;
import uk.gov.register.views.ItemView;
import uk.gov.register.views.RecordView;

import javax.inject.Provider;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class RecordTurtleWriterTest {

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
    public void shouldCreateRdfModel() throws Exception {
        Entry entry = new Entry(1, new HashValue(HashingAlgorithm.SHA256, "ab"), Instant.ofEpochSecond(1470403440), "b");
        ItemView itemView = new ItemView(new HashValue(HashingAlgorithm.SHA256, "ab"), ImmutableMap.of("a", new StringValue("b")), emptyList());
        ItemView itemView2 = new ItemView(new HashValue(HashingAlgorithm.SHA256, "ad"), ImmutableMap.of("a", new StringValue("d")), emptyList());
        RecordView recordView = new RecordView(entry, Lists.newArrayList(itemView, itemView2), emptyList());

        RecordTurtleWriter writer = new RecordTurtleWriter(registerNameProvider, registerResolver);

        Model model = writer.rdfModelFor(recordView);
        StmtIterator statements = model.listStatements();

        List<List<String>> triples = new LinkedList<>();

        while (statements.hasNext()) {
            Statement statement = statements.nextStatement();
            List<String> triple = Arrays.asList( statement.getSubject().toString(), statement.getPredicate().toString(), statement.getObject().toString());
            triples.add(triple);
        }

        List<String> tripleList0 = Arrays.asList("http://reg.openregister.org/record/b", "http://field.openregister.org/record/a", "d");
        List<String> tripleList1 = Arrays.asList("http://reg.openregister.org/record/b","http://field.openregister.org/record/a", "b");
        List<String> tripleList2 = Arrays.asList("http://reg.openregister.org/record/b","https://openregister.github.io/specification/#entry-number-field", "1");
        List<String> tripleList3 = Arrays.asList("http://reg.openregister.org/record/b","https://openregister.github.io/specification/#entry-timestamp-field", "2016-08-05T13:24:00Z");
        List<String> tripleList4 = Arrays.asList("http://reg.openregister.org/record/b","https://openregister.github.io/specification/#item-resource", "http://reg.openregister.org/item/sha-256:ab");
        List<String> tripleList5 = Arrays.asList("http://reg.openregister.org/record/b","http://field.openregister.org/record/a", "d");

        assertThat(triples, hasItems(tripleList0, tripleList1, tripleList2, tripleList3, tripleList4, tripleList5) ) ;

    }

}
