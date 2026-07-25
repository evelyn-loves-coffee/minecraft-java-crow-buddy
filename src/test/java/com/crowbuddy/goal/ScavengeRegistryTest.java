package com.crowbuddy.goal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScavengeRegistryTest {
    @Test
    void claimsAreAtomicAndOwnerScoped() throws Exception {
        var constructor = ScavengeRegistry.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        ScavengeRegistry registry = constructor.newInstance();
        assertTrue(registry.claim(10, 1));
        assertTrue(registry.claim(10, 1));
        assertFalse(registry.claim(10, 2));
        registry.releaseAll(1);
        assertTrue(registry.claim(10, 2));
    }
}
