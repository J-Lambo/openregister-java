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

    private static final ObjectMapper OBJECT_MAPPER = Jackson.newObjectMapper();

    private static final String ENTRY_BY_KEY_SQL = "SELECT " +
            " entry_number, e.timestamp, e.sha256hex AS sha256hex, key, content " +
            "FROM entry e JOIN item i ON e.sha256hex = i.sha256hex " +
            "WHERE e.key = :key " +
            "ORDER BY entry_number DESC LIMIT 1";

    private static final String RECORDS_WITH_LIMIT_SQL = "SELECT " +
            " ck.key, ck.entry_number, e.timestamp, e.sha256hex, i.content " +
            "FROM " +
            " ( " +
            " SELECT e.key AS key, max( e.entry_number ) AS entry_number " +
            " FROM entry e " +
            " GROUP BY e.key " +
            " ORDER BY e.key ASC LIMIT :limit OFFSET :offset" +
            " ) ck JOIN entry AS e ON ck.entry_number = e.entry_number " +
            " JOIN item AS i ON e.sha256hex = i.sha256hex ";

    private static final String RECORDS_BY_CONTENT_SQL = "SELECT" +
            " current_key.key, current_key.entry_number, e.timestamp, e.sha256hex, i.content " +
            "FROM (" +
            " SELECT e2.key AS key, max( e2.entry_number ) AS entry_number" +
            " FROM entry e2" +
            " WHERE e2.entry_number in (" +
            " SELECT  e3.entry_number" +
            " FROM  entry e3 join item i2 ON" +
            "  e3.sha256hex = i2.sha256hex" +
            " WHERE  i2.content @> :contentPGobject LIMIT 100" +
            " )" +
            " GROUP BY e2.key" +
            " ) AS current_key JOIN entry e ON" +
            " e.entry_number = current_key.entry_number JOIN item i ON" +
            " e.sha256hex = i.sha256hex;";

    @SqlQuery("SELECT count FROM total_records")
    public abstract int getTotalRecords();

    //@SqlQuery("select entry_number, timestamp, e.sha256hex as sha256hex, key, content from entry e, item i where e.sha256hex=i.sha256hex and e.entry_number = (select entry_number from current_keys where current_keys.key=:key)")
    @SqlQuery(ENTRY_BY_KEY_SQL)
    @SingleValueResult(Record.class)
    @RegisterMapper(RecordMapper.class)
    public abstract Optional<Record> findByPrimaryKey(@Bind("key") String key);

    //@SqlQuery("select entry.entry_number, timestamp, entry.sha256hex as sha256hex, entry.key, content from item, entry, current_keys where current_keys.entry_number = entry.entry_number and item.sha256hex=entry.sha256hex order by entry.entry_number desc limit :limit offset :offset")
    @SqlQuery(RECORDS_WITH_LIMIT_SQL)
    @RegisterMapper(RecordMapper.class)
    public abstract List<Record> getRecords(@Bind("limit") long limit, @Bind("offset") long offset);

    @SqlQuery("SELECT entry_number, timestamp, sha256hex, key FROM entry WHERE sha256hex IN (select sha256hex from item where (content @> :contentPGobject)) ORDER BY entry_number ASC")
    @RegisterMapper(EntryMapper.class)
    public abstract Collection<Entry> __findAllEntriesOfRecordBy(@Bind("contentPGobject") PGobject content);

    //@SqlQuery("select entry.entry_number, timestamp, entry.sha256hex as sha256hex, entry.key, content from item, entry, current_keys where current_keys.entry_number = entry.entry_number and item.content @> :contentPGobject and item.sha256hex=entry.sha256hex limit 100")
    @SqlQuery(RECORDS_BY_CONTENT_SQL)
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
            json.setValue(OBJECT_MAPPER.writeValueAsString(ImmutableMap.of(key, value)));
            return json;
        } catch (SQLException | JsonProcessingException e) {
            throw Throwables.propagate(e);
        }
    }


}
