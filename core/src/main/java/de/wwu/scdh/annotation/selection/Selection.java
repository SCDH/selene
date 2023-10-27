package de.wwu.scdh.annotation.selection;

import java.net.URI;
import java.util.List;


/**
 * A selection is a pair of a selector and a web resource.
 */
public interface Selection<S extends Selector> {

    /**
     * Returns the web {@link Resource}.
     */
    Resource getResource();

    /**
     * Returns the selector.
     */
    S getSelector();

    /**
     * Returns True, if and only if this selection is referentially
     * equivalent to the other Selection.
     *
     * @param other the other {@link Selection<Selector>} parametrized
     * with the same type of {@link Selector} as this selection.
     */
    boolean referentiallyEquals(Selection<S> other);

    /**
     * Returns True, if and only if this selection is referentially
     * equivalent to the other Selection.
     *
     * @param other - the other {@link Selection<Selector>}
     */
    boolean referentiallySameAs(Selection<Selector> other);

}
