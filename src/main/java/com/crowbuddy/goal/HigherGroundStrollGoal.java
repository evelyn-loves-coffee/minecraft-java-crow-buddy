package com.crowbuddy.goal;

import com.crowbuddy.entity.CrowBehaviorPolicy;
import com.crowbuddy.entity.CrowEntity;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

/** Random destination selection remains goal policy; all movement is delegated. */
public final class HigherGroundStrollGoal extends Goal {
    private static final int CANDIDATE_COUNT = 8;
    private static final int INTERVAL = 120;
    private final CrowEntity crow;
    private final double speed;
    private Vec3 target;
    public HigherGroundStrollGoal(CrowEntity crow, double speed) { this.crow = crow; this.speed = speed; setFlags(EnumSet.of(Flag.MOVE)); }
    @Override public boolean canUse() { if (crow.isInSittingPose() || crow.isOrderedToSit() || crow.getRandom().nextInt(INTERVAL) != 0) return false; target = chooseTarget(); return target != null; }
    @Override public void start() { crow.getCrowNavigator().navigateTo(crow, target, speed); }
    @Override public boolean canContinueToUse() { return !crow.isInSittingPose() && !crow.isOrderedToSit() && crow.getCrowNavigator().hasPath() && !crow.getCrowNavigator().hasReachedTarget(crow, 1.0); }
    @Override public void stop() { crow.getCrowNavigator().clear(crow); target = null; }
    private Vec3 chooseTarget() {
        Vec3 best = null; double bestScore = Double.NEGATIVE_INFINITY;
        for (int attempt = 0; attempt < CANDIDATE_COUNT; attempt++) {
            Vec3 candidate = LandRandomPos.getPos(crow, 10, 7); if (candidate == null) continue;
            BlockPos pos = BlockPos.containing(candidate);
            double score = CrowBehaviorPolicy.terrainPreferenceScore(crow.level().canSeeSky(pos), candidate.y - crow.getY());
            if (score > bestScore) { best = candidate; bestScore = score; }
        }
        return best;
    }
}
