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

    @SuppressWarnings("unused, used by @BindBean")
    public String getKey() {
        return key;
    }

    @SuppressWarnings("unused, used by @BindBean")
    public int getStartEntryNumber() {
        return startEntryNumber;
    }

    @SuppressWarnings("unused, used by @BindBean")
    public Optional<Integer> getStartIndexEntryNumber() {
        return startIndexEntryNumber;
    }
}
