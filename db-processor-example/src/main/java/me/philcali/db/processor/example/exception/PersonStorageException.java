package me.philcali.db.processor.example.exception;

public class PersonStorageException extends RuntimeException {
    private static final long serialVersionUID = -8751458669271117910L;

    public PersonStorageException(final String message, final Throwable ex) {
        super(message, ex);
    }
}
