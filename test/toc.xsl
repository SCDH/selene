<xsl:package name="http://example.org/toc" package-version="1.0" version="3.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:toc="http://example.org/toc" xmlns:trace="http://wwu.de/scdh/selection-engine/node-tracing"
  exclude-result-prefixes="#all" xpath-default-namespace="http://www.tei-c.org/ns/1.0">

  <xsl:use-package name="http://wwu.de/scdh/selection-engine/node-tracing" package-version="1.0.0"/>

  <xsl:template name="toc:toc" visibility="public">
    <xsl:context-item as="document-node()" use="required"/>
    <div class="toc">
      <xsl:apply-templates mode="toc:toc"/>
    </div>
  </xsl:template>

  <xsl:mode name="toc:toc" on-no-match="shallow-skip" visibility="public"/>

  <xsl:mode name="toc:entry" on-no-match="shallow-skip" visibility="private"/>

  <xsl:template mode="toc:toc" match="head">
    <xsl:variable name="level" as="xs:integer" select="ancestor::div => count()"/>
    <div class="toc-entry level-{$level}">
      <xsl:call-template name="trace:source-id"/>
      <xsl:apply-templates mode="toc:entry"/>
    </div>
  </xsl:template>

  <xsl:template mode="toc:entry" match="text()">
    <xsl:call-template name="trace:text"/>
  </xsl:template>

</xsl:package>
