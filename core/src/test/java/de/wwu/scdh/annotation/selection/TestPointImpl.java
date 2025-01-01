package de.wwu.scdh.annotation.selection;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;


public class TestPointImpl {

    @Test
    void testNonPoint() throws NoSuchComponentException {
	Point point = new PointImpl();
	assertFalse(point.hasComponent(XPathComponent.class));
	assertThrows(NoSuchComponentException.class, () -> point.getComponent(XPathComponent.class));
    }

    @Test
    void testSimplePoint() throws NoSuchComponentException {
	XPathComponent xpath = new XPathComponent("/");
	Point point = new PointImpl(xpath);
	assertTrue(point.hasComponent(xpath.getClass()));
	assertTrue(point.hasComponent(XPathComponent.class));
	assertEquals("/", point.getComponent(xpath.getClass()));
	assertEquals(String.class, point.getComponent(xpath.getClass()).getClass());
	assertEquals(xpath.getType(), point.getComponent(xpath.getClass()).getClass());
	assertFalse(point.hasComponent(RFC5147CharComponent.class));
	assertThrows(NoSuchComponentException.class, () -> point.getComponent(RFC5147CharComponent.class));
    }

    @Test
    void testComplexPoint() throws NoSuchComponentException {
	XPathComponent xpath = new XPathComponent("/");
	RFC5147CharComponent chr = new RFC5147CharComponent(0);
	Point point = new PointImpl(xpath, chr);
	assertTrue(point.hasComponent(xpath.getClass()));
	assertTrue(point.hasComponent(XPathComponent.class));
	assertEquals("/", point.getComponent(xpath.getClass()));
	assertTrue(point.hasComponent(chr.getClass()));
	assertTrue(point.hasComponent(RFC5147CharComponent.class));
	assertEquals(0, point.getComponent(chr.getClass()));
    }

    @Test
    void testSameComponentsPoint() throws NoSuchComponentException {
	XPathComponent xpath1 = new XPathComponent("/");
	XPathComponent xpath2 = new XPathComponent("/root");
	Point point = new PointImpl(xpath1, xpath2); // overrides
	assertTrue(point.hasComponent(XPathComponent.class));
	assertTrue(point.getComponent(XPathComponent.class).equals(xpath1.getValue()) ||
		   point.getComponent(XPathComponent.class).equals(xpath2.getValue()));
	assertFalse(point.getComponent(XPathComponent.class).equals(xpath1.getValue()) &&
		   point.getComponent(XPathComponent.class).equals(xpath2.getValue()));
    }


}
