package de.wwu.scdh.annotation.selection;


/**
 * A factory that makes a {@link Rewriter} for a given combination of
 * points etc.
 */
public interface RewriterFactory {

    /**
     * For a combination of {@link Point} classes, the first of which
     * is the input point, the second is the desired output, return a
     * Rewriter.
     *
     * @param point1  input point to be rewritten
     * @param point2  desired output representation
     * @param config  global {@link RewriterConfig} record
     */
    <R extends Resource, P1 extends Point, P2 extends Point, RW extends Rewriter<R, P1, P2>> RW getRewriter(Class<P1> point1, Class<P2> point2, RewriterConfig config) throws ConfigurationException;

}
