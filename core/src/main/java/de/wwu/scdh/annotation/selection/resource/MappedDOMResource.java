package de.wwu.scdh.annotation.selection.resource;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.net.URI;
import java.net.URL;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.SaxonApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.wwu.scdh.annotation.selection.MappedResource;
import de.wwu.scdh.annotation.selection.ResourceException;

/**
 * A {@link MappedResource} mapping two {@link DOMResource}s to each
 * other. The mapping is based on attributes and text node wrappers,
 * that are in <code>libtrace.xsl</code> and its internal
 * replacements.
 */
public class MappedDOMResource extends DOMResource implements MappedResource<XdmNode, XdmValue, XdmNode, XdmNode> {

    private static final Logger LOG = LoggerFactory.getLogger(MappedDOMResource.class);

    public static final String TRACKING_NAMESPACE = "http://wwu.de/scdh/selection-engine/node-tracing";

    public static final String TRACKING_PREFIX = "track";

    public static final String TRACKING_ATTRIBUTE = "source-id";

    public static final String TRACKING_XPATH = "@" + TRACKING_PREFIX + ":" + TRACKING_ATTRIBUTE;

    public static final String ID_XPATH = "generate-id(.)";

    protected Map<String, XdmNode> idToPreimageNode = new HashMap<String, XdmNode>();
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
	XPathCompiler compiler = getProcessor().newXPathCompiler();
	compiler.declareNamespace(TRACKING_PREFIX, TRACKING_NAMESPACE);
	XPathExecutable xpathExecutable;
	try {
	    xpathExecutable = compiler.compile(ID_XPATH);
	} catch (SaxonApiException e) {
	    LOG.error("failed to compile XPath expression {}", ID_XPATH);
	    throw new ResourceException("failed to compile XPath expression " + ID_XPATH);
	}
	XPathSelector selector;
	XdmItem nodeId;
	while (nodes.hasNext()) {
	    XdmNode node = nodes.next();
	    try {
		// generate/get ID
		selector = xpathExecutable.load();
		selector.setContextItem(node);
		nodeId = selector.evaluateSingle();
		if (nodeId == null) {
		    LOG.error("cannot leave trace on {}", node.getNodeKind());
		    continue;
		}
		LOG.debug("set source id on {} {}: {}", node.getNodeKind(), node.getNodeName(), nodeId.getStringValue());
		// add to mappings
		idToPreimageNode.put(nodeId.getStringValue(), node);
	    } catch (SaxonApiException e) {
		LOG.error("failed to generate ID for preimage node");
		throw new ResourceException(e.getMessage());
	    }
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
	XPathSelector selector;
	XPathExecutable xpathExecutable;
	XPathCompiler compiler = getProcessor().newXPathCompiler();
	compiler.declareNamespace(TRACKING_PREFIX, TRACKING_NAMESPACE);
	try {
	    xpathExecutable = compiler.compile(TRACKING_XPATH);
	} catch (SaxonApiException e) {
	    LOG.error("failed to compile XPath expression {}", TRACKING_XPATH);
	    throw new ResourceException("failed to compile XPath expression " + TRACKING_XPATH);
	}
	XdmItem nodeId;
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
		// if (!node.getNodeKind().equals(XdmNodeKind.ELEMENT))
		// 	continue;
		try {
		    // generate/get ID
		    selector = xpathExecutable.load();
		    selector.setContextItem(node);
		    nodeId = selector.evaluateSingle();
		    // Id known in preimage?
		    if (nodeId == null || !idToPreimageNode.containsKey(nodeId.getStringValue())) {
			// if no user data present, we can only set the reverse map
			reverseMap.put(node, null);
			continue;
		    }
		    XdmNode preimageNode = idToPreimageNode.get(nodeId.getStringValue());
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
		} catch (SaxonApiException e) {
		    LOG.error("failed to generate Id for image node");
		    throw new ResourceException("failed to generate Id for imae node");
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
    protected static Optional<String> getNodeTrace(XdmNode node, String xpath) throws ResourceException {
	try {
	    Processor processor = node.getProcessor();
	    XPathCompiler compiler = processor.newXPathCompiler();
	    compiler.declareNamespace(TRACKING_PREFIX, TRACKING_NAMESPACE);
	    XPathExecutable executable = compiler.compile(xpath);
	    XPathSelector selector = executable.load();
	    selector.setContextItem(node);
	    XdmItem value = selector.evaluateSingle();
	    if (value == null) {
		return Optional.of(xpath + " not present");
	    }
	    // if (!value.isAtomicValue()) {
	    // 	LOG.error("evaluating '{}' on provided node does not return an atomic value");
	    // 	throw new ResourceException("evaluating '" + xpath + "' on provided node does not return an atomic value");
	    // }
	    return Optional.of(value.getStringValue());
	}  catch (SaxonApiException e) {
	    LOG.error(e.getMessage());
	    throw new ResourceException(e.getMessage());
	}
    }

    /**
     * Return an {@link URL} to a stylesheet for mapping;
     */
    public URL getMappingResource() {
	return this.getClass().getResource("/xslt/libtrace-xml.xsl");
    }

}
