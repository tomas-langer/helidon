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

FROM container-registry.oracle.com/os/oraclelinux:9-slim

COPY . /usr/local/share/helidon-test-image/recipe/
RUN sh /usr/local/share/helidon-test-image/recipe/oracle-rpms.sh install /image-rpms.txt \
        postgresql-13.23 postgresql-private-libs-13.23 postgresql-server-13.23
RUN mkdir -p /var/run/postgresql && \
    chown postgres:postgres /var/run/postgresql
ADD entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/entrypoint.sh
RUN mkdir -p /usr/local/share/helidon-test-image \
    && sh /usr/local/share/helidon-test-image/recipe/oracle-rpms.sh record \
        /usr/local/share/helidon-test-image/rpms.txt \
    && postgres --version > /usr/local/share/helidon-test-image/postgres-version.txt \
    && sha256sum "$(command -v postgres)" "$(command -v initdb)" \
        "$(command -v pg_ctl)" "$(command -v psql)" \
        > /usr/local/share/helidon-test-image/executables.sha256 \
    && find /usr/local/share/helidon-test-image/recipe -maxdepth 1 -type f ! -name '.*' \
        -exec sha256sum {} + > /usr/local/share/helidon-test-image/source-inputs.sha256 \
    && printf '%s\n' \
        'container-registry.oracle.com/os/oraclelinux:9-slim' \
        > /usr/local/share/helidon-test-image/base-image.txt
ENV PGDATA /var/lib/pgsql/data
USER postgres
ENTRYPOINT ["entrypoint.sh"]
STOPSIGNAL SIGINT
EXPOSE 5432
CMD ["postgres"]
