package de.wwu.scdh.annotation.selection.rewriter;

import java.util.ArrayList;
import java.util.Collections;
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

import de.wwu.scdh.annotation.selection.*;


/**
 * This class provides methods common to normalizers and mappers, that
 * operate on XPath selectors refined by RFC5147 character schemes.
 */
public abstract class XPathRewriterBase {

    private static final Logger LOG = LoggerFactory.getLogger(XPathRewriterBase.class);


    //protected final DOMResource resource;

    /**
     * Make a new {@link XPathNormalizer} for a {@link DOMResource}.
     */
    public XPathRewriterBase() {
    }



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
     * @param resource  the {@link DOMResource} on the base of which the normalization is done
     * @param xpath  the XPath part of the XPath selector
     * @param position  the position following the character scheme of RFC5147
     * @param mode  an normalization algorithm selected from {@link Mode}
     * @throws {@link SelectorException}
     * @return a pair of node and position
     */
    protected Pair<XdmNode, Integer> getTextNodeAtPosition(DOMResource resource, String xpath, int position, Mode mode) throws SelectorException {
	return switch(mode) {
	case FIRST -> getFirstNodeAtPosition(resource, xpath, position);
	case SECOND -> getSecondNodeAtPosition(resource, xpath, position);
	case FIRST_OF_DEEPEST_NODES -> getFirstOfDeepestNodesAtPosition(resource, xpath, position);
	case LAST_OF_DEEPEST_NODES -> getLastOfDeepestNodesAtPosition(resource, xpath, position);
	case DEEP_NODE_STOP_AT_END -> getDeepTextNodeAtPositionStopAtEnd(resource, xpath, position);
	case DEEP_NODE_STEP_OVER_END -> getDeepTextNodeAtPositionStepOverEnd(resource, xpath, position);
	default -> {
	    LOG.error("mode {} not implemented", mode.name());
	    throw new SelectorException("mode " + mode.name() + " not implemented");
	}
	};
    }

    /**
     * The implementation of step 1 of the normalization algorithm in
     * in mode {@link Mode#FIRST}.
     */
    protected final Pair<XdmNode, Integer> getFirstNodeAtPosition(DOMResource resource, String xpath, int position) throws SelectorException {
	XdmNode fragment = getNode(resource, xpath);
	List<Pair<XdmNode, Integer>> nodesAtPosition = getTextNodesWithPosition(fragment, position);
	if (nodesAtPosition.isEmpty()) {
	    return reportNotFound(xpath, position);
	} else {
	    return nodesAtPosition.get(0);
	}
    }

    /**
     * The implementation of step 1 of the normalization algorithm in
     * in mode {@link Mode#SECOND}.
     */
    protected final Pair<XdmNode, Integer> getSecondNodeAtPosition(DOMResource resource, String xpath, int position) throws SelectorException {
	XdmNode fragment = getNode(resource, xpath);
	List<Pair<XdmNode, Integer>> nodesAtPosition = getTextNodesWithPosition(fragment, position);
	if (nodesAtPosition.isEmpty()) {
	    return reportNotFound(xpath, position);
	} else if (nodesAtPosition.size() == 1) {
	    return nodesAtPosition.get(0);
	} else {
	    return nodesAtPosition.get(1);
	}
    }

