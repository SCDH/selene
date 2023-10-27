package de.wwu.scdh.annotation.selection;

import java.net.URI;

/**
 * The {@link NormalizableSelection} interface defines methods for
 * normalizing selections and testing if a selection is normalized.
 *
 * For a normalizable selection, a unique URI can be generated. This
 * URI may contain components of the normalized form of the selector
 * and the URI of the web resource.
 */
public interface NormalizableSelection<S extends Selector> {

    /**
     * Returns the normalized form of the selector.
     *
     */
    S normalize();

    /**
     * A predicate to test whether the selection is in normalized
     * form.
     */
    boolean isNormalized();

    /**
     * Returns a URI for the Selector based on the normalized selection.
     *
     */
    URI toUri();

}
