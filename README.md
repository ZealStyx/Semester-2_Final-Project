# Resonance

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

This project was generated with a template including simple application launchers and a main class extending `Game` that sets the first screen.

### Summary
Resonance is a 3D psychological horror game where the player navigates near-total darkness using sound instead of sight. The core mechanic transforms sound events into temporary spatial awareness by running Dijkstra's shortest-path propagation through a graph representation of the level. A Director AI powered by K-Means clustering continuously analyzes player behavior and adapts tension in real time (ambient audio, hallucination cues, enemy pressure, and recovery windows). Each run consists of three procedurally generated levels so no two playthroughs are identical, while each level retains handcrafted puzzle logic to ensure meaningful progression.

### The Three Pillars
```
PILLAR 1 - ACOUSTIC NAVIGATION
  Sound reveals geometry through short-lived sonar pulses
  Dijkstra pathing simulates non-line-of-sight sound travel
  Player learns space by listening, not by seeing
  Breaking or dropping objects produce sounds enemies can hear

PILLAR 2 - DIRECTOR AI ADAPTATION
  K-Means clusters player behavior into psychological states
  Dynamic pacing adjusts pressure and relief moments
  Fear intensity responds to player movement patterns

PILLAR 3 - PROCEDURAL SURVIVAL LOOP
  3 procedurally generated levels — different layout every run
  Each level has unique handcrafted puzzle logic and goal items
  Items spawn randomly across generated rooms
  Mic-enabled Clap/Shout pulse mechanic
  Stylized HUD with stress and noise indicators
  Resource-limited tools in a high-tension environment
```

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.
- `server`: A separate application without access to the `core` module.
- `shared`: A common module shared by `core` and `server` platforms.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `server:run`: runs the server application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
