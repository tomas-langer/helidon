#
# Copyright (c) 2026 Oracle and/or its affiliates.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM container-registry.oracle.com/os/oraclelinux:9-slim AS build
ENV GO111MODULE=on
ENV GOPROXY=https://proxy.golang.org
ENV GOSUMDB=sum.golang.org
ENV GOTOOLCHAIN=local
ENV CGO_ENABLED=0
ENV VERSION=2.6.1-SNAPSHOT
ENV COMMIT=af83a65f0b6273ef38bf778d400d98892e7653d8
ENV SOURCE_SHA256=eaf456f4021325349615020cb8ceaaf0c51c7c5f0e1c74db089dced212b2060c
ENV GO_SUM_SHA256=0ef5b9b9ba63e6f8822718a8e19b68146b1309fa0e18fb32c082d9402f04e560

COPY oracle-rpms.sh /oracle-rpms.sh
RUN sh /oracle-rpms.sh install /build-rpms.txt go-toolset ca-certificates curl tar gzip

WORKDIR /workspace
RUN curl --fail --show-error --location --proto '=https' --proto-redir '=https' \
        "https://codeload.github.com/summerwind/h2spec/tar.gz/${COMMIT}" -o h2spec.tar.gz \
    && echo "${SOURCE_SHA256}  h2spec.tar.gz" | sha256sum --check \
    && mkdir h2spec \
    && tar -xzf h2spec.tar.gz --strip-components=1 -C h2spec

WORKDIR /workspace/h2spec
# h2specd hardcodes a 3-second launcher timeout in spec/specd.go. That is too short for a
# JVM client started repeatedly inside a container, so honor the existing --timeout flag instead.
RUN echo "${GO_SUM_SHA256}  go.sum" | sha256sum --check \
    && mkdir -p /usr/share/h2spec-client \
    && sha256sum go.mod go.sum > /usr/share/h2spec-client/go-inputs.sha256 \
    && sha256sum spec/specd.go > /usr/share/h2spec-client/specd-original.sha256 \
    && test "$(grep -oF 'time.Duration(3) * time.Second' spec/specd.go | wc -l)" -eq 1 \
    && test "$(grep -oF 'time.After(time.Duration(3) * time.Second)' spec/specd.go | wc -l)" -eq 1 \
    && test "$(grep -oF 'time.After(c.Timeout)' spec/specd.go | wc -l)" -eq 0 \
    && sed -i 's/time.Duration(3) \* time.Second/c.Timeout/' spec/specd.go \
    && test "$(grep -oF 'time.Duration(3) * time.Second' spec/specd.go | wc -l)" -eq 0 \
    && test "$(grep -oF 'time.After(c.Timeout)' spec/specd.go | wc -l)" -eq 1 \
    && sha256sum spec/specd.go > /usr/share/h2spec-client/specd-patched.sha256 \
    && printf '%s\n' 'spec/specd.go: time.After(time.Duration(3) * time.Second) -> time.After(c.Timeout)' \
        > /usr/share/h2spec-client/timeout-patch.txt \
    && go mod download \
    && go mod verify \
    && go build -mod=readonly -trimpath \
        -ldflags "-X main.VERSION=${VERSION} -X main.COMMIT=${COMMIT}" ./cmd/h2specd \
    && sha256sum --check /usr/share/h2spec-client/go-inputs.sha256 \
    && cp LICENSE /usr/share/h2spec-client/LICENSE \
    && mkdir -p /usr/share/h2spec-client/licenses/modules /usr/share/h2spec-client/licenses/go \
    && cp /usr/share/doc/golang/LICENSE /usr/share/doc/golang/PATENTS /usr/share/h2spec-client/licenses/go \
    && (cd "$(go env GOMODCACHE)" \
        && find . -type f \( -iname 'LICENSE*' -o -iname 'NOTICE*' -o -iname 'COPYING*' \) \
            -exec cp --parents -t /usr/share/h2spec-client/licenses/modules {} +) \
    && go version > /usr/share/h2spec-client/go-version.txt \
    && go version -m h2specd > /usr/share/h2spec-client/binary-build.txt \
    && sh /oracle-rpms.sh record /usr/share/h2spec-client/build-rpms.txt \
    && printf 'VERSION=%s\nCOMMIT=%s\nSOURCE_SHA256=%s\nGOPROXY=%s\nGOSUMDB=%s\nGOTOOLCHAIN=%s\nCGO_ENABLED=%s\n' \
        "${VERSION}" "${COMMIT}" "${SOURCE_SHA256}" "${GOPROXY}" "${GOSUMDB}" \
        "${GOTOOLCHAIN}" "${CGO_ENABLED}" > /usr/share/h2spec-client/source-inputs.txt

FROM container-registry.oracle.com/java/openjdk:27-oraclelinux9

COPY oracle-rpms.sh /oracle-rpms.sh

WORKDIR /opt/helidon/app
RUN mkdir -p /opt/helidon/app
COPY --from=build /workspace/h2spec/h2specd /usr/local/bin/h2specd
COPY --from=build /usr/share/h2spec-client /usr/share/h2spec-client
RUN sha256sum /usr/local/bin/h2specd > /usr/share/h2spec-client/binary.sha256 \
    && java -version > /usr/share/h2spec-client/java-version.txt 2>&1 \
    && sh /oracle-rpms.sh record /usr/share/h2spec-client/runtime-rpms.txt

CMD ["/usr/local/bin/h2specd"]
