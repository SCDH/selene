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

import de.wwu.scdh.annotation.selection.DOMResource;
import de.wwu.scdh.annotation.selection.XPathNormalizer;


public class NormalizeTarget implements Consumer<Resource> {

    private static final Logger LOG = LoggerFactory.getLogger(NormalizeTarget.class);

    protected final Optional<DOMResource> dom;
    protected Model model;
    protected final XPathNormalizer normalizer;
    protected final Processor processor;

    protected Optional<Exception> error = null;

    public NormalizeTarget(Processor processor, XPathNormalizer normalizer, Model model, Optional<DOMResource> dom) {
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
    public void accept(Resource target) {
	LOG.debug("normalizing target '{}'", target.toString());
	String targetSource = null;
	// either we got a Source passed or we get a Source from the Target
	DOMResource source;
	try {
	    if (dom.isEmpty() && target.listProperties(OA.hasSource).hasNext()) {
		// get target source from annotation
		targetSource = target.getProperty(OA.hasSource).getObject().toString();
		LOG.debug("getting and parsing source '{}'", targetSource);
		URI targetUri = new URI(targetSource);
		source = DOMResource.fromHTML(targetUri, null, processor);
	    } else {
		source = dom.get();
	    }

	    // normalize RangeSelectors
	    model
		.listStatements(target, OA.hasSelector, (RDFNode) null)
		.mapWith((stmt) -> stmt.getResource())
		.filterKeep(selector -> {
			return model.listStatements(selector, RDF.type, OA.RangeSelector).hasNext();
		    })
		.forEach(new NormalizeRangeSelector(processor, normalizer, model, source));

	} catch (URISyntaxException e) {
	    LOG.error("bad source URI '{}'", targetSource);
	    error = Optional.of(e);
	} catch (IOException e) {
	    LOG.error("failed to load URI: {}", e.getMessage());
	    error = Optional.of(e);
	} catch (SaxonApiException e) {
	    LOG.error(e.getMessage());
	    error = Optional.of(e);
	}

    }
}
