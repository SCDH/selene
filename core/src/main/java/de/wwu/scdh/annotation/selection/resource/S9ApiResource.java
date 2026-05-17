package de.wwu.scdh.annotation.selection.resource;

import de.wwu.scdh.annotation.selection.Resource;
import net.sf.saxon.s9api.Processor;

public interface S9ApiResource<T> extends Resource<T> {

	/**
	 * Returns the Saxon {@link Processor} the resource was parsed
	 * with.
	 * @return a Saxon {@link Processor}
	 */
	public Processor getProcessor();
}
