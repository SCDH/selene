package de.wwu.scdh.annotation.selection.rewriter;

import de.wwu.scdh.annotation.selection.ConfigurationException;
import de.wwu.scdh.annotation.selection.Point;
import de.wwu.scdh.annotation.selection.Resource;
import de.wwu.scdh.annotation.selection.Rewriter;
import de.wwu.scdh.annotation.selection.RewriterConfig;
import de.wwu.scdh.annotation.selection.RewriterFactory;
import de.wwu.scdh.annotation.selection.point.RFC5147CharScheme;
import de.wwu.scdh.annotation.selection.point.XPathRefinedByRFC5147CharScheme;
import net.sf.saxon.s9api.XPathCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForwardMappingFactory implements RewriterFactory {

	private static Logger Log = LoggerFactory.getLogger(ForwardMappingFactory.class);

	private final XPathCompiler compiler;

	/**
	 * Creates a new {@link ForwardMappingFactory} with a {@link XPathCompiler}.
	 * @param compiler - A compiler used for evaluating XPath expressions.
	 */
	public ForwardMappingFactory(XPathCompiler compiler) {
		this.compiler = compiler;
	}

	// @SuppressWarnings("unchecked")
	@Override
	public <R extends Resource<?>, P1 extends Point, P2 extends Point, P3 extends Point, RW extends Rewriter<R, P1, P3>>
			RW getRewriter(Class<P1> point1, Class<P2> point2, RewriterConfig config) throws ConfigurationException {

		Log.debug("looking up mapper for {}, {}", point1.getCanonicalName(), point2.getCanonicalName());

		Class<P3> mappedPointClass;
		if (config.getPointClassMap().containsKey(point2)) {
			mappedPointClass = (Class<P3>) config.getPointClassMap().get(point2);
		} else {
			mappedPointClass = (Class<P3>) point2;
		}

		RW rc;
		if (XPathRefinedByRFC5147CharScheme.class.isAssignableFrom(point1)
				&& XPathRefinedByRFC5147CharScheme.class.isAssignableFrom(mappedPointClass)) {
			rc = (RW) new XPathRefinedByRFC5147CharSchemeForwardMapper(compiler, config.getXPath());
		} else if (XPathRefinedByRFC5147CharScheme.class.isAssignableFrom(point1)
				&& RFC5147CharScheme.class.isAssignableFrom(mappedPointClass)) {
			rc = (RW) new XPathRefinedByRFC5147CharSchemeToTextForwardMapper(compiler, config.getXPath());
		} else {
			Log.error("no forward mapping for {}, {}", point1.getCanonicalName(), mappedPointClass.getCanonicalName());
			throw new ConfigurationException(
					"no forward mapping for " + point1.getClass().getCanonicalName() + " ; "
							+ mappedPointClass.getClass().getCanonicalName());
		}
		return rc;
	}
}
