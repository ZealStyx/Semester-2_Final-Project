package io.github.superteam.resonance.particles;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import java.util.ArrayList;

public class ParticleEffectBundle {
    public int presetVersion = 1;
    public String name = "DefaultEffectBundle";
    public ArrayList<Entry> entries = new ArrayList<>();

    public static ParticleEffectBundle createDefault() {
        ParticleEffectBundle bundle = new ParticleEffectBundle();
        Entry entry = new Entry();
        entry.presetName = "default";
        bundle.entries.add(entry);
        bundle.sanitize();
        return bundle;
    }

    public static ParticleEffectBundle load(FileHandle file) {
        Json json = new Json();
        if (!file.exists()) {
            ParticleEffectBundle bundle = createDefault();
            bundle.save(file);
            return bundle;
        }

        ParticleEffectBundle bundle = json.fromJson(ParticleEffectBundle.class, file);
        if (bundle == null) {
            return createDefault();
        }

        bundle.sanitize();
        return bundle;
    }

    public void save(FileHandle file) {
        sanitize();
        Json json = new Json();
        if (file.parent() != null) {
            file.parent().mkdirs();
        }
        file.writeString(json.prettyPrint(this), false);
    }

    public void sanitize() {
        presetVersion = Math.max(1, presetVersion);

        if (name == null || name.isBlank()) {
            name = "DefaultEffectBundle";
        }

        if (entries == null) {
            entries = new ArrayList<>();
        }

        ArrayList<Entry> sanitizedEntries = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            if (entry == null) {
                continue;
            }
            entry.sanitize();
            sanitizedEntries.add(entry);
        }

        if (sanitizedEntries.isEmpty()) {
            Entry entry = new Entry();
            entry.presetName = "default";
            sanitizedEntries.add(entry);
        }

        entries = sanitizedEntries;
    }

    public static final class Entry {
        public String presetName = "default";
        public String preset = "";
        public float[] offset = {0f, 0f, 0f};
        public float[] rotation = {0f, 0f, 0f};
        public float scale = 1f;

        public String getResolvedPresetName() {
            if (presetName != null && !presetName.isBlank()) {
                return presetName;
            }
            if (preset != null && !preset.isBlank()) {
                return preset;
            }
            return "default";
        }

        public void sanitize() {
            if (presetName == null) {
                presetName = "";
            }
            if (preset == null) {
                preset = "";
            }
            if (presetName.isBlank() && !preset.isBlank()) {
                presetName = preset;
            }
            if (preset.isBlank() && !presetName.isBlank()) {
                preset = presetName;
            }

            offset = normalizeArray(offset, 3, new float[]{0f, 0f, 0f});
            rotation = normalizeArray(rotation, 3, new float[]{0f, 0f, 0f});
            scale = Math.max(0.001f, scale);
        }
    }

    private static float[] normalizeArray(float[] values, int size, float[] fallback) {
        float[] normalized = new float[size];
        float[] source = values;
        if (source == null || source.length < size) {
            source = fallback;
        }

        for (int i = 0; i < size; i++) {
            normalized[i] = source[i];
        }
        return normalized;
    }
}
