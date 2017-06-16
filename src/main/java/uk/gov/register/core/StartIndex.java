package uk.gov.register.core;

import java.util.Optional;

public class StartIndex extends Index {
    private final String key;
    private final int startEntryNumber;
    private final Optional<Integer> startIndexEntryNumber;

    public StartIndex(String indexName, String key, String itemHash, int startEntryNumber, Optional<Integer> startIndexEntryNumber) {
        super(indexName, itemHash);
        this.key = key;
        this.startEntryNumber = startEntryNumber;
        this.startIndexEntryNumber = startIndexEntryNumber;
    }

    public String getKey() {
        return key;
    }

    public int getStartEntryNumber() {
        return startEntryNumber;
    }

    public Optional<Integer> getStartIndexEntryNumber() {
        return startIndexEntryNumber;
    }

    @Override
    public boolean isStart() {
        return true;
    }
}
