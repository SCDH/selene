[![Tests](https://github.com/SCDH/selene/actions/workflows/test.yaml/badge.svg)](https://github.com/SCDH/selene/actions/workflows/test.yaml)

# Selene Selection Engine

*Selene* is a tool of processing deep **references** into 
resources like used in stand-off annotations. The [Web Annotation Data
Model](https://www.w3.org/TR/annotation-model/#selectors) (WADM) calls
the mechanisms for referencing a portion of a textual, pictorial
etc. resource **selectors**. Other standards may call them
**pointers**. The purpose of *Selene* is processing of selectors and
pointers respectively, not the rest of stand-off annotations.

*Selene* offers commands for

- [normalizing selectors](#normalizing-selectors)
- [transforming selectors](#transforming-selectors)
- [converting selector serializations](#converting-selector-serializations)

### Normalizing Selectors

A selection of the same portion of a resource may be expressed in
different ways. Normalizing selectors is important for storing them in
a uniform way and key for testing equality without applying them to
the resource.

### Transforming Selectors

Deriving different representations (**images**) from a resource
(**preimage**) is a common task, e.g. transforming an XML encoded text
to HTML or plain text. *Selene* can transform *pointers into the
preimage* to *pointers into the image* (**forward** transformation of
selectors) and vice versa (**backward** transformation of selectors)
with the same transformation that is used for deriving a
representation of the resource. Transforming selectors is important
for making standoff annotations interoperable when they were
aggregated for a particular representation of a resource; it is thus a
corner stone of generating and processing FAIR research
data.

| preimage | image      | Transformation Techn. | forward selector transformation    | backward selector transformation   |
|:---------|------------|-----------------------|------------------------------------|------------------------------------|
| XML      | XML        | arbitrary XSLT        | ✅ (XML selector to XML selector)  | ✅ (XML selector to XML selector)  |
| XML      | XHTML      | arbitrary XSLT        | ✅ (XML selector to HTML selector) | ✅ (HTML selector to XML selector) |
| XML      | HTML       | arbitrary XSLT        | ❓ (XML selector to HTML selector) | ❓ (HTML selector to XML selector) |
| XML      | plain text | arbitrary XSLT        | ✅ (XML selector to text selector) | ✅ (text selector to XML selector) |

Currently, selector transformations forward and backward are only
supported alongside XSLT transformations of preimage to image. Why?
Because XSLT is a formidable technology based on a truly declarative
programming paradigm. And *Selene*'s selector transformation exploits
the possibilities only a declarative language can provide. Lern more
about the preconditions to the XSLT in the [project's Wiki](wiki/XSLT).

Yes, as you can see in the table, *Selene* is capable of rewriting
pointers into plain text (inter-glyph character positions) to pointers
into XML (XPath selectors refined by inter-glyph character positions)
if the plain text was derived from XML with XSLT.

### Converting Selector Serializations

Converting between different selector serialization formats.

## Roadmap

- ✅ Fall 2025: core library with transformation of pointers and unit
  tests, minimal CLI 
- ✅ Spring 2026: implement distributed text service (DTS) as a
  deployment platform for Selene: See [SEED
  DTS](https://github.com/scdh/seed-xc/dts)!
- ✅ Sommer 2026: add REST API endpoints for normalizing and
  transforming pointers as an extension to DTS. See [branch `selene`
  of SEED DTS](https://github.com/SCDH/seed-xc/tree/selene) and
  [Milestone 6](https://github.com/SCDH/seed-xc/milestone/6).
- September 2026: Introduce the technology at Editopia conference

## Selene

![The Moon-goddess Selene or Luna accompanied by the Dioscuri, or
Phosphoros (the Morning Star) and Hesperos (the Evening Star). Marble
altar, Roman artwork, 2nd century CE. From Italy. Image:
[Wikimedia](https://commons.wikimedia.org/wiki/File:Altar_Selene_Louvre_Ma508.jpg)](https://upload.wikimedia.org/wikipedia/commons/thumb/0/09/Altar_Selene_Louvre_Ma508.jpg/330px-Altar_Selene_Louvre_Ma508.jpg)

The Selene Selection Engine can track the identity of nodes over different
representations of resource like the Moon goddess Selene can track the Venus appearing
as the Morning Star and the Evening Star.

## Getting Started


Java version required: OpenJDK 17+

Tesing and building:

```{shell}
./mvnw clean test package
```

After building with Maven, there is a command line interface in
`cli/target/bin/oasel`.

Generating Java Docs, which will be in `target/site/apidocs`:

```{shell}
./mvnw javadoc:aggregate
```

[Javadocs are online](https://scdh.zivgitlabpages.uni-muenster.de/selectors/selection-engine/apidocs/) for the latest release.

Usage Examples (Linux):

```shell
cli/target/bin/oasel normalize test/gesang.annot1.json -l jsonld -f ttl
```

```shell
cli/target/bin/oasel normalize test/gesang.annot1.json -l jsonld  -x "path(root(.))" -f ntriples
```

Building native executable:

1. set up graalvm

```shell
tar -zxf graalvm.tgz
export GRAALVM_HOMT=graalvm-...
```

2. build native executable

```shell
./mvnw package -Pnative
```


```shell
cli/target/bin/selwr transforms --xpath='//*:head[1]/text()[1]' --character=5 --xsl=test/text-with-toc-pkg.xsl test/Gesang.tei.xml --normalizer-xpath="path(.)" --config=saxon-config.xml
```



## Further Reading

- [JSON-LD compact in RIOT](https://github.com/apache/jena/issues/2031)
