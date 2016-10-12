package uk.gov.register.db;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;

@UseStringTemplate3StatementLocator
public interface CurrentKeysUpdateDAO {
    String CURRENT_KEYS_TABLE = "current_keys";

    @SqlUpdate("delete from " + CURRENT_KEYS_TABLE + " where key in (<keys>)")
    int removeRecordWithKeys(@BindIn("keys") Iterable<String> allKeys);

    @SqlBatch("insert into " + CURRENT_KEYS_TABLE + "(entry_number, key) values(:entry_number, :key)")
    void writeCurrentKeys(@BindBean Iterable<CurrentKey> values);

    @SqlUpdate("update total_records set count=count+:noOfNewRecords")
    void updateTotalRecords(@Bind("noOfNewRecords") int noOfNewRecords);

    @SqlUpdate("update total_records set count=:recordCount")
    void setTotalRecords(@Bind("recordCount") int recordCount);

    @SqlUpdate("truncate table current_keys")
    void removeAllCurrentKeys();
}

