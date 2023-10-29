package de.wwu.scdh.annotation.selection;


/**
 * An exception that occurred while processing a selector an a
 * resource.
 */
public class SelectorException extends Exception {
    public SelectorException(String msg) {
        super(msg);
    }
    public SelectorException(Throwable cause) {
        super(cause);
    }
    public SelectorException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
