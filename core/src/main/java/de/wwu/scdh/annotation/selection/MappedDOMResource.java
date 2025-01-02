package de.wwu.scdh.annotation.selection;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.Axis;

import org.w3c.dom.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MappedDOMResource extends DOMResource implements
		MappedResource<XdmNode, XdmNode, XdmNode, XdmNode, XPathRefinedByRFC5147CharScheme, XPathRefinedByRFC5147CharScheme> {

    private static final Logger LOG = LoggerFactory.getLogger(MappedDOMResource.class);

    public static final String NODE_ID_USER_DATA = "mapped-dom-id";

    protected Map<Integer, XdmNode> idToPreimageNode = new HashMap<Integer, XdmNode>();
    protected Map<XdmNode, List<XdmNode>> forwardMap = new HashMap<XdmNode, List<XdmNode>>();
    protected Map<XdmNode, XdmNode> reverseMap = new HashMap<XdmNode, XdmNode>();

    private DOMResource image = null;

    /**
     * Make a {@link MappedDOMResource} from a {@link DOMResource}
     * which is used as the preimage.
     */
    public MappedDOMResource(DOMResource preimage) throws ResourceException {
	super(preimage.getUri(), preimage.getContents(), preimage.getProcessor());
	leaveTraces();
    }

    /**
     * Set the image {@link DOMResource}.
     */
    public void setImage(DOMResource image) throws ResourceException {
	this.image = image;
	readTraces();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DOMResource getImage() {
	return image;
    }

    protected void leaveTraces() throws ResourceException {
	XdmSequenceIterator<XdmNode> nodes = getContents().axisIterator(Axis.DESCENDANT_OR_SELF);
	int nodeId = 1;
	while (nodes.hasNext()) {
	    XdmNode node = nodes.next();
	    // write node ID to DOM level 3 user data
	    if (!Node.class.isAssignableFrom(node.getExternalNode().getClass())) {
		LOG.error("underlying nodes from preimage are not org.w3c.dom nodes");
		throw new ResourceException("underlying nodes from preimage are not org.w3c.dom nodes");
	    }
	    Node domNode = (Node) node.getExternalNode();
	    domNode.setUserData(NODE_ID_USER_DATA, nodeId, null);
	    // add to mappings
	    idToPreimageNode.put(nodeId, node);
	    // increment node ID
	    nodeId++;
	}
    }

    protected void readTraces() throws ResourceException {
	XdmSequenceIterator<XdmNode> nodes = getImage().getContents().axisIterator(Axis.DESCENDANT_OR_SELF);
	while (nodes.hasNext()) {
	    XdmNode node = nodes.next();
	    // get node ID from DOM level 3 user data
	    if (!Node.class.isAssignableFrom(node.getExternalNode().getClass())) {
		LOG.error("underlying nodes from image are not org.w3c.dom nodes");
		throw new ResourceException("underlying nodes from image are not org.w3c.dom nodes");
	    }
	    Node domNode = (Node) node.getExternalNode();
	    Object userData = domNode.getUserData(NODE_ID_USER_DATA);
	    if (userData == null) {
		// if no user data present, we can only set the reverse map
		reverseMap.put(node, null);
		continue;
	    }
	    // set the reverse map
	    int nodeId = (int) userData;
	    // set the forward map, where a preimage node may be
	    // mapped to multiple nodes in the image
	    XdmNode preimageNode = idToPreimageNode.get(nodeId);
	    reverseMap.put(node, preimageNode);
	    if (forwardMap.containsKey(preimageNode)) {
		forwardMap.get(preimageNode).add(node);
	    } else {
		List<XdmNode> imageNodes = new ArrayList<XdmNode>();
		imageNodes.add(node);
		forwardMap.put(preimageNode, imageNodes);
	    }
	}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<XdmNode> getCorrespondingInPreimage(XdmNode imageNode) {
	return Optional.of(reverseMap.get(imageNode));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<XdmNode> getCorrespondingInImage(XdmNode preimageNode) {
	return forwardMap.get(preimageNode);
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
