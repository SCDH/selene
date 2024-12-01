package de.wwu.scdh.annotation.selection.wadm;

import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import net.sf.saxon.s9api.Processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.wwu.scdh.annotation.selection.DOMResource;
import de.wwu.scdh.annotation.selection.XPathNormalizer;
import de.wwu.scdh.annotation.selection.XPathNormalizer.Mode;
import de.wwu.scdh.annotation.selection.SelectorException;

public class NormalizeXPathSelectorRefinedByRFC5147CharScheme implements Consumer<Resource> {

    private static final Logger LOG = LoggerFactory.getLogger(NormalizeXPathSelectorRefinedByRFC5147CharScheme.class);

    public static final String RFC5147 = "http://tools.ietf.org/rfc/rfc5147";


    protected final DOMResource dom;
    protected Model model;
    protected final XPathNormalizer normalizer;
    protected final Processor processor;
    protected final Mode normalizerMode;

    protected Optional<Exception> error = null;

    public NormalizeXPathSelectorRefinedByRFC5147CharScheme(Processor processor, XPathNormalizer normalizer, Model model, DOMResource dom, XPathNormalizer.Mode mode) {
	this.model = model;
	this.normalizer = normalizer;
	this.dom = dom;
	this.processor = processor;
	this.normalizerMode = mode;
    }

    public void accept(Resource selector) {
	try {
	    acceptThrows(selector);
	} catch (ModelException e) {
	    error = Optional.of(e);
	} catch (NumberFormatException e) {
	    error = Optional.of(e);
	} catch (SelectorException e) {
	    error = Optional.of(e);
	}
    }

    /**
     * This is the method of the function interface {@link Consumer}
     * and actually does the normalization without throwing errors.
     *
     */
    public void acceptThrows(Resource selector) throws ModelException, NumberFormatException, SelectorException {
	LOG.info("normalizing XPathSelector '{}'", selector);

	// 1. get XPath component
	String xpath;
	Statement xpathStatement;
	StmtIterator xpathStatements = model.listStatements(selector, RDF.value, (RDFNode) null);
	if (xpathStatements.hasNext()) {
	    xpathStatement = xpathStatements.next();
	    xpath = xpathStatement.getLiteral().toString();
	    xpathStatements.close();
	} else {
	    xpathStatements.close();
	    LOG.error("no value for oa:XPathSelector {}", selector);
	    throw new ModelException("no value for oa:XPathSelector");
	}

	// 2. get RFC5147 component
	Resource refinement;
	Statement refinementValueStatement;
	String refinementValue;
	ExtendedIterator<Resource> refinementIter = model
	    .listStatements(selector, OA.refinedBy, (RDFNode) null)
	    .mapWith(stmt -> stmt.getResource())
	    .filterKeep(ref -> !(model.listStatements(ref, RDF.type, OA.FragmentSelector).toSet().isEmpty()
				 && model.listStatements(ref, DCTerms.conformsTo, RFC5147).toSet().isEmpty()));
	if (refinementIter.hasNext()) {
	    refinement = refinementIter.next();
	    refinementIter.close();
	    StmtIterator values = model.listStatements(refinement, RDF.value, (Literal) null);
	    if (values.hasNext()) {
		refinementValueStatement = values.next();
		refinementValue = refinementValueStatement.getLiteral().toString();
		values.close();
	    } else {
		LOG.error("no value for RFC5147-conforming FragmentSelector '{}'", selector);
		throw new ModelException("no value for RFC5147-conforming FragmentSelector");
	    }
	} else {
	    LOG.error("no value for RFC5147-conforming FragmentSelector '{}'", selector);
	    throw new ModelException("no value for RFC5147-conforming FragmentSelector");
	}

	// only the character scheme is supported
	int startPos;
	if (refinementValue.startsWith("char=", 0)) {
	    startPos = Integer.parseInt(refinementValue.substring(5));
	} else {
	    LOG.error("value of RFC5147-conforming oa:FragmentSelector does not use the character scheme: {}", refinement);
	    throw new ModelException("value of RFC5147-conforming oa:FragmentSelector does not use the character scheme");
	}

	// 3. normalize the components
	LOG.info("normalizing refined XPath {};{}", xpath, startPos);
	Pair<String, Integer> normalized = normalizer.normalizeXPathRefinedByCharScheme(dom, xpath, startPos, normalizerMode);
	LOG.info("normalized to {};{}", normalized.getLeft(), normalized.getRight());
	LOG.info("normalized to {};{}", normalized.getLeft(), normalized.getRight());

	// 4. write the normalized values back to the model
	model.remove(xpathStatement);
	Statement xpathStmt = model.createLiteralStatement(selector, RDF.value, normalized.getLeft());
	model.add(xpathStmt);
	model.remove(refinementValueStatement);
	Statement charStatement = model.createLiteralStatement(refinement, RDF.value,
							       "char=" + String.valueOf(normalized.getRight()));
	model.add(charStatement);
    }
}
