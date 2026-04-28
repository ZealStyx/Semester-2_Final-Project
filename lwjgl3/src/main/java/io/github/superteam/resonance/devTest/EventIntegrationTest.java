package io.github.superteam.resonance.devTest;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import io.github.superteam.resonance.dialogue.DialogueSystem;
import io.github.superteam.resonance.save.SaveSystem;
import io.github.superteam.resonance.transition.RoomTransitionSystem;
import io.github.superteam.resonance.event.EventBus;
import io.github.superteam.resonance.event.EventLoader;
import io.github.superteam.resonance.event.EventContext;
import io.github.superteam.resonance.event.EventState;

/** Simple integration test scene to exercise Dialogue + Transition + Save stubs. */
public final class EventIntegrationTest extends ApplicationAdapter {
    private SpriteBatch batch;
    private DialogueSystem dialogue;
    private RoomTransitionSystem transition;
    private SaveSystem saveSystem;
    private EventBus events;

    @Override
    public void create() {
        batch = new SpriteBatch();
        dialogue = new DialogueSystem();
        transition = new RoomTransitionSystem();
        saveSystem = new SaveSystem();

        // Load events from assets/events/sample-events.json if present
        events = EventLoader.loadOrDefault("events/sample-events.json");

        // Wire a minimal EventContext with concrete systems so actions can run
        if (events != null) {
            EventState state = new EventState();
            EventContext ctx = new EventContext(
                null,
                null,
                0f,
                null,
                null,
                events,
                state,
                (text, dur) -> dialogue.showSubtitle(text, dur)
            );

            events.fire("dev-test-intro", ctx, state);
        }
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        Gdx.gl.glClearColor(0f,0f,0f,1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        dialogue.update(delta);
        transition.update(delta, dialogue);

        batch.begin();
        dialogue.render(batch);
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
    }
}
