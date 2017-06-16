package uk.gov.register.core;

import java.util.Optional;

public class EndIndex extends Index {
    private final String entryKey;
    private final String indexKey;
    private final int endEntryNumber;
    private final Optional<Integer> endIndexEntryNumber;

    public EndIndex(String indexName, String entryKey, String indexKey, String itemHash, int endEntryNumber, Optional<Integer> endIndexEntryNumber) {
        super(indexName, itemHash);
        this.entryKey = entryKey;
        this.indexKey = indexKey;
        this.endEntryNumber = endEntryNumber;
        this.endIndexEntryNumber = endIndexEntryNumber;
    }

    public String getEntryKey() {
        return entryKey;
    }

    public String getIndexKey() {
        return indexKey;
    }

    public int getEndEntryNumber() {
        return endEntryNumber;
    }

    public Optional<Integer> getEndIndexEntryNumber() {
        return endIndexEntryNumber;
    }

    @Override
    public boolean isStart() {
        return false;
    }
}
