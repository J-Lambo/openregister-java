package uk.gov.register.db;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.OverrideStatementRewriterWith;

import java.util.Optional;

@OverrideStatementRewriterWith(SubstituteSchemaRewriter.class)
public interface IndexDAO {
    @SqlUpdate("insert into :schema.index (name, key, sha256hex, start_entry_number, start_index_entry_number) values (:name, :key, :sha256hex, :start_entry_number, :start_index_entry_number)")
    void start(@Bind("name") String indexName, @Bind("key") String key, @Bind("sha256hex") String itemHash, @Bind("start_entry_number") int startEntryNumber, @Bind("start_index_entry_number") Optional<Integer> startIndexEntryNumber, @Bind("schema") String schema );

    @SqlUpdate("update :schema.index i set end_entry_number = :end_entry_number, end_index_entry_number = :end_index_entry_number from :schema.entry e where e.entry_number = i.start_entry_number and e.key = :entryKey and i.name = :name and i.key = :indexKey and i.sha256hex = :sha256hex")
    void end(@Bind("name") String indexName, @Bind("entryKey") String entryKey, @Bind("indexKey") String indexKey, @Bind("sha256hex") String itemHash, @Bind("end_entry_number") int endEntryNumber, @Bind("end_index_entry_number") Optional<Integer> endIndexEntryNumber, @Bind("schema") String schema );
}
