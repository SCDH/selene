package de.wwu.scdh.annotation.selection;

import java.net.URI;

/**
 * Implementors of the {@link IsomorphicallyNormalizable<S>} interface
 * must be isomorphically normalizable, i.e., the normalized form must
 * be of the same type as the form to be normalized. This interface
 * defines methods for normalizing components (selections, selectors)
 * and for testing if it is already normalized.
 *
 * For a normalizable component, a unique URI (at least a part, e.g. a
 * fragment identifier for a selector) can be generated. This URI may
 * contain components of the normalized form of the selector and the
 * URI of the web resource.
 *
 * @see Normalizable
 */
public interface IsomorphicallyNormalizable<S extends Selector> {

    /**
     * Returns the normalized form.
     *
     */
    S normalize() throws SelectorException;

    /**
     * A predicate to test whether the component is in normalized
     * form.
     */
    boolean isNormalized() throws SelectorException;

    /**
     * Returns a {@link URI} (part) for the normalized component.
     */
    URI toUri() throws SelectorException;

}
