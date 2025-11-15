# Selene Selection Engine

Selene is the Swiss Army Knife of processing references to parts of
resources like used in standoff annotations. The [Web Annotation Data
Model](https://www.w3.org/TR/annotation-model/#selectors) (WADM) calls
the mechanisms for referencing a part of a textual, pictorial
etc. resource **selectors**. Other standards may call them
**pointers**.

Selene offers commands for

- normalizing selectors: A selection of the same part of a resource
  may be expressed in different ways. Normalizing selectors is
  important for storing them in a uniform way and key for testing
  equality without applying them to the resource.
- transforming selectors: Deriving different representations (images)
  from a resource (preimage) is a common task, e.g. transforming an
  XML encoded text to HTML or plain text. Selene can transform
  selectors into the preimage to selectors into the image (forward
  transformation of selectors) and vice versa (backward transformation
  of selectors) with the same transformation that is used for deriving
  a representation of the resource. Transforming selectors is
  important for making standoff annotations interoperable when they
  were aggregated for a particular image; it is thus a corner stone of
  generating and processing FAIR research data.
- converting between different selector serialization formats

## Details

### Normalizing Selectors

### Transforming Selectors

| preimage | image      | Techn.         | forward selector transformation    | backward selector transformation   |
|:---------|------------|----------------|------------------------------------|------------------------------------|
| XML      | XML        | arbitrary XSLT | ✅ (XML selector to XML selector)  | ✅ (XML selector to XML selector)  |
| XML      | XHTML      | arbitrary XSLT | ✅ (XML selector to HTML selector) | ✅ (HTML selector to XML selector) |
| XML      | HTML       | arbitrary XSLT | ❓ (XML selector to HTML selector) | ❓ (HTML selector to XML selector) |
| XML      | plain text | arbitrary XSLT | ✅ (XML selector to text selector) | ✅ (text selector to XML selector) |

Currently only selector transformation forward and backward is only
supported alongside XSLT transformation of preimage to image. Why?
Because XSLT is a formidable technology based on a truly declarative
programming paradigm. And Selene's selector transformation exploits
the possibilities to only declarative languages provide.

### Converting Selector Serializations


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


## Further Reading

- [JSON-LD compact in RIOT](https://github.com/apache/jena/issues/2031)
