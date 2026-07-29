# game421

Console implementation of the classic French dice game 421.

## Prerequisites

Before you start, you need these two tools installed on your computer:

- **Java 21 or newer** – Download from https://adoptium.net/ or https://www.oracle.com/java/
- **Maven 3.9 or newer** – Download from https://maven.apache.org/download.cgi

Check if you already have them:

```bash
java -version
mvn -version
```

## Download & Play

If you have never used Git or GitHub:

1. Go to https://github.com/tibuski/j421
2. Click the green **Code** button → **Download ZIP**
3. Unzip the downloaded file
4. Open a terminal in the `j421/game421` folder
5. Run:

```bash
mvn clean package
java -jar target/game421-1.0.0.jar
```

If you have Git installed:

```bash
git clone https://github.com/tibuski/j421.git
cd j421/game421
mvn clean package
java -jar target/game421-1.0.0.jar
```

## Run directly (no jar needed)

```bash
cd j421/game421
mvn compile exec:java -Dexec.mainClass="com.game421.Main"
```
