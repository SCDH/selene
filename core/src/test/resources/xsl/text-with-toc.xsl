<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:trace="http://wwu.de/scdh/selection-engine/node-tracing"
    xmlns="http://www.w3.org/1999/xhtml"
    xpath-default-namespace="http://www.tei-c.org/ns/1.0" exclude-result-prefixes="#all"
    version="3.0">

    <xsl:output method="xhtml" indent="false"/>

    <xsl:use-package name="http://wwu.de/scdh/selection-engine/node-tracing" package-version="1.0.0"/>

    <xsl:mode on-no-match="shallow-skip"/>

    <xsl:mode name="toc" on-no-match="deep-skip"/>

    <xsl:template mode="toc #unnamed" match="text()">
        <xsl:call-template name="trace:text"/>
    </xsl:template>

    <xsl:template match="document-node()">
        <html>
            <head>
                <title>
                    <xsl:apply-templates select="//teiHeader//title"/>
                </title>
            </head>
            <body>
                <div class="toc" style="font-size:0.8em; background-color:lightgray">
                    <h1>Table of contents:</h1>
                    <xsl:apply-templates mode="toc" select="//text//head"/>
                </div>
                <hr/>
                <div class="content">
                    <xsl:apply-templates select="//text"/>
                </div>
            </body>
        </html>
    </xsl:template>

    <xsl:template mode="toc #unnamed" match="lg | div">
        <div>
            <xsl:call-template name="trace:source-id"/>
            <xsl:apply-templates mode="#current"/>
        </div>
    </xsl:template>

    <xsl:template match="l | p">
        <p>
            <xsl:call-template name="trace:source-id"/>
            <xsl:apply-templates/>
        </p>
    </xsl:template>

    <xsl:template match="caesura">
        <span style="margin-left:2em">
            <xsl:call-template name="trace:source-id"/>
        </span>
    </xsl:template>

    <xsl:template mode="toc #unnamed" match="head">
        <xsl:variable name="level" as="xs:integer" select="count(ancestor::div | ancestor::lg) + 1"/>
        <xsl:element name="{'h' || $level}">
            <xsl:call-template name="trace:source-id"/>
            <xsl:apply-templates mode="#current"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="app">
        <xsl:apply-templates select="lem"/>
    </xsl:template>

    <xsl:template match="lem">
        <span>
            <xsl:call-template name="trace:source-id"/>
            <xsl:apply-templates/>
        </span>
    </xsl:template>


</xsl:stylesheet>
