package de.wwu.scdh.annotation.selection;

import java.util.HashMap;
import java.util.Map;

public class PointImpl implements Point {

    /**
     * We are using the canonical name of the compononent class as
     * keys in this {@link Map} of {@link Component}s.
     */
    private Map<String, Component<?>> components =
	new HashMap<String, Component<?>>();

    // private Map<Class<Component<?>>, Component<?>> components =
    // 	new HashMap<Class<Component<?>>, Component<?>>();

    // private Map<Class<Component>, Component> components =
    // 	new HashMap<Class<Component>, Component>();

    /**
     * Make a {@link PointImpl} from as many {@link Component}s as you
     * want. Note: When multiple components of the same type are
     * given, only one is used.
     *
     * @param components  an open number of components
     */
    public PointImpl(Component<?> ... components) {
	for (Component<?> c: components) {
	    this.components.put(c.getClass().getCanonicalName(), c);
	}
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <C extends Component<?>> boolean hasComponent(Class<C> component) {
	return components.containsKey(component.getCanonicalName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, C extends Component<T>> T getComponent(Class<C> component)
	throws NoSuchComponentException {
	if (components.containsKey(component.getCanonicalName())) {
	    try {
		return (T) components.get(component.getCanonicalName()).getValue();
	    } catch (Exception e) {
		throw new NoSuchComponentException("failed to cast component " + component.getCanonicalName() + ": " + e.getMessage());
	    }
	} else {
	    throw new NoSuchComponentException(component.getCanonicalName());
	}
    }

}
