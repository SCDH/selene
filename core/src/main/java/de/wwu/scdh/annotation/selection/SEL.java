package de.wwu.scdh.annotation.selection;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;

/**
 * Defines components of the Selene selector ontology in a type-safe way.
 */
public class SEL {

	public static final String NAMESPACE = "https://scdh.uni-muenster.de/ontology/selene/";

	/**
	 * A blanked selector is a selector, that points to a portion of a resource representation which is not part of the
	 * resource representation which the selectors of a mapping request point to. E.g. in a forward mapping request,
	 * the selected node of the preimage is not in the image. The node and the selector pointing to it are blanked out
	 * by the transformation, or say: muted or collimated. Instead of deleting the selector from the result set of a
	 * mapping operation without trace, this class can be assigned to the selector.
	 */
	public static final Resource BlankedSelector = new ResourceImpl(NAMESPACE, "BlankedSelector");

	/**
	 * A bad selector is user input, that can not be processed. Instead of returning a BadRequest error code,
	 * an application can assign this RDF class to a selector, the processing of which would throw an exception.
	 */
	public static final Resource BadSelector = new ResourceImpl(NAMESPACE, "BadSelector");

	/**
	 * This property may be added when an error occurred when processing a selector.
	 */
	public static final Property error = new PropertyImpl(NAMESPACE, "error");
}
