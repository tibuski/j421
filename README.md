# game421

Console implementation of the classic French dice game 421.

## Prerequisites

- Java 21+
- Maven 3.9+

## Clone & Launch

```bash
git clone git@github.com:tibuski/j421.git
cd j421/game421
mvn clean package
java -jar target/game421-1.0.0.jar
```

Alternatively, run directly with Maven:

```bash
mvn compile exec:java -Dexec.mainClass="com.game421.Main"
```
