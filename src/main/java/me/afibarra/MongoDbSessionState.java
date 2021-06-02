package me.afibarra;

import static com.mongodb.client.model.Filters.eq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.PreDestroy;
import org.bson.Document;
import org.bson.conversions.Bson;

public class MongoDbSessionState<T> {

    // To have the same Redis field/value names
    private static final String FIELD = "field";
    private static final String VALUE = "value";

    private final Class<T> typeParameterClass;
    private final MongoClient client;
    private final ObjectMapper objectMapper;

    private final String dbName;

    public MongoDbSessionState(Class<T> typeParameterClass, MongoClient client) {
        this.typeParameterClass = typeParameterClass;
        this.client = client;
        this.objectMapper = new ObjectMapper();
        this.dbName = client.listDatabaseNames().first();
    }

    public MongoDbSessionState(Class<T> typeParameterClass, MongoClient client,
            ObjectMapper objectMapper) {
        this.typeParameterClass = typeParameterClass;
        this.client = client;
        this.objectMapper = objectMapper;
        this.dbName = client.listDatabaseNames().first();
    }

    public Optional<T> get(String key, String field) throws JsonProcessingException {
        Optional<T> result = Optional.empty();

        Bson filter = eq(FIELD, field);
        MongoCursor<Document> cursor = getCollection(key).find(filter).iterator();
        Document document;
        if (cursor.hasNext()) {
            document = cursor.next();
            result = Optional.ofNullable(objectMapper
                    .readValue(String.valueOf(document.get(FIELD)), typeParameterClass));
        }

        return result;
    }

    public void remove(String key) {
        getCollection(key).drop();
    }

    public void set(String key, String field, T value) throws JsonProcessingException {
        String stringValue = objectMapper.writeValueAsString(value);

        Map<String, Object> data = new HashMap<>();
        data.put(field, stringValue);
        Document document = new Document(data);

        getCollection(key).insertOne(document);
    }

    public void update(String key, String field, T value) throws JsonProcessingException {
        Bson filter = eq(FIELD, field);
        String stringValue = objectMapper.writeValueAsString(value);
        Bson updateOperation = com.mongodb.client.model.Updates.set(VALUE, stringValue);

        getCollection(key).updateOne(filter, updateOperation);
    }

    private MongoCollection getCollection(String key) {
        MongoDatabase database = client.getDatabase(dbName);

        if (!database.getCollection(key).find().iterator().hasNext()) {
            client.getDatabase(dbName).createCollection(key);
        }

        return client.getDatabase(dbName).getCollection(key);
    }

    @PreDestroy
    private void dispose() {
        this.client.close();
    }
}
