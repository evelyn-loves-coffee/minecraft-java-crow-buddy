# Crow Buddy

A vanilla-plus Fabric mod for Minecraft 26.2+ that introduces the Crow entity. 

### Overview
Crows are unique, tameable entities that possess distinct behaviors:

* **Tamability & Diet:** Crows can be healed with various seeds, but only specialized **Black oil sunflower seeds** (a bonus drop from broken sunflowers) can tame them. They respond negatively to chocolate, which causes poisoning.
* **Swarm Intelligence:** Crows exhibit a "swarm" mechanic. Attacking a crow triggers nearby crows to join the fray. Tamed crows will defend their player with similar aggression to wolves.
* **Item Retrieval:** Similar to Allays, crows can scavenge dropped items in the world, with a specific preference for "shiny" objects like gold, diamonds, and netherite.
* **Companionship:** Owners can right-click a crow to make it sit, become neutral, and abandon its current target.
* **Canopy Nesting:** After breeding, one parent builds a temporary nest above exposed `#minecraft:leaves` within 16 blocks. The nest disappears when its baby hatches and can be trampled like turtle eggs.
* **Vanilla-Plus Integration:** Designed to adhere to the "Tiny Takeover" entity standards (Unique baby textures, golden dandelion interaction).

The custom sound events are wired to gameplay, but the checked-in `.ogg` files are placeholders pending final audio selection. Planned source links are retained in [`docs/SOUNDS.md`](docs/SOUNDS.md).

### Sound Attributions

All sounds are sourced from [Freesound.org](https://freesound.org). Attributions below follow Freesound's required format.

**CC BY 4.0**

- "Carrion Crow CLEAN 732AM 210228_0261.wav" by [klankbeeld](https://freesound.org/people/klankbeeld/) is licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). ([source](https://freesound.org/s/626378/))
- "Crow.WAV" by [inchadney](https://freesound.org/people/inchadney/) is licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). ([source](https://freesound.org/s/159426/))
- "Hatched.wav" by [pcaeldries](https://freesound.org/people/pcaeldries/) is licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). ([source](https://freesound.org/s/75167/))
- "Crow_001.mp3" by [ShadowSilhouette](https://freesound.org/people/ShadowSilhouette/) is licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). ([source](https://freesound.org/s/486534/))

**CC0 (Public Domain)**

- "Crows flying and cawing over their nests" by [etienne.leplumey](https://freesound.org/people/etienne.leplumey/) is licensed under [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/). ([source](https://freesound.org/s/534322/))
- "Small Bird Flying.wav" by [XfiXy8](https://freesound.org/people/XfiXy8/) is licensed under [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/). ([source](https://freesound.org/s/467294/))
- "egg hatching OWI.wav" by [nayahnaidoo](https://freesound.org/people/nayahnaidoo/) is licensed under [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/). ([source](https://freesound.org/s/656049/))

AI usage disclosure:
* LLMs were used heavily in development. However, LLMs were not involved in defining mod intent, behavior planning, or in generating ideas.
* Majority of project structure and implementation was created using open-weights agentic models running locally on llama.cpp.
* Creation of textures and behavior implementation created using closed-weights agentic models.
