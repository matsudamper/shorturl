FROM debian:12-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/data \
    && curl -fsSL "https://download.db-ip.com/free/dbip-country-lite-$(date +%Y-%m).mmdb.gz" \
    | gzip -d > /app/data/dbip-country-lite.mmdb

# CC BY 4.0 帰属表示
LABEL io.shorturl.geoip.attribution="IP geolocation data by DB-IP.com (https://db-ip.com), licensed under CC BY 4.0"

WORKDIR /app

COPY server/build/native/nativeCompile/shorturl-prod /app/shorturl

ENV HOST=0.0.0.0
ENV PORT=8080
ENV DATA_DIR=/app/data
ENV GEOIP_MMDB=/app/data/dbip-country-lite.mmdb

EXPOSE 8080

ENTRYPOINT ["/app/shorturl"]
