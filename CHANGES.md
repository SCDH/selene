## Changes

## 0.2.0

- filter annotations by `oa:hasSource` when normalizing or mapping
  WADM selections (see issue #18)
- rewrite `oa:hasSource` IRI when mapping WADM from forwards or
  backwards (see issue #17)
- rewrite `trace:text` segments
  (`Q{http://wwu.de/scdh/selection-engine/node-tracing}text`) in
  XPaths produced by DOM mapping to `text()` (see issue #16)
- improves the Java API for use outside of the project. The core
  library is used in SEED DTS.

## 0.1.1

early release with basic normalization and mapping functionality
