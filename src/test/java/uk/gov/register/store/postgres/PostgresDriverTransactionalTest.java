package uk.gov.register.store.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.skife.jdbi.v2.Handle;
import uk.gov.register.core.Entry;
import uk.gov.register.core.Item;
import uk.gov.register.core.Record;
import uk.gov.register.db.*;
import uk.gov.verifiablelog.store.memoization.MemoizationStore;

import java.util.*;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PostgresDriverTransactionalTest {

    List<Entry> entries;
    List<Item> items;
    List<CurrentKey> currentKeys;

    EntryQueryDAO entryQueryDAO;
    EntryDAO entryDAO;
    ItemQueryDAO itemQueryDAO;
    ItemDAO itemDAO;
    RecordQueryDAO recordQueryDAO;
    CurrentKeysUpdateDAO currentKeysUpdateDAO;

    @Mock
    Handle handle;
    @Mock
    MemoizationStore memoizationStore;

    ArgumentCaptor<Collection> argumentCaptor = ArgumentCaptor.forClass(Collection.class);

    @Before
    public void setup() {
        entries = new ArrayList<>();
        items = new ArrayList<>();
        currentKeys = new ArrayList<>();

        entryQueryDAO = mock(EntryQueryDAO.class);
        entryDAO = mock(EntryDAO.class);
        itemQueryDAO = mock(ItemQueryDAO.class);
        itemDAO = mock(ItemDAO.class);
        recordQueryDAO = mock(RecordQueryDAO.class);
        currentKeysUpdateDAO = mock(CurrentKeysUpdateDAO.class);

        mockEntryDAOInsert();
        mockItemDAOInsert();
        mockCurrentKeysUpdateDAOInsert();
    }

    @Test
    public void insertItemEntryRecordShouldNotCommitData() {
        PostgresDriverTransactional postgresDriver = new PostgresDriverTransactional(
                handle, memoizationStore, h -> entryQueryDAO, h -> entryDAO, h -> itemQueryDAO, h -> itemDAO, h -> recordQueryDAO, h -> currentKeysUpdateDAO);

        postgresDriver.insertItem(mock(Item.class));
        postgresDriver.insertEntry(mock(Entry.class));
        postgresDriver.insertEntry(mock(Entry.class));
        postgresDriver.insertRecord(mockRecord("country", "DE", 1), "country");

        assertThat(items, is(empty()));
        assertThat(entries, is(empty()));
        assertThat(currentKeys, is(empty()));
    }

    @Test
    public void getEntryShouldAlwaysCommitStagedData() {
        when(entryQueryDAO.findByEntryNumber(1)).thenReturn(Optional.empty());
        assertStagedDataIsCommittedOnAction(postgresDriver -> postgresDriver.getEntry(1));
    }

    @Test
    public void getEntriesShouldAlwaysCommitStagedData() {
        when(entryQueryDAO.getEntries(1, 10)).thenReturn(asList());
        assertStagedDataIsCommittedOnAction(postgresDriver -> postgresDriver.getEntries(1, 10));
    }

    @Test
    public void getAllEntriesShouldAlwaysCommitStagedData() {
        when(entryQueryDAO.getAllEntriesNoPagination()).thenReturn(asList());
        assertStagedDataIsCommittedOnAction(PostgresDriverTransactional::getAllEntries);
    }

    @Test
    public void getTotalEntriesShouldAlwaysCommitStagedData() {
        when(entryQueryDAO.getTotalEntries()).thenReturn(10);
        assertStagedDataIsCommittedOnAction(PostgresDriverTransactional::getTotalEntries);
    }

    @Test
    public void getLastUpdatedTimeShouldAlwaysCommitStagedData() {
        when(entryQueryDAO.getLastUpdatedTime()).thenReturn(Optional.empty());
        assertStagedDataIsCommittedOnAction(PostgresDriverTransactional::getLastUpdatedTime);
    }

    @Test
    public void getAllItemsShouldAlwaysCommitStagedData() {
        when(itemQueryDAO.getAllItemsNoPagination()).thenReturn(asList());
        assertStagedDataIsCommittedOnAction(PostgresDriverTransactional::getAllItems);
    }

    @Test
    public void getRecordShouldAlwaysCommitStagedData() {
        when(recordQueryDAO.findByPrimaryKey("DE")).thenReturn(Optional.empty());
        assertStagedDataIsCommittedOnAction(postgresDriver -> postgresDriver.getRecord("DE"));
    }

    @Test
    public void getTotalRecordsShouldAlwaysCommitStagedData() {
        when(recordQueryDAO.getTotalRecords()).thenReturn(10);
        assertStagedDataIsCommittedOnAction(PostgresDriverTransactional::getTotalRecords);
    }

    @Test
    public void getRecordsShouldAlwaysCommitStagedData() {
        when(recordQueryDAO.getRecords(10, 0)).thenReturn(asList());
        assertStagedDataIsCommittedOnAction(postgresDriver -> postgresDriver.getRecords(10, 0));
    }

    @Test
    public void findMax100RecordsByKeyValueShouldAlwaysCommitStagedData() {
        when(recordQueryDAO.findMax100RecordsByKeyValue("name", "Germany")).thenReturn(asList());
        assertStagedDataIsCommittedOnAction(postgresDriver -> postgresDriver.findMax100RecordsByKeyValue("name", "Germany"));
    }

    @Test
    public void findAllEntriesOfRecordByShouldAlwaysCommitStagedData() {
        when(recordQueryDAO.findAllEntriesOfRecordBy("country", "DE")).thenReturn(asList());
        assertStagedDataIsCommittedOnAction(postgresDriver -> postgresDriver.findAllEntriesOfRecordBy("country", "DE"));
    }

    @Test
    public void getItemBySha256ShouldCommitStagedDataOnlyIfItemNotStaged() {
        ArgumentCaptor<String> hashArgumentCaptor = ArgumentCaptor.forClass(String.class);
        when(itemQueryDAO.getItemBySHA256(hashArgumentCaptor.capture()))
                .thenReturn(items.stream().filter(item -> item.getSha256hex().equals(hashArgumentCaptor.getValue())).findFirst());

        PostgresDriverTransactional postgresDriver = new PostgresDriverTransactional(
                handle, memoizationStore, h -> entryQueryDAO, h -> entryDAO, h -> itemQueryDAO, h -> itemDAO, h -> recordQueryDAO, h -> currentKeysUpdateDAO);

        items.add(new Item("itemhash1", new ObjectMapper().createObjectNode()));
        entries.add(mock(Entry.class));
        currentKeys.add(new CurrentKey("DE", 1));

        assertThat(items.size(), is(1));
        assertThat(entries.size(), is(1));
        assertThat(currentKeys.size(), is(1));

        postgresDriver.insertItem(new Item("itemhash2", new ObjectMapper().createObjectNode()));
        postgresDriver.insertEntry(mock(Entry.class));
        postgresDriver.insertRecord(mockRecord("country", "DE", 2), "country");

        postgresDriver.getItemBySha256("itemhash2");

        assertThat(items.size(), is(1));
        assertThat(entries.size(), is(1));
        assertThat(currentKeys.size(), is(1));

        postgresDriver.getItemBySha256("itemhash1");

        assertThat(items.size(), is(2));
        assertThat(entries.size(), is(2));
        assertThat(currentKeys.size(), is(2));

    }

    private void assertStagedDataIsCommittedOnAction(Consumer<PostgresDriverTransactional> actionToTest) {
        PostgresDriverTransactional postgresDriver = new PostgresDriverTransactional(
                handle, memoizationStore, h -> entryQueryDAO, h -> entryDAO, h -> itemQueryDAO, h -> itemDAO, h -> recordQueryDAO, h -> currentKeysUpdateDAO);

        postgresDriver.insertItem(mock(Item.class));
        postgresDriver.insertItem(mock(Item.class));
        postgresDriver.insertEntry(mock(Entry.class));
        postgresDriver.insertRecord(mockRecord("country", "DE", 1), "country");

        assertThat(items, is(empty()));
        assertThat(entries, is(empty()));
        assertThat(currentKeys, is(empty()));

        actionToTest.accept(postgresDriver);

        assertThat(items.size(), is(2));
        assertThat(entries.size(), is(1));
        assertThat(currentKeys.size(), is(1));
    }

    private Record mockRecord(String registerName, String key, Integer entryNumber) {
        Entry entry = mock(Entry.class);
        Item item = mock(Item.class);
        when(entry.getEntryNumber()).thenReturn(entryNumber);
        when(item.getKey(registerName)).thenReturn(key);
        return new Record(entry, item);
    }

    private void mockEntryDAOInsert() {
        Mockito.doAnswer(invocation -> {
            entries.addAll(argumentCaptor.getValue());
            return null;
        }).when(entryDAO).insertInBatch(argumentCaptor.capture());
    }

    private void mockItemDAOInsert() {
        Mockito.doAnswer(invocation -> {
            items.addAll(argumentCaptor.getValue());
            return null;
        }).when(itemDAO).insertInBatch(argumentCaptor.capture());
    }

    private void mockCurrentKeysUpdateDAOInsert() {
        Mockito.doAnswer(invocation -> {
            currentKeys.addAll(argumentCaptor.getValue());
            return null;
        }).when(currentKeysUpdateDAO).writeCurrentKeys(argumentCaptor.capture());
    }
}
