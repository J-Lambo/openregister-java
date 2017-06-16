package uk.gov.register.core;

import org.jvnet.hk2.annotations.Contract;

import java.util.List;

@Contract
public interface Register extends RegisterReadOnly {
    void putItem(Item item);
    void appendEntry(Entry entry);
    void updateIndexes(List<Entry> stagedEntries);
}
