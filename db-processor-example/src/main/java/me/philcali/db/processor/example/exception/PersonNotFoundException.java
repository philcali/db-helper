package me.philcali.db.processor.example.exception;

public class PersonNotFoundException extends PersonStorageException {
    private static final long serialVersionUID = 5270868157888473702L;

    public PersonNotFoundException(final String message, final Throwable ex) {
        super(message, ex);
    }
}
