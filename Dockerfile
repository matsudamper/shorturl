FROM debian:12-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/data

WORKDIR /app

COPY server/build/native/nativeCompile/shorturl /app/shorturl

ENV HOST=0.0.0.0
ENV PORT=8080
ENV DATA_DIR=/app/data

EXPOSE 8080

ENTRYPOINT ["/app/shorturl"]
