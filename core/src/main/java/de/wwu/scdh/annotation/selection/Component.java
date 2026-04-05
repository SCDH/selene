package de.wwu.scdh.annotation.selection;


/**
 * A {@link Point} has one or multiple {@link Component}s. Each
 * component has a type. This interface is for type-safe programming
 * with components of points.<P>
 *
 * E.g., an <code>oa:XPathSelector</code> has a <code>XPath</code>
 * component of type {@link String}; an <code>oa:XPathSelector</code>,
 * which is refined by a <code>oa:FragmentSelector</code> conforming
 * RFC5147's character scheme, has a <code>XPath</code> component of
 * type {@link String} and an <code>RFC5147CHAR</code> component of
 * type {@link Integer}.
 */
public interface Component<T> {

    /**
     * Returns the type of component.
     */
    Class<T> getType();

    /**
     * Returns the value of the component as type T.
     */
    T getValue();

}
