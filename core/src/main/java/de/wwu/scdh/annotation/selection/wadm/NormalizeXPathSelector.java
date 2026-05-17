package de.wwu.scdh.annotation.selection.wadm;

import de.wwu.scdh.annotation.selection.*;
import java.util.Optional;
import java.util.function.Consumer;
import net.sf.saxon.s9api.Processor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NormalizeXPathSelector implements Consumer<Resource> {

	private static final Logger LOG = LoggerFactory.getLogger(NormalizeXPathSelector.class);

	protected final de.wwu.scdh.annotation.selection.Resource<?> dom;
	protected Model model;
	protected final RewriterFactory rewriterFactory;
	protected final Processor processor;
	protected final Mode normalizerMode;

	protected Optional<Exception> error = null;

	public NormalizeXPathSelector(
			Processor processor,
			RewriterFactory rewriterFactory,
			Model model,
			de.wwu.scdh.annotation.selection.Resource<?> dom,
			Mode mode) {
		this.model = model;
		this.rewriterFactory = rewriterFactory;
		this.dom = dom;
		this.processor = processor;
		this.normalizerMode = mode;
	}

	/**
	 * This is the method of the function interface {@link Consumer}
	 * and actually does the normalization without throwing errors.
	 *
	 */
	public void accept(Resource selector) {
		LOG.info("normalizing XPathSelector '{}'", selector);
	}
}
