package uk.gov.register.core;

import org.jvnet.hk2.annotations.Contract;

@Contract
public interface Register extends RegisterReadOnly {
    void mintItems(Iterable<Item> items);

    void addEntry(Entry entry);

    void addItem(Item item);
}
