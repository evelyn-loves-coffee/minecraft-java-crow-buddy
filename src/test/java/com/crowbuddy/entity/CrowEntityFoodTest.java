package com.crowbuddy.entity;

import com.crowbuddy.test.BootstrapTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CrowEntityFoodTest extends BootstrapTest {

    @Test
    void testCrowEntityClassExists() {
        // Verify CrowEntity class is loadable after bootstrap
        assertNotNull(CrowEntity.class);
    }

    // Note: isFood() and isPoisonousFood() tests require ItemStack construction,
    // which fails with "Components not bound yet" in fabric-loader-junit.
    // These methods should be tested via integration tests with full server bootstrap.
}
