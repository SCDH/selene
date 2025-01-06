package de.wwu.scdh.annotation.selection.rewriter;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.wwu.scdh.annotation.selection.*;


public class XPathRefinedByRFC5147CharSchemeForwardMapper extends XPathRewriterBase implements Rewriter<MappedDOMResource, XPathRefinedByRFC5147CharScheme, XPathRefinedByRFC5147CharScheme> {

    private final Logger LOG = LoggerFactory.getLogger(XPathRefinedByRFC5147CharSchemeForwardMapper.class);


    public XPathRefinedByRFC5147CharSchemeForwardMapper() {

    }

    @Override
    public List<XPathRefinedByRFC5147CharScheme> rewrite
	(MappedDOMResource preimage,
	 XPathRefinedByRFC5147CharScheme preimagePoint,
	 RewriterConfig config) throws SelectorException {

	// TODO
	return null;
    }

}
