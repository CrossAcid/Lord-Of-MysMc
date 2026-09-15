# Whisperway core mod

This directory contains the primary NeoForge mod for **Whisperway / 秘闻之径**.
It owns shared gameplay data and systems such as progression, combat, quests,
items, world interaction, persistence, multiplayer synchronization, and codex knowledge.

## Development

Requirements: Minecraft 1.21.1, NeoForge 21.1.250, and JDK 21.

On Windows PowerShell:

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

The repository may later add separately released compatibility or expansion
mods. Until a second JAR has a real independent release need, this remains a
single Gradle project to keep development and debugging simple.
