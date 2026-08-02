package de.wwu.scdh.annotation.selection.rewriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class XPathRewriterBaseTest {

	@Test
	public void testReplaceTraceTextLeaf() {
		assertEquals(
				"text()[1]",
				XPathRewriterBase.replaceTraceTextLeaf("Q{http://wwu.de/scdh/selection-engine/node-tracing}text[1]"));
		assertEquals(
				"/html/body/div[1]/text()[0]",
				XPathRewriterBase.replaceTraceTextLeaf(
						"/html/body/div[1]/Q{http://wwu.de/scdh/selection-engine/node-tracing}text[0]"));
		assertEquals(
				"id('schach')/text()[42]",
				XPathRewriterBase.replaceTraceTextLeaf(
						"id('schach')/Q{http://wwu.de/scdh/selection-engine/node-tracing}text[42]"));
		assertEquals(
				"//Q{http://wwu.de/scdh/selection-engine/node-tracing}text[23]/text()[1]",
				XPathRewriterBase.replaceTraceTextLeaf(
						"//Q{http://wwu.de/scdh/selection-engine/node-tracing}text[23]/Q{http://wwu.de/scdh/selection-engine/node-tracing}text[1]"));
	}
}
