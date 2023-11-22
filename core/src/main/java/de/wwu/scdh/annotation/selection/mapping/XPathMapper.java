package de.wwu.scdh.annotation.selection.mapping;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.ImmutablePair;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XPathSelector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.wwu.scdh.annotation.selection.XPathNormalizer;
import de.wwu.scdh.annotation.selection.SelectorException;


/**
 * An {@link XPathMapper} is the workhorse of mapping selections
 * between preimage (original XML) and an image (its HTML projection).
 *
 * This is an abstract base class.
 */
public abstract class XPathMapper {

    private static final Logger LOG = LoggerFactory.getLogger(XPathMapper.class);

    protected final XPathNormalizer preimageNormalizer;
    protected final XPathNormalizer imageNormalizer;

    protected final String preimageXPathGenerator;
    protected final String imageXPathGenerator;

    /**
     * Create a new {@link XPathMapper} object.
     *
     * @param preimageNormalizer a normalizer for the preimage resource (XML)
     * @param imageNormalizer a normalizer for the image resource (HTML projection)
     * @param lookup  an {@link XPathLookup} that maps nodes of the image to the preimage
     */
    protected XPathMapper
	(XPathNormalizer preimageNormalizer,
	 XPathNormalizer imageNormalizer,
	 XPathLookup lookup) {
	this.preimageNormalizer = preimageNormalizer;
	this.imageNormalizer = imageNormalizer;
	this.preimageXPathGenerator = lookup.xmlXPathGenerator;
	this.imageXPathGenerator = lookup.htmlXPathGenerator;
    }

    /**
     * Create a new {@link XPathMapper} object.
     *
     * @param preimageNormalizer a normalizer for the preimage resource (XML)
     * @param imageNormalizer a normalizer for the image resource (HTML projection)
     * @param preimageXPathGenerator an XPath expression for getting a lookup path expression for a preimage context node
     * @param imageXPathGenerator an XPath expression for getting a lookup path expression for a image context node
     */
    protected XPathMapper
	(XPathNormalizer preimageNormalizer,
	 XPathNormalizer imageNormalizer,
	 String preimageXPathGenerator,
	 String imageXPathGenerator) {
	this.preimageNormalizer = preimageNormalizer;
	this.imageNormalizer = imageNormalizer;
	this.preimageXPathGenerator = preimageXPathGenerator;
	this.imageXPathGenerator = imageXPathGenerator;
    }

    /**
     * This first normalizes an character-scheme-refined XPath
     * selector that references the image resource and then maps it to
     * an refined XPath selector that references the corresponding
     * position in the preimage resource.
     *
     * @param imageXPath  the XPath part of the selector
     * @param position  the character scheme part of the selector
     * @param stepOverEnd  how to handle normalization ambiguity
     * @return a pair making the mapped refined XPath selector
     */
    public Pair<String,Integer> getNormalizedPreimageSelector
	(String imageXPath, int position, boolean stepOverEnd)
	throws MappingException {
	return normalizeAndMapSelector
	    (imageXPath, position, stepOverEnd,
	     imageNormalizer, preimageNormalizer,
	     imageXPathGenerator);
    }

    /**
     * This first normalizes an character-scheme-refined XPath
     * selector that references the preimage resource and then maps it
     * to an refined XPath selector that references the corresponding
     * position in the image resource.
     *
     * @param preimageXPath  the XPath part of the selector
     * @param position  the character scheme part of the selector
     * @param stepOverEnd  how to handle normalization ambiguity
     * @return a pair making the mapped refined XPath selector
     */
    public Pair<String,Integer> getNormalizedImageSelector
	(String preimageXPath, int position, boolean stepOverEnd)
	throws MappingException {
	return normalizeAndMapSelector
	    (preimageXPath, position, stepOverEnd,
	     preimageNormalizer, imageNormalizer,
	     preimageXPathGenerator);
    }

    protected Pair<String,Integer> normalizeAndMapSelector
	(String xpath, int position, boolean stepOverEnd,
	 XPathNormalizer normalizer, XPathNormalizer otherNormalizer,
	 String lookupXPathGenerator)
	throws MappingException {
	try {
	    // 1. get the referred node like in normalization
	    Pair<XdmNode, Integer> normalized = normalizer.getTextNodeAtPosition
		(xpath, position, stepOverEnd);
	    // 2. generate path expression for lookup
	    String normalizedXPath = evaluateMappingXPath
		(normalizer, normalized.getLeft(), lookupXPathGenerator);
	    // 3. lookup
	    String mappedXPath = getPreimageXPath(normalizedXPath);
	    // TODO: should we first get the node and then normalize the path expression to it?
	    return new ImmutablePair<String, Integer>(mappedXPath, normalized.getRight());
	} catch (SelectorException e) {
	    throw new MappingException(e);
	}
    }

    /**
     * A helper method.
     */
    protected String evaluateMappingXPath(XPathNormalizer normalizer, XdmNode context, String xpath)
	throws MappingException {
	try {
	    Processor proc = normalizer.getResource().getProcessor();
	    XPathCompiler compiler = proc.newXPathCompiler();
	    XPathExecutable executable = compiler.compile(xpath);
	    XPathSelector selector = executable.load();
	    selector.setContextItem(context);
	    XdmValue result = selector.evaluate();
	    if (result.size() == 1 && result.itemAt(0).isAtomicValue()) {
		return result.itemAt(0).getUnderlyingValue().getStringValue();
	    } else {
		LOG.error("the lookup XPath {} did not evaluate to exactly one item: returned {} items", xpath, result.size());
		throw new MappingException("the lookup XPath " +
					   xpath +
					   " did not evaluate to exactly one item: returned " +
					   result.size() +
					   " items");
	    }
	} catch (SaxonApiException e) {
	    LOG.error(e.getMessage());
	    throw new MappingException(e);
	}
    }

    abstract protected String getPreimageXPath(String imageXPath) throws MappingException;

    abstract protected String getImageXPath(String preimageXPath) throws MappingException;

}