    /**
     * The implementation of step 1 of the normalization algorithm in
     * in mode {@link Mode#FIRST_OF_DEEPEST_NODES}.
     */
    protected final Pair<XdmNode, Integer> getFirstOfDeepestNodesAtPosition(DOMResource resource, String xpath, int position) throws SelectorException {
	XdmNode fragment = getNode(resource, xpath);
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
     * The implementation of step 1 of the normalization algorithm in
     * in mode {@link Mode#LAST_OF_DEEPEST_NODES}.
     */
    protected final Pair<XdmNode, Integer> getLastOfDeepestNodesAtPosition(DOMResource resource, String xpath, int position) throws SelectorException {
	XdmNode fragment = getNode(resource, xpath);
	List<Pair<XdmNode, Integer>> nodesAtPosition = getDescendantTextNodesWithPosition(fragment, position);
	if (nodesAtPosition.isEmpty()) {
	    return reportNotFound(xpath, position);
	} else if (nodesAtPosition.size() == 1) {
	    return nodesAtPosition.get(0);
	} else {
	    // we still have to get the text node with the deepest path
	    LOG.debug("found {} nodes, getting deepest", nodesAtPosition.size());
	    // note, that Stream.max() returns the first of the items with the maximum value
	    Collections.reverse(nodesAtPosition);
	    Optional<Pair<XdmNode, Integer>> deepest = nodesAtPosition.stream()
		.max(Comparator.comparing(XPathNormalizer::getDepth));
	    return deepest.get();
	}
    }

    /**
     * This is an implementation of the normalization step 1 for the
     * mode {@link Mode#DEEP_NODE_STOP_AT_END}.
     *
     * @param resource  the {@link DOMResource} on the base of which the normalization is done
     * @param xpath  the XPath part of the XPath selector
     * @param position  the position following the character scheme of RFC5147
     * @throws {@link SelectorException}
     * @return a pair of node and position
     */
    protected final Pair<XdmNode, Integer> getDeepTextNodeAtPositionStopAtEnd(DOMResource resource, String xpath, int position) throws SelectorException {
	XdmNode fragment = getNode(resource, xpath);
	List<Pair<XdmNode, Integer>> nodesAtPosition = getDescendantTextNodesWithPosition(fragment, position);
	if (nodesAtPosition.isEmpty()) {
	    return reportNotFound(xpath, position);
	} else {
	    return nodesAtPosition.get(0);
	}
    }

    /**
     * This is an implementation of the normalization step 1 for the
     * mode {@link Mode#DEEP_NODE_STEP_OVER_END}.
     *
     * @param resource  the {@link DOMResource} on the base of which the normalization is done
     * @param xpath  the XPath part of the XPath selector
     * @param position  the position following the character scheme of RFC5147
     * @throws {@link SelectorException}
     * @return a pair of node and position
     */
    protected final Pair<XdmNode, Integer> getDeepTextNodeAtPositionStepOverEnd(DOMResource resource, String xpath, int position) throws SelectorException {
	XdmNode fragment = getNode(resource, xpath);
	List<Pair<XdmNode, Integer>> nodesAtPosition = getDescendantTextNodesWithPosition(fragment, position);
	if (nodesAtPosition.isEmpty()) {
	    return reportNotFound(xpath, position);
	} else if (nodesAtPosition.size() == 1) {
	    return nodesAtPosition.get(0);
	} else {
	    return nodesAtPosition.get(1);
	}
    }

    /**
     * Get the node from the DOM resource given by the the XPath
     * passed as argument. If the XPath does not evaluate to a single
     * node, this method raises an {@link SelectorException}.
     *
     * @param resource  the {@link DOMResource} on the base of which the normalization is done
     * @param xpath  the XPath as {@link String}
     * @return an {@link XdmNode} which the XPath points to
     */
    protected final XdmNode getNode(DOMResource resource, String xpath) throws SelectorException {
	Processor proc = resource.getProcessor();
	XPathCompiler compiler = proc.newXPathCompiler();
	try {
	    XPathExecutable executable = compiler.compile(xpath);
	    XPathSelector selector = executable.load();
	    selector.setContextItem(resource.getContents());
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
     * position inside a given fragment.<P>
     *
     * This method collects all canditates in case of referential
     * ambiguity. See {@link Mode}.
     *
     * @param fragment  as {@link XdmNode} inside a {@link DOMResource}
     * @param position  the RFC 5147 character scheme position inside the fragment
     */
    protected final List<Pair<XdmNode, Integer>> getDescendantTextNodesWithPosition(XdmNode fragment, int position) {
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
     * Helper method used by implementations of normalization stage 1,
     * that search deep text nodes. This return a list of all pairs of
     * a node and a position, that contain the character scheme
     * position inside a given fragment, or in the first text node
     * before or after it.<P>
     *
     * This method collects all canditates in case of referential
     * ambiguity. See {@link Mode}.
     *
     * @param fragment  as {@link XdmNode} inside a {@link DOMResource}
     * @param position  the RFC 5147 character scheme position inside the fragment
     */
    protected final List<Pair<XdmNode, Integer>> getTextNodesWithPosition(final XdmNode fragment, final int position) {
	List<Pair<XdmNode, Integer>> nodesAtPosition = new ArrayList<Pair<XdmNode, Integer>>();
	XdmNode node;
	// 1. before the fragment, if position is zero
	if (position == 0) {
	    Iterator<XdmNode> preceding = fragment.axisIterator(Axis.PRECEDING);
	    boolean textNodeSeen = false;
	    while (preceding.hasNext() && !textNodeSeen) {
		node = preceding.next();
		if (node.getNodeKind().equals(XdmNodeKind.TEXT)) {
		    textNodeSeen = true;
		    int length = node.getUnderlyingValue().getUnicodeStringValue().length32();
		    nodesAtPosition.add(new ImmutablePair<XdmNode,Integer>(node, length));
		}
	    }
	}
	// 2. Inside the fragment
	Iterator<XdmNode> descendants = fragment.axisIterator(Axis.DESCENDANT_OR_SELF);
	int charsEaten = 0;
	node = fragment;
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
	// 3. after the fragment, if reached the end
	if (!descendants.hasNext() && charsEaten == position) {
	    Iterator<XdmNode> following = fragment.axisIterator(Axis.FOLLOWING);
	    boolean textNodeSeen = false;
	    while (following.hasNext() && !textNodeSeen) {
		node = following.next();
		if (node.getNodeKind().equals(XdmNodeKind.TEXT)) {
		    textNodeSeen = true;
		    nodesAtPosition.add(new ImmutablePair<XdmNode,Integer>(node, 0));
		}
	    }
	}
	return nodesAtPosition;
    }

    /**
     * Get the new position value based on the old one and the
     * fragment selected by the normalized XPath.
     *
     * @param resource the {@link DOMResource} operating on
     * @param textNode the text node as {@link XdmNode} gotten from step 1
     * @param pos the position gotten form step 1
     * @param fragment the {@link XdmNode} selected by the XPath resulting from step 2
     * @return the position from step 2
     */
    protected int posInNormalizedNode(DOMResource resource, XdmNode textNode, int pos, XdmNode fragment) throws SelectorException {
	if (textNode.equals(fragment)) {
	    return pos;
	}
	// iter over all descendant text nodes until we found textNode
	boolean found = false;
	int posAcc = pos;
	Iterator<XdmNode> descenant = fragment.axisIterator(Axis.DESCENDANT);
	while (descenant.hasNext() && !found) {
	    XdmNode node = descenant.next();
	    if (!node.getNodeKind().equals(XdmNodeKind.TEXT))
		continue;
	    if (node.equals(textNode)) {
		found = true;
		break;
	    }
	    posAcc += node.getUnderlyingValue().getUnicodeStringValue().length32();
	}
	if (!found) {
	    LOG.error("text node not in fragment described by normalized XPath");
	    throw new SelectorException("text node not in fragment described by normalized XPath");
	}
	return posAcc;
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

    /**
     * A utility function for unescaping XPaths.
     */
    protected String unespace(String in) {
	return in.replace("&apos;", "'");
    }

}
