package de.wwu.scdh.annotation.selection.wadm;

import java.util.Optional;
import java.util.function.Consumer;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.rdf.model.Resource;

import net.sf.saxon.s9api.Processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.wwu.scdh.annotation.selection.DOMResource;
import de.wwu.scdh.annotation.selection.XPathNormalizer;


public class NormalizeRangeSelector implements Consumer<Resource> {

    private static final Logger LOG = LoggerFactory.getLogger(NormalizeRangeSelector.class);

    protected final DOMResource dom;
    protected Model model;
    protected final XPathNormalizer normalizer;
    protected final Processor processor;

    protected Optional<Exception> error = null;

    public NormalizeRangeSelector(Processor processor, XPathNormalizer normalizer, Model model, DOMResource dom) {
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
    public void accept(Resource selector) {
	LOG.debug("normalizing range selector '{}'", selector.toString());

	selector
	    .listProperties(OA.hasStartSelector)
	    .mapWith(stmt -> stmt.getResource())
	    .filterKeep(sel -> !model.listStatements(sel, RDF.type, OA.XPathSelector).toSet().isEmpty())
	    .forEach(new NormalizeXPathSelector(processor, normalizer, model, dom, XPathNormalizer.Mode.LAST_OF_DEEPEST_NODES));

	selector
	    .listProperties(OA.hasEndSelector)
	    .mapWith(stmt -> stmt.getResource())
	    .filterKeep(sel -> !model.listStatements(sel, RDF.type, OA.XPathSelector).toSet().isEmpty())
	    .forEach(new NormalizeXPathSelector(processor, normalizer, model, dom, XPathNormalizer.Mode.FIRST_OF_DEEPEST_NODES));

    }
}
