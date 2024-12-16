package de.wwu.scdh.annotation.selection;

/**
 * The {@link XPathNormalizer.Mode} is an enum type for selecting
 * an algorithm for the first stage of the normalization.<P>
 *
 * For corner cases, normalization is ambigous.<P>
 *
 * Case 1: For the simple, but wellformed XML document
 * <code>&lt;r>Sol&lt;t>ar&lt;/t>!&lt;/r></code> the normalization
 * of the XPath pointer `/r` refined by the character scheme
 * `char=3` is ambigous:
 *
 * <pre>
 *              &lt;r>|S|o|l|&lt;t>|a|r|&lt;/t>|!|&lt;/r>
 * /r;char=        0 1 2 3   3 4 5    5 6
 * /r/t[1];char=             0 1 2
 * </pre>
 *
 * Valid normalization results would be
 * <code>/r[1]/text()[1}</code> refined by <code>char=3</code>,
 * and also <code>/r[1]/t[1]/text()[1]</code> refined by
 * <code>char=0</code>.<P>
 *
 * Case 2: For the XML document
 * <code>&lt;r>Sol&lt;t>ar&lt;/t>&lt;g>pan&lt;/g>el!&lt;/r></code>,
 * the selector <code>/r;char=5</code> may select the position
 * after the character <code>r</code> in the node
 * <code>/r/t[1]/text()[1]</code> <b>or</b> the position before the
 * character <code>p</code> in the node
 * <code>/r/g[1]/text()[1]</code>.
 */
public enum Mode {

    /**
     * Always take the first text node with the position, i.e.,
     * the first one in document order.<P>
     *
     * If the selector selects the first/last position in a
     * subtree, this algorithm checks for the first text node on
     * the preceding/following axis of the node selected by the
     * XPath component.
     */
    FIRST,

    /**
     * Always take the second text node with the position, i.e.,
     * the second in document order.
     *
     * If the selector selects the first/last position in a
     * subtree, this algorithm checks for the first text node on
     * the preceding/following axis of the node selected by the
     * XPath component.
     */
    SECOND,

    /**
     * Descend to the deepest text node. In corner cases, stop at
     * the first text node, that contains the position.<P>
     *
     * This will return <code>/r;char3</code> for the first corner
     * case described in {@link Mode}.
     */
    DEEP_NODE_STOP_AT_END,

    /**
     * Descend to the deepest text node. In corner cases, step
     * over the end of a text node and try to get the position
     * from the next text node.
     *
     * This will return <code>/r/t[1];char=0</code> for the first
     * corner case described in {@link Mode}.
     */
    DEEP_NODE_STEP_OVER_END,

    /**
     * Descend to the deepest text node. In corner cases, take the
     * deepest text node. If there are equally deep text nodes,
     * take the first one in document order.
     *
     * This will return <code>/r/t[1];char=0</code> for the first
     * corner case described in {@link Mode}. For case 2, it
     * selects the text node in the <code>t</code> element.
     */
    FIRST_OF_DEEPEST_NODES,

    /**
     * Descend to the deepest text node. In corner cases, take the
     * deepest text node. If there are equally deep text nodes,
     * take the last one in document order.
     *
     * This will return <code>/r/t[1];char=0</code> for the first
     * corner case described in {@link Mode}. For case 2, it
     * selects the text node in the <code>g</code> element.
     */
    LAST_OF_DEEPEST_NODES

}
