package io.github.superteam.resonance.particles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import java.util.ArrayList;
import java.util.List;

public final class ParticlePresetStore {
    private static final String LOCAL_PRESET_DIR = "particle-presets";
    private static final String LOCAL_EFFECT_BUNDLE_DIR = "particle-bundles";

    public static class ValidationResult {
        public boolean valid;
        public List<String> warnings = new ArrayList<>();
        public List<String> errors = new ArrayList<>();
    }

    private ParticlePresetStore() {
    }

    public static void ensureLocalPresetFromInternal(String presetName, String internalPath) {
        FileHandle localFile = getLocalPresetFile(presetName);
        if (localFile.exists()) {
            return;
        }

        FileHandle internalFile = Gdx.files.internal(internalPath);
        if (internalFile.exists()) {
            localFile.parent().mkdirs();
            localFile.writeString(internalFile.readString(), false);
        }
    }

    public static ParticleDefinition loadLocalPreset(String presetName, ParticleDefinition fallback) {
        FileHandle localFile = getLocalPresetFile(presetName);
        if (localFile.exists()) {
            return ParticleDefinition.load(localFile);
        }

        if (fallback != null) {
            fallback.sanitize();
            return fallback;
        }
        return ParticleDefinition.createDefault();
    }

    public static void saveLocalPreset(String presetName, ParticleDefinition definition) {
        FileHandle localFile = getLocalPresetFile(presetName);
        definition.save(localFile);
    }

    public static void ensureLocalEffectBundleFromInternal(String bundleName, String internalPath) {
        FileHandle localFile = getLocalEffectBundleFile(bundleName);
        if (localFile.exists()) {
            return;
        }

        FileHandle internalFile = Gdx.files.internal(internalPath);
        if (internalFile.exists()) {
            localFile.parent().mkdirs();
            localFile.writeString(internalFile.readString(), false);
        }
    }

    public static ParticleEffectBundle loadLocalEffectBundle(String bundleName, ParticleEffectBundle fallback) {
        FileHandle localFile = getLocalEffectBundleFile(bundleName);
        if (localFile.exists()) {
            return ParticleEffectBundle.load(localFile);
        }

        if (fallback != null) {
            fallback.sanitize();
            return fallback;
        }

        return ParticleEffectBundle.load(localFile);
    }

    public static void saveLocalEffectBundle(String bundleName, ParticleEffectBundle bundle) {
        FileHandle localFile = getLocalEffectBundleFile(bundleName);
        bundle.save(localFile);
    }

    public static ValidationResult validate(ParticleDefinition definition) {
        ValidationResult result = new ValidationResult();
        if (definition == null) {
            result.errors.add("ParticleDefinition is null");
            result.valid = false;
            return result;
        }

        if (definition.emissionRate <= 0f && !definition.burstMode) {
            result.errors.add("emissionRate is 0 and burstMode is false, emitter will emit nothing");
        }

        if (definition.maxParticles < definition.burstCount) {
            result.warnings.add(
                "maxParticles (" + definition.maxParticles + ") is less than burstCount (" + definition.burstCount + ")"
            );
        }

        if (definition.collisionResponse == null) {
            result.errors.add("collisionResponse must not be null");
        }

        if (definition.softParticleFadeDistance <= 0f) {
            result.errors.add("softParticleFadeDistance must be greater than 0");
        }

        if (definition.gravityScaleMin < 0f || definition.gravityScaleMax < definition.gravityScaleMin) {
            result.errors.add("gravityScaleMin/Max must be non-negative and gravityScaleMax >= gravityScaleMin");
        }

        if (definition.warmupTimeStep <= 0f) {
            result.errors.add("warmupTimeStep must be greater than 0");
        }

        if (definition.lodDistanceMin < 0f || definition.lodDistanceMax < definition.lodDistanceMin) {
            result.errors.add("lodDistanceMin/Max must satisfy 0 <= min <= max");
        }

        if (definition.colorGradient != null) {
            for (int i = 0; i < definition.colorGradient.length; i++) {
                float[] stop = definition.colorGradient[i];
                if (stop == null || stop.length != 5) {
                    result.errors.add("colorGradient stop at index " + i + " must have 5 values: [ageRatio, r, g, b, a]");
                }
            }
        }

        validateCurve(result, "emissiveCurve", definition.emissiveCurve);
        validateCurve(result, "rotationCurve", definition.rotationCurve);
        validateCurve(result, "sizeCurve", definition.sizeCurve);

        result.valid = result.errors.isEmpty();
        return result;
    }

