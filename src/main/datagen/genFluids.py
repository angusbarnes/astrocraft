from PIL import Image, ImageEnhance, ImageOps
from read_csv import get_chemical_defs
import numpy as np
import hashlib
from TextureUtils import *
from context import Context
import textwrap
import json
import os

def JSON(obj):
    return json.dumps(obj, ensure_ascii=False)

def BEE_DEFINITION(primary_colour, secondary_colour, bee_text, flower_block_tag, bee_size, is_hazard):
    return JSON(
        {
            "/*primaryColor_comment*/": "Base color of the bee used to color the bee, comb and spawn egg.",
            "primaryColor": primary_colour,
            "secondaryColor": secondary_colour,
            "/*particleColor_comment*/": "Color of the pollen located on the texture when it has nectar and also the particles dripping from the bee",
            "particleColor": primary_colour,
            "description": "This bee is added by astrocraft. " + bee_text,
            "createComb": True,
            "attackResponse": "fire",
            "flowerTag": flower_block_tag,
            "size:": bee_size,
            "teleporting": False,
            "redstoned": False,
            "fireproof": True,
            "withered": is_hazard,
            "blinding": is_hazard,
            "attributes": {
                "temper": 1 if is_hazard else 0,
                "endurance": 1,
                "behavior": 1,
                "productivity": 1,
                "weather_tolerance": 1
            }
        }
    )

def BEE_PRODUCTION(bee_type):
    return JSON(
        {
            "type": "productivebees:advanced_beehive",
            "ingredient": f"productivebees:{bee_type}",
            "results": [
                {
                    "item": {
                        "type": "forge:nbt",
                        "item": "productivebees:configurable_honeycomb",
                        "nbt": {
                            "EntityTag": {
                                "type": f"productivebees:{bee_type}"
                            }
                        }
                    }
                }
            ],
            "conditions": [
                {
                    "type": "productivebees:bee_exists",
                    "bee": f"productivebees:{bee_type}"
                }
            ]
        }
    )

def BEE_CENTRIFUGE(bee_type, output, min_count, max_count):
    return JSON(
        {
            "type": "productivebees:centrifuge",
            "ingredient": {
            "type": "forge:nbt",
            "item": "productivebees:configurable_honeycomb",
            "nbt": {
                "EntityTag": {
                "type": f"productivebees:{bee_type}"
                }
            }
            },
            "outputs": [
            {
                "item": {
                "item": output
                },
                "min": min_count,
                "max": max_count,
                "chance": 60
            },
            {
                "fluid": {
                "fluid": "productivebees:honey"
                },
                "amount": 50
            }
            ]
        }
    )

def UNCOMPACTING_RECIPE(from_item, to_item):
    return JSON(
        {
            "type": "minecraft:crafting_shapeless",
            "category": "misc",
            "ingredients": [
                {
                "item": from_item
                }
            ],
            "result": {
                "count": 9,
                "item": to_item
            }
        }
    )

def PLACED_ORE_FEATURE(ore_name, max_veins_per_chunk = 4):
    return JSON(
        {
            "feature": f"astrocraft:{ore_name}_ore",
            "placement": [
                {
                    "type": "minecraft:count",
                    "count": max_veins_per_chunk
                },
                {
                    "type": "minecraft:in_square"
                },
                {
                    "height": {
                    "min_inclusive": {
                        "absolute": -64
                    },
                    "max_inclusive": {
                        "absolute": 36
                    },
                    "type": "minecraft:trapezoid"
                    },
                    "type": "minecraft:height_range"
                },
                {
                    "type": "minecraft:biome"
                }
            ]
        }
    )

def CONFIGURED_ORE_FEATURE(ore_name, max_vein_size = 9):
    return JSON(
        {
            "type": "minecraft:ore",
            "config": {
                "size": max_vein_size,
                "discard_chance_on_air_exposure": 0,
                "targets": [
                    {
                        "target": {
                            "predicate_type": "minecraft:tag_match",
                            "tag": "minecraft:stone_ore_replaceables"
                        },
                        "state": {
                            "Name": f"astrocraft:{ore_name}_ore"
                        }
                    },
                    {
                        "target": {
                            "predicate_type": "minecraft:tag_match",
                            "tag": "minecraft:deepslate_ore_replaceables"
                        },
                        "state": {
                            "Name": f"astrocraft:deepslate_{ore_name}_ore"
                        }
                    }
                ]
            }
        }
    )

def FORGE_BIOME_MODIFIER(ore_name, biome_tag = "#minecraft:is_overworld"):
    return JSON(
        {
            "type": "forge:add_features",
            "biomes": biome_tag,
            "features": f"astrocraft:ore_{ore_name}_placed",
            "step": "underground_ores"
        }
    )

def CRYSTALISE(chemical_tag, output_dust):
    return JSON(
        {
            "type": "thermal:crystallizer",
            "ingredients": [
                {
                "fluid": f"astrocraft:{chemical_tag}",
                "amount": 144
                },
                {
                    "tag": "forge:dusts/salt"
                }
            ],
            "result": [
                {
                "item": f"astrocraft:{output_dust}_dust"
                }
            ],
            "energy": 5000
        }
    )

def MELT(input_tag, output_fluid):
    return JSON(
        {
            "type": "thermal:crucible",
            "ingredient": {
                "tag": f"forge:ingots/{input_tag}"
            },
            "result": [
                {
                "fluid": f"astrocraft:{output_fluid}",
                "amount": 110
                }
            ],
            "energy": 10000
        }
    )

def merge_json_file(json_file_path, new_data):
    # Load existing data from the JSON file if it exists
    if os.path.exists(json_file_path):
        with open(json_file_path, 'r', encoding='utf-8') as json_file:
            try:
                existing_data = json.load(json_file)
            except json.JSONDecodeError:
                existing_data = {}
    else:
        existing_data = {}

    # Merge the existing data with the new data
    merged_data = {**existing_data, **new_data}

    # Save the merged data back to the JSON file
    with open(json_file_path, 'w', encoding='utf-8') as json_file:
        json.dump(merged_data, json_file,ensure_ascii=False, indent=4)

file_path = '../java/net/astr0/astrocraft/Fluid/ModFluids.java'  # Replace with your file path
base_image_path = './templates/bucket_base_layer.png'  # Replace with your base image file path
top_image_path = './templates/bucket_fluid_layer.png'   # Replace with your top image file path

ingot_textures = ['./templates/ingot1.png', './templates/ingot2.png', './templates/ingot3.png', './templates/ingot4.png']
dust_textures = ['./templates/dust1.png', './templates/dust2.png', './templates/dust3.png', './templates/dust4.png', './templates/dust5.png', './templates/dust6.png']
plate_textures = ['./templates/plate1.png', './templates/plate2.png']
rod_textures = ['./templates/rod1.png', './templates/rod2.png']
nugget_textures = ['./templates/nugget1.png', './templates/nugget2.png']
raw_ore_textures = ['./templates/raw_ore1.png', './templates/raw_ore2.png', './templates/raw_ore3.png', './templates/raw_ore4.png', './templates/raw_ore5.png', './templates/raw_ore6.png', './templates/raw_ore7.png', './templates/raw_ore8.png']
ore_textures = ['./templates/ore1.png', './templates/ore2.png', './templates/ore3.png', './templates/ore4.png', './templates/ore5.png', './templates/ore6.png', './templates/ore7.png', './templates/ore8.png']
block_textures = ['./templates/block1.png', './templates/block2.png', './templates/block3.png', './templates/block4.png', './templates/block5.png', './templates/block6.png', './templates/block7.png']
gear_textures = ['./templates/gear.png', './templates/gear2.png', './templates/gear2.png']

SHARD_TEXTURE = './templates/shard.png'
SHARD_TEXTURE_OVERLAY = './templates/shard_overlay.png'
CLUMP_TEXTURE = './templates/clump.png'
CLUMP_TEXTURE_OVERLAY = './templates/clump_overlay.png'
DIRTY_DUST_TEXTURE = './templates/dirty_dust.png'
DIRTY_DUST_TEXTURE_OVERLAY = './templates/dirty_dust_overlay.png'
CRYSTAL_TEXTURE = './templates/crystal.png'
CRYSTAL_TEXTURE_OVERLAY = './templates/crystal_overlay.png'

def get_texture(filename, textures):
    # Step 1: Hash the filename using a hash function (e.g., SHA-256)
    hash_object = hashlib.sha256(filename.encode())
    hash_value = int(hash_object.hexdigest(), 16)
    
    # Step 2: Map the hash value to one of three textures
    texture_index = hash_value % len(textures)
    
    return textures[texture_index]

def add_simple_tint_item(ctx: Context, item_id: str, item_name, tint, template_file, material_name, hazard):

    TAB_FILE = '../java/net/astr0/astrocraft/ModCreativeModTab.java'
    ITEM_FILE = '../java/net/astr0/astrocraft/item/ModItems.java'
    FLUIDS_FILE = '../java/net/astr0/astrocraft/Fluid/ModFluids.java' 

    template = Image.open(template_file)
    texture = blend_overlay(template, hex_to_rgb(tint))
    texture.save(f'../resources/assets/astrocraft/textures/item/{item_id}.png', format='PNG')
    ctx.add_translation(f"item.astrocraft.{item_id}", f"{item_name}")
    ctx.add_text_to_region(ITEM_FILE, 'MATERIAL_REGION', f"""public static final RegistryObject<AsTechMaterialItem> {item_id.upper()} = ITEMS.register("{item_id}", () -> new AsTechMaterialItem(new Item.Properties().stacksTo(64), "{material_name}", {hazard}));""")
    ctx.add_text_to_region(TAB_FILE, 'TAB_REGION', f"output.accept(ModItems.{item_id.upper()}.get());")
    ctx.add_simple_item_model(f'{item_id}')

def add_mek_tint_item(ctx: Context, item_id: str, item_name, tint, template_file, overlay, material_name, hazard):

    TAB_FILE = '../java/net/astr0/astrocraft/ModCreativeModTab.java'
    ITEM_FILE = '../java/net/astr0/astrocraft/item/ModItems.java'
    FLUIDS_FILE = '../java/net/astr0/astrocraft/Fluid/ModFluids.java' 

    layer_images_but_backwards(template_file, overlay, f'../resources/assets/astrocraft/textures/item/{item_id}.png', hex_to_rgb(tint))

    ctx.add_translation(f"item.astrocraft.{item_id}", f"{item_name}")
    ctx.add_text_to_region(ITEM_FILE, 'MATERIAL_REGION', f"""public static final RegistryObject<AsTechMaterialItem> {item_id.upper()} = ITEMS.register("{item_id}", () -> new AsTechMaterialItem(new Item.Properties().stacksTo(64), "{material_name}", {hazard}));""")
    ctx.add_text_to_region(TAB_FILE, 'TAB_REGION', f"output.accept(ModItems.{item_id.upper()}.get());")
    ctx.add_simple_item_model(f'{item_id}')

