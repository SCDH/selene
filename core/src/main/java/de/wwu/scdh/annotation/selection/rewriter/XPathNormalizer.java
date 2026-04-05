package de.wwu.scdh.annotation.selection.rewriter;

import java.util.List;
import net.sf.saxon.s9api.XdmNode;

import org.apache.commons.lang3.tuple.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.wwu.scdh.annotation.selection.*;
import de.wwu.scdh.annotation.selection.point.XPathRefinedByRFC5147CharScheme;
import de.wwu.scdh.annotation.selection.resource.DOMResource;


/**
 * The same position (or range) in a {@link DOMResource} can be
 * selected by different pairs of XPaths and RFC 5147 character
 * schemes (or range schemes), i.e. *referentially equal* selectors
 * may have different values. Normalization maps referentially equal
 * selectors to the same selector. This {@link XPathNormalizer} is a
 * base class for such normalization tasks.<P>
 *
 * Normalization of selectors is a 2-stage process: 1) In the first
 * stage, the text position (or range) which is referenced by the
 * selector, has to be found. A pair containing a text node and a RFC
 * 5147 character scheme position is returned. 2) In the second stage,
 * this position is expressed as a selector again, i.e., the node is
 * referenced with an XPath, where the XPath may be written as a path
 * expression descending from the root element, or from the deepest
 * element with an XML-ID, etc., and even the character scheme
 * component of the selector may be recalculated.<P>
 *
 * There is **not** the one and only normalization. Both stages of the
 * normalization process may be implemented differently, leading to
 * different results. For corner cases, even the first stage may lead
 * to different results.<P>
 *
 * This class implements the first stage of the normalization
 * process. The algorithm is selected by values of the {@link Mode}
 * enum type, which is passed to the normalization methods. The second
 * stage of the normalization process has to be implemented by
 * subclasses of the abstract base class.<P>
 *
 * XPath expressions to be normalized may be arbitrary XPath 4.0
 * expressions which select a single node from the
 * {@link DOMResource}. Expressions selecting not exactly one node
 *  result in an {@link SelectorException}.
 */
public abstract class XPathNormalizer extends XPathRewriterBase
    implements Rewriter<DOMResource, XPathRefinedByRFC5147CharScheme, XPathRefinedByRFC5147CharScheme> {

    private static final Logger LOG = LoggerFactory.getLogger(XPathNormalizer.class);


    //protected final DOMResource resource;

    /**
     * Make a new {@link XPathNormalizer} for a {@link DOMResource}.
     */
    public XPathNormalizer() {
    }


    /**
     * Normalize an XPath Selector refined by an RFC5147 character
     * scheme. The position must be a valid character position
     * *inside* the fragment selected by the XPath expression. If the
     * fragment's text (its concatenated text nodes) is shorter than
     * the position's value, a {@link SelectorException} is thrown.<P>
     *
     * In general, this method will result in a recalculated XPath
     * *and* position.
     *
     * @param resource  the {@link DOMResource} on the base of which the normalization is done
     * @param point     an {@link XPathRefinedByRFC5147CharScheme} record representing point in the resource
     * @param config    a record with configuration options
     * @return a recalculated {@link XPathRefinedByRFC5147CharScheme}
     */
    @Override
    public List<XPathRefinedByRFC5147CharScheme> rewrite
	(DOMResource resource, XPathRefinedByRFC5147CharScheme point, RewriterConfig config)
	throws SelectorException {
	    Pair<XdmNode, Integer> textNode = getTextNodeAtPosition
		(resource, point.getXPath(),
		 point.getChar(), config.getMode());
	    String normalizedXPath = getNormalizedXPath(resource, textNode.getLeft(), config.getEscaped());
	    XdmNode normalizedNode = getNode(resource, unespace(normalizedXPath));
	    Integer normalizedPos = posInNormalizedNode(textNode.getLeft(), textNode.getRight(), normalizedNode);
	    return List.of(new XPathRefinedByRFC5147CharScheme(normalizedXPath, normalizedPos));
    }

    /**
     * This abstract method must be implemented by normalizers. It
     * returns the normalized path to the node given as argument.
     *
     * @param node  an {@link XdmNode} to which the normlized path must be generated to
     * @param escaped  whether or not the normalized XPath is to be escaped for X-processing, e.g., <code>'</code> escaped to <code>&amp;apos;</code>.
     * @return the normalized XPath expression as a String
     * @see XPathNormalizer#getTextNodeAtPosition(String, int, boolean)
     */
    protected abstract String getNormalizedXPath(DOMResource resource, XdmNode node, boolean escaped) throws SelectorException;


}
