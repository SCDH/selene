package de.wwu.scdh.annotation.selection.wadm;

import de.wwu.scdh.annotation.selection.*;
import de.wwu.scdh.annotation.selection.point.RFC5147CharScheme;
import de.wwu.scdh.annotation.selection.resource.DOMResource;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component for normalizing and rewriting fragment selectors that conform to RFC5147.
 */
public class NormalizeRFC5147CharScheme implements Consumer<Resource> {

	private static final Logger LOG = LoggerFactory.getLogger(NormalizeRFC5147CharScheme.class);

	protected final de.wwu.scdh.annotation.selection.Resource<?> resource;
	protected final URI iri;
	protected Model model;
	protected final RewriterFactory rewriterFactory;
	protected Rewriter<DOMResource, RFC5147CharScheme, ? extends Point> rewriter = null;
	protected final RewriterConfig normalizerConfig;

	protected Optional<Exception> error = null;

	static final Pattern charScheme = Pattern.compile("^char=(\\d*)");

	public NormalizeRFC5147CharScheme(
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
		try {
			// note: The second point class, i.e., the output point, may be rewritten by the factory!
			this.rewriter =
					rewriterFactory.getRewriter(RFC5147CharScheme.class, RFC5147CharScheme.class, normalizerConfig);
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
	@Override
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

	public void acceptThrows(Resource selector) throws ModelException, NumberFormatException, SelectorException {
		LOG.info("normalizing/rewriting Fragment selector conforming to RFC 5147");

		DOMResource domResource;
		if (resource instanceof DOMResource) {
			domResource = (DOMResource) resource;
		} else {
			throw new SelectorException("cannot rewrite XPathSelector without a DOM resource");
		}

		// 1. get value
		String value;
		int position;
		StmtIterator valueStmts = model.listStatements(selector, RDF.value, (RDFNode) null);
		if (valueStmts.hasNext()) {
			value = valueStmts.next().getObject().asLiteral().getString();
			valueStmts.close();
			Matcher matcher = charScheme.matcher(value);
			if (matcher.matches()) {
				position = Integer.parseInt(matcher.group(1));
			} else {
				position = Integer.parseInt(value);
			}
		} else {
			valueStmts.close();
			LOG.error("no value for oa:FragmentSelector");
			throw new ModelException("no value for oa:FragmentSelector");
		}
		LOG.debug("position: {}", position);

		// 2. normalize/rewrite the point
		LOG.debug("normalizing/rewriting RFC5147 char scheme {}", position);
		RFC5147CharScheme point = new RFC5147CharScheme(position);
		List<? extends Point> points = rewriter.rewrite(domResource, point, normalizerConfig);

		// 3. write back to model
		LOG.debug("{} points rewritten", points.size());
		if (points.isEmpty()) {
			// remove values and indicate, that the selector does not point into the image/preimage
			model.removeAll(selector, RDF.value, null);
			Statement nullXPath = model.createLiteralStatement(selector, RDF.type, SEL.BlankedSelector);
			model.add(nullXPath);
		} else {
			// TODO: see #23
			model.removeAll(selector, null, null);
			SelectorBuilder build = new SelectorBuilder(model, selector);
			points.forEach(build);
		}
	}

	/**
	 * Use this to filter out Fragment selectors conforming to RFC5147.
	 * @param model - The RDF {@link Model}
	 * @param selector - A selector {@link Resource}
	 * @return a filtered subset of <code>selectors</code>
	 */
	protected static boolean filter(Model model, Resource selector) {
		LOG.info("filter");
		StmtIterator types = model.listStatements(selector, RDF.type, OA.FragmentSelector);
		boolean isFragSel = types.hasNext();
		types.close();
		StmtIterator rfc5147s =
				model.listStatements(selector, DCTerms.conformsTo, model.createResource(RFC5147CharScheme.RFC5147));
		boolean conforms = rfc5147s.hasNext();
		rfc5147s.close();
		StmtIterator values = model.listStatements(selector, RDF.value, (RDFNode) null);
		boolean hasValue = values.hasNext();
		boolean hasCharValue = false;
		if (hasValue) {
			String value = values.next().getObject().asLiteral().getString();
			hasCharValue = value.startsWith("char=");
		}
		values.close();
		StmtIterator refined = model.listStatements(null, OA.refinedBy, selector);
		boolean isRefinement = refined.hasNext();
		refined.close();
		StmtIterator refinedBys = model.listStatements(selector, OA.refinedBy, (RDFNode) null);
		boolean isRefined = refinedBys.hasNext();
		refinedBys.close();
		StmtIterator xpaths = model.listStatements(selector, RDF.type, OA.XPathSelector);
		boolean isXPathSel = xpaths.hasNext();
		xpaths.close();
		LOG.info("filter: {}, {}, {}, {}, {}", isFragSel, conforms, hasCharValue, isRefinement, isRefined);
		return isFragSel && conforms && hasCharValue && !isRefinement && !isRefined;
		// TODO: filter char scheme
	}
}
