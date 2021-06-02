package me.afibarra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.MongoClient;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/")
public class GreetingResource {

    final MongoClient mongoClient;
    final MongoDbSessionState<String> sessionState;
    @ConfigProperty(name = "mongo.db.name")
    private String mongoDbName;

    @Inject
    public GreetingResource(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
        sessionState = new MongoDbSessionState<>(String.class, mongoClient);
    }

    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() throws JsonProcessingException {
        return "Hello RESTEasy";
    }
}
