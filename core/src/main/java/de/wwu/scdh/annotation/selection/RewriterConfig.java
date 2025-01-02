package de.wwu.scdh.annotation.selection;


/**
 * A record for configuration parameters of a {@link Rewriter}.
 * The parameters are documented in the constructor.
 */
public class RewriterConfig {

    private Mode mode;
    private boolean escaped;
    private String xpath;

    /**
     * Make a new {@link RewriterConfig}.
     *
     * @param mode     a {@link Mode} used to cope with referential ambiguity
     * @param escaped  whether or not the output has to be escaped for XML
     * @param xpath    an XPath expression which will be evaluated on the context node for normalization
     */
    public RewriterConfig(Mode mode, boolean escaped, String xpath) {
	this.mode = mode;
	this.escaped = escaped;
	this.xpath = xpath;
    }

    /**
     * Clone the {@link RewriterConfig}, but set a new {@link Mode}.
     */
    public static RewriterConfig withMode(RewriterConfig config, Mode mode) {
	return new RewriterConfig(mode, config.escaped, config.xpath);
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

}
