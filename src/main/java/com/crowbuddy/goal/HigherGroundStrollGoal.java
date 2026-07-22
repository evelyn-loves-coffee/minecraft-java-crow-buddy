package com.crowbuddy.goal;

import com.crowbuddy.entity.CrowBehaviorPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

/** Random stroll that samples several valid destinations and favors open, elevated terrain. */
public class HigherGroundStrollGoal extends RandomStrollGoal {
    private static final int CANDIDATE_COUNT = 8;

    public HigherGroundStrollGoal(PathfinderMob mob, double speedModifier) {
        super(mob, speedModifier);
    }

    @Override
    protected Vec3 getPosition() {
        Vec3 best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int attempt = 0; attempt < CANDIDATE_COUNT; attempt++) {
            Vec3 candidate = LandRandomPos.getPos(this.mob, 10, 7);
            if (candidate == null) continue;
            BlockPos pos = BlockPos.containing(candidate);
            double score = CrowBehaviorPolicy.terrainPreferenceScore(
                this.mob.level().canSeeSky(pos), candidate.y - this.mob.getY());
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }
}
