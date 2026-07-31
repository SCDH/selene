package de.wwu.scdh.annotation.selection.wadm;

import de.wwu.scdh.annotation.selection.*;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.impl.OntModelImpl;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link NormalizeAnnotation} can be used to normalize
 * the WADM annotations targeting a given resource in a graph.<P>
 *
 * This class implements the {@link Consumer} interface and can thus
 * be used in a functional style like <code>forEach(new
 * NormalizeAnnoation(...))</code> on some resource iterator. The
 * normalization will by side effect be written to the {@link Model}
 * which was passed into the constructor.
 *
 * USAGE: Use the static methods <code>normalize</code> to do the
 * normalization.
 */
public class NormalizeAnnotation implements Consumer<Resource> {

	private static final Logger LOG = LoggerFactory.getLogger(NormalizeAnnotation.class);

	protected final de.wwu.scdh.annotation.selection.Resource<?> resource;
	protected final URI iri;
	protected Model model;
	protected final RewriterFactory rewriterFactory;
	protected final RewriterConfig normalizerConfig;

	protected Optional<Exception> error = null;

	public NormalizeAnnotation(
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
	public void accept(Resource annotation) {
		LOG.debug("normalizing annotation '{}'", annotation.getURI());
		annotation
				.listProperties(OA.hasTarget)
				.mapWith(stmt -> stmt.getResource())
				.forEach(new NormalizeTarget(resource, iri, rewriterFactory, normalizerConfig, model));
	}

	/**
	 * Returns the {@link Model}. The model is normalized, when
	 * <code>accept</code> was called.
	 */
	public Model getModel() {
		return model;
	}

	/**
	 * Normalize all annotations in the provided {@link Model}.
	 *
	 * @param resource - the resource the rewriting has to done with
	 * @param iri - the IRI of sources (<code>oa:hasSource</code>) the rewriting has to done on
	 * @param rewriterFactory - a factory that returns a rewriter for a point
	 * @param normalizerConfig - a configuration
	 * @param model - the RDF model (graph) containing the annotations
	 * @return the normalized {@link Model}
	 */
	public static Model normalize(
			de.wwu.scdh.annotation.selection.Resource<?> resource,
			URI iri,
			RewriterFactory rewriterFactory,
			RewriterConfig normalizerConfig,
			Model model) {
		NormalizeAnnotation normalizeAnnotation =
				new NormalizeAnnotation(resource, iri, rewriterFactory, normalizerConfig, model);
		ResIterator annotations = model.listResourcesWithProperty(RDF.type, OA.Annotation);
		annotations.forEach(normalizeAnnotation);
		return normalizeAnnotation.getModel();
	}

	/**
	 * Normalize all annotations in a {@link Model} given by a URI
	 * as {@link String} which may reference a local file (file URI)
	 * or an online resource.
	 *
	 * @param resource - the resource the rewriting has to done with
	 * @param iri - the IRI of sources (<code>oa:hasSource</code>) the rewriting has to done on
	 * @param rewriterFactory - a factory that returns a rewriter for a point
	 * @param normalizerConfig - a configuration
	 * @param graph  the URI where to read the RDF from
	 * @param lang the serialization language of the graph at the URI
	 * @return the normalized {@link Model}
	 */
	public static Model normalize(
			de.wwu.scdh.annotation.selection.Resource<?> resource,
			URI iri,
			RewriterFactory rewriterFactory,
			RewriterConfig normalizerConfig,
			String graph,
			Optional<String> lang) {
		Model model;
		if (lang.isEmpty()) {
			model = RDFDataMgr.loadModel(graph);
		} else {
			model = RDFDataMgr.loadModel(graph, RDFLanguages.nameToLang(lang.get()));
		}
		return normalize(resource, iri, rewriterFactory, normalizerConfig, model);
	}

	/**
	 * Normalize all annotations in a {@link Model} which is read from
	 * an {@link InputStream}.
	 *
	 * @param resource - the resource the rewriting has to done with
	 * @param iri - the IRI of sources (<code>oa:hasSource</code>) the rewriting has to done on
	 * @param rewriterFactory - a factory that returns a rewriter for a point
	 * @param normalizerConfig - a configuration
	 * @param input - the {@link InputStream}
	 * @param modelBase - a base URI of the model, given as {@link String}
	 * @param lang - optionally the serialization language of stream data; if not provided, NTriples are assumed
	 * @return the normalized {@link Model}
	 */
	public static Model normalize(
			de.wwu.scdh.annotation.selection.Resource<?> resource,
			URI iri,
			RewriterFactory rewriterFactory,
			RewriterConfig normalizerConfig,
			InputStream input,
			Optional<String> lang,
			Optional<String> modelBase) {
		Model model = new OntModelImpl(OntModelSpec.OWL_DL_MEM);
		Lang langHint;
		if (lang.isEmpty()) {

			langHint = RDFLanguages.nameToLang(lang.get());
		} else {
			langHint = RDFLanguages.NTRIPLES;
		}
		if (modelBase.isEmpty()) {
			RDFDataMgr.read(model, input, langHint);
		} else {
			RDFDataMgr.read(model, input, modelBase.get(), langHint);
		}
		return normalize(resource, iri, rewriterFactory, normalizerConfig, model);
	}
}
