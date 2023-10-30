package de.wwu.scdh.annotation.selection;

import java.net.URI;

/**
 * The {@link Normalizable} interface defines methods for normalizing
 * a component––be it a selection or a selector–-and testing if a it
 * is in normalized form.
 *
 * For a normalizable selection, a unique URI can be generated. This
 * URI may contain components of the normalized form of the selector
 * and the URI of the web resource.
 *
 * @see IsomorphicallyNormalizable
 */
public interface Normalizable {

    /**
     * Returns the normalized form of the component.
     *
     */
    Selector normalize() throws SelectorException;

    /**
     * A predicate to test whether the component is in normalized
     * form.
     */
    boolean isNormalized() throws SelectorException;

    /**
     * Returns a URI based on the normalized form.
     *
     */
    URI toUri() throws SelectorException;

}
