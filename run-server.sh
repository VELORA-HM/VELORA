#!/bin/sh
# Compiles the Java backend and starts the API on http://localhost:8080
# Needs the MySQL Connector/J jar inside lib/ and your password in db.properties.
cd "$(dirname "$0")" || exit 1

JAR=$(ls lib/mysql-connector-j-*.jar 2>/dev/null | head -n 1)
if [ -z "$JAR" ]; then
  echo "Put mysql-connector-j-<version>.jar in the lib/ folder first."
  exit 1
fi
if [ ! -f db.properties ]; then
  echo "Copy db.properties.example to db.properties and set your MySQL password."
  exit 1
fi

rm -rf out && mkdir -p out
javac -d out -cp "$JAR" $(find src -name '*.java') || exit 1
exec java -cp "out:$JAR" VeloraServer
