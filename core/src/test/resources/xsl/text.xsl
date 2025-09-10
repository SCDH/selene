<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:trace="http://wwu.de/scdh/selection-engine/node-tracing" exclude-result-prefixes="#all"
    version="3.0">

    <xsl:output method="text"/>

    <xsl:use-package name="http://wwu.de/scdh/selection-engine/node-tracing" package-version="1.0.0"/>

    <xsl:mode on-no-match="shallow-skip"/>

    <xsl:template match="/">
        <xsl:call-template name="trace:root"/>
    </xsl:template>

    <xsl:template match="text()">
        <xsl:call-template name="trace:text"/>
    </xsl:template>

</xsl:stylesheet>
