package de.wwu.scdh.annotation.selection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XPathSelector;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.ImmutablePair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The same position (or range) in a {@link DOMResource} can be
 * selected by different pairs of XPaths and RFC 5147 character
 * schemes (or range schemes), i.e. *referentially equal* selectors
 * may have different values. Normalization maps referentially equal
 * selectors to the same selector.<P>
 *
 * Normalization of selectors is a 2-stage process: 1) In the first
 * stage, the text position (or range) which is referenced by the
 * selector, has to be found. A pair containing a text node and a RFC
 * 5147 character scheme position is returned. 2) In the second stage,
 * this position is expressed as a selector again, i.e., the node is
 * referenced with an XPath, where the XPath may be written as a path
 * expression descending from the root element, or from the deepest
 * element with an XML-ID, etc., and even the character scheme
 * component of the selector may be recalculated.<P>
 *
 * There is **not** the one and only normalization. Both stages of the
 * normalization process may be implemented differently, leading to
 * different results. For corner cases, even the first stage may lead
 * to different results.<P>
 *
 * This class implements the first stage of the normalization
 * process. The algorithm is selected by values of the {@link Mode}
 * enum type, which is passed to the normalization methods. The second
 * stage of the normalization process has to be implemented by
 * subclasses of the abstract base class.<P>
 *
 * XPath expressions to be normalized may be arbitrary XPath 4.0
 * expressions which select a single node from the
 * {@link DOMResource}. Expressions selecting not exactly one node
 *  result in an {@link SelectorException}.
 */
public abstract class XPathNormalizer {

