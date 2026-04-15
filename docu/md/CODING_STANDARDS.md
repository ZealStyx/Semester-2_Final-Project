# Coding Standards

## Design Principles
- Keep systems separated: each major system should live in its own class or package (e.g. audio, AI, gameplay, UI, networking).
- Follow the Single Responsibility Principle: a class should do one thing and do it well.
- Avoid spaghetti code by keeping methods short, focused, and easy to read.
- Use descriptive names for classes, methods, variables, and constants.
- Prefer composition over monolithic global classes.
- Keep code modular so changes in one system do not force changes in unrelated systems.

## Naming Conventions
- Use clear, descriptive names: `totalAmount` instead of `a`, `currentHealth` instead of `h`.
- Use consistent conventions for the project language: camelCase for Java, PascalCase for class names, UPPER_SNAKE_CASE for constants.
- Name methods for their action and intent: `calculatePathCost()`, `spawnEnemy()`, `updateNoiseLevel()`.
- Avoid abbreviations unless they are widely known and unambiguous.

## Formatting and Structure
- Use consistent indentation (usually four spaces) and spacing throughout the project.
- Use braces consistently for all control blocks, even when they are one line.
- Break long expressions into multiple lines for readability.
- Keep functions and methods small; one task per method is easier to test and understand.
- Keep classes focused and avoid large "god" classes that own too many responsibilities.

## Code Organization
- Create one class per system or subsystem.
- Group related classes in packages or folders by responsibility.
- Separate game logic from engine-specific details and utility code.
- Avoid mixing unrelated logic in the same class (for example, do not handle UI updates inside AI logic).
- Use helper classes for reusable utilities and keep high-level systems clean.

## Comments and Documentation
- Write comments that explain why code is written a certain way, not just what it does.
- Document important assumptions, invariants, and non-obvious design decisions.
- Keep comments concise and up to date; remove outdated comments during refactors.
- Use documentation comments for public APIs and systems that others will call.
- Prefer readable code over overly verbose comments.

## Avoid Global State
- Minimize the use of global variables and singletons.
- Prefer passing dependencies explicitly or using dependency injection patterns.
- Keep state encapsulated inside the systems that own it.
- Global state increases coupling and makes code harder to test.

## Error Handling
- Anticipate failure modes and handle them gracefully.
- Validate inputs and guard against invalid states early.
- Use exceptions or error results consistently, and do not ignore errors silently.
- Prefer fail-fast behavior for invalid invariants during development.

## DRY (Don't Repeat Yourself)
- Refactor duplicated logic into reusable functions or components.
- Avoid copying and pasting code across classes.
- Use utility methods, shared services, or base classes when logic is truly reusable.
- Keep repeated values in constants or configuration rather than hard-coding them multiple times.

## Automated Tools
- Use automated linting and formatting tools where available.
- Keep a consistent coding style with project-wide tooling.
- Use static analysis tools to catch common issues early.

## Project Workflow
- Start each new feature with a small, focused design: what class will own the behavior, what data it needs, what it should not do.
- Refactor early when a class grows too large or responsibilities blur.
- Write code in a way that makes future debugging and extension easy.
- Keep the codebase modular so changes in one system do not force changes in unrelated systems.
- After modifying a module, run and test that module immediately; if there is an error, fix it before moving on.
- Place all markdown files in `docu\md`, except for `README.md` which belongs at the project root.
- Create and complete a TODO/task list for every development iteration, and do not move on until all items are finished.

## Example Rule
- `SoundPropagationSystem` handles only sound graph updates and intensity computation.
- `EnemyBrain` handles only enemy perception and state transitions.
- `UIController` handles only HUD updates and input display logic.
- If a class is doing two or more of these jobs, split it into separate classes.
