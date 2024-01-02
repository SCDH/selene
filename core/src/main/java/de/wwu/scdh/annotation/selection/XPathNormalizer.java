package de.wwu.scdh.annotation.selection;

import java.util.Iterator;

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
 * An {@link XPathNormalizer} offers methods for normalizing an XPath
 * selectors. The constructor takes a {@link DOMResource} on the base
 * of which the normalization of subsequent calls of normalization
 * methods is performed.<P>
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
     * a normalization mode.
     */
    public enum Mode {
	DEEP_NODE_STOP_AT_END,
	DEEP_NODE_STEP_OVER_END
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
     * This method gets the node where the position of an
     * character-scheme-refined XPath selector points to. Either this
     * node is a text node, or the selector is invalid in the context
     * of the current document. In general, the resulting position is
     * not the same as the input position.<P>
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
	default -> {
	    LOG.error("mode {} not implemented", mode.name());
	    throw new SelectorException("mode " + mode.name() + " not implemented");
	}
	};
    }

    /**
     * This is an implementation of the normalization step 1 for the
     * {@link Mode} <code>DEEP_NODE_STOP_AT_END</code> and
     * <code>DEEP_NODE_STEP_OVER_END</code>, which both descend the
     * DOM tree to the deepest text node possible.
     *
     * However, at some positions, normalization is ambigous. E.g., for the
     * simple, but wellformed XML document
     * <code>&lt;r>Sol&lt;t>ar&lt;/t>!&lt;/r></code> the normalization of the
     * XPath pointer `/r` refined by the character scheme `char=3` is
     * ambigous:
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
     * <code>char=0</code>. Setting <code>stepOverEnd</code> to
     * <code>false</code> results in the first normalized selector,
     * setting it to <code>true</code> results in the first.
     *
     *
     * @param xpath  the XPath part of the XPath selector
     * @param position  the position following the character scheme of RFC5147
     * @param stepOverEnd  how to resolve positional ambiguity at changeover between text nodes
     * @throws {@link SelectorException}
     * @return a pair of node and position
     */
    private Pair<XdmNode, Integer> getDeepTextNodeAtPositionWithEndParam(String xpath, int position, boolean stepOverEnd) throws SelectorException {
	XdmNode fragment = getNode(xpath);
	Iterator<XdmNode> axis = fragment.axisIterator(Axis.DESCENDANT_OR_SELF);
	boolean found = false;
	int charsEaten = 0;
	XdmNode node = fragment;
	// int length = 0;
	while (!found && axis.hasNext()) {
	    node = axis.next();
	    LOG.debug("investigating '{}' node", node.getUnderlyingNode().getLocalPart());
	    if (node.getNodeKind().equals(XdmNodeKind.TEXT)) {
		int length = node.getUnderlyingValue().getUnicodeStringValue().length32();
		int diff = position - charsEaten;
		if (diff < length ||
		    // we have to check <= (less or equal) if
		    // a. we do not step over the end
		    // b. we are investigating the last node
		    (diff == length && (!stepOverEnd || !axis.hasNext()))) {
		    // the position is in the current node
		    found = true;
		} else {
		    // we have seen a text node but position is not in it
		    charsEaten += length;
		}
	    }
	}
	if (!found) {
	    LOG.error("Position char={} is not in the fragment selected by the XPath '{}'", position, xpath);
	    throw new SelectorException("Position char=" +
					position +
					" is not in the fragment selected by the XPath '" +
					xpath +
					"'");
	} else {
	    LOG.debug("found node containing position char={} in XPath '{}'", position, xpath);
	    return new ImmutablePair<XdmNode, Integer>(node, position - charsEaten);
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


}
