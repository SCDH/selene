# Selection Engine

The Selection Engine is a Swiss army knife for dealing with Standoff
Annotations following the Web Annotation Data Model (WADM).

- normalize selectors
- convert serialization formats
- rewrite XPathSelectors, which are refined by a
  RFC5147-FragmentSelector, to an other XPath component and
  recalculate the position component accordingly
- map selectors referencing a target to selectors referencing a
  derived target

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
