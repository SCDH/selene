package de.wwu.scdh.annotation.selection;

import net.sf.saxon.s9api.XdmNode;

public record NodePositionPair(XdmNode node, int position) {}
