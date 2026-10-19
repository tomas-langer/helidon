#
# Copyright (c) 2024, 2026 Oracle and/or its affiliates.
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
RUN echo "${GO_SUM_SHA256}  go.sum" | sha256sum --check \
    && mkdir -p /usr/share/h2spec \
    && sha256sum go.mod go.sum > /usr/share/h2spec/go-inputs.sha256 \
    && go mod download \
    && go mod verify \
    && go build -mod=readonly -trimpath \
        -ldflags "-X main.VERSION=${VERSION} -X main.COMMIT=${COMMIT}" ./cmd/h2spec \
    && sha256sum --check /usr/share/h2spec/go-inputs.sha256 \
    && cp LICENSE /usr/share/h2spec/LICENSE \
    && mkdir -p /usr/share/h2spec/licenses/modules /usr/share/h2spec/licenses/go \
    && cp /usr/share/doc/golang/LICENSE /usr/share/doc/golang/PATENTS /usr/share/h2spec/licenses/go \
    && (cd "$(go env GOMODCACHE)" \
        && find . -type f \( -iname 'LICENSE*' -o -iname 'NOTICE*' -o -iname 'COPYING*' \) \
            -exec cp --parents -t /usr/share/h2spec/licenses/modules {} +) \
    && go version > /usr/share/h2spec/go-version.txt \
    && go version -m h2spec > /usr/share/h2spec/binary-build.txt \
    && sh /oracle-rpms.sh record /usr/share/h2spec/build-rpms.txt \
    && printf 'VERSION=%s\nCOMMIT=%s\nSOURCE_SHA256=%s\nGOPROXY=%s\nGOSUMDB=%s\nGOTOOLCHAIN=%s\nCGO_ENABLED=%s\n' \
        "${VERSION}" "${COMMIT}" "${SOURCE_SHA256}" "${GOPROXY}" "${GOSUMDB}" \
        "${GOTOOLCHAIN}" "${CGO_ENABLED}" > /usr/share/h2spec/source-inputs.txt

FROM container-registry.oracle.com/os/oraclelinux:9-slim
COPY oracle-rpms.sh /oracle-rpms.sh

ARG PORT=8080
ARG HOST=localhost
ENV PORT=${PORT}
ENV HOST=${HOST}
COPY --from=build /workspace/h2spec/h2spec /usr/local/bin/h2spec
COPY --from=build /usr/share/h2spec /usr/share/h2spec
RUN sha256sum /usr/local/bin/h2spec > /usr/share/h2spec/binary.sha256 \
    && sh /oracle-rpms.sh record /usr/share/h2spec/runtime-rpms.txt
CMD ["/usr/local/bin/h2spec", "-h", "${HOST}", "-p", "${PORT}"]
