package uk.gov.register.core;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RecordIndex {
    //void updateRecordIndex(Entry entry);

    Optional<Record> getRecord(String key);

    int getTotalRecords();

    List<Record> getRecords(int limit, int offset);

    List<Record> findMax100RecordsByKeyValue(String key, String value);

    Collection<Entry> findAllEntriesOfRecordBy(String key);

    // indexes
    Optional<Record> getRecord(String key, String derivationName);

    List<Record> getRecords(int limit, int offset, String derivationName);

    int getTotalRecords(String derivationName);
}
