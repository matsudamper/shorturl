FROM gcr.io/distroless/cc-debian12:nonroot

WORKDIR /app

COPY server/build/native/nativeCompile/shorturl-prod /app/shorturl

ENV HOST=0.0.0.0
ENV PORT=8080
ENV DATA_DIR=/app/data
ENV GEOIP_MMDB=/app/data/GeoLite2-Country.mmdb

EXPOSE 8080

ENTRYPOINT ["/app/shorturl"]
