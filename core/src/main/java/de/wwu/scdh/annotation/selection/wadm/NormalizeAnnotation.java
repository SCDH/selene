package de.wwu.scdh.annotation.selection.wadm;

import java.io.InputStream;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.Lang;
import org.apache.jena.ontology.impl.OntModelImpl;
import org.apache.jena.ontology.OntModelSpec;

import net.sf.saxon.s9api.Processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.wwu.scdh.annotation.selection.*;


/**
 * {@link NormalizeAnnotation} can be used to normalize the model of
 * all WADM annotations given in a graph.<P>
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

    protected Optional<de.wwu.scdh.annotation.selection.Resource<?>> dom;
    protected Model model;
    protected final RewriterFactory rewriterFactory;
    protected final Processor processor;
    protected final RewriterConfig normalizerConfig;

    protected Optional<Exception> error = null;

    public NormalizeAnnotation(Processor processor, RewriterFactory rewriterFactory, RewriterConfig normalizerConfig, Model model, Optional<de.wwu.scdh.annotation.selection.Resource<?>> dom) {
	this.model = model;
	this.rewriterFactory = rewriterFactory;
	this.dom = dom;
	this.processor = processor;
	this.normalizerConfig = normalizerConfig;
    }

    /**
     * This is the method of the function interface {@link Consumer}
     * and actually does the normalization without throwing errors.
     *
     */
    public void accept(Resource annotation) {
	LOG.debug("normalizing annotation '{}'", annotation.getURI());
	annotation.listProperties(OA.hasTarget)
	    .mapWith(stmt -> stmt.getResource())
	    .forEach(new NormalizeTarget(processor, rewriterFactory, normalizerConfig, model, dom));
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
     * @param processor  a Saxon {@link Processor} for parsing and processing the target source
     * @param normalizer  the normalizer
     * @parma model  the RDF model (graph) containing the annotations
     * @param dom  a optional {@link de.wwu.scdh.annotation.selection.Resource} which instead the one given by the target's hasSource property
     * @return the normalized {@link Model}
     */
    public static Model normalize(Processor processor, RewriterFactory rewriterFactory, RewriterConfig normalizerConfig, Model model, Optional<de.wwu.scdh.annotation.selection.Resource<?>> dom) {
	NormalizeAnnotation normalizeAnnotation = new NormalizeAnnotation(processor, rewriterFactory, normalizerConfig,  model, dom);
	ResIterator annots = model.listResourcesWithProperty(RDF.type, OA.Annotation);
	annots.forEach(normalizeAnnotation);
	return normalizeAnnotation.getModel();
    }

    /**
     * Normalize all annotations in a {@link Model} given by a URI
     * as {@link String} which may reference a local file (file URI)
     * or an online resource.
     *
     * @param processor  a Saxon {@link Processor} for parsing and processing the target source
     * @param normalizer  the normalizer
     * @param uri  the URI where to read the RDF from
     * @param lang the serialization language of the graph at the URI
     * @param dom  a optional {@link de.wwu.scdh.annotation.selection.Resource} which instead the one given by the target's hasSource property
     * @return the normalized {@link Model}
     */
    public static Model normalize(Processor processor, RewriterFactory rewriterFactory, RewriterConfig normalizerConfig, String uri, Optional<String> lang, Optional<de.wwu.scdh.annotation.selection.Resource<?>> dom) {
	Model model;
	if (lang.isEmpty()) {
	    model = RDFDataMgr.loadModel(uri);
	} else {
	    model = RDFDataMgr.loadModel(uri, RDFLanguages.nameToLang(lang.get()));
	}
	return normalize(processor, rewriterFactory, normalizerConfig, model, dom);
    }

    /**
     * Normalize all annotations in a {@link Model} which is read from
     * an {@link InputStream}.
     *
     * @param processor  a Saxon {@link Processor} for parsing and processing the target source
     * @param normalizer  the normalizer
     * @param input  the {@link InputStream}
     * @param modelBase  a base URI of the model, given as {@link String}
     * @param lang  optionally the serialization language of stream data; if not provided, NTriples are assumed
     * @param dom  a optional {@link de.wwu.scdh.annotation.selection.Resource} which instead the one given by the target's hasSource property
     * @return the normalized {@link Model}
     */
    public static Model normalize(Processor processor, RewriterFactory rewriterFactory, RewriterConfig normalizerConfig, InputStream input, Optional<String> lang, Optional<String> modelBase, Optional<de.wwu.scdh.annotation.selection.Resource<?>> dom) {
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
	return normalize(processor, rewriterFactory, normalizerConfig, model, dom);
    }

}