def add_static_asset_item(ctx: Context, item_id: str, item_name):

    TAB_FILE = '../java/net/astr0/astrocraft/ModCreativeModTab.java'
    ITEM_FILE = '../java/net/astr0/astrocraft/item/ModItems.java'
    FLUIDS_FILE = '../java/net/astr0/astrocraft/Fluid/ModFluids.java'
    ctx.add_translation(f"item.astrocraft.{item_id}", f"{item_name}")
    ctx.add_text_to_region(ITEM_FILE, 'MATERIAL_REGION', f"""public static final RegistryObject<Item> {item_id.upper()} = SimpleIngredientItem("{item_id}", 64);""")
    ctx.add_text_to_region(TAB_FILE, 'TAB_REGION', f"output.accept(ModItems.{item_id.upper()}.get());")
    ctx.add_simple_item_model(f'{item_id}')

def add_simple_tint_block(ctx: Context, block_id: str, block_name, tint, template_file):
    TAB_FILE = '../java/net/astr0/astrocraft/ModCreativeModTab.java'
    BLOCK_FILE = '../java/net/astr0/astrocraft/block/ModBlocks.java'

    template = Image.open(template_file)
    texture = blend_overlay(template, hex_to_rgb(tint))
    texture.save(f'../resources/assets/astrocraft/textures/block/{block_id}.png', format='PNG')

    ctx.add_translation(f"block.astrocraft.{block_id}", f"{block_name}")
    ctx.add_text_to_region(BLOCK_FILE, 'BLOCK_REGION', f"""public static final RegistryObject<Block> {block_id.upper()} = registerBlock("{block_id}", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));""")
    ctx.add_text_to_region(TAB_FILE, 'TAB_REGION', f"output.accept(ModBlocks.{block_id.upper()}.get());")
    ctx.add_simple_block_model(f'{block_id}')
    ctx.add_block_item_model(f'{block_id}')


def add_simple_ore_block(ctx: Context, block_id: str, block_name, tint, template_file):
    TAB_FILE = '../java/net/astr0/astrocraft/ModCreativeModTab.java'
    BLOCK_FILE = '../java/net/astr0/astrocraft/block/ModBlocks.java'

    STONE_BASE = './templates/deepslate.png' if "deepslate" in block_id else './templates/stone.png'

    layer_images(STONE_BASE, template_file, f'../resources/assets/astrocraft/textures/block/{block_id}.png', hex_to_rgb(tint), 'Liquid')

    ctx.add_translation(f"block.astrocraft.{block_id}", f"{block_name}")
    ctx.add_text_to_region(BLOCK_FILE, 'BLOCK_REGION', f"""public static final RegistryObject<Block> {block_id.upper()} = registerBlock("{block_id}", () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(3, 7)));""")
    ctx.add_text_to_region(TAB_FILE, 'TAB_REGION', f"output.accept(ModBlocks.{block_id.upper()}.get());")
    ctx.add_simple_block_model(f'{block_id}')
    ctx.add_block_item_model(f'{block_id}')

headers, chemicals = get_chemical_defs("chems.csv")
datapack = Context('../resources/', 'astrocraft')

