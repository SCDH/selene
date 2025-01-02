package de.wwu.scdh.annotation.selection;

import java.util.Optional;

import net.sf.saxon.s9api.XdmNode;

import org.w3c.dom.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MappedDOMResource extends DOMResource implements
		MappedResource<XdmNode, XdmNode, XdmNode, XdmNode, XPathRefinedByRFC5147CharScheme, XPathRefinedByRFC5147CharScheme> {

    private static final Logger LOG = LoggerFactory.getLogger(MappedDOMResource.class);

    public static final String NODE_ID_USER_DATA="mapped-dom-id";

    private DOMResource image;

    @Override
    public DOMResource getImage() {
	return image;
    }



    /**
     * Get the node ID of the given node.
     */
    protected static Optional<Integer> getNodeTrace(XdmNode node) {
	Object underlyingNodeObject = node.getExternalNode();
	if (!Node.class.isAssignableFrom(underlyingNodeObject.getClass())) {
	    LOG.error("node has no user data");
	    return Optional.empty();
	}
	Node domNode = (Node) underlyingNodeObject;
	Integer nodeId = (Integer) domNode.getUserData(NODE_ID_USER_DATA);
	return Optional.of(nodeId);
    }



}
