package de.wwu.scdh.annotation.selection;

import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.SaxonApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class XPathNormalizerWithXPath extends XPathNormalizer {

    private static final Logger LOG = LoggerFactory.getLogger(XPathNormalizerWithXPath.class);

    public static final String PATH_EXPRESSION_XPATH = "path(.)";

    public static final String PATH_EXPRESSION_CLARK_XPATH = "let $ctx:=., $elsteps:=$ctx/ancestor-or-self::element(), $txtstep:=$ctx[self::text()] return ($elsteps ! (let $step:=., $ns:=namespace-uri($step), $name:=name($step), $pos:=count($step/preceding-sibling::node()[name() eq $name])+1 return concat('/Q{', $ns, '}', $name, '[', $pos, ']')), $txtstep ! (concat('/text()[', count($txtstep/preceding-sibling::text())+1, ']'))) => string-join('')";

    public static final String FROM_DEEPEST_ID_CLARK_XPATH = "let $ctx:=., $elsteps:=$ctx/ancestor-or-self::element(), $ids:=for $step in 1 to count($elsteps) return if ($elsteps[$step]/@xml:id) then $step else -1, $idat:=$ids[. ne -1][last()], $idstep:=if ($idat) then concat('id(&apos;', $elsteps[$idat]/@xml:id, '&apos;)') else '', $txtstep:=$ctx[self::text()] return concat($idstep, ($elsteps[position() gt $idat] ! (let $step:=., $ns:=namespace-uri($step), $name:=name($step), $pos:=count($step/preceding-sibling::node()[name() eq $name])+1 return concat('/Q{', $ns, '}', $name, '[', $pos, ']')), $txtstep ! (concat('/text()[', count($txtstep/preceding-sibling::text())+1, ']'))) => string-join(''))";

    protected final String xpath;

    public XPathNormalizerWithXPath(DOMResource resource, String xpath) {
	super(resource);
	this.xpath = xpath;
    }

    @Override
    protected String getNormalizedXPath(XdmNode node, boolean escaped) throws SelectorException {
	XPathCompiler compiler = this.resource.getProcessor().newXPathCompiler();
	XdmValue nodes;
	try {
	    XPathExecutable executable = compiler.compile(xpath);
	    XPathSelector selector = executable.load();
	    selector.setContextItem(node);
	    nodes = selector.evaluate();
	} catch (SaxonApiException e) {
	    LOG.error("failed to normalize XPath using '{}': ", xpath, e.getMessage());
	    throw new SelectorException(e);
	}
	if (nodes.size() != 1) {
	    LOG.error("normalizing XPath '{}' did not return exactly one item: returned {} items", xpath, nodes.size());
	    throw new SelectorException("normalizing XPath '" +
					xpath +
					"' did not return exactly one item: returned " +
					String.valueOf(nodes.size()) +
					" item");
	} else if (!nodes.itemAt(0).isAtomicValue()) {
	    LOG.error("normalizing XPath '{}' did not return an atomic value", xpath, nodes.size());
	    throw new SelectorException("normalizing XPath '" +
					xpath +
					"' did not return an atomic value");
	} else {
	    if (escaped) {
		return nodes.itemAt(0).getStringValue();
	    } else {
		return nodes.itemAt(0).getUnderlyingValue().getUnicodeStringValue().toString();
	    }
	}
    }

    public String getXPath() {
	return xpath;
    }
    
}
