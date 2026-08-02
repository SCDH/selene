package de.wwu.scdh.annotation.selection;

/**
 * A record for configuration parameters of a {@link Rewriter}.
 * The parameters are documented in the constructor.
 */
public class RewriterConfig {

	private final Mode mode;
	private final boolean escaped;
	private final String xpath;
	private final boolean rewritesTraceLeaf;

	/**
	 * Make a new {@link RewriterConfig}.
	 *
	 * @param mode     a {@link Mode} used to cope with referential ambiguity
	 * @param escaped  whether the output has to be escaped for XML
	 * @param xpath    an XPath expression which will be evaluated on the context node for normalization
	 * @param rewritesTraceLeaf - whether a <code>Q{http://wwu.de/scdh/selection-engine/node-tracing}text</code>
	 *                             segment has to be rewritten with <code>text()</code>
	 */
	public RewriterConfig(Mode mode, boolean escaped, String xpath, boolean rewritesTraceLeaf) {
		this.mode = mode;
		this.escaped = escaped;
		this.xpath = xpath;
		this.rewritesTraceLeaf = rewritesTraceLeaf;
	}

	/**
	 * Clone the {@link RewriterConfig}, but set a new {@link Mode}.
	 */
	public static RewriterConfig withMode(RewriterConfig config, Mode mode) {
		return new RewriterConfig(mode, config.escaped, config.xpath, config.rewritesTraceLeaf);
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

    public boolean isRewritesTraceLeaf() {
        return rewritesTraceLeaf;
    }
}
