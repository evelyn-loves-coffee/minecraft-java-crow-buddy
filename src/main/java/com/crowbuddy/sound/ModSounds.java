package com.crowbuddy.sound;

import com.crowbuddy.CrowBuddy;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.List;

public class ModSounds {
    public static final SoundEvent CROW_DISTRESS = SoundEvent.createVariableRangeEvent(
        CrowBuddy.id("entity.crow.distress"));
    public static final SoundEvent CROW_MATE = SoundEvent.createVariableRangeEvent(
        CrowBuddy.id("entity.crow.mate"));
    public static final SoundEvent CROW_EGG_LAY = SoundEvent.createVariableRangeEvent(
        CrowBuddy.id("entity.crow.egg_lay"));
    public static final SoundEvent CROW_HATCH = SoundEvent.createVariableRangeEvent(
        CrowBuddy.id("entity.crow.hatch"));
    public static final SoundEvent CROW_FLEDGLING = SoundEvent.createVariableRangeEvent(
        CrowBuddy.id("entity.crow.fledgling"));
    public static final SoundEvent CROW_BABY_FLIGHT = SoundEvent.createVariableRangeEvent(
        CrowBuddy.id("entity.crow.baby_flight"));
    public static final SoundEvent CROW_GROW = SoundEvent.createVariableRangeEvent(
        CrowBuddy.id("entity.crow.grow"));

    private static final List<SoundEvent> ALL_SOUNDS = List.of(
        CROW_DISTRESS, CROW_MATE, CROW_EGG_LAY, CROW_HATCH,
        CROW_FLEDGLING, CROW_BABY_FLIGHT, CROW_GROW
    );

    public static void register() {
        CrowBuddy.LOGGER.info("Registering Sounds for " + CrowBuddy.MOD_ID);
        for (SoundEvent sound : ALL_SOUNDS) {
            Registry.register(BuiltInRegistries.SOUND_EVENT, sound.location(), sound);
        }
    }
}
