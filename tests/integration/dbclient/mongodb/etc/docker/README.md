# MongoDB test image materials

This amd64-only recipe preserves MongoDB 7 and the existing test entrypoint. It
selects eight official RPMs: MongoDB server components `7.0.45-1.el9`, mongosh
`2.13.0-1.el8`, and database tools `100.19.1-1`. `mongodb-rpms.lock` fixes each
HTTPS URL, SHA-256 and exact epoch-inclusive NEVRA. `mongodb-key.lock` fixes the
MongoDB 7 signing key's URL and SHA-256 and records its fingerprint
`E58830201F7DD82CD808AA84160D26BB1785BA38`.

The helper follows no download redirects, verifies every downloaded archive's
hash and package identity, and requires a valid RPM signature in an isolated
RPM database containing only the hash-pinned MongoDB key. Digest-only RPM
checks do not qualify. It then imports that verified key into the image and
installs Oracle dependencies using microdnf. The eight verified local MongoDB
RPMs are installed together with ordinary `rpm -Uvh`, preceded by a test
transaction. Dependency checks and scriptlets remain enabled; documentation is
explicitly included. This avoids microdnf's lack of local RPM installation.
Oracle repository signature and TLS settings remain unchanged. No MongoDB repository metadata is
fetched during the build. RPM documentation and license notices remain installed.

Oracle-hosted images use version tags without digest pins and may refresh.
Oracle dependencies are installed from the configured signed repositories without
a frozen whole-system closure. The image records the actual installed RPM
inventory; refreshed base packages and dependencies do not fail an old-inventory
comparison. Record the image ID, platform and receipts with each validation.

## Oracle dependency refresh

The normal Dockerfile installs `python3`, `openssl`, `shadow-utils`, `systemd`
and `tzdata` from the configured signed Oracle repositories, allowing their
dependencies and base packages to refresh. It records the complete actual
epoch-inclusive RPM inventory without comparing against an old closure.
The eight external MongoDB packages and key remain fixed and verified.

`Dockerfile.discovery` is retained as a receipt-inspection convenience using
exactly the same package policy and external material checks as the normal
Dockerfile. It is not an alternative that relaxes verification:

```shell
docker build --pull --no-cache --file Dockerfile.discovery --tag helidon-test-mongodb:discovery .
```

Inspect the receipts and license files, then validate through the normal
Testcontainers local/remote suite. Discovery alone is not integration coverage.

## Receipts, refresh and access

`/usr/local/share/helidon-test-image` retains the full RPM inventory,
`mongod`, `mongosh` and `mongodump` versions, executable hashes, input checksums,
base reference, RPM hash/signature verification logs, signing-key information,
and installed MongoDB file paths. The copied `recipe` directory retains the
material locks and recipe sources. Record the immutable image ID, platform,
recipe revision and executed test reports alongside these receipts. Local tags
are mutable and are not registry manifest digests; these recipes publish no image.

Builds require approved verified HTTPS access to Oracle Container Registry,
Oracle Linux repositories, `repo.mongodb.org` and `pgp.mongodb.com`, or an approved
retained-material route. Do not weaken TLS or signature checks or reuse an
unapproved mirror or image transfer. Oracle dependencies resolve from the configured signed repositories at build
time. Actual inventories are receipts, not Oracle archive hash locks; neither
byte-identical images nor offline rebuilds are promised. Long-term availability
of the fixed external payloads requires approved material retention.

Refresh the eight payload URLs, hashes, identities and signing key deliberately,
keeping MongoDB major 7 unless an upgrade is intended. Review the Oracle base
version tag and recorded dependency inventory, then validate the
normal recipe and both local and remote MongoDB integration paths. Linux amd64
is the only selected architecture; arm64 and fresh-VM bootstrap remain separate
acceptance work. There is no automatic alternate architecture selection.
