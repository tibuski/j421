# game421

Console implementation of the classic French dice game 421.

## Prerequisites

You need three tools installed on your Windows computer:

1. **Git** – Download from https://git-scm.com/download/win
   - Run the installer with default settings (keep "Git Bash" selected)
2. **Java 21 or newer** – Download from https://adoptium.net/
   - During install, check **"Add to PATH"**
3. **Maven 3.9 or newer** – Download from https://maven.apache.org/download.cgi
   - Unzip the `.zip` file to `C:\maven\` (or any folder)
   - Add `C:\maven\bin` to your system PATH (see below)

### Check your installation

Open **Command Prompt** (press `Win + R`, type `cmd`, press Enter) and run:

```cmd
git --version
java -version
mvn -version
```

If you see `'command' is not recognized`, the tool is not in your PATH.

### Add Maven to PATH (if needed)

1. Press `Win + S`, type **Environment Variables**, open **Edit the system environment variables**
2. Click **Environment Variables** → under **System Variables**, find and select **Path** → **Edit**
3. Click **New** and add the path to your Maven `bin` folder (e.g. `C:\maven\bin`)
4. Click **OK** on all dialogs, then **restart Command Prompt**

## Clone the project

1. Open **Command Prompt** (press `Win + R`, type `cmd`, press Enter)
2. Choose where to put the project, for example your Desktop:

```cmd
cd %USERPROFILE%\Desktop
```

3. Clone the repository:

```cmd
git clone https://github.com/tibuski/j421.git
```

4. Go into the project folder:

```cmd
cd j421\game421
```

## Build & Run

```cmd
mvn clean package
java -jar target\game421-1.0.0.jar
```

## Run without building the jar

```cmd
mvn compile exec:java -Dexec.mainClass="com.game421.Main"
```