    private static final Logger LOG = LoggerFactory.getLogger(XPathNormalizer.class);

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
	DEEPEST_NODE
    }

    protected final DOMResource resource;

    /**
     * Make a new {@link XPathNormalizer} for a {@link DOMResource}.
     * @param resource  the {@link DOMResource} on the base of which the normalization is done
     */
    public XPathNormalizer(DOMResource resource) {
	this.resource = resource;
    }

    /**
     * Same as
     * {@link #normalizeXPathRefinedByCharScheme(String,int,boolean)},
     * but returns an escaped string.
     * @param xpath  a XPath expression selecting a single node
     * @param position  the inter-character position following the character scheme from RFC5147
     * @param stepOverEnd  how to resolve positional ambiguity at changeover between text nodes
     * @return a {@link Pair} of XPath expression and character scheme position
     * @see XPathNormalizer#getTextNodeAtPosition(String, int, boolean)
     */
    public Pair<String, Integer> normalizeXPathRefinedByCharScheme(String xpath, int position, Mode mode)
	throws SelectorException {
	return normalizeXPathRefinedByCharScheme(xpath, position, mode, true);
    }

    /**
     * Normalize an XPath Selector refined by an RFC5147 character
     * scheme. The position must be a valid character position
     * *inside* the fragment selected by the XPath expression. If the
     * fragment's text (its concatenated text nodes) is shorter than
     * the position's value, a {@link SelectorException} is thrown.<P>
     *
     * In general, this method will result in a recalculated pair of
     * XPath *and* position.
     *
     * @param xpath  a XPath expression selecting a single node
     * @param position  the inter-character position following the character scheme from RFC5147
     * @param escaped   whether or not the normalized XPath is to be escaped for X-processing, e.g., <code>'</code> escaped to <code>&amp;apos;</code>.
     * @param stepOverEnd  how to resolve positional ambiguity at changeover between text nodes
     * @return a {@link Pair} of XPath expression and character scheme position
     */
    public Pair<String, Integer> normalizeXPathRefinedByCharScheme(String xpath, int position, Mode mode, boolean escaped)
	throws SelectorException {
	Pair<XdmNode, Integer> textNode = getTextNodeAtPosition(xpath, position, mode);
	// call the normalization function
	String normalizedXPath = getNormalizedXPath(textNode.getLeft(), escaped);
	return new ImmutablePair<String, Integer>(normalizedXPath, textNode.getRight());
    }

    /**
     * This abstract method must be implemented by normalizers. It
     * returns the normalized path to the node given as argument.
     *
     * @param node  an {@link XdmNode} to which the normlized path must be generated to
     * @param escaped  whether or not the normalized XPath is to be escaped for X-processing, e.g., <code>'</code> escaped to <code>&amp;apos;</code>.
     * @return the normalized XPath expression as a String
     * @see XPathNormalizer#getTextNodeAtPosition(String, int, boolean)
     */
    protected abstract String getNormalizedXPath(XdmNode node, boolean escaped) throws SelectorException;

    /**
     * This method run the first stage of the normalization
     * process. It gets the node where the pair of XPath and position
     * of an character-scheme-refined XPath selector points to. Either
     * this node is a text node, or the selector is invalid in the
     * context of the current document. In general, the resulting
     * position is not the same as the input position.<P>
     *
     * There are several algorithms for this first stage of the
     * normalization process and their return values differ for corner
     * cases. The algorithm is selected by the <code>mode</code>
     * argument. See {@link Mode} for possible values.<P>
     *
     * A {@link SelectorException} is thrown if the xpath does not
     * select exactly one node or if the position is not inside
     * the fragment (subtree) selected by the xpath.<P>
     *
     *
     * @param xpath  the XPath part of the XPath selector
     * @param position  the position following the character scheme of RFC5147
     * @param mode  an normalization algorithm selected from {@link Mode}
     * @throws {@link SelectorException}
     * @return a pair of node and position
     */
    protected Pair<XdmNode, Integer> getTextNodeAtPosition(String xpath, int position, Mode mode) throws SelectorException {
	return switch(mode) {
	case DEEP_NODE_STOP_AT_END -> getDeepTextNodeAtPositionWithEndParam(xpath, position, false);
	case DEEP_NODE_STEP_OVER_END -> getDeepTextNodeAtPositionWithEndParam(xpath, position, true);
	case DEEPEST_NODE -> getDeepestTextNodeAtPosition(xpath, position);
	default -> {
	    LOG.error("mode {} not implemented", mode.name());
	    throw new SelectorException("mode " + mode.name() + " not implemented");
	}
	};
    }

    /**
     * This is an implementation of the normalization step 1 for the
     * modes {@link Mode#DEEP_NODE_STOP_AT_END} and
     * {@link Mode#DEEP_NODE_STEP_OVER_END}, which both descend the
     * DOM tree to the deepest text node possible.
     *
     * @param xpath  the XPath part of the XPath selector
     * @param position  the position following the character scheme of RFC5147
     * @param stepOverEnd  how to resolve positional ambiguity at changeover between text nodes
     * @throws {@link SelectorException}
     * @return a pair of node and position
     */
    private Pair<XdmNode, Integer> getDeepTextNodeAtPositionWithEndParam(String xpath, int position, boolean stepOverEnd) throws SelectorException {
	XdmNode fragment = getNode(xpath);
	List<Pair<XdmNode, Integer>> nodesAtPosition = getDescendantTextNodesWithPosition(fragment, position);
	if (nodesAtPosition.isEmpty()) {
	    return reportNotFound(xpath, position);
	} else if (nodesAtPosition.size() == 1) {
	    return nodesAtPosition.get(0);
	} else if (stepOverEnd) {
	    return nodesAtPosition.get(1);
	} else {
	    return nodesAtPosition.get(0);
	}
    }

    /**
     * The implementation of step 1 of the normalization algorithm in
     * in mode {@link Mode#DEEPEST_NODE}.
     */
    private Pair<XdmNode, Integer> getDeepestTextNodeAtPosition(String xpath, int position) throws SelectorException {
	XdmNode fragment = getNode(xpath);
	List<Pair<XdmNode, Integer>> nodesAtPosition = getDescendantTextNodesWithPosition(fragment, position);
	if (nodesAtPosition.isEmpty()) {
	    return reportNotFound(xpath, position);
	} else if (nodesAtPosition.size() == 1) {
	    return nodesAtPosition.get(0);
	} else {
	    // we still have to get the text node with the deepest path
	    LOG.debug("found {} nodes, getting deepest", nodesAtPosition.size());
	    // note, that Stream.max() returns the first of the items with the maximum value
	    Optional<Pair<XdmNode, Integer>> deepest = nodesAtPosition.stream()
		.max(Comparator.comparing(XPathNormalizer::getDepth));
	    return deepest.get();
	}
    }

    /**
     * Get the node from the DOM resource given by the the XPath
     * passed as argument. If the XPath does not evaluate to a single
     * node, this method raises an {@link SelectorException}.
     *
     * @param xpath  the XPath as {@link String}
     * @return an {@link XdmNode} which the XPath points to
     */
    protected XdmNode getNode(String xpath) throws SelectorException {
	Processor proc = resource.getProcessor();
	XPathCompiler compiler = proc.newXPathCompiler();
	try {
	    XPathExecutable executable = compiler.compile(xpath);
	    XPathSelector selector = executable.load();
	    selector.setContextItem(resource.getDOM());
	    XdmValue nodes = selector.evaluate();
	    // assert that the XPath selects exactly 1 node
	    if (nodes.size() != 1) {
		LOG.error("XPath '{}' does not select exactly one node: selects {} nodes", xpath, nodes.size());
		throw new SelectorException("XPath '" +
					    xpath +
					    "' does not select exactly one node: selects " +
					    String.valueOf(nodes.size()) +
					    " nodes");
	    } else if (!nodes.itemAt(0).isNode()) {
		LOG.error("Node selected by XPath '{}' does not select a node", xpath);
		throw new SelectorException("XPath '" + xpath + "' does not select a node");
	    } else {
		return (XdmNode) nodes.itemAt(0);
	    }
	} catch (SaxonApiException e) {
	    LOG.error(e.getMessage());
	    throw new SelectorException(e);
	}
    }

    /**
     * Helper method used by implementations of normalization stage 1,
     * that search deep text nodes. This return a list of all pairs of
     * a node and a position, that contain the character scheme
     * position inside a given fragment.
     *
     * @param fragment  as {@link XdmNode} inside a {@link DOMResource}
     * @param position  the RFC 5147 character scheme position inside the fragment
     */
    protected List<Pair<XdmNode, Integer>> getDescendantTextNodesWithPosition(XdmNode fragment, int position) {
	Iterator<XdmNode> descendants = fragment.axisIterator(Axis.DESCENDANT_OR_SELF);
	int charsEaten = 0;
	XdmNode node = fragment;
	List<Pair<XdmNode, Integer>> nodesAtPosition = new ArrayList<Pair<XdmNode, Integer>>();
	while (descendants.hasNext()) {
	    node = descendants.next();
	    LOG.debug("investigating '{}' node", node.getUnderlyingNode().getLocalPart());
	    if (node.getNodeKind().equals(XdmNodeKind.TEXT)) {
		int length = node.getUnderlyingValue().getUnicodeStringValue().length32();
		if (position > charsEaten + length) {
		    // position not yet reached
		    charsEaten += length;
		} else {
		    // position is inside this text node or at its end
		    nodesAtPosition.add(new ImmutablePair<XdmNode, Integer>(node, position - charsEaten));
		    if (position < charsEaten + length) {
			// we can stop, since positions in all further text nodes will have greater positions
			break;
		    } else {
			charsEaten += length;
		    }
		}
	    }
	}
	return nodesAtPosition;
    }

    /**
     * Report that the position is not found inside the selected fragment.
     * @param xpath  XPath expression selecting a fragment from a {@link DOMResource}
     * @param position  the character scheme position inside the fragment
     */
    protected Pair<XdmNode, Integer> reportNotFound(String xpath, int position) throws SelectorException {
	LOG.error("Position char={} is not in the fragment selected by the XPath '{}'", position, xpath);
	throw new SelectorException
	    ("position char=" + position + " is not in the fragment selected by the XPath '" + xpath + "'");
    }

    /**
     * A utility method that gets the depth of a node by counting
     * its ancestors.
     */
    protected static int getDepth(XdmNode node) {
	Iterator<XdmNode> ascendents = node.axisIterator(Axis.ANCESTOR_OR_SELF);
	int depth = 0;
	while (ascendents.hasNext()) {
	    depth += 1;
	    ascendents.next();
	}
	return depth;
    }

    /**
     * A utiltity method that gets the depth of a node of a pair of
     * node and position like used in this module.
     */
    protected static int getDepth(Pair<XdmNode, Integer> pair) {
	return getDepth(pair.getLeft());
    }

}
