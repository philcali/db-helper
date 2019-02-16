package me.philcali.db.processor.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;

import me.philcali.db.api.QueryParams;
import me.philcali.db.dynamo.local.runner.DynamoDBArgs;
import me.philcali.db.dynamo.local.runner.DynamoDBLocalRunner;
import me.philcali.db.dynamo.local.runner.DynamoDBSeed;
import me.philcali.db.dynamo.local.runner.DynamoDBTestClient;
import me.philcali.db.processor.example.exception.PersonAlreadyExistsException;
import me.philcali.db.processor.example.exception.PersonNotFoundException;
import me.philcali.zero.lombok.example.Person;
import me.philcali.zero.lombok.example.PersonData;
import me.philcali.zero.lombok.example.VehicleData;

@DynamoDBArgs({ "-clientOnly", "-port", "8001" })
@DynamoDBSeed(TestDataSeed.class)
@RunWith(DynamoDBLocalRunner.class)
public class PersonRepositoryTest {
    private PersonRepository repo;
    @DynamoDBTestClient
    private AmazonDynamoDB client;
    private DynamoDB db;

    @Before
    public void setUp() {
        db = new DynamoDB(client);
        repo = new PersonRepositoryDynamo("GeneratedPeople", db);
    }

    @Test
    public void testList() {
        List<Person> people = repo.list(QueryParams.builder().withMaxSize(10).build()).getItems();
        assertTrue(people.size() >= 3);
    }

    @Test(expected = PersonAlreadyExistsException.class)
    public void testCreateConflict() {
        repo.create(PersonData.builder().withName("Philip Cali").withDead(true).build());
    }

    @Test
    public void testCreateDeleteLifecycle() {
        Person dude = PersonData.builder()
                .withName("Dude")
                .withAge(13)
                .withDead(true)
                .addScopes("auth")
                .addScopes("fortitude")
                .putVehicles("car", VehicleData.builder()
                        .withMake("Nissan")
                        .withModel("Leaf")
                        .withYear(2015)
                        .build())
                .build();
        assertEquals(dude, repo.create(dude));
        repo.delete(dude.getName());
    }

    @Test
    public void testGet() {
        Person philip = repo.get("Philip Cali").get();
        assertEquals("Philip Cali", philip.getName());
    }

    @Test(expected = PersonNotFoundException.class)
    public void testUpdateNotFound() {
        repo.update(PersonData.builder().withName("Fartso").withAge(50).build());
    }

    @Test
    public void testUpdate() {
        assertEquals(33, repo.update(PersonData.builder().withName("Philip Cali").withAge(33).build()).getAge());
    }

}
