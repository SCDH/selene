package de.wwu.scdh.annotation.selection;

import java.util.List;


/**
 * A {@link Rewriter} is a utility for rewriting a representation of a
 * {@link Point} in {@link Resource} to an other representation. There
 * are different types of rewriters: normalizers, forward mappers, and
 * reverse mappers, which all do some kind of rewriting a
 * representation of a point. This common interface offers type-safe
 * rewriting of points in resources by type parameters.
 */
public interface Rewriter<R extends Resource<?>, P1 extends Point, P2 extends Point> {

    /**
     * Rewrite a {@link Point} in a {@link Resource} to the same
     * point, but with a different (or potentially different)
     * representation.<P>
     *
     * A normalizer will typically return one recalculated point. A so
     * called forward mapper, which maps a point from a preimage
     * resource to a direved resource, the image, may return zero, one
     * or multiple points, depending on how many times a point in the
     * preimage was mapped to the image. Thus, the return type is
     * {@link List} of {@link Point}s.<P>
     *
     * Note: It would be desirable, if we could declare a monadic
     * return type with a type parameter, <code>M<P2></code>; and then
     * use, e.g., the identity monad for normalizers, the list monad
     * for forward mappers, the maybe monad {@link java.util.Optional}
     * for reverse mappers. However, that cannot be expressed in the
     * Java type system. Since the implied arities can easly be covered
     * with an iterable, we use a {@link List}.
     *
     * @param resource   the {@link Resource} to operate on
     * @param position   a {@link Point} in the resource
     * @param config     a record with configuration parameters
     * @return the same point like <code>position</code>, but in a
     * recalculated representation and wrapped into a {@link List},
     * which should represent the occurrences of the point in document
     * order, if an order exists in the source.
     * @throws  SelectorException when the position cannot be found in the resource
     // */
    public List<P2> rewrite(R resource, P1 position, RewriterConfig config) throws SelectorException;

}
