random notes

- codes = base62(id + 1_000_000) so the first ones are 4 chars instead of "1", "2"...
- aliases can't have `_`, generated codes get a `_` suffix if they'd collide with an alias. hacky but works
- cache only stores url + expiresAt, expiry is checked on every hit (ttl is just 1h to keep redis small)
- the generic jackson redis serializer can't do Instant out of the box -> using a typed one for CachedLink
- @Async click counting means the stats test needs awaitility

ideas
- bloom filter for unknown codes so random scanning doesn't hammer the db?
- batch click updates (see commented bit in ClickService)
