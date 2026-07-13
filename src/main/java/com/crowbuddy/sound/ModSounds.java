package com.crowbuddy.sound;

import com.crowbuddy.CrowBuddy;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final SoundEvent CROW_DISTRESS = SoundEvent.createVariableRangeEvent(
        CrowBuddy.id("entity.crow.distress"));

    public static void register() {
        CrowBuddy.LOGGER.info("Registering Sounds for " + CrowBuddy.MOD_ID);
        Registry.register(BuiltInRegistries.SOUND_EVENT, CROW_DISTRESS.location(), CROW_DISTRESS);
    }
}
