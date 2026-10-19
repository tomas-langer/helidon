# SSH helper for h2spec tests

Validated with Oracle Linux 9, OpenSSH 9.9p1 and Testcontainers 1.21.4 on
Linux x86_64 with Podman 5.8.2: all 146 h2spec server cases passed with no skips.
This validates the SSH helper; the h2spec image itself must also be available.
Fresh-VM bootstrap and other architectures require separate validation.

This image provides the SSH reverse forwarding used by Testcontainers when
h2spec containers connect to a server running in the test JVM. It is an ephemeral
test helper. Testcontainers supplies a random `PASSWORD`, authenticates as root
on port 22 and requests remote TCP forwarding. Root is needed inside the
container; this does not require a privileged container or host SSH changes.

The image uses Oracle Linux 9 and the supported `openssh-server` package from its
configured Oracle repositories. It generates host keys at container startup and
has no default password. Testcontainers 1.21.4 bundles an older SSH client that
requires SHA-1 key exchange and an RSA/SHA-1 host signature. The helper adds
`diffie-hellman-group14-sha1` and `ssh-rsa` to OpenSSH's defaults solely for that
client. These settings apply inside the disposable container, not to host SSH,
TLS verification, or system-wide cryptographic policy. Recheck whether they can
be removed when upgrading Testcontainers.

Oracle Linux also disables RSA/SHA-1 signing through OpenSSL. Only the final
sshd process receives `OPENSSL_CONF=/etc/ssh/openssl-sshd.cnf`.
That configuration includes the existing Oracle system configuration and
overrides only `rh-allow-sha1-signatures` in its `evp_properties` section.
It retains other crypto settings and does not enable a legacy provider or
lower the OpenSSL security level. Package installation, host-key generation
and other image processes do not receive this override.

The SSH configuration enables remote forwarding on wildcard addresses so other
test containers can reach the forwarded ports. It is unsuitable as a general
purpose SSH service.

Oracle-hosted images use version tags without digest pins and may refresh.
Oracle dependencies are installed from the configured signed repositories without
a frozen whole-system closure. The image records the actual installed RPM
inventory; refreshed base packages and dependencies do not fail an old-inventory
comparison. Record the image ID, platform and receipts with each validation.

## Build and bootstrap

Build the image on each VM from this directory using the approved Oracle
registry and package endpoints:

```shell
podman build --pull=always --no-cache --tag localhost/helidon-test/sshd:ol9-5 .
podman image inspect localhost/helidon-test/sshd:ol9-5
```

Build into the private image store exposed by the subsequent
Maven/Testcontainers workload's container API. A helper built in another store
is not available to that workload merely because its tag matches.

No registry publication or transfer from another machine is required. A fresh
VM needs working TLS trust and access to the Oracle registry and package
repositories. Preserve registry authentication and TLS verification.

Record the source revision, Oracle base version tag, build date, architecture, resulting
image ID and `/image-rpms.txt` with the VM's provisioning receipt. The base image
uses the Oracle Linux `9-slim` version tag; the actual RPM set is recorded.
This recipe does not promise byte-for-byte reproducible image builds. Do not embed registry credentials or
test passwords in the receipt.

## Select the image

The default Testcontainers image is unchanged unless explicitly selected.
After building the helper in the workload's private store, run the focused test
from the repository root with a supported JDK and required Helidon artifacts:

```shell
mvn verify -Ptests -pl :helidon-tests-integration-h2spec \
    -Dhelidon.tests.h2spec.sshd.image=localhost/helidon-test/sshd:ol9-5 \
    '-Dfailsafe.argLine=-Xmx1024m -Dfile.encoding=UTF-8 --enable-native-access=ALL-UNNAMED'
```

For a whole-repository build, add the same property to the normal root command:

```shell
mvn clean install -Ptests \
    -Dhelidon.tests.h2spec.sshd.image=localhost/helidon-test/sshd:ol9-5
```

The property activates this module's `h2spec-sshd-image` profile. Only its
Failsafe test JVM receives `TESTCONTAINERS_IMAGE_SUBSTITUTOR` and
`HELIDON_TEST_SSHD_IMAGE`; the client h2spec module and other modules do not.
Without the property the profile is inactive. Keep any other JVM and build
options required by the target environment on these commands.

Do not globally export `TESTCONTAINERS_IMAGE_SUBSTITUTOR=H2SpecSshdImageSubstitutor`
or `HELIDON_TEST_SSHD_IMAGE`; clear any previous exports before running a root
build. The selector class exists only on this module's test classpath.

The custom image retains its own name; it is not retagged as the upstream
`testcontainers/sshd:1.3.0` image. Do not combine this selection with a
Testcontainers Hub image prefix or a separate SSH image override: Testcontainers
applies its default substitutions before this class. Compatibility must be
established by running the real h2spec host-port forwarding tests under the
target runtime. Image build success alone does not establish that compatibility.

## Refresh

Periodically check Oracle Linux base updates and OpenSSH security advisories.
Review the Oracle base version tag, rebuild without the package cache, record
the new RPM inventory, and validate host-port forwarding with h2spec before
adopting the result. Give a changed recipe a new local image tag and update the
provisioning receipt and workload selection together. Keep the previous receipt
to explain differences between VM builds. No host SSH, TLS or global crypto
policy changes are part of this process.

## Oracle packages and refresh

`oracle-rpms.sh` installs only the packages named in the Dockerfile and lets the
configured Oracle repositories resolve their dependencies. It preserves
repository signature verification and installs RPM documentation and license
notices. The helper chooses the already installed curl variant, avoiding a
conflict between `curl` and `curl-minimal`. Runtime-only stages record their
actual inventory without requiring a historical base package set.

The resulting inventories are receipts, not immutable material locks. Oracle
base images and package dependencies may refresh; rebuild without cached recipe
layers, inspect the actual image ID, package inventory and tool versions, then
run the consuming tests before adopting a refreshed image. A successful build
does not establish integration coverage. Fresh-VM bootstrap and arm64 remain
unvalidated. Repository availability is required; these recipes do not promise
offline or byte-identical rebuilds.

External source revisions, archive/module checksums and signature requirements
remain fixed. Change those inputs only through deliberate review and validation.
These recipes publish no image. Registry publication requires an approved route.
Non-Oracle registry images require fixed version and digest selection.
