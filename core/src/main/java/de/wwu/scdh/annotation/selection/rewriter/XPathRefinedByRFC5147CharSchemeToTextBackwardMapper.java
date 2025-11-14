package de.wwu.scdh.annotation.selection.rewriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

import net.sf.saxon.s9api.XdmNode;

import org.apache.commons.lang3.tuple.Pair;

import de.wwu.scdh.annotation.selection.*;
import de.wwu.scdh.annotation.selection.resource.MappedDOMResource;
import de.wwu.scdh.annotation.selection.point.XPathRefinedByRFC5147CharScheme;
import de.wwu.scdh.annotation.selection.point.RFC5147CharScheme;

import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmItem;

/**
 * Map an {@link XPathRefinedByRFC5147CharScheme} point inside a
 * preimage to the analog point inside the derived plain text image.<P>
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
 * 3) Calculate the RFC5147 character scheme, i.e. the inter-glyph
 * character position, by summing up the lengths of text nodes
 * preceding the node gotten in step 2 in the image and add the char
 * scheme position gotten in step 2. This sum is the new inter-glyph
 * character position in the plain text image.
 */
public class XPathRefinedByRFC5147CharSchemeToTextBackwardMapper
    extends XPathRewriterBase
    implements Rewriter<MappedDOMResource, RFC5147CharScheme, XPathRefinedByRFC5147CharScheme> {

    protected final String xpath;

    /**
     * Create a {@link XPathRefinedByRFC5147CharSchemeToTextBackwardMapper}
     * for a {@link MappedDOMResource}.
     *
     * @param xpath  an XPath expression which will be evaluated on a context node for normalization
     */
    public XPathRefinedByRFC5147CharSchemeToTextBackwardMapper(String xpath) {
	this.xpath = xpath;
    }

    /**
     * Utility function that returns the length of an {@link XdmItem}
     * when serialized.
     */


    /**
     * {@inheritDoc}
     */
    @Override
    public List<XPathRefinedByRFC5147CharScheme> rewrite
	(MappedDOMResource preimage,
	 RFC5147CharScheme position,
	 RewriterConfig config) throws SelectorException {
	// step 1: get text node in image where position is located
	Pair<XdmNode, Integer> imagePair =
	    getTextNodeAtPosition(preimage.getImage(), "/", position.getChar(), config.getMode());
	// text nodes are wrapped in trace:text and these wrappers are
	// mapped to the preimage text nodes. So we have to get the
	// wrapper node, i.e. the parent.
	XdmNode textTraceElement = imagePair.getLeft().getParent();
	System.out.println(imagePair.toString());
	System.out.println(textTraceElement);
	System.out.println("constant null: " + null);
	System.out.println("preimage node: " + preimage.getCorrespondingInPreimage(textTraceElement));
	// step 2: map text node from image to maybe (one or none) of text node from preimage
	Optional<XdmNode> preimageNode = preimage.getCorrespondingInPreimage(textTraceElement);
	System.out.println("preimage node: ");
	// step 3: rebase each image node as char scheme only: The
	// charScheme component of the preimagePair is the offset to
	// the text node in the result. Add the lenght of all text
	// nodes before the imageNode to get the actual position.
	List<XPathRefinedByRFC5147CharScheme> transformed = new ArrayList<XPathRefinedByRFC5147CharScheme>();
	if (preimageNode.isPresent()) {
	    String normalizedXPath = pathExpressionWithXPath(getXPath(), preimageNode.get(), config.getEscaped(), preimage.getProcessor());
	    XdmNode normalizedNode = getNode(preimage.getContents(), unespace(normalizedXPath), preimage.getProcessor());
	    Integer normalizedPos = posInNormalizedNode(preimageNode.get(), imagePair.getRight(), normalizedNode);
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