datapack.set_base_dictionary({
    "modifier.astrocraft.forged": "Forged",
    "modifier.astrocraft.forged.flavor": "The work of a master?",
    "modifier.astrocraft.forged.description": "Skillfully forge the head of your tool piece for better stats",
    "curios.identifier.key": "Key",
    "config.jade.plugin_astrocraft.crop" : "Crop",
    "item.astrocraft.fluid_cell" : "Fluid Cell",
    "item.astrocraft.large_fluid_cell" : "Large Fluid Cell",
    "item.astrocraft.pressurised_fluid_cell" : "Pressurised Fluid Cell",
    "tooltip.fluid_cell.contents": "Contains: %s (%d/%d mB)",
    "tooltip.fluid_cell.empty": "Empty",
    "item.astrocraft.deez_nuts": "Deez Nuts",
    "item.astrocraft.harddrive": "Hard Drive",
    "item.astrocraft.nasdrive": "NAS Hard Drive",
    "item.astrocraft.deez_nuts": "Deez Nuts",
    "item.astrocraft.world_eater": "World Eater",
    "item.astrocraft.twig_bundle": "Twig Bundle",
    "item.astrocraft.tree_key": "Tree Key",
    "item.astrocraft.overworld_key": "Overworld Key",
    "item.astrocraft.nether_key": "Nether Key",
    "item.astrocraft.end_key": "End Key",
    "item.astrocraft.transcendent_key": "Transcendent Key",
    "item.astrocraft.fire_starter": "Fire Starter",
    "item.astrocraft.full_shwaxe": "Full Shwaxe",
    "item.astrocraft.galentine_energy_sword": "Galentine Energy Blade",
    "item.astrocraft.tablet": "Tablet",
    "item.astrocraft.crop_sticks": "Crop Sticks",
    "item.astrocraft.broad_sword": "Diamond Broad Sword",
    "item.astrocraft.airhorn": "Airhorn",
    "item.astrocraft.deez_butts": "Deez Butts",
    "item.astrocraft.zeolite_catalyst": "Zeolite Catalyst",
    "item.astrocraft.monocrystalline_silicon": "Monocrystalline Silicon Boole",
    "item.astrocraft.cable_laying_tool": "Cable Laying Tool",
    "item.astrocraft.god_forged_ingot": "God Forged Ingot",
    "creativetab.astrocraft_tab": "astrocraft Items",
    "block.astrocraft.crop_sticks": "Crop Sticks",
    "block.astrocraft.nic_block": "§cNic Block",
    "block.astrocraft.brick_kiln_pile": "Brick Pile",
    "block.astrocraft.brick_kiln": "Brick Kiln",
    "block.astrocraft.log_pile": "Stick Stack",
    "block.astrocraft.knapping_stone": "Knapping Stone",
    "block.astrocraft.vacuum_freezer_controller": "Vaccum Freezer Controller",
    "block.astrocraft.regulated_machine_casing": "Regulated Machine Casing",
    "block.astrocraft.cleanroom_wall": "Cleanroom Wall",
    "block.astrocraft.coolant_block": "Coolant Block",
    "block.astrocraft.gem_polishing_station": "Gem Polishing Station",
    "block.astrocraft.chemical_mixer": "Chemical Mixer",
    "block.astrocraft.assembler": "Assembler",
    "block.astrocraft.fluid_output_hatch": "Fluid Output Hatch",
    "block.astrocraft.fluid_input_hatch": "Fluid Input Hatch",
    "block.astrocraft.energy_input_hatch": "Energy Input Hatch",
    "block.astrocraft.advanced_assembler": "Advanced Assembler",
    "block.astrocraft.chemical_reactor": "Chemical Reactor",
    "block.astrocraft.electrolytic_seperator": "Electrolytic Seperator",
    "block.astrocraft.pyrolysis_chamber": "Pyrolysis Chamber",
    "block.astrocraft.euv_machine": "EUV Machine",
    "item.astrocraft.as_an_llm_disc": "Music Disc",
    "item.astrocraft.as_an_llm_disc.desc": "As a Large Language Model - Ol' Yodel",
        "item.astrocraft.run_nic_run_disc": "Music Disc",
    "item.astrocraft.run_nic_run_disc.desc": "STIP NIC! - Grunge Parade",
        "item.astrocraft.help_of_disc": "Music Disc",
    "item.astrocraft.help_of_disc.desc": "Help My Mum - w$tPantyParty",
        "item.astrocraft.stolen_identity_disc": "Music Disc",
    "item.astrocraft.stolen_identity_disc.desc": "Stolen I.D",
            "item.astrocraft.bangarang_disc": "Music Disc",
    "item.astrocraft.bangarang_disc.desc": "Bangarang - Skrillex",
                "item.astrocraft.mir_disc": "Multiorgasmic Interplanetary Record",
    "item.astrocraft.mir_disc.desc": "The Final Song",


     "atm9.quest.industrialForegoing.hover.mycelialReactorFeatures.1": "Mycelial Reactor consists of all the Mycelial generators working at the same time, near the reactor block.",
        "atm9.quest.industrialForegoing.hover.mycelialReactorFeatures.2": "It generates 25MFE/t.",
        "atm9.quest.industrialForegoing.crimedMycelialGenerator": "Crimed Mycelial Generator",
        "atm9.quest.industrialForegoing.culinaryMycelialGenerator": "Culinary Mycelial Generator",
        "atm9.quest.industrialForegoing.deathMycelialGenerator": "Death Mycelial Generator",
        "atm9.quest.industrialForegoing.disenchantmentMycelialGenerator": "Disenchantment Mycelial Generator",
        "atm9.quest.industrialForegoing.enderMycelialGenerator": "Ender Mycelial Generator",
        "atm9.quest.industrialForegoing.explosiveMycelialGenerator": "Explosive Mycelial Generator",
        "atm9.quest.industrialForegoing.frostyMycelialGenerator": "Frosty Mycelial Generator",
        "atm9.quest.industrialForegoing.furnaceMycelialGenerator": "Furnace Mycelial Generator",
        "atm9.quest.industrialForegoing.halitosisMycelialGenerator": "Halitosis Mycelial Generator",
        "atm9.quest.industrialForegoing.magmaMycelialGenerator": "Magma Mycelial Generator",
        "atm9.quest.industrialForegoing.meatallurgicMycelialGenerator": "Meatallurgic Mycelial Generator",
        "atm9.quest.industrialForegoing.netherstarMycelialGenerator": "Netherstar Mycelial Generator",
        "atm9.quest.industrialForegoing.pinkMycelialGenerator": "Pink Mycelial Generator",
        "atm9.quest.industrialForegoing.potionMycelialGenerator": "Potion Mycelial Generator",
        "atm9.quest.industrialForegoing.rocketMycelialGenerator": "Rocket Mycelial Generator",
        "atm9.quest.industrialForegoing.slimeyMycelialGenerator": "Slimey Mycelial Generator",
        "atm9.quest.industrialForegoing.fluidExtractor": "Fluid Extractor - Extracts Latex from logs, some give more latex than others",
        "atm9.quest.industrialForegoing.blockPlacer": "Block Placer - to automate the log placing",
        "atm9.quest.industrialForegoing.acaciaLogs": "Acacia Logs give the most latex",
        "atm9.quest.industrialForegoing.latexProcessingUnit": "Latex Processing Unit",
        "atm9.quest.industrialForegoing.hover.witherInStasisFeatures.1": "Wither in Stasis - Fluid Drill on top",
        "atm9.quest.industrialForegoing.hover.witherInStasisFeatures.2": "This is how you get Ether Gas",
        "atm9.quest.industrialForegoing.desc.welcome": "Welcome to &aIndustrial Foregoing&f!",
        "atm9.quest.industrialForegoing.industrialForegoing": "Industrial Foregoing",
        "atm9.quest.industrialForegoing.desc.extractLatex": "Extracts latex from logs.",
        "atm9.quest.industrialForegoing.desc.checkJEI": "Check JEI for acceptable logs and latex amounts. Best log to use is Acacia.",
        "atm9.quest.industrialForegoing.itemAndFluidTransport": "Item \\\\& Fluid Transport",
        "atm9.quest.industrialForegoing.desc.givesPlastic": "When smelted, gives Plastic, which is the main resource in Industrial Foregoing",
        "atm9.quest.industrialForegoing.commonBlackHoleStorage": "Common Black Hole Storage",
        "atm9.quest.industrialForegoing.desc.morePinkSlime": "Passive Mobs -> More Pink Slime",
        "atm9.quest.industrialForegoing.desc.moreMeat": "Hostile Mobs -> More Meat",
        "atm9.quest.industrialForegoing.pinkSlimeAndLiquidMeat": "Pink Slime \\\\& Liquid Meat",
        "atm9.quest.industrialForegoing.conveyorInsertionAndExtraction": "Conveyor Insertion \\\\& Extraction",
        "atm9.quest.industrialForegoing.otherConveyorUpgrades": "Other Conveyor Upgrades",
        "atm9.quest.industrialForegoing.fluids": "Fluids",
        "atm9.quest.industrialForegoing.desc.blockAutomation": "Automate block placing/breaking using these, especially useful when automating latex.",
        "atm9.quest.industrialForegoing.blocks": "Blocks",
        "atm9.quest.industrialForegoing.animals": "Animals",
        "atm9.quest.industrialForegoing.plants": "Plants",
        "atm9.quest.industrialForegoing.bioPower": "Bio Power",
        "atm9.quest.industrialForegoing.otherMachines": "Other Machines",
        "atm9.quest.industrialForegoing.desc.meatTube": "Meat through a tube, yummy",
        "atm9.quest.industrialForegoing.simpleBlackHoleStorage": "Simple Black Hole Storage",
        "atm9.quest.industrialForegoing.laserDrills": "Laser Drills (Void Miner)",
        "atm9.quest.industrialForegoing.desc.laserDrillSuggestion.1": "Suggestion:",
        "atm9.quest.industrialForegoing.desc.laserDrillSuggestion.2": "Use some sort of wither-proof glass.",
        "atm9.quest.industrialForegoing.desc.mycelialReactorFeatures.1": "The &bMycelial Reactor&r consists of all the Mycelial Generators working at the same time, near the reactor block, and it produces a total of &a25MFE/t&r.",
        "atm9.quest.industrialForegoing.desc.mycelialReactorFeatures.2": "All sounds good, but you need to automate some stuff to get it working, see what each Mycelial Generator consumes to work, and automate it, most things are simple, but others, not that much... &olooking at Disenchanting Mycelial Generator&r.",
        "atm9.quest.industrialForegoing.desc.mycelialReactorFeatures.3": "But after you get it all automated, you don't need to stop at one, you can make more reactors.",
        "atm9.quest.industrialForegoing.mycelialReactor": "Mycelial Reactor, huh?",
        "atm9.quest.industrialForegoing.mycelialReactorQuestion": "Mycelial Reactor? Huh?",
        "atm9.quest.industrialForegoing.desc.etherGas.1": "Getting your first &bEther Gas&r is going to be scary.",
        "atm9.quest.industrialForegoing.desc.etherGas.2": "&bEther Gas&r is made from drilling a &0Wither&r, using a Fluid Drill with &5Purple Lens&r.",
        "atm9.quest.industrialForegoing.desc.etherGas.3": "But worry not, Industrial Foregoing has a machine just to help you in that task: &4Stasis Chamber&r - this machine freezes in place anything that is on top, in a 3x3 area, so spawning a wither on there is safe.",
        "atm9.quest.industrialForegoing.desc.etherGas.4": "&cHope you don't run out of power, because if you do... well... i hope you are ready to fight the &0Wither&r.",
        "atm9.quest.industrialForegoing.etherGasQuestion": "Ether Gas? Huh?",
        "atm9.quest.industrialForegoing.desc.latexIntro.1": "Welcome to &bIndustrial Foregoing&r, one of the main resources in this mod, is &fLatex.&r Its used to craft machine frames, needed to make.. well... machines, and upgrades.",
        "atm9.quest.industrialForegoing.desc.latexIntro.2": "&oJEI is your friend&r",
        "atm9.quest.industrialForegoing.desc.latexIntro.3": "Making Latex is pretty simple, &aFluid Extractor&r extracts Latex from &6Logs&r (Acacia gives the most).",
        "atm9.quest.industrialForegoing.desc.latexIntro.4": "Now about making Plastic: Plastic results from smelting Dry Rubber - which is made in the &aLatex Processing Unit&r, that transforms Latex into Dry Rubber.",
        "atm9.quest.industrialForegoing.desc.latexIntro.5": "&bSo basically Latex -> Dry Rubber -> Plastic.&r",
        "atm9.quest.industrialForegoing.latexQuestion": "Latex? Huh?",
        "atm9.quest.industrialForegoing.latexAndPlasticQuestion": "Latex and Plastic? Huh?",
            "atm9.quest.pneumatic.pneumatic": "PneumaticCraft: Repressurized",
        	"atm9.quest.pneumatic.compressedArmor": "Compressed Iron Armor",
            "atm9.quest.pneumatic.autoCompressedIron": "Automating Compressed Iron",
            "atm9.quest.pneumatic.moving": "Transporting Pressure",
            "atm9.quest.pneumatic.generating": "Generating Pressure",
            "atm9.quest.pneumatic.liquidCompressor": "Pressure from Liquids",
            "atm9.quest.pneumatic.thermalCompressor": "Pressure from Heat",
            "atm9.quest.pneumatic.understand": "Understanding Pressure",
            "atm9.quest.pneumatic.explosions": "Preventing Explosions?",
        	"atm9.quest.pneumatic.reinforcedStone": "Reinforced Stone",
        	"atm9.quest.pneumatic.logistics": "Logistics",
        	"atm9.quest.pneumatic.logisticsFrames": "Logistic Frames",
        	"atm9.quest.pneumatic.minigunAmmo": "Minigun Ammo",
        	"atm9.quest.pneumatic.chamber": "The Pressure Chamber",
            "atm9.quest.pneumatic.oil": "Crude Oil",
            "atm9.quest.pneumatic.heat": "Heat Production",
            "atm9.quest.pneumatic.betterHeat": "Better Heat Production",
            "atm9.quest.pneumatic.movingHeat": "Transporting Heat",
            "atm9.quest.pneumatic.refinery": "Oil Refinery",
            "atm9.quest.pneumatic.diesel": "Diesel",
            "atm9.quest.pneumatic.lubricant": "Lubricant",
            "atm9.quest.pneumatic.kerosene": "Kerosene",
            "atm9.quest.pneumatic.gasoline": "Gasoline",
            "atm9.quest.pneumatic.lpg": "LPG",
            "atm9.quest.pneumatic.vegetableOil": "Vegetable Oil",
            "atm9.quest.pneumatic.fishNChips": "Fish and Chips!",
            "atm9.quest.pneumatic.yeast": "Yeast",
            "atm9.quest.pneumatic.ethanol": "Ethanol",
            "atm9.quest.pneumatic.biodiesel": "Biodiesel",
            "atm9.quest.pneumatic.elevators": "Elevators",
            "atm9.quest.pneumatic.autoPlastic": "Automating Plastic",
            "atm9.quest.pneumatic.constructionBricks": "Construction Bricks™",
            "atm9.quest.pneumatic.amadron": "Amadron",
            "atm9.quest.pneumatic.charging": "Charging with UVs",
            "atm9.quest.pneumatic.acid": "Etching Acid",
            "atm9.quest.pneumatic.capacitors": "Capacitors",
            "atm9.quest.pneumatic.transistors": "Transistors",
            "atm9.quest.pneumatic.pneumaticArmor": "Pneumatic Armor",
            "atm9.quest.pneumatic.drones": "Programming Drones",
            "atm9.quest.pneumatic.assembly": "Using Assembly Lines",
        	"atm9.quest.pneumatic.chest": "Any Chest",
        	"atm9.quest.pneumatic.jetBoots": "Jet Boots",
            "atm9.quest.pneumatic.autoPCBs": "Automating PCBs",
            "atm9.quest.pneumatic.solarCells": "Solar Cells",
            "atm9.quest.pneumatic.solarCompressor": "Pressure from the Sun",
            "atm9.quest.pneumatic.power": "Generating Power",
            "atm9.quest.pneumatic.fluxCompressor": "Pressure from Power",
            "atm9.quest.pneumatic.electrostaticCompressor": "Pressure from Lightning",
            "atm9.quest.pneumatic.upgrades": "Simple Upgrades",

        	"atm9.quest.pneumatic.desc.pneumatic": "PneumaticCraft is all about Pressure! These quests won't go over everything the mod has, but will still go over a lot! Please remember to check JEI, read the tooltips and read the PCB:R, they are all very useful.\\n\\nTo get started, you'll need to craft some &3Compressed Iron Ingots&r! The easiest way to do this is to make a hole in the ground, throw in some iron ingots (or blocks) and then blow it up with some TNT!\\n\\nSome might get lost in the explosion, but that's a risk we'll have to take!",
        	"atm9.quest.pneumatic.desc.compressedArmor": "Compressed Iron can be used to craft &3Compressed Iron Armor&r, it's the same as Iron Armor but with better Armor Toughness and Knockback Resistance.",
        	"atm9.quest.pneumatic.desc.directionHopper": "Thie &3Omnidirectional Hopper&r can input items from any side, plus it can be upgraded to be way faster than a regular Hopper! Nice for when Pipes aren't needed.",
        	"atm9.quest.pneumatic.desc.transfer": "PneumaticCraft is all The &3Transfer Gadget&r acts like a Hopper that can be placed inbetween blocks.",
        	"atm9.quest.pneumatic.desc.liquidHopper": "The &3Liquid Hopper&r is a Hopper, but for liquids. Not a replacement for pipes.",
        	"atm9.quest.pneumatic.desc.support": "&3Crop Supports&r can be placed over your crops to make them grow faster.",
        	"atm9.quest.pneumatic.desc.autoCompressedIron": "Is that even possible?\\n\\nIf you wish to automate this process you can use &aMystical Agriculture&r or &eProductive Bees&r. Or I guess you could automate it, with...explosions.",
        	"atm9.quest.pneumatic.desc.fluidTank": "When a &3Fluid Tank&r is put on top of another &3Fluid Tank&r and wrenched together they become one big tank.\\n\\nThese can be used in recipes using the bucketed version of PneumaticCraft liquids. ",
        	"atm9.quest.pneumatic.desc.moving": "To move pressure you will need to obtain &3Pressure Tubes&r. What pressure you may be asking? Continue the questline to see what these are used for.",
        	"atm9.quest.pneumatic.desc.junction": "The &3Tube Junction&r gives you more control over the transportation of your pressure by allowing you to move your Pressure Tubes in more directions.",
        	"atm9.quest.pneumatic.desc.generating": "The &3Air Compressor&r is an easy way of making pressure, simply put some burnable items into it, and watch the magic happen! (I recommend making at least 3 of these to start off with).",
        	"atm9.quest.pneumatic.desc.liquidCompressor": "The &3Liquid Compressor&r creates pressure using certain liquids. You can put the fuel in by right-clicking the bucket onto the machine, pumping in the fluid, or by putting a bucket in the top slot in the GUI.",
        	"atm9.quest.pneumatic.desc.thermalCompressor": "The &3Thermal Compressor&r creates pressure using heat! Place somthing hot on one side and something cold on the other to create pressure.",
        	"atm9.quest.pneumatic.desc.understand": "When making pressure in an Air Compressor, if you don't have anywhere for the pressure to go. it will just disappear. Make sure you have somewhere for the pressure to go before making some.",
        	"atm9.quest.pneumatic.desc.explosions": "Did I forget to mention everything can blow up...\\n\\n Luckily there's an easy solution! The &3Security Upgrade&r when placed in a machine makes sure they don't explode, put these into any machines they can go into.",
        	"atm9.quest.pneumatic.desc.reinforcedStone": "One of the most important materials you'll be needing is &3Reinforced Stone&r, start by making 32 using Compressed Iron and some regular Stone.",
        	"atm9.quest.pneumatic.desc.reinforcedChest": "The &3Reinforced Chest&r is a blast-proof Chest with 36 slots. Not Double Chest-able. Also works like a Shulker Box.",
        	"atm9.quest.pneumatic.desc.smartChest": "The &3Smart Chest&r is a blast-proof Chest with 72 slots, includes a built in, fully configuarble Omnidirectional Hopper and has upgrade slots. Not Double Chest-able. Also works like a Shulker Box.",
        	"atm9.quest.pneumatic.desc.logistics": "To start with Logistics craft a &3Logistics Core&r, I recommend reading the PNC:R Manual for a full tutorial on how to use these items.",
        	"atm9.quest.pneumatic.desc.logisticsFrames": "Each &3Logistics Frame&r can be used for different types of automation.",
        	"atm9.quest.pneumatic.desc.logisticsConfig": "The &3Logistics Configurator&r can be used to configure Logistic Frames and Transfer Gadgets.",
        	"atm9.quest.pneumatic.desc.logisticsModule": "The Logistic Frames need to connect to a &3Logistics Module&r to work.",
        	"atm9.quest.pneumatic.desc.minigunAmmo": "Load up your Minigun with this &3Minigun Ammo&r to use it.",
        	"atm9.quest.pneumatic.desc.minigun": "The &3Minigun&r is a gun that needs pressure and ammo to work. Read the tooltip for more info.",
        	"atm9.quest.pneumatic.desc.sentry": "The &3Sentry Turret&r can be placed somewhere and set to attack mobs or players when they are in range. It just needs some Minigun Ammo.",
        	"atm9.quest.pneumatic.desc.station": "The &3Charging Station&r is used to charge various tools and gadgets in &aPneumaticCraft&r using pressure.",
        	"atm9.quest.pneumatic.desc.mechanic": "I suppose I need to let you know that you can buy stuff off of Villagers...\\n\\nThe &3Pressure Machanic's&r workstation is a Charging Station. They have some very useful trades.",
        	"atm9.quest.pneumatic.desc.chargingModule": "The &3Charging Module&r depressurizes any pressurizable things in containers it points at.",
        	"atm9.quest.pneumatic.desc.expansionCard": "The &3Module Expansion Card&r makes the Charging Module even better.",
        	"atm9.quest.pneumatic.desc.camouflage": "The &3Camouflage Applicator&r can be used to hide PneumaticCraft machinery inside of other blocks.",
        	"atm9.quest.pneumatic.desc.wrench": "The &3Pneumatic Wrench&r is used to move around or break your machines and tubes.",
        	"atm9.quest.pneumatic.desc.manometer": "The &3Manometer&r is used to show extra information about a machine.",
        	"atm9.quest.pneumatic.desc.chamber": "The &3Pressure Chamber&r is an important multiblock structure made up of &3Pressure Chamber Walls&r (The faces can be replaced with &3Pressure Chamber Glass&r). While the Pressure Chamber can be as small as a 3x3x3 multiblock, the one required for the&d Pulsating Black Hole&r is 5x5x5, so it's the one we'll be building. The blocks required for this are what's needed for the quest. 4.9 bars of pressure are required for the &dPulsating Black Hole.&d\\n\\nTo use the Pressure Chamber you will need to pipe pressure into a &3Pressure Chamber Valve&r using the &3Pressure Tubes&r you made earlier, remember to put a Security Upgrade into the Pressure Valve. &lDifferent recipes require different amounts of pressure&r.\\n\\nYou'll need to be able to input and output items to and from the &3Pressure Chamber&r, this is where &3Pressure Chamber Interfaces&r come into it. Blue on the outside means it's for importing items, while if gold is on the outside it's for outputing items, place one of each on any face.",
        	"atm9.quest.pneumatic.desc.oil": "The next stage of the mod requires the collection of &3Crude Oil&r. This Oil can be found naturally on the surface of the overworld.",
        	"atm9.quest.pneumatic.desc.gasLift": "Although you can find Crude Oil on the surface, drilling is a great way to obtain a bunch of oil. First use a &3Seismic Sensor&r until you find oil underneath you, then use your &3Gas Lift&r filled with drill pipes to drill for oil. (This requires pressure to work).",
        	"atm9.quest.pneumatic.desc.heat": "The &3Vortex Tube&r seperates pressure into hot and cold air. The cold air goes to the blue side and the hot air goes to the red side.",
        	"atm9.quest.pneumatic.desc.betterHeat": "To increase your heat production, place a &3Heat Sink&r on the cold side of your Vortex Tube.",
        	"atm9.quest.pneumatic.desc.movingHeat": "To move the heat being made by the Vortex Tube, make some &3Heat Pipes&r.",
        	"atm9.quest.pneumatic.desc.refinery": "The &3Oil Refinery&r we'll be making is a 1x1x5 multiblock made up of 1 &3Refinery Controller&r (at the bottom) and four &3Refinery Outputs&r (on top). You will need to input Oil and heat for this to work. This will produce Diesel, Kerosene, Gasoline and LPG. Putting &3Thermal Laggings&r on the sides helps keep it warm.",
        	"atm9.quest.pneumatic.desc.diesel": "&3Diesel&r can be used to make more Kerosene or to make Lubricant..",
        	"atm9.quest.pneumatic.desc.lubricant": "&3Lubricant&r can be used to make Drill Bits.",
        	"atm9.quest.pneumatic.desc.diamondBit": "&3Drill Bits&r can be used in a Pneumatic Jackhammer.",
        	"atm9.quest.pneumatic.desc.kerosene": "&3Kerosene&r can be used to make Gasoline or it can be used as fuel in a Kerosene Lamp.",
        	"atm9.quest.pneumatic.desc.lamp": "The &3Kerosene Lamp&r is a great light source that uses fuel (Kerosene being the best) to produce light.",
        	"atm9.quest.pneumatic.desc.gasoline": "&3Gasoline&r can be used to make LPG.",
        	"atm9.quest.pneumatic.desc.lpg": "&3LPG&r can be used to make Molten Plastic.",
        	"atm9.quest.pneumatic.desc.processing": "The &3Thermopneumatic Processing Plant&r will be used to make Molten Plastic, simply pump in some LPG, Coal and some heat.",
        	"atm9.quest.pneumatic.desc.vegetableOil": "&3Vegetable Oil&r can be made by putting either crops or seeds into a &3Thermopneumatic Processing Plant&r.",
        	"atm9.quest.pneumatic.desc.fishNChips": "Could I get that with some Mashed Peas with a side of Beans on Toast please Governor?",
        	"atm9.quest.pneumatic.desc.yeast": "Why doesn't regular Bread need Yeast?",
        	"atm9.quest.pneumatic.desc.sourdough": "Made with &3Wheat Flour&r. Can be used to make Salmon Tempura and Sourdough Bread.",
        	"atm9.quest.pneumatic.desc.ethanol": "&3Ethanol&r can be used to make Biodiesel.",
        	"atm9.quest.pneumatic.desc.biodiesel": "&3Biodiesel&r can be used to make &3Lubricant&r and &3Molten Plastic&r.",
        	"atm9.quest.pneumatic.desc.matrix": "Only use 1 Lapis for an Upgrade instead of 4.",
        	"atm9.quest.pneumatic.desc.heatFrame": "When the &3Heat Frame&r is placed on a container containing certain fluids and made cold enough, it will turn said fluids into items.\\n\\nTurns Molten Plastic into Plastic.\\nTurns Water into Ice.\\nTurns Lava into Obsidian.",
        	"atm9.quest.pneumatic.desc.plastic": "Place your Liquid Plastic onto the ground or store some in a Chest with a Heat Frame attached and there's your &3Plastic&r!",
        	"atm9.quest.pneumatic.desc.jackhammer": "The &3Pneumatic Jackhammer&r uses pressure to mine, it can be upgraded to be amazing!",
        	"atm9.quest.pneumatic.desc.netheriteBit": "The &3Netherite Drill Bit&r is the best Drill Bit, it's even better than the Diamond Drill Bit.",
        	"atm9.quest.pneumatic.desc.elevators": "With these 3 components you can create some amazing &3Elevators&r.",
        	"atm9.quest.pneumatic.desc.door": "The &3Pneumatic Door&r acts as automatic door that can be made to only open for you!",
        	"atm9.quest.pneumatic.desc.autoPlastic": "Making Plastic can take a while, if you wish to automate this process you can use &aMystical Agriculture&r or &eProductive Bees&r. Or I guess you could automate it using these previous quests.",
        	"atm9.quest.pneumatic.desc.constructionBricks": "&3Construction Bricks&r are cool building blocks, be careful they hurt. Craft them again to make them smooth.",
        	"atm9.quest.pneumatic.desc.reinforcedTube": "&3Reinforced Pressure Tubes&r Just like regular Pressure Tubes. only these can hold 10 bars of pressure.",
        	"atm9.quest.pneumatic.desc.empty": "Now that we have Plastic, the next step is getting some Finished PCBs, first make some &3Empty PCBs&r in the Pressure Chamber.",
        	"atm9.quest.pneumatic.desc.amadron": "We will need to order some stuff off of &3Amadron&r using the &3Amadron Tablet&r in order to continue progression. To use it, shift-right click a Chest and a Fluid tank, put your currency in a linked container (Emeralds, etc) and order something. You'll see an Amadrone come down to take your money and another Amadrone come to drop off what you ordered.",
        	"atm9.quest.pneumatic.desc.blueprint": "&3PCB Blueprints&r On SALE NOW for 8 Emeralds. (For a limited time only).",
        	"atm9.quest.pneumatic.desc.charging": "Place your Empty PCBs in a &3UV Light Box&r to charge them. Needs pressure and access to sunlight.",
        	"atm9.quest.pneumatic.desc.tank": "The next step of making PCBs involves using the &3Etching Tank&r with Etching Acid. Requires heat to run.",
        	"atm9.quest.pneumatic.desc.acid": "&3Etching Acid&r is made in the Pressure Chamber with Molten Plastic...and some other stuff. Used in the Etching Tank.",
        	"atm9.quest.pneumatic.desc.unassembled": "Put a Charged Empty PCB into the Etching Tank with some Etching Acid and some heat and you'll get an &3Unassembled PCBs&r.",
        	"atm9.quest.pneumatic.desc.finished": "Craft together your Unassmbled PCB with 2 Capacitors and 2 Transistors to get yourself a &3Finished PCB&r.",
        	"atm9.quest.pneumatic.desc.drones": "Although these quests won't go over Drones, they are amazing! Look into using them if you want to.",
        	"atm9.quest.pneumatic.desc.micromissiles": "This is as far as you need to get to make the &dPulsating Black Hole&r. Congrats for making it this far!",
        	"atm9.quest.pneumatic.desc.pneumaticArmor": "The &3Pneumatic Armor&r by itself is pretty great, however if you want to make this armor amazing, you'll need to install upgrades. Press 'U' by default to open the Upgrade GUI, some upgrades use pressure, some don't.\\n\\nJust like upgrading other tools in PneumaticCraft, to upgrade &3Pneumatic Armor&r you will need a Charging Station with pressure.\\n\\nNote: The subtitle will tell you the max amount of that upgrade that can be put in something.",
        	"atm9.quest.pneumatic.desc.blockTracker": "Allows you to see details about certain blocks/fluid, even through walls.",
        	"atm9.quest.pneumatic.desc.entityTracker": "Living creatures nearby will be automatically tracked, even through walls. If it's an aggressive mob you'll get a warning when it targets you.",
        	"atm9.quest.pneumatic.desc.itemSearch": "Select an item an this upgrade will search fo it nearby in chests or on the floor. To find it in chests it will need the Block Tracker Upgrade and to find it on the floor it will need the Entity Tracker Upgrade.",
        	"atm9.quest.pneumatic.desc.CoordinateTracker": "Right-click somewhere and you will be able to pathfind your way back. If you're close enough and have a direct route, a path will appear.",
        	"atm9.quest.pneumatic.desc.armor": "Each upgrade adds +0.5 Armor Defence and +1 Armor Toughness.",
        	"atm9.quest.pneumatic.desc.stomp": "Any damage you take when falling (ignoring armor) will also be dealt to nearby mobs.",
        	"atm9.quest.pneumatic.desc.flippers": "Makes you swim faster.",
        	"atm9.quest.pneumatic.desc.chargingU": "Charges other pressurizable items in your inventory from the chestplate's storage.",
        	"atm9.quest.pneumatic.desc.scuba": "Keep your breathing underwater, once your bubbles go down to 5 they will be refilled! It also allows you to see much more easily underwater!",
        	"atm9.quest.pneumatic.desc.vision": "Grants the player Night Vision when worn.",
        	"atm9.quest.pneumatic.desc.jetBootsI": "Allows you to fly like Iron Man.\\n\\nYou can only have one level of these Upgrades in the Boots at a time. Each level will use more pressure than the previous.\\n\\nUse these upgrades, don't just craft them.",
        	"atm9.quest.pneumatic.desc.jetBootsII": "Allows you to travel faster than with Tier I.",
        	"atm9.quest.pneumatic.desc.jetBootsIII": "Allows you to travel even faster than Tier II.",
        	"atm9.quest.pneumatic.desc.jetBootsIV": "Maybe a little too fast even.",
        	"atm9.quest.pneumatic.desc.jetBootsV": "100% way too fast! Try not to die!",
        	"atm9.quest.pneumatic.desc.jetBoots": "You will only see this quest if you have used Jet Boots.\\n\\nThere's no way of using Jet Boots without taking damage, don't even try...",
        	"atm9.quest.pneumatic.desc.jumpingI": "Allows you to jump higher than usual and makes you springy, making for faster travel.\\n\\nYou can only have one level of these Upgrades in the Leggings at a time.",
        	"atm9.quest.pneumatic.desc.jumpingII": "Allows you to jump higher than with Tier I.",
        	"atm9.quest.pneumatic.desc.jumpingIII": "Allows you to jump higher than with Tier II.",
        	"atm9.quest.pneumatic.desc.jumpingIV": "Allows you to jump VERY high.",
        	"atm9.quest.pneumatic.desc.radiation": "Protects you from Mekanism radiation.",
        	"atm9.quest.pneumatic.desc.gilded": "Makes Piglins Passive (same as wearing Gold Armor), you only need to put the Upgrade on one piece.",
        	"atm9.quest.pneumatic.desc.magnet": "Allows you to pickup items and experience from furthur away.",
        	"atm9.quest.pneumatic.desc.elytra": "Allows you to use an Elytra while still having your Pneumatic Chestplate on.",
        	"atm9.quest.pneumatic.desc.visor": "Makes Endermen not get angry at you, the same way Carved Pumpkins would, only this way you can actually see.",
        	"atm9.quest.pneumatic.desc.controller": "The next stage of this mod requires you to set up &3Assembly Lines&r. The main component of an &3Assembly Line&r is the &3Assembly Controller&r.\\n\\nThis is where the pressure will go.",
        	"atm9.quest.pneumatic.desc.export": "This will drop the finished product into your output chest.",
        	"atm9.quest.pneumatic.desc.platform": "The item will need to be placed here before getting picked back up.",
        	"atm9.quest.pneumatic.desc.import": "This will take the items out of your input chest.",
        	"atm9.quest.pneumatic.desc.advancedTube": "&3Advanced Pressure Tubes&r are the best Pressure Tubes available, being able to hold 20 bars of pressure!\\n\\nAlthough these are really cool on their own, they can also be used in some even cooler recipes.",
        	"atm9.quest.pneumatic.desc.assembly": "Although not every recipe requires the same components, you can set up your &3Assembly Lines&r with everything installed. A simple &3Assembly Line&r setup that can be used for everything is shown in the image below. Read the quests for each component for some important information about them.\\n\\nSpeed Upgrades make this process a lot quicker!",
        	"atm9.quest.pneumatic.desc.autoPCBs": "Using a Laser Assembly Program you can speed up the production of &3Unassembled PCBs&r. Just put an Empty PCB in the input Chest, pump in some pressure and wait.",
        	"atm9.quest.pneumatic.desc.tile": "A smart Sign which can store and display arbitrary amounts of text, scaled so the text always fits.\\n\\nRight-click with any Dye to recolor the tile, you can color the border and the background independentrly.",
        	"atm9.quest.pneumatic.desc.solarCells": "&3Solar Cells&r are used to make Solar Compressors.",
        	"atm9.quest.pneumatic.desc.solarCompressor": "The &3Solar Compressor&r generates Pressure by using sunlight, the warmer it gets, the more Prssure it will produce. However. be careful, if it gets too hot it will malfunction and you will need to repair it.",
        	"atm9.quest.pneumatic.desc.power": "Whoever told you PneumaticCraft wasn't about generating power?\\n\\nThe &3Pneumatic Dynamo&r allows you to turn your pressure in FE. Read the tooltip or read about it to learn how it works.",
        	"atm9.quest.pneumatic.desc.fluxCompressor": "The &3Flux Compressor&r can create a ton of pressure using power. Simply pump some FE in and it will start generating some. If you have enough FE to spare, this is probably the easiest way to generate pressure.",
        	"atm9.quest.pneumatic.desc.aerial": "The &3Aerial Interface&r can be used to refill your pressurizable things with pressure. Now you can use your Pneumatic Armor without needing to recharge!",
        	"atm9.quest.pneumatic.desc.advancedCompressor": "The &3Advanced Air Compressor&r works the same way as the Air Compressor, only way better!",
        	"atm9.quest.pneumatic.desc.advancedLiquidCompressor": "Using a Laser Assembly Program you can speed up the production of &3Unassembled PCBs&r. Just put an Empty PCB in the input Chest, pump in some pressure and wait.",
        	"atm9.quest.pneumatic.desc.electrostaticCompressor": "The &3Electrostatic Compressor&r generates a lot of pressure from Lightning Strikes.\\n\\nFor more info check out the 'Information' tab in JEI or check the PNC:R.",
        	"atm9.quest.pneumatic.desc.upgrades": "Below are some &3Upgrades&r you can use early on in this mod. Shift while hovering over an &3Upgrade&r to see what it can be used in, or shift while hovering over something to see what upgrades it can use.",
        	"atm9.quest.pneumatic.desc.range": "Increases range, does different things when used in different things.",
        	"atm9.quest.pneumatic.desc.volume": "Increases how much pressure things can hold.",
        	"atm9.quest.pneumatic.desc.security": "Prevents things from exploding.",
        	"atm9.quest.pneumatic.desc.speed": "Makes things faster.",
        	"atm9.quest.pneumatic.desc.dispenser": "Makes things able to dispense items.",

            "atm9.quest.pneumatic.subt.pneumatic": "The Power of Air!",
        	"atm9.quest.pneumatic.subt.compressedArmor": "Better than Iron!",
            "atm9.quest.pneumatic.subt.directionHopper": "A Configurable Hopper",
            "atm9.quest.pneumatic.subt.transfer": "A Smaller Hopper",
            "atm9.quest.pneumatic.subt.liquidHopper": "Who needs pipes?",
            "atm9.quest.pneumatic.subt.support": "Growing Crops Faster",
            "atm9.quest.pneumatic.subt.autoCompressedIron": "Tired of blowing stuff up?",
            "atm9.quest.pneumatic.subt.fluidTank": "32,000mb",
            "atm9.quest.pneumatic.subt.mediunFluidTank": "64,000mb",
            "atm9.quest.pneumatic.subt.largeFluidTank": "128,000mb",
            "atm9.quest.pneumatic.subt.hugeFluidTank": "512,000mb",
            "atm9.quest.pneumatic.subt.moving": "Pressure?",
            "atm9.quest.pneumatic.subt.junction": "No, go this way!",
            "atm9.quest.pneumatic.subt.generating": "Feeling the Pressure!",
            "atm9.quest.pneumatic.subt.understand": "What happened to RF or FE?",
            "atm9.quest.pneumatic.subt.explosions": "If only there was an easy solution...",
            "atm9.quest.pneumatic.subt.reinforcedStone": "You'll need a lot of this",
            "atm9.quest.pneumatic.subt.reinforcedChest": "Better than your Everyday Chest",
            "atm9.quest.pneumatic.subt.smartChest": "The Nerd of Chests",
            "atm9.quest.pneumatic.subt.logistics": "Transporting Items/Fluids",
            "atm9.quest.pneumatic.subt.logisticsFrames": "You're being Framed!",
            "atm9.quest.pneumatic.subt.minigun": "Pew Pew!",
            "atm9.quest.pneumatic.subt.sentry": "Pew Pew (Without you)",
            "atm9.quest.pneumatic.subt.station": "Where's the cord?",
            "atm9.quest.pneumatic.subt.mechanic": "Ugghhh...",
            "atm9.quest.pneumatic.subt.camouflage": "Want to make things look cool?",
            "atm9.quest.pneumatic.subt.wrench": "Torque-ing about Tools!",
            "atm9.quest.pneumatic.subt.manometer": "A useful tool",
            "atm9.quest.pneumatic.subt.chamber": "Pressurizing!",
            "atm9.quest.pneumatic.subt.oil": "The Path to Plastic",
            "atm9.quest.pneumatic.subt.gasLift": "Drilling for Oil",
            "atm9.quest.pneumatic.subt.heat": "Still not RF...",
            "atm9.quest.pneumatic.subt.betterHeat": "More Heat",
            "atm9.quest.pneumatic.subt.movingHeat": "We have the heat to move :)",
            "atm9.quest.pneumatic.subt.refinery": "Now we're getting somewhere!",
        	"atm9.quest.pneumatic.subt.lubricant": "Lube Production!",
            "atm9.quest.pneumatic.subt.lamp": "Mega Torches are nothing!",
            "atm9.quest.pneumatic.subt.lpg": "The one we need",
            "atm9.quest.pneumatic.subt.processing": "The longer the name, the better",
            "atm9.quest.pneumatic.subt.vegetableOil": "Better Oil",
            "atm9.quest.pneumatic.subt.fishNChips": "Fancy some Fish and Chips?",
        	"atm9.quest.pneumatic.subt.yeast": "The Yeast of your Problems",
        	"atm9.quest.pneumatic.subt.sourdough": "You knead this",
        	"atm9.quest.pneumatic.subt.biodiesel": "Bio/Diesel",
        	"atm9.quest.pneumatic.subt.matrix": "Saving Lapis",
        	"atm9.quest.pneumatic.subt.plastic": "Finally!",
         	"atm9.quest.pneumatic.subt.constructionBricks": "There's no pain worse than stepping on these",
            "atm9.quest.pneumatic.subt.reinforcedTube": "Upgraded Pressure Tubes",
            "atm9.quest.pneumatic.subt.jackhammer": "Sorry Jack!",
            "atm9.quest.pneumatic.subt.netheriteBit": "This is the Best Bit!",
            "atm9.quest.pneumatic.subt.autoPlastic": "Very useful!",
            "atm9.quest.pneumatic.subt.empty": "The path to PCBs",
            "atm9.quest.pneumatic.subt.amadron": "I wonder if they have 1 day delivery?",
            "atm9.quest.pneumatic.subt.blueprint": "Thanks Amadron!",
            "atm9.quest.pneumatic.subt.charging": "I hope you have some Sunglasses!",
            "atm9.quest.pneumatic.subt.tank": "It's Etching Time!",
            "atm9.quest.pneumatic.subt.acid": "Don't Ask...",
            "atm9.quest.pneumatic.subt.unassembled": "Almost there!",
            "atm9.quest.pneumatic.subt.finished": "You did it!",
            "atm9.quest.pneumatic.subt.needed": "Needed for Finished PCBs",
            "atm9.quest.pneumatic.subt.micromissiles": "Firing Explosions from afar!",
            "atm9.quest.pneumatic.subt.max1": "Max: 1",
            "atm9.quest.pneumatic.subt.max4": "Max: 4",
            "atm9.quest.pneumatic.subt.max6": "Max: 6",
            "atm9.quest.pneumatic.subt.jetBoots": "You thought using an Elytra was hard?",
            "atm9.quest.pneumatic.subt.always": "Always Required",
            "atm9.quest.pneumatic.subt.laser": "Required for Laser recipes",
            "atm9.quest.pneumatic.subt.drill": "Required for Drill recipes",
            "atm9.quest.pneumatic.subt.assembly": "Some Assembly Required",
            "atm9.quest.pneumatic.subt.tile": "Signs? Never heard of them!",
            "atm9.quest.pneumatic.subt.advancedTube": "A Whole new Level!",
            "atm9.quest.pneumatic.subt.power": "Pressure = Power",
            "atm9.quest.pneumatic.subt.fluxCompressor": "Power = Pressure",
            "atm9.quest.pneumatic.subt.aerial": "Going Wireless!",
            "atm9.quest.pneumatic.subt.advancedCompressor": "No need for boring old Air Compressors now!",
            "atm9.quest.pneumatic.subt.upgrades": "Very useful stuff",
            "atm9.quest.pneumatic.subt.range": "Come Closer!",
            "atm9.quest.pneumatic.subt.volume": "Turn it up!",
            "atm9.quest.pneumatic.subt.security": "Secure!",
            "atm9.quest.pneumatic.subt.speed": "Speedy!",
            "atm9.quest.pneumatic.subt.dispenser": "Dispense her? I hardly know her!",
            "atm9.chapters.67.title": "Pneumatic Craft",
            "atm9.chapters.20.title": "Industrial Foregoing"
})

