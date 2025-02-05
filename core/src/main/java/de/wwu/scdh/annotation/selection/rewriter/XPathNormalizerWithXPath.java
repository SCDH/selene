package de.wwu.scdh.annotation.selection.rewriter;

import net.sf.saxon.s9api.XdmNode;

import de.wwu.scdh.annotation.selection.*;
import de.wwu.scdh.annotation.selection.resource.DOMResource;


/**
 * A {@link XPathNormalizer} that generates normalized path
 * expressions by using an XPath expression that is evaluated on the
 * context node in question.<P>
 *
 * The context item on which the XPath will be evaluated, is
 * determined by {@link XPathNormalizer#getTextNodeAtPosition(String, int,boolean)}.
 * This is generally a text node.
 */
public class XPathNormalizerWithXPath extends XPathNormalizer {

    /**
     * An XPath expression suitable for normalizing path expressions
     * to nodes.<P>
     *
     * Generates a path expression using the <code>path(.)</code>
     * XPath function. This result in a path expression stepping from
     * the root element to the context node. Element names are QNames
     * in Clark notation.
     */
    public static final String PATH_FUNCTION_XPATH = "path(.)";

    /**
     * An XPath expression suitable for normalizing path expressions
     * to nodes.<P>
     *
     * Generates a simple path expression stepping from the root
     * element to the context node. Element names are QNames in Clark
     * notation.
     */
    public static final String FROM_ROOT_CLARK_XPATH = "let $ctx:=., $elsteps:=$ctx/ancestor-or-self::element(), $txtstep:=$ctx[self::text()] return ($elsteps ! (let $step:=., $ns:=namespace-uri($step), $name:=name($step), $pos:=count($step/preceding-sibling::node()[name() eq $name])+1 return concat('/Q{', $ns, '}', $name, '[', $pos, ']')), $txtstep ! (concat('/text()[', count($txtstep/preceding-sibling::text())+1, ']'))) => string-join('')";

    /**
     * An XPath expression suitable for normalizing path expressions
     * to nodes.<P>
     *
     * Generates a path expression stepping from the deepest element
     * with an XML-ID (<code>@xml:id</code>) to the context
     * node. Element names are QNames in Clark notation.
     */
    public static final String FROM_DEEPEST_ID_CLARK_XPATH = "let $ctx:=., $elsteps:=$ctx/ancestor-or-self::element(), $ids:=for $step in 1 to count($elsteps) return if ($elsteps[$step]/@xml:id) then $step else -1, $idat:=$ids[. ne -1][last()], $idstep:=if ($idat) then concat('id(&apos;', $elsteps[$idat]/@xml:id, '&apos;)') else '', $txtstep:=$ctx[self::text()] return concat($idstep, ($elsteps[position() gt $idat or empty($idat)] ! (let $step:=., $ns:=namespace-uri($step), $name:=name($step), $pos:=count($step/preceding-sibling::node()[name() eq $name])+1 return concat('/Q{', $ns, '}', $name, '[', $pos, ']')), $txtstep ! (concat('/text()[', count($txtstep/preceding-sibling::text())+1, ']'))) => string-join(''))";

    protected final String xpath;

    /**
     * Create a new {@link XPathNormalizerWithXPath} normalizer for a
     * {@link DOMResource}.
     *
     * @param xpath  an XPath expression which will be evaluated on a context node for normalization
     */
    public XPathNormalizerWithXPath(String xpath) {
	super();
	this.xpath = xpath;
    }

    /**
     * Get the path to the {@link XdmNode} given as parameter by using
     * the XPath expressing the normalizer was initialized with.
     */
    @Override
    protected String getNormalizedXPath(DOMResource resource, XdmNode node, boolean escaped) throws SelectorException {
	return pathExpressionWithXPath(getXPath(), node, escaped, resource.getProcessor());
    }

    public String getXPath() {
	return xpath;
    }

}
