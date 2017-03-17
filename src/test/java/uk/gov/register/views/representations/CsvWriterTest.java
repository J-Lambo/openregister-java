package uk.gov.register.views.representations;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.register.core.*;
import uk.gov.register.util.HashValue;
import uk.gov.register.views.EntryListView;
import uk.gov.register.views.ItemView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CsvWriterTest {

    CsvWriter csvWriter = new CsvWriter();
    private final ImmutableList<Field> fields = ImmutableList.of(
            new Field("key1", "datatype", new RegisterName("register"),Cardinality.ONE, "text"),
            new Field("key2", "datatype", new RegisterName("register"),Cardinality.ONE, "text"),
            new Field("key3", "datatype", new RegisterName("register"),Cardinality.ONE, "text"),
            new Field("key4", "datatype", new RegisterName("register"),Cardinality.ONE, "text"));

    @Test
    public void writes_EntryListView_to_output_stream() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        csvWriter.writeTo(new EntryListView(
                        ImmutableList.of(new Entry(1, new HashValue(HashingAlgorithm.SHA256, "1234abcd"), Instant.ofEpochSecond(1400000000L), "abc"))),
                EntryListView.class,
                null,
                null,
                null,
                null,
                outputStream);
        byte[] bytes = outputStream.toByteArray();
        String generatedCsv = new String(bytes, StandardCharsets.UTF_8);
        assertThat(generatedCsv, is("entry-number,entry-timestamp,item-hashes,key\r\n1,2014-05-13T16:53:20Z,sha-256:1234abcd,abc\r\n"));
    }

    @Test
    public void writeEntriesTo_writesCsvEscapedEntries() throws IOException {

        ImmutableMap<String, FieldValue> fieldValueMap = ImmutableMap.of(
                "key1", new StringValue("valu\te1"),
                "key2", new StringValue("val,ue2"),
                "key3", new StringValue("val\"ue3"),
                "key4", new StringValue("val\nue4")
        );
        ItemView itemView = new ItemView(null, fieldValueMap, fields);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        csvWriter.writeTo(itemView, itemView.getClass(), null, null, null, null, outputStream);
        byte[] bytes = outputStream.toByteArray();
        String generatedCsv = new String(bytes, StandardCharsets.UTF_8);
        assertThat(generatedCsv, is("key1,key2,key3,key4\r\n\"valu\te1\",\"val,ue2\",\"val\"\"ue3\",\"val\nue4\"\r\n"));
    }

    @Test
    public void writeEntriesTo_writesLists() throws IOException {
        ImmutableMap<String, FieldValue> fieldValueMap = ImmutableMap.of("key1",
                new ListValue(asList(
                        new StringValue("value1"),
                        new StringValue("value2"),
                        new StringValue("value3"))
                ),
                "key2",
                new ListValue(asList(
                        new StringValue("value4"),
                        new StringValue("value5"),
                        new StringValue("value6"))
                ));

        ItemView itemView = new ItemView(null, fieldValueMap, fields);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        csvWriter.writeTo(itemView, itemView.getClass(), null, null, null, null, outputStream);
        byte[] bytes = outputStream.toByteArray();
        String generatedCsv = new String(bytes, StandardCharsets.UTF_8);
        assertThat(generatedCsv, is("key1,key2,key3,key4\r\nvalue1;value2;value3,value4;value5;value6,,\r\n"));
    }

    @Test
    public void writeEntriesTo_includesAllColumnsEvenWhenValuesAreNotPresent() throws Exception {

        ImmutableMap<String, FieldValue> fieldValueMap = ImmutableMap.of(
                "key1", new StringValue("value1"),
                "key2", new StringValue(""),
                "key3", new StringValue(""),
                "key4", new StringValue("")
        );
        ItemView itemView = new ItemView(null, fieldValueMap, fields);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        csvWriter.writeTo(itemView, itemView.getClass(), null, null, null, null, outputStream);
        byte[] bytes = outputStream.toByteArray();
        String generatedCsv = new String(bytes, StandardCharsets.UTF_8);
        assertThat(generatedCsv, is("key1,key2,key3,key4\r\nvalue1,,,\r\n"));
    }
}
