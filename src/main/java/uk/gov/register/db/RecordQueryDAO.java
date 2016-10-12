package uk.gov.register.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.jackson.Jackson;
import org.postgresql.util.PGobject;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import uk.gov.register.core.Entry;
import uk.gov.register.core.Record;
import uk.gov.register.db.mappers.EntryMapper;
import uk.gov.register.db.mappers.RecordMapper;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public abstract class RecordQueryDAO {

    private static final ObjectMapper objectMapper = Jackson.newObjectMapper();

    //is used is used here as string
    private static String CURRENT_HEAD_FILTER = " (entry_number <= (select value from current_entry_number)) ";


    @SqlQuery("SELECT count FROM total_records")
    public abstract int getTotalRecords();

    @SqlQuery("select entry_number, timestamp, e.sha256hex as sha256hex, content from entry e, item i where e.sha256hex=i.sha256hex and e.entry_number = (select entry_number from current_keys where current_keys.key=:key)")
    @SingleValueResult(Record.class)
    @RegisterMapper(RecordMapper.class)
    public abstract Optional<Record> findByPrimaryKey(@Bind("key") String key);

    @SqlQuery("select entry.entry_number, timestamp, entry.sha256hex as sha256hex, content from item, entry, current_keys where current_keys.entry_number = entry.entry_number and item.sha256hex=entry.sha256hex order by entry.entry_number desc limit :limit offset :offset")
    @RegisterMapper(RecordMapper.class)
    public abstract List<Record> getRecords(@Bind("limit") long limit, @Bind("offset") long offset);


    //here be changes to
    @SqlQuery("select entry_number, timestamp, sha256hex from entry where sha256hex in (select sha256hex from item where (content @> :contentPGobject)) and (entry_number <= (select value from current_entry_number))  order by entry_number asc")
    @RegisterMapper(EntryMapper.class)
    public abstract Collection<Entry> __findAllEntriesOfRecordBy(@Bind("contentPGobject") PGobject content);

    @SqlQuery("select entry.entry_number, timestamp, entry.sha256hex as sha256hex, content from item, entry, current_keys where current_keys.entry_number = entry.entry_number and item.content @> :contentPGobject and item.sha256hex=entry.sha256hex limit 100")
    @RegisterMapper(RecordMapper.class)
    public abstract List<Record> __findMax100RecordsByKeyValue(@Bind("contentPGobject") PGobject content);

    public Collection<Entry> findAllEntriesOfRecordBy(String key, String value) {
        return __findAllEntriesOfRecordBy(writePGObject(key, value));
    }

    public List<Record> findMax100RecordsByKeyValue(String key, String value) {
        return __findMax100RecordsByKeyValue(writePGObject(key, value));
    }


    private PGobject writePGObject(String key, String value) {
        try {
            PGobject json = new PGobject();
            json.setType("jsonb");
            json.setValue(objectMapper.writeValueAsString(ImmutableMap.of(key, value)));
            return json;
        } catch (SQLException | JsonProcessingException e) {
            throw Throwables.propagate(e);
        }
    }
}
