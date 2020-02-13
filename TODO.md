# TODO

This document is essentially a list of shorthand notes describing work yet to be completed.
Unfortunately it is not complete enough for other people to pick work off the list and
complete as there is too much un-said.

* Optionally generate statistics and logging for each annotation pass indicating:
  * how many types were deferred in each round and in total.
  * how many types were pulled from deferred in each round and in total.
  * how many warnings were generated in each round and in total.
  * how many types were emitted in each round and in total.
  * how long each type took and how long each round took and how long it took in total.

* Convert all of our downstream dependencies to use something like https://mapstruct.org/news/2020-02-03-announcing-gem-tools/
  It is unclear whether it just creates a wrapper around mirrors for reading or if there is some
  constructors as well. Either way we should either use or re-implement and make use of to remove significant
  amounts of boilerplate from our downstream applications.
