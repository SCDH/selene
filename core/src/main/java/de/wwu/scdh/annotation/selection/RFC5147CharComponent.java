package de.wwu.scdh.annotation.selection;

/**
 * An {@link RFC5147CharComponent} is a {@link Component} following
 * the RFC5147 character scheme. Its value type is {@link Integer}.
 */
public class RFC5147CharComponent implements Component<Integer> {

    private int value;

    public RFC5147CharComponent(int value) {
	this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<Integer> getType() {
	return Integer.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getValue() {
	return value;
    }

}
