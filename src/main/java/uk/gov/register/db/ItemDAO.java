package uk.gov.register.db;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize;
import org.skife.jdbi.v2.sqlobject.customizers.OverrideStatementRewriterWith;
import uk.gov.register.core.Item;
import uk.gov.register.store.postgres.BindItem;

@OverrideStatementRewriterWith(SubstituteSchemaRewriter.class)
public interface ItemDAO {
    @SqlBatch("insert into :schema.item(sha256hex, content) values(:sha256hex, :content) on conflict do nothing")
    @BatchChunkSize(1000)
    void insertInBatch(@BindItem Iterable<Item> items, @Bind("schema") String schema );
}
