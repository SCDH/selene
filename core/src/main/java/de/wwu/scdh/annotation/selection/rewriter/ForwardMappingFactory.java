package de.wwu.scdh.annotation.selection.rewriter;

import de.wwu.scdh.annotation.selection.RewriterFactory;
import de.wwu.scdh.annotation.selection.Point;
import de.wwu.scdh.annotation.selection.Resource;
import de.wwu.scdh.annotation.selection.Rewriter;
import de.wwu.scdh.annotation.selection.RewriterConfig;
import de.wwu.scdh.annotation.selection.ConfigurationException;

import de.wwu.scdh.annotation.selection.point.XPathRefinedByRFC5147CharScheme;
import de.wwu.scdh.annotation.selection.point.RFC5147CharScheme;


public class ForwardMappingFactory implements RewriterFactory {

    //@SuppressWarnings("unchecked")
    @Override
    public <R extends Resource<?>, P1 extends Point, P2 extends Point, RW extends Rewriter<R, P1, P2>> RW getRewriter(Class<P1> point1, Class<P2> point2, RewriterConfig config) throws ConfigurationException {

	//return null;
	RW rc;
	if (XPathRefinedByRFC5147CharScheme.class.isAssignableFrom(point2) && XPathRefinedByRFC5147CharScheme.class.isAssignableFrom(point2)) {
	    rc = (RW) new XPathRefinedByRFC5147CharSchemeForwardMapper(config.getXPath());
	} else if (XPathRefinedByRFC5147CharScheme.class.isAssignableFrom(point2) && RFC5147CharScheme.class.isAssignableFrom(point2)) {
	    rc = (RW) new XPathRefinedByRFC5147CharSchemeToTextForwardMapper(config.getXPath());
	} else {
	    throw new ConfigurationException("no forward mapping for " + point1.getClass().getCanonicalName() + " ; "
					     + point2.getClass().getCanonicalName());
	}
	return rc;
    }


}
