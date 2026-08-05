package de.wwu.scdh.annotation.selection.rewriter;

import de.wwu.scdh.annotation.selection.*;
import de.wwu.scdh.annotation.selection.point.XPathRefinedByRFC5147CharScheme;
import de.wwu.scdh.annotation.selection.resource.MappedDOMResource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XdmNode;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Map an {@link XPathRefinedByRFC5147CharScheme} point into a image
 * to a {@link XPathRefinedByRFC5147CharScheme} point into the XML
 * preimage.<P>
 *
 * The mapping involves 3 steps:<P>
 *
 * 1) In the Xdm Value representation of the image, descend to the
 * image's text node that has the point and save this node and the
 * char scheme position inside it.<P>
 *
 * 2) Get the corresponding node of the preimage resource. We now have
 * the image's text node and the char scheme position inside it. Note,
 * that there may be one or no text node in the preimage, that
 * corresponds to the same text node in the image. So the next step
 * will be applied to maybe a single or no nodes.<P>
 *
 * 3) Calculate a normalized pair of path expression and char scheme
 * that is referentially equivalent. This involves getting the path
 * expression using an XPath that returns a path expression, and then
 * recalculating the position inside the fragment that is selected by
 * this path expression. The XPath that returns a path expression is a
 * configuration feature of this rewriter and thus passed to the
 * constructor.
 */
public class XPathRefinedByRFC5147CharSchemeBackwardMapper extends XPathRewriterBase
		implements Rewriter<MappedDOMResource, XPathRefinedByRFC5147CharScheme, XPathRefinedByRFC5147CharScheme> {

	/**
	 * {@inheritDoc}
	 */
	public XPathRefinedByRFC5147CharSchemeBackwardMapper(XPathCompiler xPathCompiler, String xpath) {
		super(xPathCompiler, xpath);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<XPathRefinedByRFC5147CharScheme> rewrite(
			MappedDOMResource preimage, XPathRefinedByRFC5147CharScheme position, RewriterConfig config)
			throws SelectorException {
		// step 1: get text node in image where position is located we
		// can use the standard method getTextNodeAtPosition().
		Pair<XdmNode, Integer> imagePair = getTextNodeAtPosition(
				preimage.getImage(),
				replaceTextLeaf(position.getXPath(), config),
				position.getChar(),
				config.getMode());
		// text nodes are wrapped in trace:text and these wrappers are
		// mapped to the preimage text nodes. So we have to get the
		// wrapper node, i.e. the parent.
		XdmNode textTraceElement = imagePair.getLeft().getParent();
		// step 2: map text node from image to maybe (one or none) of text node from preimage
		Optional<XdmNode> preimageNode = preimage.getCorrespondingInPreimage(textTraceElement);
		// step 3: rebase each image node
		List<XPathRefinedByRFC5147CharScheme> transformed = new ArrayList<XPathRefinedByRFC5147CharScheme>();
		if (preimageNode.isPresent()) {
			String normalizedXPath = pathExpressionWithXPath(getXPath(), preimageNode.get(), config.getEscaped());
			XdmNode normalizedNode = getNode(preimage.getContents(), unespace(normalizedXPath));
			Integer normalizedPos = posInNormalizedNode(preimageNode.get(), imagePair.getRight(), normalizedNode);
			transformed.add(new XPathRefinedByRFC5147CharScheme(normalizedXPath, normalizedPos));
		}
		return transformed;
	}
}
