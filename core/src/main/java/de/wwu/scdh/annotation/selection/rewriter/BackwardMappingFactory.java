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

public class BackwardMappingFactory implements RewriterFactory {

	private static final Logger Log = LoggerFactory.getLogger(ForwardMappingFactory.class);

	private final XPathCompiler compiler;

	/**
	 * Creates a new {@link BackwardMappingFactory} with a {@link XPathCompiler}.
	 * @param compiler - A compiler used for evaluating XPath expressions.
	 */
	public BackwardMappingFactory(XPathCompiler compiler) {
		this.compiler = compiler;
	}

	// @SuppressWarnings("unchecked")
	@Override
	public <R extends Resource<?>, P1 extends Point, P2 extends Point, P3 extends Point, RW extends Rewriter<R, P1, P3>>
			RW getRewriter(Class<P1> point1, Class<P2> point2, RewriterConfig config) throws ConfigurationException {

		Class<P3> mappedPointClass;
		mappedPointClass = (Class<P3>) config.getPointClassMap().getOrDefault(point2, point2);

		RW rc;
		if (XPathRefinedByRFC5147CharScheme.class.isAssignableFrom(point1)
				&& XPathRefinedByRFC5147CharScheme.class.isAssignableFrom(mappedPointClass)) {
			rc = (RW) new XPathRefinedByRFC5147CharSchemeBackwardMapper(compiler, config.getXPath());
		} else if (RFC5147CharScheme.class.isAssignableFrom(point1)
				&& XPathRefinedByRFC5147CharScheme.class.isAssignableFrom(mappedPointClass)) {
			rc = (RW) new XPathRefinedByRFC5147CharSchemeToTextBackwardMapper(compiler, config.getXPath());
		} else {
			Log.error("no backward mapping for {}, {}", point1.getCanonicalName(), mappedPointClass.getCanonicalName());
			throw new ConfigurationException("no backward mapping for " + point1.getCanonicalName() + " ; "
					+ mappedPointClass.getCanonicalName());
		}
		return rc;
	}
}
