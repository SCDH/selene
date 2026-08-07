package de.wwu.scdh.annotation.selection.wadm;

import de.wwu.scdh.annotation.selection.Point;
import de.wwu.scdh.annotation.selection.point.RFC5147CharScheme;
import de.wwu.scdh.annotation.selection.point.XPathRefinedByRFC5147CharScheme;
import java.util.function.Consumer;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link SelectorBuilder} has recipes for building selectors for certain types of {@link Point}s.
 */
public class SelectorBuilder implements Consumer<Point> {

	private static final Logger LOG = LoggerFactory.getLogger(SelectorBuilder.class);

	private Model model;
	private Resource selector;

	/**
	 * Makes a new {@link SelectorBuilder} for a {@link Model}. It will use the {@link Resource} <code>selector</code>
	 * as RDF resource representing the selector and add properties to it.
	 *
	 * @param model - The {@link Model} of the annotations
	 * @param selectorNode - The {@link Resource} being the object of a <code>oa:hasSelector</code> statement
	 *                     (<code>oa:startSelector</code> etc. respectively)
	 */
	public SelectorBuilder(Model model, Resource selectorNode) {
		this.model = model;
		this.selector = selectorNode;
	}

	@Override
	public void accept(Point point) {
		if (point instanceof RFC5147CharScheme) build((RFC5147CharScheme) point);
		else if (point instanceof XPathRefinedByRFC5147CharScheme) build((XPathRefinedByRFC5147CharScheme) point);
		else {
			LOG.warn("cannot make web annotation data model selector for {}", point);
		}
	}

	protected void build(RFC5147CharScheme point) {
		Statement typeStmt = model.createStatement(selector, RDF.type, OA.FragmentSelector);
		model.add(typeStmt);
		Statement conformsToStmt = model.createStatement(
				selector, DCTerms.conformsTo, model.createResource("http://tools.ietf.org/rfc/rfc5147"));
		model.add(conformsToStmt);
		Statement valueStmt = model.createLiteralStatement(selector, RDF.value, "char=" + point.getChar());
		model.add(valueStmt);
	}

	protected void build(XPathRefinedByRFC5147CharScheme point) {
		Statement typeStmt = model.createStatement(selector, RDF.type, OA.XPathSelector);
		model.add(typeStmt);
		Statement valueStmt = model.createLiteralStatement(selector, RDF.value, point.getXPath());
		model.add(valueStmt);
		Resource refinement = model.createResource();
		Statement refinementStmt = model.createStatement(selector, OA.refinedBy, refinement);
		model.add(refinementStmt);
		Statement refinementTypeStmt = model.createStatement(refinement, RDF.type, OA.FragmentSelector);
		model.add(refinementTypeStmt);
		Statement conformsToStmt = model.createStatement(
				refinement, DCTerms.conformsTo, model.createResource("http://tools.ietf.org/rfc/rfc5147"));
		model.add(conformsToStmt);
		Statement refinementValueStmt = model.createLiteralStatement(refinement, RDF.value, "char=" + point.getChar());
		model.add(refinementValueStmt);
	}
}
