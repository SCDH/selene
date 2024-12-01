package de.wwu.scdh.annotation.selection.wadm;

/**
 * A exception cause by the triples in a given RDF model.  This should
 * be used, e.g., if some triples are missing.
 */
public class ModelException extends Exception {
    public ModelException(String msg) {
        super(msg);
    }
    public ModelException(Throwable cause) {
        super(cause);
    }
    public ModelException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
