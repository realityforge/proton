# TODO

This document is essentially a list of shorthand notes describing work yet to completed.
Unfortunately it is not complete enough for other people to pick work off the list and
complete as there is too much un-said.

* Remove dependency on the `com.google.auto:auto-common` artifact and thus `com.google.guava:guava`.
  This may simply involve copy-pasting the code and refactoring to eliminate guava usage. This would
  reduce the build time and artifact size in downstream projects that typically shade/relocate the code
  into a local package. Due to inefficiences in this process it is not uncommon to have ~18MB processor
  jars where otherwise a 100K jar would suffice.
