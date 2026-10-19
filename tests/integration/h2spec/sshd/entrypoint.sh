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

if [ -z "${PASSWORD:-}" ]; then
    echo 'PASSWORD must be supplied by Testcontainers' >&2
    exit 1
fi
case "$PASSWORD" in
    *'
'*|*"$(printf '\r')"*)
        echo 'PASSWORD must not contain a newline' >&2
        exit 1
        ;;
esac
printf 'root:%s\n' "$PASSWORD" | chpasswd
unset PASSWORD

mkdir -p /run/sshd
ssh-keygen -A

# Use a standalone configuration so distribution includes cannot change the
# authentication or forwarding contract required by Testcontainers.
cat > /run/helidon-test-sshd.conf <<'EOF'
Port 22
PasswordAuthentication yes
PermitRootLogin yes
PermitEmptyPasswords no
KbdInteractiveAuthentication no
UsePAM no
AllowTcpForwarding remote
GatewayPorts yes
AllowAgentForwarding no
X11Forwarding no
PermitTunnel no
UseDNS no
KexAlgorithms +diffie-hellman-group14-sha1
HostKeyAlgorithms +ssh-rsa
PidFile /run/sshd/sshd.pid
EOF

OPENSSL_CONF=/etc/ssh/openssl-sshd.cnf \
    exec /usr/sbin/sshd -D -e -f /run/helidon-test-sshd.conf
