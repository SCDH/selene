package de.wwu.scdh.annotation.selection;

import java.util.List;

/**
 * The {@link TouchedInResource} interface defines methods for
 * locating selections in their web resource.
 *
 */
public interface TouchedInResource<S extends Selector> {

    /**
     * Returns a list of text nodes touched by this {@link Selection}.
     */
    List<Selector> getTouchedContent();

    // boolean touches(Point point);

    // boolean touches(Range range);

    // boolean contains(Point point);

    // boolean contains(Range range);

    // boolean overlaps(Range range);

    // boolean locatedIn(Range range);

}
