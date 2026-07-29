#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
export JAVA_HOME="$ROOT/tools/jdk-21.0.12+8"
MAVEN_HOME="$ROOT/tools/apache-maven-3.9.9"
MAVEN_REPO="$ROOT/tools/m2-repo"
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"

if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
  echo "Portable Java was not found at: $JAVA_HOME" >&2
  echo "Copy the tools directory into this project directory first." >&2
  exit 1
fi

if [[ ! -x "$MAVEN_HOME/bin/mvn" ]]; then
  echo "Portable Maven was not found at: $MAVEN_HOME" >&2
  echo "Copy the tools directory into this project directory first." >&2
  exit 1
fi

"$MAVEN_HOME/bin/mvn" -f "$ROOT/game421/pom.xml" \
  -Dmaven.repo.local="$MAVEN_REPO" clean package
"$JAVA_HOME/bin/java" -jar "$ROOT/game421/target/game421-1.0.0.jar"
