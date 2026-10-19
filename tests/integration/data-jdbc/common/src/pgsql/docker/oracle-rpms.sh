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
receipt=$2
shift 2

case "$mode" in
    install)
        # Oracle slim images may contain either curl variant. Never install both.
        for package do
            if [ "$package" = curl ]; then
                if rpm -q curl-minimal > /dev/null 2>&1; then
                    package=curl-minimal
                fi
            fi
            printf '%s\n' "$package"
        done > /tmp/helidon-oracle-packages
        set --
        while IFS= read -r package; do
            set -- "$@" "$package"
        done < /tmp/helidon-oracle-packages
        test "$#" -gt 0
        # Keep configured repository signature checks; retain license documents.
        microdnf install -y --setopt=tsflags= "$@"
        microdnf clean all
        rm -f /tmp/helidon-oracle-packages
        ;;
    record) test "$#" -eq 0 ;;
    *) echo 'Expected install or record mode' >&2; exit 1 ;;
esac

# Record actual packages, including base refreshes; do not enforce an old closure.
rpm -qa --qf '%{NAME}-%{VERSION}-%{RELEASE}.%{ARCH}\n' > /tmp/helidon-oracle-rpms
LC_ALL=C sort /tmp/helidon-oracle-rpms > "$receipt"
rm -f /tmp/helidon-oracle-rpms
