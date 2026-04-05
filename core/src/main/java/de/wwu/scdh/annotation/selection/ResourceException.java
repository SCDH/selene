package de.wwu.scdh.annotation.selection;


/**
 * An exception that occurred while parsing or setting up a resource.
 */
public class ResourceException extends Exception {
    public ResourceException(String msg) {
        super(msg);
    }
    public ResourceException(Throwable cause) {
        super(cause);
    }
    public ResourceException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
