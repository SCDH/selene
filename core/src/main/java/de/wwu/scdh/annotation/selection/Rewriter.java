package de.wwu.scdh.annotation.selection;

/**
 * A {@link Rewriter} is a utility for rewriting a representation of a
 * {@link Point} in {@link Resource} to an other representation. There
 * are different kinds of rewriters: normalizers, forward and reverse
 * mappers. This interface offers type-safe rewriting of points in
 * resources by type parameters.
 */
public interface Rewriter<M<?>, R extends Resource<?>, P1 extends Point, P2 extends Point> {

    /**
     * Rewrite a {@link Point} in a {@link Resource} to the same
     * point, but with a different (or potentially different)
     * representation.
     *
     * The class level type parameter <code>M</code> is a monad. For
     * normalizers, which return one point for a given point, this
     * will be the identity monad; for a forward mapper, that rewrites
     * on point in the preimage resource to zero, one, or multiple
     * points in the derived image resource, this will be the list
     * monad; for a reverse mapper, this will be the maybe monad.
     *
     * @param resource   the {@link Resource} to operate on
     * @param position   a {@link Point} in the resource
     * @param config     a record with configuration parameters
     * @return  the same point like <code>position</code>, but in a recalculated representation
     * @throws  SelectorException when the position cannot be found in the resource
     */
    public M<P2> rewrite(R resource, P1 position, RewriterConfig config) throws SelectorException;

}
