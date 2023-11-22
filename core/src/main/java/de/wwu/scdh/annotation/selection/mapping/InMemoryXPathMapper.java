package de.wwu.scdh.annotation.selection.mapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.wwu.scdh.annotation.selection.XPathNormalizer;


/**
 * The InMemoryXPathMapper is the reference implementation of the
 * {@link XPathMapper} interface. It maps selections between image
 * (HTML) and preimage (XML).
 */
public class InMemoryXPathMapper extends XPathMapper {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryXPathMapper.class);

    protected final XPathLookup lookup;

    protected final BiMap<String,String> biLookup;

    public InMemoryXPathMapper
	(XPathNormalizer preimageNormalizer,
	 XPathNormalizer imageNormalizer,
	 XPathLookup lookup)
	throws LookupCreationException {
	super(preimageNormalizer, imageNormalizer, lookup);
	this.lookup = lookup;
	// can we mark this for lazy evalation?
	try {
	    biLookup = HashBiMap.create();
	    lookup.htmlSourceMapping.forEach((k, v) -> biLookup.put(k, v.xpath));
	} catch (Exception e) {
	    LOG.error("failed to create the lookup: {}", e.getMessage());
	    throw new LookupCreationException(e);
	}
    }

    @Override
    public String getPreimageXPath(String imageXPath) throws MappingException {
	try {
	    return this.lookup.htmlSourceMapping.get(imageXPath).xpath;
	} catch (Exception e) {
	    LOG.error(e.getMessage());
	    throw new MappingException(e);
	}
    }

    @Override
    public String getImageXPath(String preimageXPath) throws MappingException {
	try {
	    return biLookup.inverse().get(preimageXPath);
	} catch (Exception e) {
	    LOG.error(e.getMessage());
	    throw new MappingException(e);
	}
    }

}
