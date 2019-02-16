package me.philcali.db.processor.example;

import java.util.Optional;

import com.amazonaws.SdkBaseException;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;

import me.philcali.db.annotation.ExceptionTranslation;
import me.philcali.db.annotation.ExceptionTranslations;
import me.philcali.db.annotation.Key;
import me.philcali.db.annotation.Repository;
import me.philcali.db.annotation.Repository.Action;
import me.philcali.db.api.QueryParams;
import me.philcali.db.api.QueryResult;
import me.philcali.db.processor.example.exception.PersonAlreadyExistsException;
import me.philcali.db.processor.example.exception.PersonNotFoundException;
import me.philcali.db.processor.example.exception.PersonStorageException;
import me.philcali.zero.lombok.example.Person;

@Repository(keys = @Key(partition = "name"))
@ExceptionTranslations({
    @ExceptionTranslation(source = SdkBaseException.class, destination = PersonStorageException.class)
})
public interface PersonRepository {
    @Repository.Method(Action.READ)
    Optional<Person> get(String name) throws PersonStorageException;

    QueryResult<Person> list(QueryParams params) throws PersonStorageException;

    void delete(String name) throws PersonStorageException;

    @ExceptionTranslation(source = ConditionalCheckFailedException.class, destination = PersonAlreadyExistsException.class)
    Person create(Person partial) throws PersonAlreadyExistsException, PersonStorageException;

    @ExceptionTranslation(source = ConditionalCheckFailedException.class, destination = PersonNotFoundException.class)
    Person update(Person partial) throws PersonNotFoundException, PersonStorageException;

    Person put(Person completePerson) throws PersonStorageException;
}
