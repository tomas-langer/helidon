# DbClient Integration Test Oracle

To run this test:
```shell
mvn clean verify
```

Start the database:
```shell
docker run -d \
  --name oracledb \
  -e ORACLE_PWD=oracle123 \
  -p 1521:1521 \
  container-registry.oracle.com/database/free:23.26.3.0-lite
```

The fixture uses this explicit Oracle Container Registry version tag without a
digest pin. Oracle may refresh the tag contents. Record the actual image identity
and platform with validation. To change the database version, update the fixture
and this example together, and run both local and remote Oracle tests.
