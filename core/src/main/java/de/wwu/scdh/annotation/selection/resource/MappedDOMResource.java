package de.wwu.scdh.annotation.selection.resource;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.net.URI;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.Axis;

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
	int nodeId;
	while (nodes.hasNext()) {
	    XdmNode node = nodes.next();
	    nodeId = node.hashCode();
	    // add to mappings
	    idToPreimageNode.put(nodeId, node);
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
		int nodeId = node.hashCode();
		if (!idToPreimageNode.containsKey(nodeId)) {
		    // if no user data present, we can only set the reverse map
		    reverseMap.put(node, null);
		    continue;
		}
		XdmNode preimageNode = idToPreimageNode.get(nodeId);
		// set the reverse map
		reverseMap.put(node, preimageNode);
		// set the forward map, where a preimage node may be
		// mapped to multiple nodes in the image
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
	return Optional.of(node.hashCode());
    }

}
