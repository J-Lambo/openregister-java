package uk.gov.register.core;

import uk.gov.register.views.ConsistencyProof;
import uk.gov.register.views.EntryProof;
import uk.gov.register.views.RegisterProof;

import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RegisterReadOnly extends GitLikeBits {
    Optional<Item> getItemBySha256(String sha256hex);
    Collection<Item> getAllItems();

    Optional<Entry> getEntry(int entryNumber);
    Collection<Entry> getEntries(int start, int limit);
    Collection<Entry> getAllEntries();
    int getTotalEntries();

    Optional<Instant> getLastUpdatedTime();

    Optional<Record> getRecord(String key); // TODO: is "record" the right word?
    int getTotalRecords();
    Collection<Entry> allEntriesOfRecord(String key);
    List<Record> getRecords(int limit, int offset);

    List<Record> max100RecordsFacetedByKeyValue(String key, String value);

    RegisterProof getRegisterProof() throws NoSuchAlgorithmException;
    EntryProof getEntryProof(int entryNumber, int totalEntries);
    ConsistencyProof getConsistencyProof(int totalEntries1, int totalEntries2);
}

