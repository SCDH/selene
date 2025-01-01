package de.wwu.scdh.annotation.selection;

/**
 * A {@link Rewriter} is a utility for rewriting a representation of a
 * {@link Point} in {@link Resource} to an other
 * representation. Normalizers are rewriters. This interface offers
 * type-safe rewriting of points in resources by type parameters
 */
public interface Rewriter<R extends Resource, P1 extends Point, P2 extends Point> {

    /**
     * Rewrite a {@link Point} in a {@link Resource} to the same
     * point, but with a different (or potentially different)
     * representation. This is the method, that does the normalization
     * in normalizers.
     *
     * @param resource   the {@link Resource} to operate on
     * @param position   a {@link Point} in the resource
     * @param config     a record with configuration parameters
     * @return  the same point like <code>position</code>, but in a recalculated representation
     * @throws  SelectorException when the position cannot be found in the resource
     */
    public P2 rewrite(R resource, P1 position, RewriterConfig config) throws SelectorException;

}
