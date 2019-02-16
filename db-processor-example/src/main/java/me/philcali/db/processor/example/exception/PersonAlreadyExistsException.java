package me.philcali.db.processor.example.exception;

public class PersonAlreadyExistsException extends PersonStorageException {
    private static final long serialVersionUID = -1617530904952174843L;

    public PersonAlreadyExistsException(final String message, final Throwable ex) {
        super(message, ex);
    }
}
