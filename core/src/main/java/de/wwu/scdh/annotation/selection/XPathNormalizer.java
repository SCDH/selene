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
 * selector which may be refined by the RFC5147 character scheme.
 *
 */
public abstract class XPathNormalizer {

    private static final Logger LOG = LoggerFactory.getLogger(XPathNormalizer.class);

    // public record XPathPositionPair(String xpath, int position) {};
    // public record NodePositionPair(XdmNode node, int position) {};
    
    protected final DOMResource resource;

    public XPathNormalizer(DOMResource resource) {
	this.resource = resource;
    }

    public Pair<String, Integer> normalizeXPathRefinedByCharScheme(String xpath, int position)
	throws SelectorException {
	return normalizeXPathRefinedByCharScheme(xpath, position, true);
    }

    public Pair<String, Integer> normalizeXPathRefinedByCharScheme(String xpath, int position, boolean escaped)
	throws SelectorException {
	Pair<XdmNode, Integer> textNode = getTextNodeAtPosition(xpath, position);
	// call the normalization function
	String normalizedXPath = getNormalizedXPath(textNode.getLeft(), escaped);
	return new ImmutablePair<String, Integer>(normalizedXPath, textNode.getRight());
    }

    protected abstract String getNormalizedXPath(XdmNode node, boolean escaped) throws SelectorException;

    protected Pair<XdmNode, Integer> getTextNodeAtPosition(String xpath, int position) throws SelectorException {
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
		XdmNode fragment = (XdmNode) nodes.itemAt(0);
		Iterator<XdmNode> axis = fragment.axisIterator(Axis.DESCENDANT_OR_SELF);
		boolean found = false;
		int charsEaten = 0;
		XdmNode node = fragment;
		while (!found && axis.hasNext()) {
		    node = axis.next();
		    LOG.debug("investigating '{}' node", node.getUnderlyingNode().getLocalPart());
		    if (node.getNodeKind().equals(XdmNodeKind.TEXT)) {
			int length = node.getUnderlyingValue().getUnicodeStringValue().length32();
			if (position - charsEaten <= length) {
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
	} catch (SaxonApiException e) {
	    throw new SelectorException(e);
	}
    }

}
