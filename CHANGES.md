## Changes

## 0.3.3

- adds an option for writing error messages back to the model (see issue #37)
- replaces `nu.validator` with Cowan's `tagsoup` for parsing HTML (see
  issue #36)

## 0.3.1, 0.3.2

- fixes issues #33 and #34

## 0.3.0

- adds support for Web Annotation RFC 5147 plain text Fragment
  selectors (forward and backward)
- in detail: closes a number of issues #9, #10, #19, #22, #28, #30, #32
- adds to #27 by adding tests systematically

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
