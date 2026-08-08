package de.wwu.scdh.annotation.selection;

import de.wwu.scdh.annotation.selection.point.RFC5147CharScheme;
import de.wwu.scdh.annotation.selection.point.XPathRefinedByRFC5147CharScheme;
import java.util.Map;

/**
 * A record for configuration parameters of a {@link Rewriter}.
 * The parameters are documented in the constructor.
 */
public class RewriterConfig {

	private final Mode mode;
	private final boolean escaped;
	private final String xpath;
	private final boolean rewritesTraceLeaf;
	private final boolean removeEmptyNamespaces;
	private final Map<Class<? extends Point>, Class<? extends Point>> pointClassMap;

	/**
	 * Make a new {@link RewriterConfig}.
	 *
	 * @param mode     a {@link Mode} used to cope with referential ambiguity
	 * @param escaped  whether the output has to be escaped for XML
	 * @param xpath    an XPath expression which will be evaluated on the context node for normalization
	 * @param rewritesTraceLeaf - whether a <code>Q{http://wwu.de/scdh/selection-engine/node-tracing}text</code>
	 *                             segment has to be rewritten with <code>text()</code>
	 * @param removeEmptyNamespaces - whether a <code>Q{}</code> in a path expression (XPath segment) is to be removed.
	 */
	public RewriterConfig(
			Mode mode, boolean escaped, String xpath, boolean rewritesTraceLeaf, boolean removeEmptyNamespaces) {
		this.mode = mode;
		this.escaped = escaped;
		this.xpath = xpath;
		this.rewritesTraceLeaf = rewritesTraceLeaf;
		this.removeEmptyNamespaces = removeEmptyNamespaces;
		this.pointClassMap = Map.of();
	}

	/**
	 * Make a new {@link RewriterConfig}.
	 *
	 * @param mode     a {@link Mode} used to cope with referential ambiguity
	 * @param escaped  whether the output has to be escaped for XML
	 * @param xpath    an XPath expression which will be evaluated on the context node for normalization
	 * @param rewritesTraceLeaf - whether a <code>Q{http://wwu.de/scdh/selection-engine/node-tracing}text</code>
	 *                             segment has to be rewritten with <code>text()</code>
	 * @param removeEmptyNamespaces - whether a <code>Q{}</code> in a path expression (XPath segment) is to be removed.
	 * @param pointClassMap - a mapping of point classes for requesting replacement of point classes (key) in the output
	 *                      by different point classes (value). See static methods of {@link RewriterConfig} for
	 *                      conveniently getting such mappings.
	 */
	public RewriterConfig(
			Mode mode,
			boolean escaped,
			String xpath,
			boolean rewritesTraceLeaf,
			boolean removeEmptyNamespaces,
			Map<Class<? extends Point>, Class<? extends Point>> pointClassMap) {
		this.mode = mode;
		this.escaped = escaped;
		this.xpath = xpath;
		this.rewritesTraceLeaf = rewritesTraceLeaf;
		this.removeEmptyNamespaces = removeEmptyNamespaces;
		this.pointClassMap = pointClassMap;
	}

	/**
	 * Clone the {@link RewriterConfig}, but set a new {@link Mode}.
	 */
	public static RewriterConfig withMode(RewriterConfig config, Mode mode) {
		return new RewriterConfig(
				mode,
				config.escaped,
				config.xpath,
				config.rewritesTraceLeaf,
				config.removeEmptyNamespaces,
				config.pointClassMap);
	}

	public Mode getMode() {
		return mode;
	}

	public boolean getEscaped() {
		return escaped;
	}

	public String getXPath() {
		return xpath;
	}

	public boolean rewritesTraceLeaf() {
		return rewritesTraceLeaf;
	}

	public boolean removeEmptyNamespaces() {
		return removeEmptyNamespaces;
	}

	/**
	 * Gets the mapping of given point types to requested point types in the output.
	 * @return Map<Class<? extends Point>, Class<? extends Point>>
	 */
	public Map<Class<? extends Point>, Class<? extends Point>> getPointClassMap() {
		return pointClassMap;
	}

	/**
	 * Returns mapping of point classes suitable for forwarding XML to plain text selectors.
	 * @return a mapping of point classes.
	 */
	public static Map<Class<? extends Point>, Class<? extends Point>> forwardDOMToTextPointClassMap() {
		return Map.of(XPathRefinedByRFC5147CharScheme.class, RFC5147CharScheme.class);
	}

	/**
	 * Returns mapping of point classes suitable for forwarding XML to plain text selectors.
	 * @return a mapping of point classes.
	 */
	public static Map<Class<? extends Point>, Class<? extends Point>> backwardDOMToTextPointClassMap() {
		return Map.of(RFC5147CharScheme.class, XPathRefinedByRFC5147CharScheme.class);
	}

	/**
	 * Returns a mapping of point classes for an XSLT output method and a pointer rewriting direction.
	 *
	 * @param method - a valid method string as defined in {@link javax.xml.transform.OutputKeys#METHOD}.
	 * @param direction - either "forward" or "backward"
	 * @return a mapping of point classes
	 * @see - The output method strings defined by  <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.xml/javax/xml/transform/OutputKeys.html#METHOD">Saxon API</a>}
	 */
	public static Map<Class<? extends Point>, Class<? extends Point>> getPointClassMapForXslt(
			String method, Rewriter.Direction direction) {
		if (method == null || direction == null) {
			return Map.of();
		}
		if (method.equals("text")) {
			if (direction.equals(Rewriter.Direction.FORWARD)) {
				return forwardDOMToTextPointClassMap();
			} else {
				return backwardDOMToTextPointClassMap();
			}
		} else {
			return Map.of();
		}
	}
}
