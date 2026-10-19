# DbClient Integration Test MySQL

To run this test:
```shell
mvn clean verify
```

Start the database:
```shell
docker run -d \
    --name mysql \
    -e MYSQL_DATABASE=test \
    -e MYSQL_USER=test \
    -e MYSQL_PASSWORD=mysql123 \
    -p 3306:3306 \
    container-registry.oracle.com/mysql/community-server:26.7.1
```

The fixture uses this explicit Oracle Container Registry version tag without a
digest pin. Oracle may refresh the tag contents. Record the actual image identity
and platform with validation. Oracle publishes separate aarch64 tags; selecting
and validating an arm64 image remains separate work.
To change the database version, update the fixture and this example together,
and run both local and remote MySQL tests.
