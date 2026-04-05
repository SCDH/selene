package de.wwu.scdh.annotation.selection.rewriter;

import java.util.ArrayList;
import java.util.List;

import net.sf.saxon.s9api.XdmNode;

import org.apache.commons.lang3.tuple.Pair;

import de.wwu.scdh.annotation.selection.*;
import de.wwu.scdh.annotation.selection.resource.MappedDOMResource;
import de.wwu.scdh.annotation.selection.point.XPathRefinedByRFC5147CharScheme;

/**
 * Map an {@link XPathRefinedByRFC5147CharScheme} point inside a
 * preimage to the analog point inside the derived image.<P>
 *
 * The mapping involves 3 steps:<P>
 *
 * 1) Descend to the preimage's text node that has the point and save
 * this node and the char scheme position inside it.<P>
 *
 * 2) Get the corresponding node of the derived image resource. We now
 * have the image's text node and the char scheme position inside
 * it. Note, that there may be multiple text nodes in the image, that
 * correspond to the same text node in the preimage. So the next step
 * will be applied to each of these pairs of nodes and char scheme
 * positions.<P>
 *
 * 3) Calculate a normalized pair of path expression and char scheme
 * that is referentially equivalent. This involves getting the path
 * expression using an XPath that returns a path expression, and then
 * recalculating the position inside the fragment that is selected by
 * this path expression. The XPath that returns a path expression is a
 * configuration feature of this rewriter and thus passed to the
 * constructor.
 */
public class XPathRefinedByRFC5147CharSchemeForwardMapper
    extends XPathRewriterBase
    implements Rewriter<MappedDOMResource, XPathRefinedByRFC5147CharScheme, XPathRefinedByRFC5147CharScheme> {

    protected final String xpath;

    /**
     * Create a {@link XPathRefinedByRFC5147CharSchemeForwardMapper}
     * for a {@link MappedDOMResource}.
     *
     * @param xpath  an XPath expression which will be evaluated on a context node for normalization
     */
    public XPathRefinedByRFC5147CharSchemeForwardMapper(String xpath) {
	this.xpath = xpath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<XPathRefinedByRFC5147CharScheme> rewrite
	(MappedDOMResource preimage,
	 XPathRefinedByRFC5147CharScheme preimagePoint,
	 RewriterConfig config) throws SelectorException {
	// step 1: normalize to pair of text node and position
	Pair<XdmNode, Integer> preimagePair =
	    getTextNodeAtPosition (preimage, preimagePoint.getXPath(), preimagePoint.getChar(), config.getMode());
	// step 2: map text node from preimage to list of text nodes from image
	List<XdmNode> imageNodes = preimage.getCorrespondingInImage(preimagePair.getLeft());
	// step 3: rebase in image using configured XPath
	List<XPathRefinedByRFC5147CharScheme> transformed = new ArrayList<XPathRefinedByRFC5147CharScheme>();
	for (XdmNode imageNode : imageNodes) {
	    String normalizedXPath = pathExpressionWithXPath(getXPath(), imageNode, config.getEscaped(), preimage.getProcessor());
	    XdmNode normalizedNode = getNode(preimage.getImage().getContents(), unespace(normalizedXPath), preimage.getProcessor());
	    Integer normalizedPos = posInNormalizedNode(imageNode, preimagePair.getRight(), normalizedNode);
	    transformed.add(new XPathRefinedByRFC5147CharScheme(normalizedXPath, normalizedPos));
	}
	return transformed;
    }

    /**
     * Returns the XPath the mapper instance was configured with.
     */
    public String getXPath() {
	return xpath;
    }

}
