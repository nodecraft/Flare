# Contributing to Flare

Thanks for helping improve Flare! This is a short guide to get you productive quickly.

## Development setup

1) Install Java 25 and use the Gradle wrapper.
2) Provide a HytaleServer dependency:
   - Drop `HytaleServer.jar` into `libs/`, or
   - Install to your local Maven repo (see `README.md`).
3) Build:
   ```bash
   ./gradlew build
   ```

## Running locally

The project includes an IntelliJ run configuration that points at `run/`. You can also copy the built JAR into your server `mods/` folder.

## Protobuf reports

Reports are zstd-compressed protobuf files (`.flarereport`). The schema lives at `src/main/proto/flare_report.proto`.
If you add fields, update the schema and regenerate code with:
```bash
./gradlew generateProto
```

## Code style

- Keep changes focused and avoid large refactors unless required.
- Prefer small, explicit methods.
- Use ASCII unless the file already contains non-ASCII text.
- Add concise comments only where the code is not self-explanatory.

## Pull requests

- Explain the problem and the approach.
- Include screenshots or logs when behavior changes.
- Mention any config or report format changes.

## Testing

If you made behavioral changes, include how you validated them (manual steps or logs are fine).
