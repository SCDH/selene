package de.wwu.scdh.annotation.selection;


/**
 * An exception that occurred while processing a selector an a
 * resource.
 */
public class ConfigurationException extends Exception {
    public ConfigurationException(String msg) {
        super(msg);
    }
    public ConfigurationException(Throwable cause) {
        super(cause);
    }
    public ConfigurationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
