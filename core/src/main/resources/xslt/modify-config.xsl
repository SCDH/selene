<?xml version="1.0" encoding="UTF-8"?>
<!-- XSLT for manipulating a Saxon config file -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns="http://saxon.sf.net/ns/configuration"
    xpath-default-namespace="http://saxon.sf.net/ns/configuration" exclude-result-prefixes="#all"
    version="3.0">

    <xsl:param name="trace-location" as="xs:string?" select="()"/>

    <xsl:output method="xml"/>

    <xsl:mode on-no-match="shallow-copy"/>

    <xsl:template
        match="package[@name = 'http://wwu.de/scdh/selection-engine/node-tracing' and @version = '1.0.0' and $trace-location]/@sourceLocation">
        <xsl:attribute name="sourceLocation" select="$trace-location"/>
    </xsl:template>

</xsl:stylesheet>
