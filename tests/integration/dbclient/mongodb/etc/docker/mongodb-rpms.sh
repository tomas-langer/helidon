#!/bin/sh
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

set -eu

mode=$1
recipe=/usr/local/share/helidon-test-image/recipe
receipt=/usr/local/share/helidon-test-image
case "$mode" in
    install|discover) ;;
    *) echo 'Expected install or discover mode' >&2; exit 1 ;;
esac
if [ "$(uname -m)" != x86_64 ]; then
    echo 'The frozen MongoDB materials support Linux amd64 only' >&2
    exit 1
fi

work=$(mktemp -d /tmp/helidon-mongodb.XXXXXX)
trap 'rm -rf "$work"' EXIT
tab=$(printf '\t')
: > "$receipt/materials.sha256"

download() {
    url=$1
    destination=$2
    # Do not follow redirects, including redirects to other hosts.
    status=$(curl --fail --show-error --silent --proto '=https' \
        --connect-timeout 30 --max-time 600 --max-redirs 0 \
        --output "$destination" --write-out '%{http_code}' "$url")
    test "$status" = 200
}

while IFS="$tab" read -r url checksum fingerprint; do
    case "$url" in ''|\#*) continue ;; esac
    test "$url" = https://pgp.mongodb.com/server-7.0.asc
    download "$url" "$work/mongodb-key.asc"
    printf '%s  %s\n' "$checksum" "$work/mongodb-key.asc" > "$work/key.sha256"
    sha256sum --check "$work/key.sha256" > "$receipt/key-hash-verification.txt"
    sha256sum "$work/mongodb-key.asc" >> "$receipt/materials.sha256"
    printf '%s\n' "$fingerprint" > "$receipt/key-fingerprint.txt"
done < "$recipe/mongodb-key.lock"

mkdir "$work/rpmdb"
rpm --dbpath "$work/rpmdb" --initdb
rpm --dbpath "$work/rpmdb" --import "$work/mongodb-key.asc"
rpm --dbpath "$work/rpmdb" -qa --qf '%{NAME}\n' > "$work/keys.txt"
test "$(cat "$work/keys.txt")" = gpg-pubkey
rpm --dbpath "$work/rpmdb" -qi gpg-pubkey > "$receipt/signature-key.txt"

set --
: > "$receipt/rpm-hash-verification.txt"
: > "$receipt/rpm-signature-verification.txt"
: > "$receipt/mongodb-rpms.txt"
while IFS="$tab" read -r url checksum nevra; do
    case "$url" in ''|\#*) continue ;; esac
    case "$url" in
        https://repo.mongodb.org/yum/redhat/9/mongodb-org/7.0/x86_64/RPMS/*.rpm) ;;
        *) echo 'Unexpected MongoDB RPM URL' >&2; exit 1 ;;
    esac
    archive="$work/${url##*/}"
    download "$url" "$archive"
    printf '%s  %s\n' "$checksum" "$archive" > "$work/rpm.sha256"
    sha256sum --check "$work/rpm.sha256" >> "$receipt/rpm-hash-verification.txt"
    sha256sum "$archive" >> "$receipt/materials.sha256"
    actual=$(rpm --dbpath "$work/rpmdb" -qp \
        --qf '%{NAME}-%{EPOCHNUM}:%{VERSION}-%{RELEASE}.%{ARCH}' "$archive")
    test "$actual" = "$nevra"
    rpm --dbpath "$work/rpmdb" --checksig --verbose "$archive" > "$work/signature.txt" 2>&1
    # Digest-only success is insufficient: require an actual verified signature.
    grep -E 'Signature, key ID [[:xdigit:]]+: OK$' "$work/signature.txt" > /dev/null
    cat "$work/signature.txt" >> "$receipt/rpm-signature-verification.txt"
    printf '%s\n' "$actual" >> "$receipt/mongodb-rpms.txt"
    set -- "$@" "$archive"
done < "$recipe/mongodb-rpms.lock"
test "$#" = 8

# Import into this image only after verification with the isolated single-key DB.
# Leave Oracle repository signature verification and TLS settings unchanged.
rpm --import "$work/mongodb-key.asc"
# Resolve Oracle dependencies from configured signed repositories. Their actual
# identities are receipts, not an immutable dependency closure.
microdnf install -y --setopt=tsflags= python3 openssl shadow-utils systemd tzdata
microdnf clean all
# microdnf does not support local RPM paths. Keep RPM dependency checks and
# scriptlets enabled, and retain documentation despite the slim base defaults.
rpm -Uvh --test --includedocs "$work"/*.rpm
rpm -Uvh --includedocs "$work"/*.rpm

rpm -qa --qf '%{NAME}-%{EPOCHNUM}:%{VERSION}-%{RELEASE}.%{ARCH}\n' > "$work/rpms.txt"
LC_ALL=C sort "$work/rpms.txt" > "$receipt/rpms.txt"

mongod --version > "$receipt/mongod-version.txt"
mongosh --version > "$receipt/mongosh-version.txt"
mongodump --version > "$receipt/mongodump-version.txt"
sha256sum "$(command -v mongod)" "$(command -v mongosh)" "$(command -v mongodump)" \
    > "$receipt/executables.sha256"
sha256sum "$recipe"/* > "$receipt/source-inputs.sha256"
printf '%s\n' \
    'container-registry.oracle.com/os/oraclelinux:9-slim' \
    > "$receipt/base-image.txt"
rpm -ql mongodb-org-server mongodb-mongosh mongodb-database-tools \
    > "$receipt/mongodb-installed-files.txt"
