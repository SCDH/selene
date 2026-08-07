package de.wwu.scdh.annotation.selection.wadm;

import de.wwu.scdh.annotation.selection.*;
import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NormalizeRangeSelector implements Consumer<Resource> {

	private static final Logger LOG = LoggerFactory.getLogger(NormalizeRangeSelector.class);

	public static final Mode START_XPATH_SELECTOR_MODE = Mode.LAST_OF_DEEPEST_NODES;
	public static final Mode END_XPATH_SELECTOR_MODE = Mode.FIRST_OF_DEEPEST_NODES;

	protected final de.wwu.scdh.annotation.selection.Resource<?> resource;
	protected final URI iri;
	protected Model model;
	protected final RewriterFactory rewriterFactory;
	protected final RewriterConfig normalizerConfig;

	protected Optional<Exception> error = null;

	public NormalizeRangeSelector(
			de.wwu.scdh.annotation.selection.Resource<?> resource,
			URI iri,
			RewriterFactory rewriterFactory,
			RewriterConfig normalizerConfig,
			Model model) {
		this.resource = resource;
		this.iri = iri;
		this.model = model;
		this.rewriterFactory = rewriterFactory;
		this.normalizerConfig = normalizerConfig;
	}

	/**
	 * This is the method of the function interface {@link Consumer}
	 * and actually does the normalization without throwing errors.
	 *
	 */
	public void accept(Resource selector) {
		LOG.debug("normalizing range selector '{}'", selector.toString());

		boolean done = false;

		ExtendedIterator<Resource> startSelectors = selector.listProperties(OA.hasStartSelector)
				.mapWith(Statement::getResource)
				.filterKeep(s -> NormalizeXPathSelectorRefinedByRFC5147CharScheme.filter(model, s));
		ExtendedIterator<Resource> endSelectors = selector.listProperties(OA.hasEndSelector)
				.mapWith(Statement::getResource)
				.filterKeep(s -> NormalizeXPathSelectorRefinedByRFC5147CharScheme.filter(model, s));
		done = startSelectors.hasNext() || endSelectors.hasNext();
		startSelectors.forEach(new NormalizeXPathSelectorRefinedByRFC5147CharScheme(
				resource,
				iri,
				rewriterFactory,
				model,
				RewriterConfig.withMode(normalizerConfig, START_XPATH_SELECTOR_MODE)));
		endSelectors.forEach(new NormalizeXPathSelectorRefinedByRFC5147CharScheme(
				resource,
				iri,
				rewriterFactory,
				model,
				RewriterConfig.withMode(normalizerConfig, END_XPATH_SELECTOR_MODE)));

		// important to stop: otherwise rewrite over and over again
		if (done) return;

		startSelectors = model.listStatements(selector, OA.hasStartSelector, (RDFNode) null)
				.mapWith(Statement::getResource)
				.filterKeep(s -> NormalizeRFC5147CharScheme.filter(model, s));
		endSelectors = model.listStatements(selector, OA.hasEndSelector, (RDFNode) null)
				.mapWith(Statement::getResource)
				.filterKeep(s -> NormalizeRFC5147CharScheme.filter(model, s));
		done = startSelectors.hasNext() || endSelectors.hasNext();
		startSelectors.forEach(new NormalizeRFC5147CharScheme(
				resource,
				iri,
				rewriterFactory,
				model,
				RewriterConfig.withMode(normalizerConfig, START_XPATH_SELECTOR_MODE)));
		endSelectors
				.filterKeep(s -> NormalizeRFC5147CharScheme.filter(model, s))
				.forEach(new NormalizeRFC5147CharScheme(
						resource,
						iri,
						rewriterFactory,
						model,
						RewriterConfig.withMode(normalizerConfig, END_XPATH_SELECTOR_MODE)));

		if (!done) {
			LOG.info("cannot map this oa:RangeSelector");
		}
	}
}
