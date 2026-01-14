# HytaleServer JAR

Place the `HytaleServer.jar` file in this directory.

The build will automatically use this local JAR file instead of trying to resolve it from Maven repositories.

If you don't have the JAR file yet, you can:
1. Download it from your Hytale server installation
2. Or install it to your local Maven repository using:
   ```bash
   mvn install:install-file -Dfile=HytaleServer.jar -DgroupId=com.hypixel.hytale -DartifactId=HytaleServer-parent -Dversion=1.0-SNAPSHOT -Dpackaging=jar
   ```
