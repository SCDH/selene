package de.wwu.scdh.annotation.selection;


public class RewriterFactoryImpl implements RewriterFactory {

    //@SuppressWarnings("unchecked")
    @Override
    public <R extends Resource, P1 extends Point, P2 extends Point, RW extends Rewriter<R, P1, P2>> RW getRewriter(Class<P1> point1, Class<P2> point2, RewriterConfig config) throws ConfigurationException {

	RW rc;
	if (XPathRefinedByRFC5147CharScheme.class.isAssignableFrom(point2) && XPathRefinedByRFC5147CharScheme.class.isAssignableFrom(point2)) {
	    rc = (RW) new XPathNormalizerWithXPath(config.getXPath());
	} else {
	    throw new ConfigurationException("no rewriter for " + point1.getClass().getCanonicalName() + " ; "
					     + point2.getClass().getCanonicalName());
	}
	return rc;
    }


}