static_items = [
    'backwards_ingot',
    'unrefined_substrate', 
    'digital_circuit_board',
    'electronic_circuit_board', 
    'creative_flight_core', 
    'quantum_circuit', 
    'beating_heart',
    'endergem',
    'codex',
    'dynamite',
    'diode', 
    'neutron_nugget', 
    'genetic_material_c', 
    'record_fragment', 
    'neutron_gear', 
    'and_gate', 
    'memory_management_unit', 
    'mutated_genetic_material', 
    'digital_circuit',
    'unstable_mutex',
    'genetic_sample',
    'electronic_circuit', 
    'qubit', 
    'singularity',
    'or_gate', 
    'neutron_pile', 
    'analog_circuit_board', 
    'steel_upgrade', 
    'transistor', 
    'antimatter_tormentum',
    'photonic_flux_capacitor',
    'abyssium_ingot',
    'ultradense_metal_ball', 
    'analog_circuit', 
    'genetic_material_a', 
    'capacitor_array', 
    'refined_substrate', 
    'infinity_totem', 
    'infinity_catalyst', 
    'cosmic_stew', 
    'endest_pearl',
    'stellar_catalyst',
    'arithmetic_logic_unit',
    'mosfet', 
    'infinity_ingot', 
    'processing_unit_board', 
    'infinity_nugget', 
    'cooling_cell', 
    'random_access_memory', 
    'combined_bucket', 
    'vial', 
    'matter_cluster',
    'multi_biome_mining_lens', 
    'rubber_sheet', 
    'inductor', 
    'silicon_wafer', 
    'quantum_upgrade', 
    'capacitor', 
    'diamond_lattice', 
    'redstone_control_module_high', 
    'processing_unit', 
    'god_forged_ingot', 
    'op_amp', 
    'beaker', 
    'neutron_ingot', 
    'hyper_dimensional_data_transceiver', 
    'resistor', 
    'interplanetary_mining_lens', 
    'not_gate', 
    'crystal_matrix_ingot', 
    'genetic_material_b', 
    'wrench', 
    'small_heat_exchanger', 
    'cosmic_meatballs', 
    'quantum_circuit_board',
    'blank_record',
    'penfolds_grange',
    'tier_2_rocket_fin',
    'tier_3_rocket_fin',
    'tier_4_rocket_fin',
    'tier_5_rocket_fin',
    'tier_6_rocket_fin',
    'tier_7_rocket_fin',
    'tier_2_rocket_nose_cone',
    'tier_3_rocket_nose_cone',
    'tier_4_rocket_nose_cone',
    'tier_5_rocket_nose_cone',
    'tier_6_rocket_nose_cone',
    'tier_7_rocket_nose_cone',
    'rocket_fuselage',
    'tier_2_rocket_fuselage',
    'tier_3_rocket_fuselage',
    'tier_4_rocket_fuselage',
    'tier_5_rocket_fuselage',
    'tier_6_rocket_fuselage',
    'tier_7_rocket_fuselage',
    'aether_essence',
    'evil_codex',
    'rotten_heart',
    'tribal_totem',
    'ancient_king_scepter',
    'frozen_grail',
    'sack_of_magic_dust',
    'undead_essence',
    'black_pearl',
    'frozen_skull',
    'shackles',
    'unholy_candle',
    'corrupted_grail',
    'golden_lantern',
    'silver_feather',
    'warden_ears',
    'corruption_orb',
    'magnificent_clam_shell',
    'skeletal_hand',
    'wicked_tentacle',
    'crystal_petal',
    'molten_tablet',
    'steel_heart',
    'wildfire_essence',
    'cursed_crown',
    'mossy_tablet',
    'stone_idol',
    'wither_ribcage',
    'deep_see_crystal',
    'pharaohs_mask',
    'supercooled_core',
    'desert_rose',
    'red_urn',
    'toxic_vial'
]


