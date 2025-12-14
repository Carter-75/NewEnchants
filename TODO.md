# NewEnchants – Work Breakdown

## 1. Combat Enchantments
- [ ] Finish sword/axe/mace logic beyond prototypes (Chronoblade Edge ➜ Shock Lancer).
	- [x] Impact Bloom fall-charge, Radiant Execution pulses, Warcaller kill shout hooks (2025-12-13).
- [ ] Add proper cooldown overlays, stack resets, and fail-safe guards for teleport/dash effects.
- [ ] Hook up mob-type filters (e.g., undead, wither, boss checks) to avoid string-based detection.
- [ ] Add particles + sounds matching the enchantment spec.
- [ ] Unit-test damage and status-application helpers.

## 2. Ranged Enchantments
- [ ] Expand `RangedEventHooks` to spawn actual companion projectiles (Tempest Volley, Aether Salvo, etc.).
	- [x] Tempest Volley stacking, damage boosts, gust release + bow haste feedback (2025-12-13).
	- [x] Starfall Draw falling shard damage + NV bonus (2025-12-13).
	- [x] Aether Salvo airborne spectral bolts + levitation finisher (2025-12-13).
	- [x] Siege Ram shield breaker + golem bonus damage (2025-12-13).
	- [x] Storm Anchor channeling buffs + water-chain lightning damage (2025-12-13).
	- [x] Leviathan Call Riptide boons + guardian spirits (2025-12-13).
	- [x] Slipstream Repeater reload haste + rocket perks (2025-12-13).
	- [x] Abyssal Churn whirlpool stacks + drowning pull (2025-12-13).
- [ ] Implement mark/root/slow logic for bow enchants.
	- [x] Verdant Tide snare/slow/DoT on projectile impact (2025-12-13).
	- [x] Umbral Pin mark tracking, re-hit root burst, darkness/glow behaviour (2025-12-13).
- [ ] Add lightning/levitation hooks for crossbows & tridents.
- [ ] Ensure mixins cover all use cases (Charged crossbows, Riptide throws, fishing reel events).

## 3. Tool & Utility Enchantments
- [ ] Finish mining logic (vein highlighting, duplication chances, vibration reveals).
- [ ] Implement Terra Surge stack decay, Dune Veil protections, Bloom Binder charge transfers, Soul Harvester soul flames.
- [ ] Add hoes/shears/fishing-rod/elytra/shield special behaviours.

## 4. Armor & Passive Effects
- [ ] Implement helmet/chest/legs/boots buffs, absorption timers, sprint dashes, and environmental immunities.
	- [x] Mindward Veil status cleanse + buffs (2025-12-13).
- [ ] Add continuous tick handlers for Elytra solar stacks, leggings resonance, boot dashes.

## 5. Datapack Assets
- [ ] Generate `lang/en_us.json` entries for all 50 enchantments + tooltips.
- [ ] Create 50 shaped recipes that output pre-enchanted books (NBT StoredEnchantments).
- [ ] Create tags/loot tables if we decide to gate acquisition outside crafting.

## 5a. Enchantment JSON Specs
- [x] Chronoblade Edge (`enchants/chronoblade_edge.json`).
- [x] Blood Oath (`enchants/blood_oath.json`).
- [x] Voidbreaker (`enchants/voidbreaker.json`).
- [x] Radiant Execution (`enchants/radiant_execution.json`).
- [x] Phantom Guillotine (`enchants/phantom_guillotine.json`).
- [x] Rifted Lunge (`enchants/rifted_lunge.json`).
- [x] Wither Severance (`enchants/wither_severance.json`).
- [x] Solar Riposte (`enchants/solar_riposte.json`).
- [x] Tempest Volley (`enchants/tempest_volley.json`).
- [x] Starfall Draw (`enchants/starfall_draw.json`).
- [x] Umbral Pin (`enchants/umbral_pin.json`).
- [x] Verdant Tide (`enchants/verdant_tide.json`).
- [x] Aether Salvo (`enchants/aether_salvo.json`).
- [x] Siege Ram (`enchants/siege_ram.json`).
- [x] Slipstream Repeater (`enchants/slipstream_repeater.json`).
- [x] Titan Hew (`enchants/titan_hew.json`).
- [x] Grove Shatter (`enchants/grove_shatter.json`).
- [x] Warcaller (`enchants/warcaller.json`).
- [x] Thunder Chop (`enchants/thunder_chop.json`).
- [x] Lode Harmonizer (`enchants/lode_harmonizer.json`).
- [x] Seismic Resonance (`enchants/seismic_resonance.json`).
- [x] Prism Splitter (`enchants/prism_splitter.json`).
- [x] Deep Echo (`enchants/deep_echo.json`).
- [x] Terra Surge (`enchants/terra_surge.json`).
- [x] Dune Veil (`enchants/dune_veil.json`).
- [x] Bloom Binder (`enchants/bloom_binder.json`).
- [x] Soul Harvester (`enchants/soul_harvester.json`).
- [x] Abyssal Churn (`enchants/abyssal_churn.json`).
- [x] Storm Anchor (`enchants/storm_anchor.json`).
- [x] Leviathan Call (`enchants/leviathan_call.json`).
- [x] Mindward Veil (`enchants/mindward_veil.json`).
- [x] Starlit Focus (`enchants/starlit_focus.json`).
- [x] Abyssal Gaze (`enchants/abyssal_gaze.json`).
- [x] Dragonheart Bulwark (`enchants/dragonheart_bulwark.json`).
- [x] Radiant Furnace (`enchants/radiant_furnace.json`).
- [x] Umbra Reservoir (`enchants/umbra_reservoir.json`).
- [x] Resonant Stride (`enchants/resonant_stride.json`).
- [x] Bastion Lattice (`enchants/bastion_lattice.json`).
- [x] Tempest Harness (`enchants/tempest_harness.json`).
- [x] Riftwalker Steps (`enchants/riftwalker_steps.json`).
- [x] Glacial Crest (`enchants/glacial_crest.json`).
- [x] Ember Dash (`enchants/ember_dash.json`).
- [x] Bulwark Conductor (`enchants/bulwark_conductor.json`).
- [x] Mirror Guard (`enchants/mirror_guard.json`).
- [x] Solar Thermals (`enchants/solar_thermals.json`).
- [x] Tidal Lure (`enchants/tidal_lure.json`).
- [x] Echo Line (`enchants/echo_line.json`).
- [x] Silk Gardener (`enchants/silk_gardener.json`).
- [x] Impact Bloom (`enchants/impact_bloom.json`).
- [x] Shock Lancer (`enchants/shock_lancer.json`).

## 6. Build & Debug Tooling
- [x] Author `enchants_build.py` helper (validation ➜ loom build ➜ run config) modeled after UniversalMobWar’s workflow (2025-12-13).
- [ ] Wire VS Code tasks/launch settings if desired.

## 7. QA & Documentation
- [ ] Extend README with installation, crafting overview, and debugging instructions.
- [ ] Run Gradle build/test once features land; capture issues in `TODO.md` or issue tracker.
