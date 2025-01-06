package de.wwu.scdh.annotation.selection.wadm;

import java.util.Optional;
import java.util.function.Consumer;

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

import de.wwu.scdh.annotation.selection.*;
import de.wwu.scdh.annotation.selection.XPathRefinedByRFC5147CharScheme;

/**
 * This class can be used to normalize the model of all WADM
 * XPathSelectors, that are refined by RFC5147 conforming
 * FragmentSelectors. An {@link XPathNormalizer} does the task of
 * normalization.<P>
 *
 * This class implements the {@link Consumer} interface and can thus
 * be used in a functional style like <code>forEach(new
 * NormalizeAnnoation(...))</code> on some resource iterator. The
 * normalization will by side effect be written to the {@link Model}
 * which was passed into the constructor.
 */
public class NormalizeXPathSelectorRefinedByRFC5147CharScheme<S extends de.wwu.scdh.annotation.selection.Resource<?>> implements Consumer<Resource> {

    private static final Logger LOG = LoggerFactory.getLogger(NormalizeXPathSelectorRefinedByRFC5147CharScheme.class);

    public static final String RFC5147 = "http://tools.ietf.org/rfc/rfc5147";


    protected DOMResource dom;
    protected Model model;
    protected final RewriterFactory rewriterFactory;
    protected Rewriter<DOMResource, XPathRefinedByRFC5147CharScheme, XPathRefinedByRFC5147CharScheme> rewriter = null;
    protected final Processor processor;
    protected final RewriterConfig normalizerConfig;

    protected Optional<Exception> error = null;

    public NormalizeXPathSelectorRefinedByRFC5147CharScheme(Processor processor, RewriterFactory rewriterFactory, Model model, S dom, RewriterConfig normalizerConfig) {
	this.model = model;
	this.rewriterFactory = rewriterFactory;
	try {
	    this.dom = (DOMResource) dom;
	} catch (Exception e) {
	    LOG.error("failed to cast resource to DOM resource");
	}
	LOG.info("dom present");
	this.processor = processor;
	this.normalizerConfig = normalizerConfig;
	try {
	    this.rewriter = rewriterFactory.getRewriter(XPathRefinedByRFC5147CharScheme.class, XPathRefinedByRFC5147CharScheme.class, normalizerConfig);
	    LOG.info("rewriting an oa:XPathSelector which is refined by RFC5147 character scheme with rewriter {}", rewriter.getClass().getCanonicalName());
	} catch (ConfigurationException e) {
	    LOG.error(e.getMessage());
	    error = Optional.of(e);
	}
    }

    /**
     * This is the method of the function interface {@link Consumer}
     * and actually does the normalization without throwing errors.
     */
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
     * Do the normalization and throw exceptions on errors.
     */
    public void acceptThrows(Resource selector) throws ModelException, NumberFormatException, SelectorException {
	LOG.debug("normalizing XPathSelector '{}'", selector);

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
	XPathRefinedByRFC5147CharScheme point = new XPathRefinedByRFC5147CharScheme(xpath, startPos);
	for (XPathRefinedByRFC5147CharScheme p : rewriter.rewrite(dom, point, normalizerConfig)) {
	    if (XPathRefinedByRFC5147CharScheme.class.isAssignableFrom(p.getClass())) {
		XPathRefinedByRFC5147CharScheme normalized = (XPathRefinedByRFC5147CharScheme) p;
		LOG.info("normalized to {};{}", normalized.getXPath(), normalized.getChar());
		
		// 4. write the normalized values back to the model
		model.remove(xpathStatement);
		Statement xpathStmt = model.createLiteralStatement(selector, RDF.value, normalized.getXPath());
		model.add(xpathStmt);
		model.remove(refinementValueStatement);
		Statement charStatement = model.createLiteralStatement(refinement, RDF.value,
								       "char=" + String.valueOf(normalized.getChar()));
		model.add(charStatement);
	    }
	}

    }
}