for item in sorted(static_items):
    add_static_asset_item(datapack, item, item.replace('_', ' ').title())


earth_ores = [
    ('chlorine', "#minecraft:is_overworld"),
    ('nitrogen', "#minecraft:is_overworld"),
    ('sodium', "#minecraft:is_overworld"),
    ('potassium', "#minecraft:is_overworld"),
    # ('antimony', "ad_astra:glacio_snowy_barrens"),
    # ('antimony', "ad_astra:glacio_ice_peaks"),
    # ('iumium', "ad_astra:glacio_snowy_barrens"),
    # ('iumium', "ad_astra:glacio_ice_peaks"),
    # ('xenon', "ad_astra:infernal_venus_barrens"),
    # ('xenon', "ad_astra:venus_wastelands"),
    # ('radon', "ad_astra:infernal_venus_barrens"),
    # ('radon', "ad_astra:venus_wastelands"),
    # ('neon', "ad_astra:infernal_venus_barrens"),
    # ('neon', "ad_astra:venus_wastelands"),
    # ('adamantium', "ad_astra:mercury_deltas"),
    # ('carbonadium', "ad_astra:mercury_deltas"),
    # ('cobalt', "ad_astra:martian_canyon_creek"),
    # ('cobalt', "ad_astra:martian_polar_caps"),
    # ('cobalt', "ad_astra:martian_wastelands"),
    # ('bromine', "ad_astra:martian_canyon_creek"),
    # ('bromine', "ad_astra:martian_polar_caps"),
    # ('bromine', "ad_astra:martian_wastelands"),
]

