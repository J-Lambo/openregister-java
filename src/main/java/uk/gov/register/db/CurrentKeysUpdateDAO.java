package uk.gov.register.db;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize;
import org.skife.jdbi.v2.sqlobject.customizers.OverrideStatementRewriterWith;

@OverrideStatementRewriterWith(SubstituteSchemaRewriter.class)
public interface CurrentKeysUpdateDAO {
    @SqlBatch("delete from :schema.current_keys where key = :key")
    @BatchChunkSize(1000)
    int[] removeRecordWithKeys(@Bind("key") Iterable<String> allKeys, @Bind("schema") String schema );

    @SqlBatch("insert into :schema.current_keys(entry_number, key) values(:entry_number, :key)")
    @BatchChunkSize(1000)
    void writeCurrentKeys(@BindBean Iterable<CurrentKey> values, @Bind("schema") String schema );

    @SqlUpdate("update :schema.total_records set count=count+:noOfNewRecords")
    void updateTotalRecords(@Bind("noOfNewRecords") int noOfNewRecords, @Bind("schema") String schema );
}

