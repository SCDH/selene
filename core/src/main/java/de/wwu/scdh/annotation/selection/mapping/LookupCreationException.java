package de.wwu.scdh.annotation.selection.mapping;


/**
 * An exception that occurred while loading a lookup.
 */
public class LookupCreationException extends Exception {
    public LookupCreationException(String msg) {
        super(msg);
    }
    public LookupCreationException(Throwable cause) {
        super(cause);
    }
    public LookupCreationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
