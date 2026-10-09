# snip-url

yet another url shortener. mostly built this to play with redis caching + spring boot 3.

## what it does

- `POST /api/urls` -> gives you a short code (base62 of the db id, or your own alias)
- `GET /{code}` -> 302 to the original url (404 if unknown, 410 if expired)
- `GET /api/urls/{code}/stats` -> clicks, created/last accessed, top referrers + user agents
- lookups are cached in redis, clicks are counted async so redirects stay fast
- creates are rate limited per ip (30/min by default)

## running it

need postgres + redis. easiest:

```
docker compose up --build
```

or locally (java 17+), with postgres on 5432 (db/user/pass `snip`) and redis on 6379:

```
./mvnw spring-boot:run
```

env vars: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `BASE_URL`, `CREATE_RATE_LIMIT`

## api

```
curl -X POST localhost:8080/api/urls -H 'content-type: application/json' \
  -d '{"url":"https://spring.io/projects/spring-boot"}'
# {"code":"4c93","shortUrl":"http://localhost:8080/4c93","url":"https://spring.io/projects/spring-boot","expiresAt":null}

curl -X POST localhost:8080/api/urls -H 'content-type: application/json' \
  -d '{"url":"https://github.com","customAlias":"gh","expiresAt":"2030-01-01T00:00:00Z"}'

curl -i localhost:8080/4c93
curl localhost:8080/api/urls/4c93/stats
```

aliases: 3-32 chars, letters/digits/dash. dupes get a 409.

## tests

`./mvnw verify` - uses H2 + a simple in-memory cache, so no postgres/redis needed.

## todo

- [ ] delete / disable links (and evict from cache)
- [ ] move rate limiting to redis
- [ ] block shortening our own urls
- [ ] maybe QR codes?
- [ ] clicks per day in stats
