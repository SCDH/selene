package de.wwu.scdh.annotation.selection;


/**
 * An exception that occurred while processing a selector an a
 * resource.
 */
public class NoSuchComponentException extends Exception {
    public NoSuchComponentException(String msg) {
        super(msg);
    }
    public NoSuchComponentException(Throwable cause) {
        super(cause);
    }
    public NoSuchComponentException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
