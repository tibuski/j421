# game421

Console implementation of the classic French dice game 421, with a graphical
table dressed as a corner of the Yawning Portal inn in Waterdeep (Forgotten
Realms): a candle-lit taproom, a green felt gaming table, animated bone dice
that tumble and settle when cast, a score ledger with wax seals and gold
coins, and a bard narrating the match. Your opponent is drawn from the
setting — Volo, Durnan, Mirt, or Laeral Silverhand.

## Prerequisites

Git is the only tool that needs to be installed globally. Java and Maven are
portable tools kept inside this development directory, so they do not need to
be installed globally or added to the system PATH.

- **Git** – Download from https://git-scm.com/download/win on Windows. Use the
  default installer options.
- **Portable Java 21** – Place the JDK directory at
  `tools\jdk-21.0.12+8` on Windows or `tools/jdk-21.0.12+8` on GNOME.
- **Portable Maven 3.9.9** – Place Maven at
  `tools\apache-maven-3.9.9` on Windows or `tools/apache-maven-3.9.9` on GNOME.

The `tools/` directory is intentionally ignored by Git because it contains
large platform-specific binaries. Copy it from the development machine, or
download and unpack matching portable JDK and Maven distributions there.

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

4. Go into the repository folder:

```cmd
cd j421
```

## Build & Run on Windows

Double-click `run-game.cmd`, or open Command Prompt in the repository folder
and run:

```cmd
run-game.cmd
```

The JAR opens the graphical game table: name your adventurer, take a seat at
the Yawning Portal, and click the dice to keep them between casts. The
interface uses Java Swing, which is included with Java and works on Windows
and Linux desktops such as GNOME.

The script uses the local JDK, Maven, and Maven dependency cache from `tools/`.
It does not change your Windows system environment.

## Build & Run on GNOME

Open **Terminal** in the repository folder, make the script executable once,
then run it:

```bash
chmod +x run-game.sh
./run-game.sh
```

The script uses the local tools and dependency cache from `tools/` and does not
require a system-wide Java or Maven installation.

### Play from the source code

You can also launch the interface directly with the local tools.

```cmd
tools\apache-maven-3.9.9\bin\mvn.cmd -f game421\pom.xml -Dmaven.repo.local=tools\m2-repo compile exec:java -Dexec.mainClass="com.game421.Main"
```

The source is in `game421\src\main\java`. The main UI is in
`game421\src\main\java\com\game421\ui\SwingGame.java`, so you can edit it and run the
command again to see your changes.

On GNOME, use the equivalent command with `/` path separators:

```bash
tools/apache-maven-3.9.9/bin/mvn -f game421/pom.xml -Dmaven.repo.local=tools/m2-repo compile exec:java -Dexec.mainClass="com.game421.Main"
```

## Console Mode

```cmd
tools\jdk-21.0.12+8\bin\java.exe -jar game421\target\game421-1.0.0.jar --console
```

`--console` starts the original terminal version instead of the graphical UI.