# for ore, biome_tag in earth_ores:
#     placed_feature = PLACED_ORE_FEATURE(ore)
#     configured_feature = CONFIGURED_ORE_FEATURE(ore)
#     biome_modifier = FORGE_BIOME_MODIFIER(ore, biome_tag=biome_tag)

#     with open(f'./resources/data/astrocraft/worldgen/placed_feature/ore_{ore}_placed.json', 'w') as placed_feature_file:
#         placed_feature_file.write(placed_feature)

#     with open(f'./resources/data/astrocraft/forge/biome_modifier/astrocraft/{ore}_ore.json', 'w') as modifier_file:
#         modifier_file.write(biome_modifier)

#     with open(f'./resources/data/astrocraft/worldgen/configured_feature/{ore}_ore.json', 'w') as configured_feature_file:
#         configured_feature_file.write(configured_feature)



datapack.add_item_tag('forge:genetic_material', 'astrocraft:genetic_material_a')
datapack.add_item_tag('forge:genetic_material', 'astrocraft:genetic_material_b')
datapack.add_item_tag('forge:genetic_material', 'astrocraft:genetic_material_c')
datapack.add_item_tag('forge:genetic_material', 'astrocraft:mutated_genetic_material')
datapack.add_fluid_tag('forge:flux_resin', 'astrocraft:soldering_flux')
datapack.add_fluid_tag('forge:flux_resin', 'astrocraft:ammonium_chloride')
datapack.add_fluid_tag('forge:alcohol', '#forge:ethanol')
#datapack.add_fluid_tag('forge:tier_1_photoresist', '#forge:phenolic_acid')
datapack.add_fluid_tag('forge:tier_2_photoresist', 'astrocraft:polymethyl_methacrylate')
#datapack.add_fluid_tag('forge:photoresist', '#forge:tier_1_photoresist')
datapack.add_fluid_tag('forge:photoresist', '#forge:tier_2_photoresist')

datapack.add_fluid_tag('forge:tier_1_coolant', 'astrocraft:methyl_chloride')
datapack.add_fluid_tag('forge:tier_2_coolant', 'astrocraft:gelid_cryotheum')

