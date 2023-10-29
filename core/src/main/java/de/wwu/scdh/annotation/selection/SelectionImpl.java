package de.wwu.scdh.annotation.selection;

import java.net.URI;
import java.net.URISyntaxException;


/**
 * An {@link SelectionImpl<S>} object represents a pair of web
 * resource and selector from the Web Annotation Data Model.<P>
 *
 * A {@link SelectionImpl<S>} is parametrized with a type variable
 * extending {@link Selector}.
 */
public class SelectionImpl<S extends Selector> implements Selection<S>, Normalizable {

    protected final S selector;

    protected final Resource resource;

    protected Selector normalized = null;

    protected URI uri = null;

    public SelectionImpl(Resource resource, S selector) {
	this.resource = resource;
	this.selector = selector;
    }

    @Override
    public Resource getResource() {
	return this.resource;
    }

    @Override
    public S getSelector() {
	return this.selector;
    }

    @Override
    public boolean referentiallyEquals(Selection<S> other) {
	return false; // TODO
    }

    @Override
    public boolean referentiallySameAs(Selection<Selector> other) {
	return false; // TODO
    }

    /**
     * Whether or not the selection is isomorphically normalizable,
     * i.e., whether the type of the normalized form is the same type
     * as the form to be normalized.<P>
     *
     * This operates on a type level: Although a selection that is
     * already in its normal form, and thus has the same type as its
     * normalized form (since it is identical to it), the return type
     * only depends on the type of the selector, not its content.
     */
    public boolean isIsomorphicallyNormalizable() {
	return IsomorphicallyNormalizable.class.isAssignableFrom(selector.getClass());
    }

    /**
     * Whether or not the selection is normalizable, be it
     * isomorphically or with a type change.<p>
     *
     * This operates on a type level: Although a selection that is
     * already in its normal form, and thus has the same type as its
     * normalized form (since it is identical to it), the return type
     * only depends on the type of the selector, not its content.
     */
    public boolean isNormalizable() {
	return Normalizable.class.isAssignableFrom(selector.getClass());
    }


    /**
     * The {@link Normalizable#normalize()} method is offered, but
     * there is no guaranty that a selection with arbitrary selectors
     * is normalizable. If the selector is not normalizable, a
     * exception is thrown.
     */
    @Override
    public Selector normalize() throws SelectorException {
	if (normalized != null) {
	    return normalized;
	} else if (IsomorphicallyNormalizable.class.isAssignableFrom(selector.getClass())) {
	    normalized = ((IsomorphicallyNormalizable<S>) selector).normalize();
	    return normalized;
	} else if (Normalizable.class.isAssignableFrom(selector.getClass())) {
	    normalized = ((Normalizable) selector).normalize();
	    return normalized;
	} else {
	    throw new SelectorException("selector is not normalizable: " + selector.getClass());
	}
    }

    /**
     * Whether or not the selection is in normal form.
     */
    @Override
    public boolean isNormalized() throws SelectorException {
	try {
	    if (IsomorphicallyNormalizable.class.isAssignableFrom(normalized.getClass())) {
		return ((IsomorphicallyNormalizable<S>) normalized).isNormalized();
	    } else if (Normalizable.class.isAssignableFrom(normalized.getClass())) {
		return ((Normalizable) normalized).isNormalized();
	    } else {
		throw new SelectorException("selector is not normalizable and thus has no URI: " + selector.getClass());
	    }
	} catch (NullPointerException e) {
	    throw new SelectorException("");
	}
    }

    /**
     * The {@link Normalizable#toUri()} method is offered, but there
     * is no guaranty that a selection with arbitrary selectors is
     * normalizable. If the selector is not normalizable, a exception
     * is thrown.
     */
    @Override
    public URI toUri() throws SelectorException {
	URI fragment;
	if (this.uri != null) {
	    return this.uri;
	}
	if (IsomorphicallyNormalizable.class.isAssignableFrom(normalized.getClass())) {
	    fragment = ((IsomorphicallyNormalizable<S>) normalized).toUri();
	} else if (Normalizable.class.isAssignableFrom(normalized.getClass())) {
	    fragment = ((Normalizable) normalized).toUri();
	} else {
	    throw new SelectorException("selector is not normalizable and thus has no URI: " + selector.getClass());
	}
	try {
	    this.uri = new URI
		(resource.getUri().getScheme(),
		 resource.getUri().getRawAuthority(),
		 resource.getUri().getPath(),
		 resource.getUri().getRawQuery(),
		 fragment.getFragment());
	    return uri;
	} catch (URISyntaxException e) {
	    throw new SelectorException(e);
	}
    }

}
