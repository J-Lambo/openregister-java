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

    @SuppressWarnings("unused, used by @BindBean")
    public String getEntryKey() {
        return entryKey;
    }

    @SuppressWarnings("unused, used by @BindBean")
    public String getIndexKey() {
        return indexKey;
    }

    @SuppressWarnings("unused, used by @BindBean")
    public int getEndEntryNumber() {
        return endEntryNumber;
    }

    @SuppressWarnings("unused, used by @BindBean")
    public Optional<Integer> getEndIndexEntryNumber() {
        return endIndexEntryNumber;
    }
}
