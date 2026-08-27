package de.wwu.scdh.annotation.selection.wadm;

import com.apicatalog.jsonld.JsonLdOptions;
import de.wwu.scdh.annotation.selection.*;
import de.wwu.scdh.annotation.selection.utils.StaticDocumentLoader;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.*;
import org.apache.jena.riot.system.jsonld.TitaniumJsonLdOptions;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link NormalizeAnnotation} can be used to normalize or rewrite
 * the WADM annotations targeting a given resource in a graph.<P>
 *
 * This class implements the {@link Consumer} interface and can thus
 * be used in a functional style like <code>forEach(new
 * NormalizeAnnoation(...))</code> on some resource iterator. The
 * normalization will--by side effect--be written to the {@link Model}
 * which was passed into the constructor.<P/>
 *
 * USAGE: Use the static methods <code>normalize</code> to do the
 * normalization.
 */
public class NormalizeAnnotation implements Consumer<Resource> {

	private static final Logger LOG = LoggerFactory.getLogger(NormalizeAnnotation.class);

	protected final de.wwu.scdh.annotation.selection.Resource<?> resource;
	protected final URI iri;
	protected final Optional<URI> rewriteIri;
	protected Model model;
	protected final RewriterFactory rewriterFactory;
	protected final RewriterConfig normalizerConfig;

	protected Optional<Exception> error = null;

	public NormalizeAnnotation(
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
				.forEach(new NormalizeTarget(resource, iri, rewriteIri, rewriterFactory, normalizerConfig, model));
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
				new NormalizeAnnotation(resource, iri, Optional.empty(), rewriterFactory, normalizerConfig, model);
		ResIterator annotations = model.listResourcesWithProperty(RDF.type, OA.Annotation);
		annotations.forEach(normalizeAnnotation);
		return normalizeAnnotation.getModel();
	}

	/**
	 * Rewrite all annotations in the provided {@link Model}. In contrast to
	 * {@link NormalizeAnnotation#normalize(de.wwu.scdh.annotation.selection.Resource, URI, RewriterFactory, RewriterConfig, Model)}
	 * this method also rewrites the <code>oa:hasSource</code> property and is thus suitable for transforming selectors
	 * between representations.
	 *
	 * @param resource - the resource the rewriting has to done with, should be a {@link MappedResource}
	 * @param iri - the IRI of sources (<code>oa:hasSource</code>) the rewriting has to done on
	 * @param rewriteIri - the new IRI of rewritten targets
	 * @param rewriterFactory - a factory that returns a rewriter for a point
	 * @param normalizerConfig - a configuration
	 * @param model - the RDF model (graph) containing the annotations
	 * @return the rewritten {@link Model}
	 */
	public static Model rewrite(
			de.wwu.scdh.annotation.selection.Resource<?> resource,
			URI iri,
			URI rewriteIri,
			RewriterFactory rewriterFactory,
			RewriterConfig normalizerConfig,
			Model model) {
		NormalizeAnnotation normalizeAnnotation = new NormalizeAnnotation(
				resource, iri, Optional.of(rewriteIri), rewriterFactory, normalizerConfig, model);
		ResIterator annotations = model.listResourcesWithProperty(RDF.type, OA.Annotation);
		annotations.forEach(normalizeAnnotation);
		return normalizeAnnotation.getModel();
	}

	/**
	 * Normalize all annotations in a {@link Model} given by a URI
	 * as {@link String} which may reference a local file (file URI)
	 * or an online resource.<p/>
	 *
	 * Note, that this method sets options to the RDF parser, e.g., the {@link StaticDocumentLoader} as JSON-LD
	 * document loader. If you want full control over RDF parsing, the use
	 * {@link NormalizeAnnotation#normalize(de.wwu.scdh.annotation.selection.Resource, URI, RewriterFactory, RewriterConfig, Model)}
	 * instead.
	 *
	 * @param resource - the resource the rewriting has to done with
	 * @param iri - the IRI of sources (<code>oa:hasSource</code>) the rewriting has to done on
	 * @param rewriterFactory - a factory that returns a rewriter for a point
	 * @param normalizerConfig - a configuration
	 * @param graph - the URI where to read the RDF from
	 * @param lang - the serialization language of the graph at the URI
	 * @return the normalized {@link Model}
	 */
	public static Model normalize(
			de.wwu.scdh.annotation.selection.Resource<?> resource,
			URI iri,
			RewriterFactory rewriterFactory,
			RewriterConfig normalizerConfig,
			String graph,
			Optional<String> lang) {
		RDFParserBuilder parserBuilder = RDFParser.source(graph);
		setParserOptions(parserBuilder);
		lang.ifPresent(l -> parserBuilder.lang(RDFLanguages.nameToLang(l)));
		Model model = parserBuilder.toModel();
		return normalize(resource, iri, rewriterFactory, normalizerConfig, model);
	}

