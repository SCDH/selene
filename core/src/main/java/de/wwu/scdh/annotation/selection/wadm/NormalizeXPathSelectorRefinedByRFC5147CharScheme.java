package de.wwu.scdh.annotation.selection.wadm;

import de.wwu.scdh.annotation.selection.*;
import de.wwu.scdh.annotation.selection.point.RFC5147CharScheme;
import de.wwu.scdh.annotation.selection.point.XPathRefinedByRFC5147CharScheme;
import de.wwu.scdh.annotation.selection.resource.DOMResource;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class can be used to normalize the model of all WADM
 * XPathSelectors, that are refined by RFC5147 conforming
 * FragmentSelectors. An {@link Rewriter} does the task of
 * normalization.<P/>
 *
 * This class implements the {@link Consumer} interface and can thus
 * be used in a functional style like <code>forEach(new
 * NormalizeAnnoation(...))</code> on some resource iterator. The
 * normalization will by side effect be written to the {@link Model}
 * which was passed into the constructor.
 */
public class NormalizeXPathSelectorRefinedByRFC5147CharScheme implements Consumer<Resource> {

	private static final Logger LOG = LoggerFactory.getLogger(NormalizeXPathSelectorRefinedByRFC5147CharScheme.class);

	public static final String RFC5147 = "http://tools.ietf.org/rfc/rfc5147";

	protected final de.wwu.scdh.annotation.selection.Resource<?> resource;
	protected final URI iri;
	protected Model model;
	protected final RewriterFactory rewriterFactory;
	protected Rewriter<DOMResource, XPathRefinedByRFC5147CharScheme, ? extends Point> rewriter = null;
	protected final RewriterConfig normalizerConfig;

	protected Optional<Exception> error;

	public NormalizeXPathSelectorRefinedByRFC5147CharScheme(
			de.wwu.scdh.annotation.selection.Resource<?> resource,
			URI iri,
			RewriterFactory rewriterFactory,
			Model model,
			RewriterConfig normalizerConfig) {
		this.resource = resource;
		this.iri = iri;
		this.model = model;
		this.rewriterFactory = rewriterFactory;
		this.normalizerConfig = normalizerConfig;
		error = Optional.empty();
		try {
			// note: The second point class, i.e., the output point, may be rewritten by the factory!
			this.rewriter = rewriterFactory.getRewriter(
					XPathRefinedByRFC5147CharScheme.class, XPathRefinedByRFC5147CharScheme.class, normalizerConfig);
			LOG.debug(
					"rewriting an oa:XPathSelector which is refined by RFC5147 character scheme with rewriter {}",
					rewriter.getClass().getCanonicalName());
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
		} catch (ModelException | NumberFormatException | SelectorException e) {
			if (normalizerConfig.writesBackErrorMessages()) {
				Statement errorStmt = model.createLiteralStatement(selector, SEL.error, e.getMessage());
				model.add(errorStmt);
			}
			error = Optional.of(e);
		}
	}

	/**
	 * Do the normalization and throw exceptions on errors.
	 */
	public void acceptThrows(Resource selector) throws ModelException, NumberFormatException, SelectorException {
		LOG.debug("normalizing XPathSelector '{}'", selector);

		DOMResource domResource;
		if (resource instanceof DOMResource) {
			domResource = (DOMResource) resource;
		} else {
			throw new SelectorException("cannot rewrite XPathSelector without a DOM resource");
		}

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
		ExtendedIterator<Resource> refinementIter = model.listStatements(selector, OA.refinedBy, (RDFNode) null)
				.mapWith(Statement::getResource)
				.filterKeep(ref -> !(model.listStatements(ref, RDF.type, OA.FragmentSelector)
								.toSet()
								.isEmpty()
						&& model.listStatements(ref, DCTerms.conformsTo, RFC5147)
								.toSet()
								.isEmpty()));
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
		if (refinementValue.startsWith("char=")) {
			startPos = Integer.parseInt(refinementValue.substring(5));
		} else {
			LOG.error(
					"value of RFC5147-conforming oa:FragmentSelector does not use the character scheme: {}",
					refinement);
			throw new ModelException(
					"value of RFC5147-conforming oa:FragmentSelector does not use the character scheme");
		}

		// 3. normalize the components
		LOG.debug("normalizing refined XPath {};{}", xpath, startPos);
		XPathRefinedByRFC5147CharScheme point = new XPathRefinedByRFC5147CharScheme(xpath, startPos);
		List<? extends Point> points = rewriter.rewrite(domResource, point, normalizerConfig);
		// 4. write back to the model
		LOG.debug("{} points rewritten", points.size());
		if (points.isEmpty()) {
			// remove values and indicate, that the selector does not point into the image/preimage
			model.removeAll(selector, RDF.value, null);
			Statement nullXPath = model.createLiteralStatement(selector, RDF.type, SEL.BlankedSelector);
			model.add(nullXPath);
			model.removeAll(refinement, RDF.value, null);
			Statement nullRefinement = model.createLiteralStatement(refinement, RDF.type, SEL.BlankedSelector);
			model.add(nullRefinement);
		} else {
			// TODO: see #23
			model.removeAll(refinement, null, null);
			model.removeAll(selector, null, null);
			SelectorBuilder build = new SelectorBuilder(model, selector);
			points.forEach(build);
		}
	}

	/**
	 * Use this to filter out XPath selectors that are refined by Fragment selectors conforming to RFC5147.
	 * @param model - The RDF {@link Model}
	 * @param selector - A selector {@link Resource}
	 * @return a filtered subset of <code>selectors</code>
	 */
	protected static boolean filter(Model model, Resource selector) {
		LOG.info("filter");
		StmtIterator types = model.listStatements(selector, RDF.type, OA.XPathSelector);
		boolean isXPathSel = types.hasNext();
		types.close();
		StmtIterator refinedBys = model.listStatements(selector, OA.refinedBy, (RDFNode) null);
		boolean isRefined = refinedBys.hasNext();
		if (!isXPathSel || !isRefined) {
			refinedBys.close();
			return false;
		}
		Resource refinement = model.listStatements(selector, OA.refinedBy, (RDFNode) null)
				.next()
				.getResource();
		refinedBys.close();
		StmtIterator rfc5147s =
				model.listStatements(refinement, DCTerms.conformsTo, model.createResource(RFC5147CharScheme.RFC5147));
		boolean conforms = rfc5147s.hasNext();
		rfc5147s.close();
		StmtIterator values = model.listStatements(refinement, RDF.value, (RDFNode) null);
		boolean hasValue = values.hasNext();
		if (!conforms || !hasValue) {
			values.close();
			return false;
		}
		String value = values.next().getObject().asLiteral().getString();
		values.close();
		return value.startsWith("char=");
	}
}
