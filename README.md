# Crow Buddy

A vanilla-plus Fabric mod for Minecraft 26.2+ that introduces the Crow entity. 

### Overview
Crows are unique, tameable entities that spawn in biomes mirroring real-life habitats. They possess distinct behaviors:

* **Tamability & Diet:** Crows can be healed with various seeds, but only specialized **Black oil sunflower seeds** (a bonus drop from broken sunflowers) can tame them. They respond negatively to chocolate, which causes poisoning.
* **Swarm Intelligence:** Crows exhibit a "swarm" mechanic. Attacking a crow triggers nearby crows to join the fray. Tamed crows will defend their player with similar aggression to wolves.
* **Item Retrieval:** Similar to Allays, crows can scavenge dropped items in the world, with a specific preference for "shiny" objects like gold, diamonds, and netherite.
* **Vanilla-Plus Integration:** Designed to adhere to the "Tiny Takeover" entity standards (Unique baby textures, golden dandelion interaction).

The custom sound events are wired to gameplay, but the checked-in `.ogg` files are placeholders pending final audio selection. Planned source links are retained in [`docs/SOUNDS.md`](docs/SOUNDS.md).

AI usage disclosure:
* LLMs were not involved in defining mod intent, nor planned behaviors.
* Majority of project structure and implementation was created using open-weights agentic models running locally on llama.cpp.
* Creation of textures and behavior implementation created using closed-weights agentic models.
