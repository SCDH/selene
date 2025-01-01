package de.wwu.scdh.annotation.selection;


public class RewriterConfig {

    private Mode mode;

    private boolean escaped;

    public RewriterConfig(Mode mode, boolean escaped) {
	this.mode = mode;
	this.escaped = escaped;
    }

    public Mode getMode() {
	return mode;
    }

    public boolean getEscaped() {
	return escaped;
    }

    /**
     * Clone the RewriterConfig, but set a new Mode.
     */
    public static RewriterConfig withMode(RewriterConfig config, Mode mode) {
	return new RewriterConfig(mode, config.escaped);
    }
    
}
