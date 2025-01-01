package de.wwu.scdh.annotation.selection.wadm;

import java.util.Optional;
import java.util.function.Consumer;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.RDFNode;

import net.sf.saxon.s9api.Processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.wwu.scdh.annotation.selection.DOMResource;
import de.wwu.scdh.annotation.selection.XPathNormalizer;
import de.wwu.scdh.annotation.selection.RewriterConfig;
import de.wwu.scdh.annotation.selection.Mode;


public class NormalizeRangeSelector implements Consumer<Resource> {

    private static final Logger LOG = LoggerFactory.getLogger(NormalizeRangeSelector.class);

    public static final Mode START_XPATH_SELECTOR_MODE = Mode.LAST_OF_DEEPEST_NODES;
    public static final Mode END_XPATH_SELECTOR_MODE = Mode.FIRST_OF_DEEPEST_NODES;

    protected final DOMResource dom;
    protected Model model;
    protected final XPathNormalizer normalizer;
    protected final Processor processor;
    protected final RewriterConfig normalizerConfig;

    protected Optional<Exception> error = null;

    public NormalizeRangeSelector(Processor processor, XPathNormalizer normalizer, RewriterConfig normalizerConfig, Model model, DOMResource dom) {
	this.model = model;
	this.normalizer = normalizer;
	this.dom = dom;
	this.processor = processor;
	this.normalizerConfig = normalizerConfig;
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
	    .filterKeep(sel -> // keep selectors refinedBy a oa:FragmentSelector conforming to RFC5147
			! model.listStatements(sel, OA.refinedBy, (RDFNode) null)
			.mapWith(stmt -> stmt.getSubject())
			.filterKeep(refinement -> model.listStatements(refinement, RDF.type, OA.FragmentSelector).toSet().isEmpty())
			.filterKeep(refinement -> model.listStatements(refinement, DCTerms.conformsTo, NormalizeXPathSelectorRefinedByRFC5147CharScheme.RFC5147).toSet().isEmpty())
			// TODO: filter char scheme
			.toSet()
			.isEmpty())
	    .forEach(new NormalizeXPathSelectorRefinedByRFC5147CharScheme(processor, normalizer, model, dom, RewriterConfig.withMode(normalizerConfig, START_XPATH_SELECTOR_MODE)));

	selector
	    .listProperties(OA.hasEndSelector)
	    .mapWith(stmt -> stmt.getResource())
	    .filterKeep(sel -> !model.listStatements(sel, RDF.type, OA.XPathSelector).toSet().isEmpty())
	    .filterKeep(sel -> // keep selectors refinedBy a oa:FragmentSelector conforming to RFC5147
			! model.listStatements(sel, OA.refinedBy, (RDFNode) null)
			.mapWith(stmt -> stmt.getSubject())
			.filterKeep(refinement -> model.listStatements(refinement, RDF.type, OA.FragmentSelector).toSet().isEmpty())
			.filterKeep(refinement -> model.listStatements(refinement, DCTerms.conformsTo, NormalizeXPathSelectorRefinedByRFC5147CharScheme.RFC5147).toSet().isEmpty())
			// TODO: filter char scheme
			.toSet()
			.isEmpty())
	    .forEach(new NormalizeXPathSelectorRefinedByRFC5147CharScheme(processor, normalizer, model, dom, RewriterConfig.withMode(normalizerConfig, END_XPATH_SELECTOR_MODE)));

    }
}
