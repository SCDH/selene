package de.wwu.scdh.annotation.selection.resource;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.net.URI;
import java.io.InputStream;
import java.io.IOException;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.Axis;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.wwu.scdh.annotation.selection.MappedResource;
import de.wwu.scdh.annotation.selection.ResourceException;

/**
 *
 *
 */
public class MappedDOMResource extends DOMResource implements MappedResource<XdmNode, XdmValue, XdmNode, XdmNode> {

    private static final Logger LOG = LoggerFactory.getLogger(MappedDOMResource.class);

    public static final String NODE_ID_USER_DATA = "mapped-dom-id";

    protected Map<Integer, XdmNode> idToPreimageNode = new HashMap<Integer, XdmNode>();
    protected Map<XdmNode, List<XdmNode>> forwardMap = new HashMap<XdmNode, List<XdmNode>>();
    protected Map<XdmNode, XdmNode> reverseMap = new HashMap<XdmNode, XdmNode>();

    private XdmValueResource image = null;

    /**
     * Make a {@link MappedDOMResource} from a {@link DOMResource}
     * which is used as the preimage.
     */
    public MappedDOMResource(DOMResource preimage) throws ResourceException {
	super(preimage.getUri(), preimage.getContents(), preimage.getProcessor());
	leaveTraces();
    }

    /**
     * Create a new {@link MappedDOMResource}.
     *
     * @param uri a {@link URI} identifying the resource
     * @param document  the document node (root node)
     * @param processor a saxon {@link Processor} that was used by the document builder
     */
    public MappedDOMResource(URI uri, XdmNode document, Processor processor) throws ResourceException {
	super(uri, document, processor);
	leaveTraces();
    }


    /**
     * Set the image {@link DOMResource}.
     */
    public void setImage(XdmValueResource image) throws ResourceException {
	this.image = image;
	readTraces();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XdmValueResource getImage() {
	return image;
    }

    /**
     * Prepare mapping writing node identifiers into DOM level 3 user
     * data and setting up {@link MappedDOMResource.idToPreimageNode}.
     */
    protected void leaveTraces() throws ResourceException {
	XdmSequenceIterator<XdmNode> nodes = getContents().axisIterator(Axis.DESCENDANT_OR_SELF);
	int nodeId = 1;
	while (nodes.hasNext()) {
	    XdmNode node = nodes.next();
	    // write node ID to DOM level 3 user data
	    if (node.getExternalNode() == null || !Node.class.isAssignableFrom(node.getExternalNode().getClass())) {
		String nodeClass = "null";
		if (node.getExternalNode() != null) {
		    nodeClass = node.getExternalNode().getClass().getCanonicalName();
		}
		LOG.error("underlying nodes from preimage are not org.w3c.dom nodes: {}", nodeClass);
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

    /**
     * Make the mappings {@link MappedDOMResource.forwardMap} and
     * {@link MappedDOMResource.reverseMap} by iterating over the
     * items in the image, i.e., its contents, and read the DOM level
     * 3 user data in there.
     */
    protected void readTraces() throws ResourceException {
	XdmSequenceIterator<XdmItem> items = getImage().getContents().iterator();
	// iterate over all items in the mapped resource
	while (items.hasNext()) {
	    XdmItem item = items.next();
	    if (!item.isNode()) {
		continue;
	    }
	    XdmNode node = (XdmNode) item;
	    XdmSequenceIterator<XdmNode> treeIterator = node.axisIterator(Axis.DESCENDANT_OR_SELF);
	    while (treeIterator.hasNext()) {
		node = treeIterator.next();
		if (node.getExternalNode() == null || !Node.class.isAssignableFrom(node.getExternalNode().getClass())) {
		    // this may be true for text nodes generated with <xsl:text>
		    LOG.debug("underlying nodes from image are not org.w3c.dom nodes");
		    continue;
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
	if (underlyingNodeObject == null || !Node.class.isAssignableFrom(underlyingNodeObject.getClass())) {
	    LOG.error("node has no user data");
	    return Optional.empty();
	}
	Node domNode = (Node) underlyingNodeObject;
	Integer nodeId = (Integer) domNode.getUserData(NODE_ID_USER_DATA);
	return Optional.of(nodeId);
    }

}
