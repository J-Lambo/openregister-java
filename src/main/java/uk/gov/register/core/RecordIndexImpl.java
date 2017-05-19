package uk.gov.register.core;

import uk.gov.register.store.DataAccessLayer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class RecordIndexImpl implements RecordIndex {
    protected final DataAccessLayer dataAccessLayer;

    public RecordIndexImpl(DataAccessLayer dataAccessLayer) {
        this.dataAccessLayer = dataAccessLayer;
    }

//    @Override
//    public void updateRecordIndex(Entry entry) {
//        dataAccessLayer.updateRecordIndex(entry);
//    }

    @Override
    public Optional<Record> getRecord(String key) {
        return dataAccessLayer.getRecord(key);
    }

    @Override
    public int getTotalRecords() {
        return dataAccessLayer.getTotalRecords();
    }

    @Override
    public List<Record> getRecords(int limit, int offset) {
        return dataAccessLayer.getRecords(limit, offset);
    }

    @Override
    public List<Record> findMax100RecordsByKeyValue(String key, String value) {
        return dataAccessLayer.findMax100RecordsByKeyValue(key, value);
    }

    @Override
    public Collection<Entry> findAllEntriesOfRecordBy(String key) {
        return dataAccessLayer.findAllEntriesOfRecordBy(key);
    }

    // indexes
    @Override
    public Optional<Record> getRecord(String key, String derivationName) {
        Optional<Record> record = dataAccessLayer.getIndexRecord(key, derivationName);
        return record.filter(r -> r.getItems().size() != 0);
    }

    @Override
    public List<Record> getRecords(int limit, int offset, String derivationName) {
        return dataAccessLayer.getIndexRecords(limit, offset, derivationName);
    }

    @Override
    public int getTotalRecords(String derivationName) {
        return dataAccessLayer.getTotalIndexRecords(derivationName);
    }
}
