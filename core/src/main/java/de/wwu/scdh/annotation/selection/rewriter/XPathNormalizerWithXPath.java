package de.wwu.scdh.annotation.selection.rewriter;

import de.wwu.scdh.annotation.selection.*;
import de.wwu.scdh.annotation.selection.resource.DOMResource;
import de.wwu.scdh.annotation.selection.resource.S9ApiResource;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XdmNode;

/**
 * A {@link XPathNormalizer} that generates normalized path
 * expressions by using an XPath expression that is evaluated on the
 * context node in question.<P>
 *
 * The context item on which the XPath will be evaluated, is
 * determined by {@link XPathRewriterBase#getTextNodeAtPosition(S9ApiResource, String, int, Mode)}.
 * This is generally a text node.
 */
public class XPathNormalizerWithXPath extends XPathNormalizer {

	/**
	 * An XPath expression suitable for normalizing path expressions
	 * to nodes.<P/>
	 *
	 * Generates a path expression using the <code>path(.)</code>
	 * XPath function. This result in a path expression stepping from
	 * the root element to the context node. Element names are QNames
	 * in Clark notation.
	 */
	public static final String PATH_FUNCTION_XPATH = "path(.)";

	/**
	 * {@inheritDoc}
	 */
	public XPathNormalizerWithXPath(XPathCompiler xPathCompiler, String xpath) {
		super(xPathCompiler, xpath);
	}

	/**
	 * An XPath expression suitable for normalizing path expressions
	 * to nodes.<P>
	 *
	 * Generates a simple path expression stepping from the root
	 * element to the context node. Element names are QNames in Clark
	 * notation.
	 */
	public static final String FROM_ROOT_CLARK_XPATH =
			"let $ctx:=., $elSteps:=$ctx/ancestor-or-self::element(), $txtStep:=$ctx[self::text()] return ($elSteps ! (let $step:=., $ns:=namespace-uri($step), $name:=name($step), $pos:=count($step/preceding-sibling::node()[name() eq $name])+1 return concat('/Q{', $ns, '}', $name, '[', $pos, ']')), $txtStep ! (concat('/text()[', count($txtStep/preceding-sibling::text())+1, ']'))) => string-join('')";

	/**
	 * An XPath expression suitable for normalizing path expressions
	 * to nodes.<P>
	 *
	 * Generates a path expression stepping from the deepest element
	 * with an XML-ID (<code>@xml:id</code>) to the context
	 * node. Element names are QNames in Clark notation.
	 */
	public static final String FROM_DEEPEST_ID_CLARK_XPATH =
			"let $ctx:=., $elSteps:=$ctx/ancestor-or-self::element(), $ids:=for $step in 1 to count($elSteps) return if ($elSteps[$step]/@xml:id) then $step else -1, $idAt:=$ids[. ne -1][last()], $idStep:=if ($idAt) then concat('id(&apos;', $elSteps[$idAt]/@xml:id, '&apos;)') else '', $txtStep:=$ctx[self::text()] return concat($idStep, ($elSteps[position() gt $idAt or empty($idAt)] ! (let $step:=., $ns:=namespace-uri($step), $name:=name($step), $pos:=count($step/preceding-sibling::node()[name() eq $name])+1 return concat('/Q{', $ns, '}', $name, '[', $pos, ']')), $txtStep ! (concat('/text()[', count($txtStep/preceding-sibling::text())+1, ']'))) => string-join(''))";

	/**
	 * Get the path to the {@link XdmNode} given as parameter by using
	 * the XPath expressing the normalizer was initialized with.
	 */
	@Override
	protected String getNormalizedXPath(DOMResource resource, XdmNode node, boolean escaped) throws SelectorException {
		return pathExpressionWithXPath(getXPath(), node, escaped);
	}
}