	/**
	 * Rewrite all annotations in the provided {@link Model}. In contrast to
	 * {@link NormalizeAnnotation#normalize(de.wwu.scdh.annotation.selection.Resource, URI, RewriterFactory, RewriterConfig, String, Optional)}
	 * this method also rewrites the <code>oa:hasSource</code> property and is thus suitable for transforming selectors
	 * between representations.<p/>
	 *
	 * Note, that this method sets options to the RDF parser, e.g., the {@link StaticDocumentLoader} as JSON-LD
	 * document loader. If you want full control over RDF parsing, the use
	 * {@link NormalizeAnnotation#rewrite(de.wwu.scdh.annotation.selection.Resource, URI, URI, RewriterFactory, RewriterConfig, Model)}
	 * instead.
	 *
	 * @param resource - the resource the rewriting has to done with. Should be a {@link MappedResource}.
	 * @param iri - the IRI of sources (<code>oa:hasSource</code>) the rewriting has to done on
	 * @param rewriteIri - the new IRI of rewritten targets
	 * @param rewriterFactory - a factory that returns a rewriter for a point
	 * @param normalizerConfig - a configuration
	 * @param graph - the URI where to read the RDF from
	 * @param lang - the serialization language of the graph at the URI
	 * @return the rewritten {@link Model}
	 */
	public static Model rewrite(
			de.wwu.scdh.annotation.selection.Resource<?> resource,
			URI iri,
			URI rewriteIri,
			RewriterFactory rewriterFactory,
			RewriterConfig normalizerConfig,
			String graph,
			Optional<String> lang) {
		RDFParserBuilder parserBuilder = RDFParser.source(graph);
		setParserOptions(parserBuilder);
		lang.ifPresent(l -> parserBuilder.lang(RDFLanguages.nameToLang(l)));
		Model model = parserBuilder.toModel();
		return rewrite(resource, iri, rewriteIri, rewriterFactory, normalizerConfig, model);
	}

	/**
	 * Normalize all annotations in a {@link Model} which is read from
	 * an {@link InputStream}.<p/>
	 *
	 * Note, that this method sets options to the RDF parser, e.g., the {@link StaticDocumentLoader} as JSON-LD
	 * document loader. If you want full control over RDF parsing, the use
	 * {@link NormalizeAnnotation#normalize(de.wwu.scdh.annotation.selection.Resource, URI, RewriterFactory, RewriterConfig, Model)}
	 * instead.
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
		RDFParserBuilder parserBuilder = RDFParser.source(input);
		modelBase.ifPresent(parserBuilder::base);
		setParserOptions(parserBuilder);
		if (lang.isPresent()) {
			parserBuilder.lang(RDFLanguages.nameToLang(lang.get()));
		} else {
			parserBuilder.lang(Lang.NTRIPLES);
		}
		Model model = parserBuilder.toModel();
		try {
			input.close();
			LOG.warn("closed input stream");
		} catch (Exception ignored) {
			LOG.warn("failed to close input stream");
		}
		return normalize(resource, iri, rewriterFactory, normalizerConfig, model);
	}

	/**
	 * Rewrite all annotations in the provided {@link Model}. In contrast to
	 * {@link NormalizeAnnotation#normalize(de.wwu.scdh.annotation.selection.Resource, URI, RewriterFactory, RewriterConfig, InputStream, Optional, Optional)}
	 * this method also rewrites the <code>oa:hasSource</code> property and is thus suitable for transforming selectors
	 * between representations.<p/>
	 *
	 * Note, that this method sets options to the RDF parser, e.g., the {@link StaticDocumentLoader} as JSON-LD
	 * document loader. If you want full control over RDF parsing, the use
	 * {@link NormalizeAnnotation#rewrite(de.wwu.scdh.annotation.selection.Resource, URI, URI, RewriterFactory, RewriterConfig, Model)}
	 * instead.
	 * *
	 * @param resource - the resource the rewriting has to done with. Should be a {@link MappedResource}.
	 * @param iri - the IRI of sources (<code>oa:hasSource</code>) the rewriting has to done on
	 * @param rewriterFactory - a factory that returns a rewriter for a point
	 * @param normalizerConfig - a configuration
	 * @param input - the {@link InputStream}
	 * @param modelBase - a base URI of the model, given as {@link String}
	 * @param lang - optionally the serialization language of stream data; if not provided, NTriples are assumed
	 * @return the rewritten {@link Model}
	 */
	public static Model rewrite(
			de.wwu.scdh.annotation.selection.Resource<?> resource,
			URI iri,
			URI rewriteIri,
			RewriterFactory rewriterFactory,
			RewriterConfig normalizerConfig,
			InputStream input,
			Optional<String> lang,
			Optional<String> modelBase) {
		RDFParserBuilder parserBuilder = RDFParser.source(input);
		modelBase.ifPresent(parserBuilder::base);
		setParserOptions(parserBuilder);
		if (lang.isPresent()) {
			parserBuilder.lang(RDFLanguages.nameToLang(lang.get()));
		} else {
			parserBuilder.lang(Lang.NTRIPLES);
		}
		Model model = parserBuilder.toModel();
		return rewrite(resource, iri, rewriteIri, rewriterFactory, normalizerConfig, model);
	}

	/**
	 * Sets RDF parser options.
	 * @param parserBuilder - the Apache Jena {@link RDFParserBuilder}
	 */
	private static void setParserOptions(RDFParserBuilder parserBuilder) {
		JsonLdOptions options = new JsonLdOptions();
		options.setDocumentLoader(new StaticDocumentLoader());
		parserBuilder.set(TitaniumJsonLdOptions.JSONLD_OPTIONS, options);
	}
}
