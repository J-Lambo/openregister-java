package uk.gov.register.core;

import org.junit.Before;
import org.junit.Test;
import uk.gov.register.configuration.RegisterFieldsConfiguration;
import uk.gov.register.db.InMemoryEntryDAO;
import uk.gov.register.exceptions.NoSuchFieldException;
import uk.gov.register.service.ItemValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;
import static uk.gov.register.db.InMemoryStubs.inMemoryEntryLog;
import static uk.gov.register.db.InMemoryStubs.inMemoryItemStore;

public class PostgresRegisterTest {
    private final InMemoryEntryDAO entryDAO = new InMemoryEntryDAO(new ArrayList<>());
    private RecordIndex recordIndex;
    private ItemValidator itemValidator;
    private RegisterFieldsConfiguration registerFieldsConfiguration;
    private PostgresRegister register;

    @Before
    public void setup() {
        recordIndex = mock(RecordIndex.class);
        itemValidator = mock(ItemValidator.class);
        registerFieldsConfiguration = mock(RegisterFieldsConfiguration.class);
        register = new PostgresRegister(registerMetadata("register"), registerFieldsConfiguration, inMemoryEntryLog(entryDAO), inMemoryItemStore(itemValidator, entryDAO), recordIndex);

    }

    @Test(expected = NoSuchFieldException.class)
    public void findMax100RecordsByKeyValueShouldFailWhenKeyDoesNotExist() {
        register.max100RecordsFacetedByKeyValue("citizen-name", "British");
    }

    @Test
    public void findMax100RecordsByKeyValueShouldReturnValueWhenKeyExists() {
        when(registerFieldsConfiguration.containsField("name")).thenReturn(true);
        register.max100RecordsFacetedByKeyValue("name", "United Kingdom");
        verify(recordIndex, times(1)).findMax100RecordsByKeyValue("name", "United Kingdom");
    }

    @Test
    public void shouldReturnRecordWithItems() {
        Entry entry = mock(Entry.class);
        Item item = mock(Item.class);
        Record record = new Record(entry, Collections.singleton(item));
        when(recordIndex.getRecord("k")).thenReturn(Optional.of(record));
        Optional<Record> recordOptional = register.getRecord("k");
        assertThat(recordOptional.get().getEntry(), is(entry));
    }

    @Test
    public void shouldReturnEmptyWhenNoItems() {
        Entry entry = mock(Entry.class);
        Optional<Record> recordOpt = Optional.of(new Record(entry, Collections.EMPTY_SET));
        when(recordIndex.getRecord("k")).thenReturn(recordOpt);
        Optional<Record> recordOptional = register.getRecord("k");
        assertFalse(recordOptional.isPresent());
    }

    private RegisterMetadata registerMetadata(String registerName) {
        RegisterMetadata mock = mock(RegisterMetadata.class);
        when(mock.getRegisterName()).thenReturn(new RegisterName(registerName));
        return mock;
    }
}