# datapack.add_block_tag('astrocraft:locked_block/overworld', "minecraft:chest")
# datapack.add_block_tag('astrocraft:locked_block/end', "minecraft:chest")
# datapack.add_block_tag('astrocraft:locked_block/nether', "minecraft:chest")
# datapack.add_block_tag('astrocraft:locked_block/transcendent', "minecraft:chest")
datapack.add_item_tag("curios:key", 'astrocraft:tree_key')
datapack.add_item_tag("curios:key", 'astrocraft:overworld_key')
datapack.add_item_tag("curios:key", 'astrocraft:nether_key')
datapack.add_item_tag("curios:key", 'astrocraft:end_key')
datapack.add_item_tag("curios:key", 'astrocraft:transcendent_key')

datapack.add_item_tag("forge:enderio_staff", 'astrocraft:tablet')
datapack.add_item_tag("forge:wrench", 'astrocraft:tablet')
datapack.add_item_tag("forge:tools/wrench", 'astrocraft:tablet')
datapack.add_item_tag("forge:tools/wire_cutters", 'astrocraft:tablet')

datapack.add_item_tag("astrocraft:seed_groups/grass", "minecraft:wheat_seeds")
datapack.add_item_tag("astrocraft:seed_groups/dirt", "minecraft:potato" )
datapack.add_item_tag("astrocraft:seed_groups/vege", "minecraft:carrot" )


datapack.add_simple_item_model("harddrive")
datapack.add_simple_item_model("nasdrive")

datapack.add_block_item_model("vacuum_freezer_controller")
datapack.add_simple_item_model("crop_sticks")
datapack.add_block_item_model("fluid_input_hatch")
datapack.add_simple_block_model("fluid_input_hatch")

datapack.add_block_item_model("regulated_machine_casing")
datapack.add_simple_block_model("regulated_machine_casing")

datapack.add_block_item_model("energy_input_hatch")
datapack.add_simple_block_model("energy_input_hatch")

datapack.add_block_item_model("fluid_output_hatch")
datapack.add_simple_block_model("fluid_output_hatch")

datapack.add_block_item_model("knapping_stone")
datapack.make_block_mineable("knapping_stone")
datapack.make_block_mineable("crop_sticks")
datapack.make_block_mineable("fluid_input_hatch")
datapack.make_block_mineable("regulated_machine_casing")
datapack.make_block_mineable("energy_input_hatch")
datapack.make_block_mineable("fluid_output_hatch")
datapack.make_block_mineable("assembler")
datapack.make_block_mineable("advanced_assembler")
datapack.make_block_mineable("chemical_reactor")
datapack.make_block_mineable("electrolytic_seperator")
datapack.make_block_mineable("pyrolysis_chamber")
datapack.make_block_mineable("euv_machine")
datapack.make_block_mineable("coolant_block")
datapack.make_block_mineable("cleanroom_wall")
datapack.make_block_mineable("nic_block")
datapack.make_block_mineable("brick_kiln")

datapack.add_block_item_model("brick_kiln")

datapack.add_block_tag(f"minecraft:mineable/pickaxe", f"astrocraft:brick_kiln_pile")


datapack.add_item_tag('forge:chemical_protection', 'minecraft:leather_helmet')
datapack.add_item_tag('forge:chemical_protection', 'minecraft:leather_chestplate')
datapack.add_item_tag('forge:chemical_protection', 'minecraft:leather_leggings')
datapack.add_item_tag('forge:chemical_protection', 'minecraft:leather_boots')

datapack.add_item_tag('minecraft:music_discs', 'astrocraft:help_of_disc')
datapack.add_item_tag('minecraft:music_discs', 'astrocraft:as_an_llm_disc')
datapack.add_item_tag('minecraft:music_discs', 'astrocraft:run_nic_run_disc')
datapack.add_item_tag('minecraft:music_discs', 'astrocraft:stolen_identity_disc')
datapack.add_item_tag('minecraft:music_discs', 'astrocraft:bangarang_disc')
datapack.add_item_tag('minecraft:music_discs', 'astrocraft:mir_disc')

datapack.add_simple_item_model('airhorn')

datapack.add_simple_item_model('tree_key')
datapack.add_simple_item_model('overworld_key')
datapack.add_simple_item_model('end_key')
datapack.add_simple_item_model('nether_key')
datapack.add_simple_item_model('transcendent_key')
datapack.add_simple_item_model('fire_starter')
datapack.add_simple_item_model('twig_bundle')
datapack.add_simple_item_model('help_of_disc')
datapack.add_simple_item_model('tablet')
datapack.add_simple_item_model('bangarang_disc')
datapack.add_simple_item_model('as_an_llm_disc')
datapack.add_simple_item_model('run_nic_run_disc')
datapack.add_simple_item_model('mir_disc')
datapack.add_simple_item_model('stolen_identity_disc')
datapack.add_simple_item_model('zeolite_catalyst')
datapack.add_simple_item_model('monocrystalline_silicon')
datapack.add_simple_item_model('cable_laying_tool')

TAB_FILE = '../java/net/astr0/astrocraft/ModCreativeModTab.java'
ITEM_FILE = '../java/net/astr0/astrocraft/item/ModItems.java'
FLUIDS_FILE = '../java/net/astr0/astrocraft/Fluid/ModFluids.java' 
SLURRY_FILE = '../java/net/astr0/astrocraft/compat/mek/AstrocraftSlurries.java'
JEI_FILE = "../java/net/astr0/astrocraft/compat/JEI/AstrocraftJEIPlugin.java"

