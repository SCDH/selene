package de.wwu.scdh.annotation.selection.wadm;

import de.wwu.scdh.annotation.selection.*;
import de.wwu.scdh.annotation.selection.resource.DOMResource;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Consumer;
import net.sf.saxon.s9api.SaxonApiException;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Normalize an <code>oa:Target</code>.<P>
 *
 * Step 1: get the source either from <code>oa:hasTarget</code> or
 * take the {@link DOMResource} passed into the constructor.<P>
 *
 * Step 2: call normalizer on every selector.<P>
 *
 * This class implements Java's functional-style {@link Consumer}
 * interface and can be used in the <code>..forEach(new
 * NormalizeTarget(...))</code> functional pattern.<P>
 */
public class NormalizeTarget implements Consumer<Resource> {

	private static final Logger LOG = LoggerFactory.getLogger(NormalizeTarget.class);

	protected final de.wwu.scdh.annotation.selection.Resource<?> resource;
	protected final URI iri;
	protected final Optional<URI> rewriteIri;

	protected Model model;
	protected final RewriterFactory rewriterFactory;
	protected final RewriterConfig normalizerConfig;

	protected Optional<Exception> error = Optional.empty();

	public NormalizeTarget(
			de.wwu.scdh.annotation.selection.Resource<?> resource,
			URI iri,
			Optional<URI> rewriteIri,
			RewriterFactory rewriterFactory,
			RewriterConfig normalizerConfig,
			Model model) {
		this.resource = resource;
		this.iri = iri;
		this.rewriteIri = rewriteIri;
		this.model = model;
		this.rewriterFactory = rewriterFactory;
		this.normalizerConfig = normalizerConfig;
	}

	public Optional<Exception> getError() {
		return error;
	}

	public Model getModel() {
		return model;
	}

	/**
	 * This is the method of the function interface {@link Consumer}
	 * and actually does the normalization without throwing
	 * exceptions.
	 */
	public void accept(Resource target) {
		try {
			acceptThrows(target);
		} catch (URISyntaxException e) {
			LOG.error(
					"bad source URI '{}'",
					target.getProperty(OA.hasSource).getObject().toString());
			error = Optional.of(e);
		} catch (IOException e) {
			LOG.error("failed to load URI: {}", e.getMessage());
			error = Optional.of(e);
		} catch (SaxonApiException e) {
			LOG.error(e.getMessage());
			error = Optional.of(e);
		} catch (ModelException e) {
			LOG.error(e.getMessage());
			error = Optional.of(e);
		}
	}

	/**
	 * Do the normalization tasks.
	 */
	public void acceptThrows(Resource target)
			throws ModelException, URISyntaxException, IOException, SaxonApiException {
		LOG.debug("normalizing target '{}'", target.toString());
		RDFNode sourceNode = target.getProperty(OA.hasSource).getObject();
		String targetSource;
		if (sourceNode.isLiteral()) {
			targetSource = sourceNode.asLiteral().getString();
		} else if (sourceNode.isURIResource()) {
			targetSource = sourceNode.asResource().getURI();
		} else {
			targetSource = sourceNode.toString();
		}
		// guard: if the oa:hasSource does not match the IRI, we are done
		if (!targetSource.equals(iri.toString())) {
			LOG.debug("target source {} does not match IRI {}", targetSource, iri);
			return;
		}
		LOG.debug("found annotation on {} to be rewritten", iri);

		// rewrite rewriteIri back to the model
		if (rewriteIri.isPresent() && !iri.equals(rewriteIri.get())) {
			LOG.debug("rewriting oa:hasSource from {} to {}", iri, rewriteIri.get());
			model.remove(target, OA.hasSource, sourceNode);
			Statement rewriteStatement = model.createStatement(
					target, OA.hasSource, model.createResource(rewriteIri.get().toString()));
			model.add(rewriteStatement);
		}

		// normalize RangeSelectors
		model.listStatements(target, OA.hasSelector, (RDFNode) null)
				.mapWith((stmt) -> stmt.getResource())
				.filterKeep(selector -> {
					return !model.listStatements(selector, RDF.type, OA.RangeSelector)
							.toSet()
							.isEmpty();
				})
				// exceptions are not propagated from selector normalizations
				.forEach(new NormalizeRangeSelector(resource, iri, rewriterFactory, normalizerConfig, model));

		// TODO: normalize other selectors
	}
}
