package de.wwu.scdh.annotation.selection.wadm;

import java.util.Optional;
import java.util.function.Consumer;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;

import net.sf.saxon.s9api.Processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.wwu.scdh.annotation.selection.DOMResource;
import de.wwu.scdh.annotation.selection.XPathNormalizer;


/**
 * {@link NormalizeAnnotation} can be used to normalize the model of
 * all WADM annotations given in a graph.
 *
 * USAGE: Use the static methods <code>normalize</code> to do the
 * normalization.
 */
public class NormalizeAnnotation implements Consumer<Resource> {

    private static final Logger LOG = LoggerFactory.getLogger(NormalizeAnnotation.class);

    protected Optional<DOMResource> dom;
    protected Model model;
    protected final XPathNormalizer normalizer;
    protected final Processor processor;

    protected Optional<Exception> error = null;

    public NormalizeAnnotation(Processor processor, XPathNormalizer normalizer, Model model, Optional<DOMResource> dom) {
	this.model = model;
	this.normalizer = normalizer;
	this.dom = dom;
	this.processor = processor;
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
	    .forEach(new NormalizeTarget(processor, normalizer, model, dom));
    }

    /**
     * Returns the normalized {@link Model}.
     */
    public Model getModel() {
	return model;
    }

    /**
     * Normalize all annotations in the provided {@link Model}.
     *
     * @param processor  a Saxon {@link Processor} for parsing and processing the target source
     * @param normalizer  the normalizer
     * @param dom  a optional {@link DOMResource} which instead the one given by the target's hasSource property
     * @parma model  the RDF model (graph) containing the annotations
     * @return the normalized {@link Model}
     */
    public static Model normalize(Processor processor, XPathNormalizer normalizer, Model model, Optional<DOMResource> dom) {
	NormalizeAnnotation annotations = new NormalizeAnnotation(processor, normalizer, model, dom);
	ResIterator annots = model.listResourcesWithProperty(RDF.type, OA.Annotation);
	annots.forEach(annotations);
	return annotations.getModel();
    }

    /**
     * Normalize all annotations in the provided {@link Model}.
     *
     * @param processor  a Saxon {@link Processor} for parsing and processing the target source
     * @param normalizer  the normalizer
     * @param dom  a optional {@link DOMResource} which instead the one given by the target's hasSource property
     * @param uri  the URI where to read the RDF from
     * @param lang the serialization language of the graph at the URI
     * @return the normalized {@link Model}
     */
    public static Model normalize(Processor processor, XPathNormalizer normalizer, String uri, String lang, Optional<DOMResource> dom) {
	Model model = RDFDataMgr.loadModel(uri, RDFLanguages.nameToLang(lang));
	NormalizeAnnotation annotations = new NormalizeAnnotation(processor, normalizer, model, dom);
	ResIterator annots = model.listResourcesWithProperty(RDF.type, OA.Annotation);
	annots.forEach(annotations);
	return annotations.getModel();
    }

}
