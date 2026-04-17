# Project Proposal

| Group No. | |
|---|---|
| Project/Application Title: | Resonance - Adaptive 3D Horror Through Acoustic Navigation |
| What the application will do: | Resonance is a 3D horror game that uses sound-based navigation and adaptive enemy behavior to guide the player through dark, procedurally generated levels. The system reveals the environment through sonar-like audio cues, reacts to player movement patterns, and adjusts tension dynamically. |
| Target users: | Gamers who enjoy psychological horror, immersive audio-driven gameplay, and intelligent adaptive systems. Also suitable for course evaluators interested in algorithm-driven interactive systems. |
| Main problem to solve: | The game solves the challenge of creating an immersive, non-visual navigation experience using sound, while maintaining fair and adaptive pacing through intelligent enemy and atmosphere control. It also addresses the need for meaningful procedural variation without sacrificing narrative or gameplay structure. |
| Proposed algorithm: | Dijkstra's shortest-path algorithm for sound propagation, supported by K-Means clustering for live player behavior classification. |
| What the algorithm will do in the system: | Dijkstra will compute weighted acoustic paths through the level graph to determine how sound spreads around obstacles and through rooms, controlling sonar reveal intensity and enemy hearing. K-Means will classify the player's behavioral state (e.g., STEALTH, PANIC, FROZEN) from recent motion and interaction features, enabling the adaptive Director AI to choose appropriate tension responses. |
| Why this algorithm is appropriate: | Dijkstra is appropriate because it models non-line-of-sight sound travel efficiently on a level graph, balancing realism and performance. K-Means is appropriate for quickly clustering real-time player behavior into a small set of actionable states, which is ideal for adaptive game pacing without requiring expensive training or complex models. |
| Tools/technology to be used: | Java with libGDX for desktop gameplay, Gradle for build management, and optional networking support for audio/event handling. The game will also use standard game audio tools and data logging for tuning and evaluation. |
