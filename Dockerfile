# syntax=docker/dockerfile:1
FROM termux/termux-docker:aarch64 AS builder

ARG NODE_PKG=nodejs
ARG SQLITE_VERSION=26.4.0

# Install tools compiler & nodejs
RUN pkg update -y && \
    pkg install -y ${NODE_PKG} python make clang build-essential tar

WORKDIR /build

# Gunakan cache mount untuk npm agar download dependency instan
RUN --mount=type=cache,target=/root/.npm \
    npm init -y && \
    npm install better-sqlite3@${SQLITE_VERSION} --build-from-source

# Kompres modul hasil build
RUN mkdir -p /export && \
    tar -czvf /export/better-sqlite3-termux-aarch64.tar.gz -C /build/node_modules better-sqlite3

# Stage export: Mengeluarkan file .tar.gz langsung ke host
FROM scratch AS export
COPY --from=builder /export/better-sqlite3-termux-aarch64.tar.gz /