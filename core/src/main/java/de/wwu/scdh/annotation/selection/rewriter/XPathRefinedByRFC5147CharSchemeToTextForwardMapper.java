package de.wwu.scdh.annotation.selection.rewriter;

import java.util.ArrayList;
import java.util.List;
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
 * 3) Calculate a normalized pair of path expression and char scheme
 * that is referentially equivalent. This involves getting the path
 * expression using an XPath that returns a path expression, and then
 * recalculating the position inside the fragment that is selected by
 * this path expression. The XPath that returns a path expression is a
 * configuration feature of this rewriter and thus passed to the
 * constructor.
 */
public class XPathRefinedByRFC5147CharSchemeToTextForwardMapper
    extends XPathRewriterBase
    implements Rewriter<MappedDOMResource, XPathRefinedByRFC5147CharScheme, RFC5147CharScheme> {

    protected final String xpath = "preceding::text()";

    /**
     * Create a {@link XPathRefinedByRFC5147CharSchemeToTextForwardMapper}
     * for a {@link MappedDOMResource}.
     *
     * @param xpath  an XPath expression which will be evaluated on a context node for normalization
     */
    public XPathRefinedByRFC5147CharSchemeToTextForwardMapper(String xpath) {
	//this.xpath = xpath;
    }

    /**
     * Utility function that returns the length of an {@link XdmItem}
     * when serialized.
     */
    // TODO: do we have to use a serializer to make this agnostic to character encodings?
    private static ToIntFunction<XdmItem> nodeLength = (n) -> n.getUnicodeStringValue().length32();


    /**
     * {@inheritDoc}
     */
    @Override
    public List<RFC5147CharScheme> rewrite
	(MappedDOMResource preimage,
	 XPathRefinedByRFC5147CharScheme preimagePoint,
	 RewriterConfig config) throws SelectorException {
	// step 1: normalize to pair of text node and position
	Pair<XdmNode, Integer> preimagePair =
	    getTextNodeAtPosition (preimage, preimagePoint.getXPath(), preimagePoint.getChar(), config.getMode());
	// step 2: map text node from preimage to list of text nodes from image
	List<XdmNode> imageNodes = preimage.getCorrespondingInImage(preimagePair.getLeft());
	// step 3: rebase each image node as char scheme only: The
	// charScheme component of the preimagePair is the offset to
	// the text node in the result. Add the lenght of all text
	// nodes before the imageNode to get the actual position.
	List<RFC5147CharScheme> transformed = new ArrayList<RFC5147CharScheme>();
	try {
	    XPathCompiler xPathCompiler = preimage.getProcessor().newXPathCompiler();
	    XPathExecutable xPathExecutable = xPathCompiler.compile(xpath);
	    // for every node in image
	    for (XdmNode imageNode : imageNodes) {
		// preimage character pos component is the offset into the image node
		int charPos = preimagePair.getRight();
		// add the length of all preceding text nodes
		XPathSelector selector = xPathExecutable.load();
		selector.setContextItem(imageNode);
		IntStream lengths = selector.stream().mapToInt(nodeLength);
		// add the new char scheme to the result set
		transformed.add(new RFC5147CharScheme(charPos + lengths.sum()));
	    }
	} catch (SaxonApiException e) {
	    throw new SelectorException(e.getMessage());
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
