# game421

Console implementation of the classic French dice game 421.

## Prerequisites

Before you start, you need these two tools installed on your computer:

- **Java 21 or newer** – Download from https://adoptium.net/ or https://www.oracle.com/java/
- **Maven 3.9 or newer** – Download from https://maven.apache.org/download.cgi

During installation, make sure to check the option **"Add to PATH"** (or **"Add to environment variables"**) so the `java` and `mvn` commands work in the terminal.

Check if they are installed (open **Command Prompt** or **PowerShell** and type):

```cmd
java -version
mvn -version
```

If you see `'java' is not recognized` or `'mvn' is not recognized`, restart your terminal after installation, or add them to PATH manually.

## Clone & Play

```cmd
git clone https://github.com/tibuski/j421.git
cd j421\game421
mvn clean package
java -jar target\game421-1.0.0.jar
```

## Run directly (no jar needed)

```cmd
cd j421\game421
mvn compile exec:java -Dexec.mainClass="com.game421.Main"
```
