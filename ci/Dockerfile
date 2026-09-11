# =============================================================================
# iNWEB Browser — Chromium build container
#
# BUILD HOST image. Status: AUTHORED (Phase 1) — the image is built and
# validated when the build host is provisioned (blocker B-001).
# Base: Ubuntu 24.04 x86-64 per upstream Android build requirements.
#
# Note: the Android SDK/NDK and pinned toolchain pieces are fetched by
# Chromium's own gclient hooks (install-build-deps / hook-managed downloads),
# keeping the image minimal. JDK selection is validated at first image build.
# =============================================================================
FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
      git \
      python3 \
      python3-pip \
      curl \
      xz-utils \
      ca-certificates \
      build-essential \
      pkg-config \
      perl \
      zip \
      unzip \
      file \
      openjdk-17-jdk-headless \
    && rm -rf /var/lib/apt/lists/*

# depot_tools at the pinned revision (kept in sync with scripts/fetch_chromium.sh)
ARG DEPOT_TOOLS_REV=73fc5a4d6bd051f1fd58404e62dfb83f734b0cfa
RUN git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git /opt/depot_tools \
    && git -C /opt/depot_tools checkout "${DEPOT_TOOLS_REV}"

ENV PATH=/opt/depot_tools:${PATH}

WORKDIR /workspace
ENTRYPOINT ["/bin/bash"]
