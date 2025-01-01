package de.wwu.scdh.annotation.selection;


/**
 * A {@link Point} is an abstract reference to a resource which offers
 * parts selectable according some selection specification, for
 * example the Web Annotation Data Model (WADM) or
 * XPointers. Regarding the WADM, a {@link Point} is a record for the
 * values of a selector. The type can either be inferred by the kind
 * of components or the class implementing this interface.<P>
 *
 * A point has one or multiple {@link Component}s. E.g., an
 * <code>oa:XPathSelector</code> has a <code>XPath</code> component;
 * an <code>oa:XPathSelector</code>, which is refined by a
 * <code>oa:FragmentSelector</code> conforming RFC5147's character
 * scheme, has a <code>XPath</code> component and an
 * <code>RFC5147CHAR</code> component.
 */
public interface Point {

    /**
     * A predicate, that returns True if and only if the point has the
     * component of the given type.
     * @param component  the component as {@link Class}
     */
    public <C extends Component<?>> boolean hasComponent(Class<C> component);

    /**
     * Returns the value of the requested component in a type-safe
     * manner. If the component is not present or if the value does
     * not match the components type, a {@link NoSuchComponentException}
     * is thrown.
     *
     * @param component  the requested component as {@link Class}
     * @return the value of the component in its type T
     */
    public <T, C extends Component<T>> T getComponent(Class<C> component) throws NoSuchComponentException;

}
