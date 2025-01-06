package de.wwu.scdh.annotation.selection.wadm;

import java.util.Optional;
import java.util.function.Consumer;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.RDFNode;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.wwu.scdh.annotation.selection.*;

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

    protected final Optional<de.wwu.scdh.annotation.selection.Resource<?>> dom;
    protected Model model;
	protected final RewriterFactory rewriterFactory;
    protected final Processor processor;
    protected final RewriterConfig normalizerConfig;

    protected Optional<Exception> error = Optional.empty();

    public NormalizeTarget(Processor processor, RewriterFactory rewriterFactory, RewriterConfig normalizerConfig, Model model, Optional<de.wwu.scdh.annotation.selection.Resource<?>> dom) {
	this.model = model;
	this.rewriterFactory = rewriterFactory;
	this.dom = dom;
	this.processor = processor;
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
	    LOG.error("bad source URI '{}'", target.getProperty(OA.hasSource).getObject().toString());
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
    public void acceptThrows(Resource target) throws ModelException, URISyntaxException, IOException, SaxonApiException {
	LOG.debug("normalizing target '{}'", target.toString());
	// a Source was passed into the constructor or we get a
	// Source from the target; if neither is the case, it
	// would lack of information in the model
	de.wwu.scdh.annotation.selection.Resource<?> source;
	// Note: We are using the pattern
	// ..listProperties(...).toSet().isEmpty() because toSet()
	// *exhaustively* consumes the iterator returned by
	// listStatements; using .hasNext() would not close the
	// iterator and thus cause a memory leak.
	if (dom.isEmpty() && target.listProperties(OA.hasSource).toSet().isEmpty()) {
	    // bad
	    throw new ModelException("annotation target is missing the OA:hasSource property");
	}
	if (dom.isEmpty()) {
	    // get target source from annotation
	    String targetSource = target.getProperty(OA.hasSource).getObject().toString();
	    LOG.debug("getting and parsing source '{}'", targetSource);
	    URI targetUri = new URI(targetSource);
	    try {
		source = DOMResource.fromXML(targetUri, null, processor);
	    } catch (Exception e) {
		source = DOMResource.fromHTML(targetUri, null, processor);
	    }
	} else {
	    source = dom.get();
	}

	// normalize RangeSelectors
	model
	    .listStatements(target, OA.hasSelector, (RDFNode) null)
	    .mapWith((stmt) -> stmt.getResource())
	    .filterKeep(selector -> {
		    return !model.listStatements(selector, RDF.type, OA.RangeSelector).toSet().isEmpty();
		})
	    // exceptions are not propagated from selector normalizations
	    .forEach(new NormalizeRangeSelector(processor, rewriterFactory, normalizerConfig, model, source));

	// TODO: normalize other selectors
    }
}
