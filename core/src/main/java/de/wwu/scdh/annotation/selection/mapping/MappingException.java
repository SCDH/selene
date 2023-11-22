package de.wwu.scdh.annotation.selection.mapping;


/**
 * An exception that occurred while mapping selectors.
 */
public class MappingException extends Exception {
    public MappingException(String msg) {
        super(msg);
    }
    public MappingException(Throwable cause) {
        super(cause);
    }
    public MappingException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