    private static void validateCurve(ValidationResult result, String curveName, float[][] curve) {
        if (curve == null) {
            return;
        }

        for (int i = 0; i < curve.length; i++) {
            float[] stop = curve[i];
            if (stop == null || stop.length != 2) {
                result.errors.add(curveName + " stop at index " + i + " must have 2 values: [ageRatio, value]");
            }
        }
    }

    public static ValidationResult validate(ParticleEffectBundle bundle) {
        ValidationResult result = new ValidationResult();
        if (bundle == null) {
            result.errors.add("ParticleEffectBundle is null");
            result.valid = false;
            return result;
        }

        if (bundle.name == null || bundle.name.isBlank()) {
            result.errors.add("Effect bundle name is blank");
        }

        if (bundle.entries == null || bundle.entries.isEmpty()) {
            result.errors.add("Effect bundle does not contain any entries");
        } else {
            for (int i = 0; i < bundle.entries.size(); i++) {
                ParticleEffectBundle.Entry entry = bundle.entries.get(i);
                if (entry == null) {
                    result.errors.add("Effect bundle entry at index " + i + " is null");
                    continue;
                }

                String presetName = entry.getResolvedPresetName();
                if (presetName.isBlank()) {
                    result.errors.add("Effect bundle entry at index " + i + " has no preset name");
                }

                if (entry.offset == null || entry.offset.length != 3) {
                    result.errors.add("Effect bundle entry at index " + i + " must have an offset with 3 values");
                }

                if (entry.rotation == null || entry.rotation.length != 3) {
                    result.errors.add("Effect bundle entry at index " + i + " must have a rotation with 3 values");
                }

                if (entry.scale <= 0f) {
                    result.errors.add("Effect bundle entry at index " + i + " must have a positive scale");
                }
            }
        }

        result.valid = result.errors.isEmpty();
        return result;
    }

    public static String[] listLocalPresetNames() {
        FileHandle presetDirectory = Gdx.files.local(LOCAL_PRESET_DIR);
        if (!presetDirectory.exists() || !presetDirectory.isDirectory()) {
            return new String[] {"default"};
        }

        FileHandle[] files = presetDirectory.list((dir, name) -> name.endsWith(".json"));
        List<String> names = new ArrayList<>();
        for (FileHandle file : files) {
            String name = file.nameWithoutExtension();
            if (!name.isBlank()) {
                names.add(name);
            }
        }

        if (names.isEmpty()) {
            return new String[] {"default"};
        }
        return names.toArray(new String[0]);
    }

    public static FileHandle getLocalPresetFile(String presetName) {
        String safeName = sanitizePresetName(presetName);
        return Gdx.files.local(LOCAL_PRESET_DIR + "/" + safeName + ".json");
    }

    public static FileHandle getLocalEffectBundleFile(String bundleName) {
        String safeName = sanitizePresetName(bundleName);
        return Gdx.files.local(LOCAL_EFFECT_BUNDLE_DIR + "/" + safeName + ".json");
    }

    public static String[] listLocalEffectBundleNames() {
        FileHandle bundleDirectory = Gdx.files.local(LOCAL_EFFECT_BUNDLE_DIR);
        if (!bundleDirectory.exists() || !bundleDirectory.isDirectory()) {
            return new String[] {"default"};
        }

        FileHandle[] files = bundleDirectory.list((dir, name) -> name.endsWith(".json"));
        List<String> names = new ArrayList<>();
        for (FileHandle file : files) {
            String name = file.nameWithoutExtension();
            if (!name.isBlank()) {
                names.add(name);
            }
        }

        if (names.isEmpty()) {
            return new String[] {"default"};
        }
        return names.toArray(new String[0]);
    }

    private static String sanitizePresetName(String presetName) {
        if (presetName == null || presetName.isBlank()) {
            return "default";
        }

        StringBuilder sanitized = new StringBuilder();
        for (int i = 0; i < presetName.length(); i++) {
            char character = presetName.charAt(i);
            if (Character.isLetterOrDigit(character) || character == '_' || character == '-') {
                sanitized.append(character);
            }
        }

        if (sanitized.length() == 0) {
            return "default";
        }
        return sanitized.toString();
    }
}