for chemdef in chemicals:
    plain_text_name = chemdef['Name']
    fluid_name = chemdef['MATERIAL_ID']

    hazard_types = {
        'Asphyxiant' : 'HazardBehavior.BehaviorType.SUFFOCATE',
        'Explosive' : 'HazardBehavior.BehaviorType.EXPLOSION',
        'Radioactive' : 'HazardBehavior.BehaviorType.RADIO',
        'Freezing' : 'HazardBehavior.BehaviorType.FREEZE',
        'Hot' : 'HazardBehavior.BehaviorType.HEAT',
        'EXTREME' : 'HazardBehavior.BehaviorType.EXTREME'
    }

    hazard = hazard_types[chemdef['Hazard Type']] if chemdef.get('Hazard Type') in hazard_types else 'HazardBehavior.BehaviorType.NONE'

    datapack.add_text_to_region(FLUIDS_FILE, 'FLUID_REGION', f"""
    public static final RegistryObject<FluidType> {fluid_name.upper()}_FLUID_TYPE = registerType("{fluid_name}", "{chemdef['Form']}", "{chemdef['Color']}", {hazard});
    public static final RegistryObject<FlowingFluid> SOURCE_{fluid_name.upper()} = FLUIDS.register("{fluid_name}",
            () -> new ForgeFlowingFluid.Source(ModFluids.{fluid_name.upper()}_FLUID_PROPERTIES));
    public static final RegistryObject<FlowingFluid> FLOWING_{fluid_name.upper()} = FLUIDS.register("flowing_{fluid_name}",
            () -> new ForgeFlowingFluid.Flowing(ModFluids.{fluid_name.upper()}_FLUID_PROPERTIES));
    public static final ForgeFlowingFluid.Properties {fluid_name.upper()}_FLUID_PROPERTIES = new ForgeFlowingFluid.Properties(
            {fluid_name.upper()}_FLUID_TYPE, SOURCE_{fluid_name.upper()}, FLOWING_{fluid_name.upper()})
            .slopeFindDistance(2).levelDecreasePerBlock(2).block(ModBlocks.registerFluidBlock("{fluid_name}", SOURCE_{fluid_name.upper()}))
            .bucket(ModItems.registerBucketItem("{fluid_name}", SOURCE_{fluid_name.upper()}));
    """)

    if hazard != 'HazardBehavior.BehaviorType.NONE':
        datapack.add_text_to_region(JEI_FILE, 'INFO_REGION', 
            f"""registration.addIngredientInfo(new FluidStack(ModFluids.FLOWING_{fluid_name.upper()}.get().getSource(), 1000), 
            ForgeTypes.FLUID_STACK, 
            Component.literal("{chemdef['Description']}"),
            Component.literal("\\nThis chemical is §c§nHazardous§r and may inflict {hazard.split('.')[-1]}. §6Chemical protection§r§0 is recommended.")
            );""")
    else:
        datapack.add_text_to_region(JEI_FILE, 'INFO_REGION', f"""registration.addIngredientInfo(new FluidStack(ModFluids.FLOWING_{fluid_name.upper()}.get().getSource(), 1000), ForgeTypes.FLUID_STACK, Component.literal("{chemdef['Description']}"));""")

    datapack.add_text_to_region(FLUIDS_FILE, 'RENDER_REGION', f"ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_{fluid_name.upper()}.get(), RenderType.translucent());")
    datapack.add_text_to_region(FLUIDS_FILE, 'RENDER_REGION', f"ItemBlockRenderTypes.setRenderLayer(ModFluids.SOURCE_{fluid_name.upper()}.get(), RenderType.translucent());")

    output_image_path = f'../resources/assets/astrocraft/textures/item/{fluid_name}_bucket.png'

    datapack.add_translation(f"tooltip.{fluid_name}.fluid", f"§e{chemdef['Formula']}§r")
    datapack.add_translation(f"item.astrocraft.{fluid_name}_bucket", f"{plain_text_name}")
    datapack.add_translation(f"fluid_type.astrocraft.{fluid_name}", f"{plain_text_name}")


    datapack.add_translation(f"slurry.astrocraft.dirty_{fluid_name}", f"Dirty {plain_text_name} Slurry")
    datapack.add_translation(f"slurry.astrocraft.clean_{fluid_name}", f"Clean {plain_text_name} Slurry")

    datapack.add_translation(f"tooltip.{fluid_name}.material", f"§e{chemdef['Formula']}§r")

    datapack.add_simple_item_model(f'{fluid_name}_bucket')

    datapack.add_fluid_tag(f"forge:{fluid_name}", f"astrocraft:{fluid_name}")

    layer_images(base_image_path, top_image_path, output_image_path, hex_to_rgb(chemdef["Color"]), chemdef['Form'])

    add_simple_tint_item(datapack, f"{fluid_name}_dust", f"{plain_text_name} Dust", chemdef["Color"], get_texture(fluid_name, dust_textures), fluid_name, hazard)
    datapack.add_item_tag(f"forge:dusts/{fluid_name}", f"astrocraft:{fluid_name}_dust")

    datapack.add_generic_recipe(f"thermal/crystal/crystal_{fluid_name}", CRYSTALISE(fluid_name, fluid_name))

    if chemdef['Type'] == 'Chemical': continue

    # add_simple_tint_item(datapack, f"{fluid_name}_screw", f"{plain_text_name} Screw", chemdef["Color"], './templates/screw.png', fluid_name, hazard)
    add_simple_tint_item(datapack, f"{fluid_name}_rod", f"{plain_text_name} Rod", chemdef["Color"], get_texture(fluid_name, rod_textures), fluid_name, hazard)
    add_simple_tint_item(datapack, f"{fluid_name}_ingot", f"{plain_text_name} Ingot", chemdef["Color"], get_texture(fluid_name, ingot_textures), fluid_name, hazard)
    add_simple_tint_item(datapack, f"{fluid_name}_plate", f"{plain_text_name} Plate", chemdef["Color"], get_texture(fluid_name, plate_textures), fluid_name, hazard)
    add_simple_tint_item(datapack, f"{fluid_name}_nugget", f"{plain_text_name} Nugget", chemdef["Color"], get_texture(fluid_name, nugget_textures), fluid_name, hazard)
    add_simple_tint_item(datapack, f"{fluid_name}_gear", f"{plain_text_name} Gear", chemdef["Color"], get_texture(fluid_name, gear_textures), fluid_name, hazard)
    # add_simple_tint_item(datapack, f"{fluid_name}_ring", f"{plain_text_name} Ring", chemdef["Color"], './templates/ring.png', fluid_name, hazard)
    # add_simple_tint_item(datapack, f"{fluid_name}_curved_plate", f"{plain_text_name} Curved Plate", chemdef["Color"], './templates/curved_plate.png', fluid_name, hazard)
    add_simple_tint_item(datapack, f"{fluid_name}_wire", f"{plain_text_name} Wire", chemdef["Color"], './templates/wire.png', fluid_name, hazard)
    add_simple_tint_block(datapack, f"{fluid_name}_block", f"{plain_text_name} Block", chemdef["Color"], get_texture(fluid_name, block_textures))

    datapack.add_item_tag(f"forge:storage_blocks/{fluid_name}", f"astrocraft:{fluid_name}_block")
    datapack.add_block_tag(f"forge:storage_blocks/{fluid_name}", f"astrocraft:{fluid_name}_block")
    datapack.add_simple_block_loot(f"{fluid_name}_block")
    datapack.add_block_tag(f"minecraft:mineable/pickaxe", f"astrocraft:{fluid_name}_block")

    datapack.add_block_tag("minecraft:mineable/axe", "astrocraft:log_pile")

    datapack.add_block_tag("astrocraft:cable_block", "minecraft:deepslate")
    datapack.add_block_tag("astrocraft:cable_block", "mekanism:basic_universal_cable")
    
    datapack.add_item_tag(f"forge:ingots/{fluid_name}", f"astrocraft:{fluid_name}_ingot")
    datapack.add_item_tag(f"forge:nuggets/{fluid_name}", f"astrocraft:{fluid_name}_nugget")
    datapack.add_item_tag(f"forge:rods/{fluid_name}", f"astrocraft:{fluid_name}_rod")
    datapack.add_item_tag(f"forge:plates/{fluid_name}", f"astrocraft:{fluid_name}_plate")
    datapack.add_item_tag(f"forge:wires/{fluid_name}", f"astrocraft:{fluid_name}_wire")
    datapack.add_item_tag(f"forge:gears/{fluid_name}", f"astrocraft:{fluid_name}_gear")
    datapack.add_smelting_recipe(f'smelting/{fluid_name}_from_{fluid_name}_dust', f"forge:dusts/{fluid_name}", f"astrocraft:{fluid_name}_ingot")

    datapack.add_generic_recipe(f"thermal/melt/melt_{fluid_name}", MELT(fluid_name, fluid_name))

    if chemdef['Type'] == 'Element':
        add_simple_tint_item(datapack, f"raw_{fluid_name}", f"Raw {plain_text_name}", chemdef["Color"], get_texture(fluid_name, raw_ore_textures), fluid_name, hazard)
        datapack.add_item_tag(f"forge:raw_materials/{fluid_name}", f"astrocraft:raw_{fluid_name}")
        datapack.add_smelting_recipe(f'smelting/{fluid_name}_from_{fluid_name}_raw', f"forge:raw_materials/{fluid_name}", f"astrocraft:{fluid_name}_ingot")
        add_simple_ore_block(datapack, f"{fluid_name}_ore", f"{plain_text_name} Ore", chemdef["Color"], get_texture(fluid_name, ore_textures))
        add_simple_ore_block(datapack, f"deepslate_{fluid_name}_ore", f"Deepslate {plain_text_name} Ore", chemdef["Color"], get_texture(fluid_name, ore_textures))
        datapack.add_block_tag("minecraft:mineable/pickaxe", f"astrocraft:{fluid_name}_ore")
        datapack.add_block_tag("minecraft:mineable/pickaxe", f"astrocraft:deepslate_{fluid_name}_ore")
        datapack.add_block_tag("minecraft:needs_iron_tool", f"astrocraft:deepslate_{fluid_name}_ore")
        datapack.add_block_tag("minecraft:needs_iron_tool", f"astrocraft:{fluid_name}_ore")
        datapack.add_ore_block_loot(f"{fluid_name}_ore", f"raw_{fluid_name}")
        datapack.add_ore_block_loot(f"deepslate_{fluid_name}_ore", f"raw_{fluid_name}")

        datapack.add_item_tag(f"forge:ores/{fluid_name}", f"astrocraft:{fluid_name}_ore")
        datapack.add_item_tag(f"forge:ores/{fluid_name}", f"astrocraft:deepslate_{fluid_name}_ore")
        datapack.add_block_tag(f"forge:ores/{fluid_name}", f"astrocraft:{fluid_name}_ore")
        datapack.add_block_tag(f"forge:ores/{fluid_name}", f"astrocraft:deepslate_{fluid_name}_ore")

        datapack.add_smelting_recipe(f'smelting/{fluid_name}_from_{fluid_name}_ore', f"forge:ores/{fluid_name}", f"astrocraft:{fluid_name}_ingot")

        add_mek_tint_item(datapack, f"{fluid_name}_clump", f"{plain_text_name} Clump", chemdef["Color"], CLUMP_TEXTURE, CLUMP_TEXTURE_OVERLAY, fluid_name, hazard)
        datapack.add_item_tag(f"mekanism:clumps/{fluid_name}", f"astrocraft:{fluid_name}_clump")
        add_mek_tint_item(datapack, f"{fluid_name}_dirty_dust", f"{plain_text_name} Dirty Dust", chemdef["Color"], DIRTY_DUST_TEXTURE, DIRTY_DUST_TEXTURE_OVERLAY, fluid_name, hazard)
        datapack.add_item_tag(f"mekanism:dirty_dusts/{fluid_name}", f"astrocraft:{fluid_name}_dirty_dust")
        add_mek_tint_item(datapack, f"{fluid_name}_crystal", f"{plain_text_name} Crystal", chemdef["Color"], CRYSTAL_TEXTURE, CRYSTAL_TEXTURE_OVERLAY, fluid_name, hazard)
        datapack.add_item_tag(f"mekanism:crystals/{fluid_name}", f"astrocraft:{fluid_name}_crystal")
        add_mek_tint_item(datapack, f"{fluid_name}_shard", f"{plain_text_name} Shard", chemdef["Color"], SHARD_TEXTURE, SHARD_TEXTURE_OVERLAY, fluid_name, hazard)
        datapack.add_item_tag(f"mekanism:shards/{fluid_name}", f"astrocraft:{fluid_name}_shard")

        # Example usage
        template_dir = './templates/mek_processing/reference'
        dest_dir = f'../resources/data/astrocraft/recipes/compat/mek_processing/{fluid_name}'
        substitutions = {
            '$FLUID_TYPE$': fluid_name
        }

        copy_and_substitute(template_dir, dest_dir, substitutions)

        datapack.add_text_to_region(SLURRY_FILE, 'SLURRY_REGION', f"""public static SlurryRegistryObject<Slurry, Slurry> {fluid_name.upper()}_SLURRY = SLURRIES.register("{fluid_name}", "{chemdef['Color']}", new ResourceLocation("forge","tags/items/ores/{fluid_name}"));""")

    datapack.add_generic_recipe(f"compat/immersive/plate/{fluid_name}_plate", JSON(
        {
  "type": "immersiveengineering:metal_press",
  "energy": 2400,
  "input": {
    "tag": f"forge:ingots/{fluid_name}"
  },
  "mold": "immersiveengineering:mold_plate",
  "result": {
    "tag": f"forge:plates/{fluid_name}"
  }
}
    ))
    datapack.add_generic_recipe(f"crafting/{fluid_name}_ingot_to_nugget", UNCOMPACTING_RECIPE(f"astrocraft:{fluid_name}_ingot", f"astrocraft:{fluid_name}_nugget"))
    datapack.add_generic_recipe(f"compat/thermal/plate/{fluid_name}_ingot_to_plate", JSON(
{
  "type": "thermal:press",
  "ingredient": {
    "tag": f"forge:ingots/{fluid_name}"
  },
  "result": [
    {
      "item": f"astrocraft:{fluid_name}_plate"
    }
  ]
}
    ))
    datapack.add_generic_recipe(f"crafting/{fluid_name}_block_to_ingot", UNCOMPACTING_RECIPE(f"astrocraft:{fluid_name}_block", f"astrocraft:{fluid_name}_ingot"))
    
    datapack.add_generic_recipe(f"compat/thermal/pulv/{fluid_name}_ingot_to_dust", JSON(
        {
  "type": "thermal:pulverizer",
  "ingredient": {
    "tag": f"forge:ingots/{fluid_name}"
  },
  "result": [
    {
      "item": f"astrocraft:{fluid_name}_dust",
      "count": 1
    }
  ]
}
    ))

    datapack.add_generic_recipe(f"compat/thermal/pulv/{fluid_name}_plate_to_dust", JSON(
        {
  "type": "thermal:pulverizer",
  "ingredient": {
    "tag": f"forge:plates/{fluid_name}"
  },
  "result": [
    {
      "item": f"astrocraft:{fluid_name}_dust",
      "count": 1
    }
  ]
}
    ))
    
    datapack.add_generic_recipe(f"compat/mek/sawing/{fluid_name}_plate_to_rod", f"""{{
  "type": "mekanism:sawing",
  "input": {{
    "ingredient": {{
      "item": "astrocraft:{fluid_name}_plate"
    }}
  }},
  "mainOutput": {{
    "count": 3,
    "item": "astrocraft:{fluid_name}_rod"
  }},
  "secondaryChance": 0.25,
  "secondaryOutput": {{
    "item": "astrocraft:{fluid_name}_dust"
  }}
}}
""")
    
#     datapack.add_generic_recipe(f"compat/mek/sawing/{fluid_name}_nugget_to_screw", f"""{{
#   "type": "mekanism:sawing",
#   "input": {{
#     "ingredient": {{
#       "item": "astrocraft:{fluid_name}_nugget"
#     }}
#   }},
#   "mainOutput": {{
#     "count": 1,
#     "item": "astrocraft:{fluid_name}_screw"
#   }}
# }}
# """)
    
    datapack.add_generic_recipe(f"crafting/{fluid_name}_nugget_to_ingot", f"""{{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {{
    "#": {{
      "tag": "forge:nuggets/{fluid_name}"
    }},
    "P": {{
      "item": "astrocraft:{fluid_name}_nugget"
    }}
  }},
  "pattern": [
    "###",
    "#P#",
    "###"
  ],
  "result": {{
    "count": 1,
    "item": "astrocraft:{fluid_name}_ingot"
  }}
}}
""")
    
    datapack.add_generic_recipe(f"crafting/{fluid_name}_ingot_to_block", f"""{{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {{
    "#": {{
      "item": "astrocraft:{fluid_name}_ingot"
    }}
  }},
  "pattern": [
    "###",
    "###",
    "###"
  ],
  "result": {{
    "count": 1,
    "item": "astrocraft:{fluid_name}_block"
  }}
}}
""")


datapack.write_to_disk()
# Example usage
source_dir = "./resources"
destination_dir = "../resources/"

copy_and_overwrite(source_dir, destination_dir)
