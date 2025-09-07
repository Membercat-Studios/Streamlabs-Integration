package me.Domplanto.streamLabs.action.collection;

import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.tags.BlockTypeTagKeys;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import io.papermc.paper.registry.keys.tags.EntityTypeTagKeys;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import me.Domplanto.streamLabs.condition.Condition;
import me.Domplanto.streamLabs.condition.ConditionGroup;
import me.Domplanto.streamLabs.config.ActionPlaceholder;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockType;
import org.bukkit.block.TileState;
import org.bukkit.block.data.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.ItemType;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

@SuppressWarnings({"UnstableApiUsage", "unused"})
public class Collections {
    public static final NamedCollection<?> EMPTY = new NamedCollection.SimpleCollection<>(s -> Stream.of(), o -> "");
    public static final NamedCollection<OfflinePlayer> PLAYER = new NamedCollection.SimpleCollection<>(s -> Arrays.stream(s.getOfflinePlayers()), p -> p.getUniqueId().toString())
            .withNameFunction(p -> Optional.ofNullable(p.getPlayer()).map(Player::displayName).orElse(null))
            .withProperty("online", OfflinePlayer::isOnline)
            .withProperty("banned", OfflinePlayer::isBanned)
            .withProperty("operator", OfflinePlayer::isOp)
            .withProperty("flying", p -> ofPlayer(p, Player::isFlying))
            .withProperty("sleeping", p -> ofPlayer(p, Player::isSleeping))
            .withProperty("sneaking", p -> ofPlayer(p, Player::isSneaking))
            .withProperty("sprinting", p -> ofPlayer(p, Player::isSprinting))
            .withProperty("dead", p -> ofPlayer(p, Player::isDead))
            .withProperty("under_water", p -> ofPlayer(p, Player::isUnderWater))
            .withDefaultFilter(ConditionGroup.of(ConditionGroup.Mode.AND, Condition.ofStaticEquals(placeholder("online"), Boolean.TRUE.toString())));
    public static final NamedCollection<EntityType> ENTITY_TYPE = new NamedCollection.RegistryCollection<>(RegistryKey.ENTITY_TYPE)
            .withTagProperty("aquatic", EntityTypeTagKeys.AQUATIC)
            .withTagProperty("arthropod", EntityTypeTagKeys.ARTHROPOD)
            .withTagProperty("undead", EntityTypeTagKeys.UNDEAD)
            .withTagProperty("boat", EntityTypeTagKeys.BOAT)
            .withTagProperty("arrow", EntityTypeTagKeys.ARROWS)
            .withTagProperty("projectile", EntityTypeTagKeys.IMPACT_PROJECTILES)
            .withTagProperty("illager", EntityTypeTagKeys.ILLAGER)
            .withTagProperty("raider", EntityTypeTagKeys.RAIDERS)
            .withTagProperty("skeleton", EntityTypeTagKeys.SKELETONS)
            .withTagProperty("zombie", EntityTypeTagKeys.ZOMBIES)
            .withTagProperty("can_equip_saddle", EntityTypeTagKeys.CAN_EQUIP_SADDLE)
            .withTagProperty("can_breathe_under_water", EntityTypeTagKeys.CAN_BREATHE_UNDER_WATER)
            .withTagProperty("can_walk_on_powder_snow", EntityTypeTagKeys.POWDER_SNOW_WALKABLE_MOBS)
            .withTagProperty("deflects_projectiles", EntityTypeTagKeys.DEFLECTS_PROJECTILES)
            .withTagProperty("fall_damage_immune", EntityTypeTagKeys.FALL_DAMAGE_IMMUNE)
            .withProperty("alive", EntityType::isAlive)
            .withProperty("spawnable", EntityType::isSpawnable)
            .withDefaultFilter(ConditionGroup.of(ConditionGroup.Mode.AND, Condition.ofStaticEquals(placeholder("spawnable"), Boolean.TRUE.toString())));
    public static final NamedCollection<Enchantment> ENCHANTMENT = new NamedCollection.RegistryCollection<>(RegistryKey.ENCHANTMENT)
            .withTagProperty("curse", EnchantmentTagKeys.CURSE)
            .withTagProperty("tradeable", EnchantmentTagKeys.TRADEABLE)
            .withTagProperty("treasure", EnchantmentTagKeys.TREASURE)
            .withTagProperty("in_enchanting_table", EnchantmentTagKeys.IN_ENCHANTING_TABLE)
            .withTagProperty("on_random_loot", EnchantmentTagKeys.ON_RANDOM_LOOT)
            .withTagProperty("on_mob_equipment", EnchantmentTagKeys.ON_MOB_SPAWN_EQUIPMENT)
            .withTagProperty("on_traded_equipment", EnchantmentTagKeys.ON_TRADED_EQUIPMENT)
            .withTagProperty("has_double_trade_price", EnchantmentTagKeys.DOUBLE_TRADE_PRICE)
            .withProperty("description", Enchantment::description)
            .withProperty("start_level", Enchantment::getStartLevel)
            .withProperty("max_level", Enchantment::getMaxLevel)
            .withProperty("anvil_cost", Enchantment::getAnvilCost);
    public static final NamedCollection<PotionEffectType> STATUS_EFFECT = new NamedCollection.RegistryCollection<>(RegistryKey.MOB_EFFECT)
            .withProperty("instant", PotionEffectType::isInstant)
            .withProperty("category", PotionEffectType::getEffectCategory)
            .withProperty("color", e -> TextColor.color(e.getColor().asARGB()))
            .withProperty("category_color", e -> e.getEffectCategory().getColor());
    public static final NamedCollection<ItemType> ITEM = new NamedCollection.RegistryCollection<>(RegistryKey.ITEM)
            .withTagProperty("sword", ItemTypeTagKeys.SWORDS)
            .withTagProperty("pickaxe", ItemTypeTagKeys.PICKAXES)
            .withTagProperty("axe", ItemTypeTagKeys.AXES)
            .withTagProperty("shovel", ItemTypeTagKeys.SHOVELS)
            .withTagProperty("hoe", ItemTypeTagKeys.HOES)
            .withTagProperty("helmet", ItemTypeTagKeys.HEAD_ARMOR)
            .withTagProperty("chestplate", ItemTypeTagKeys.CHEST_ARMOR)
            .withTagProperty("leggings", ItemTypeTagKeys.LEG_ARMOR)
            .withTagProperty("boots", ItemTypeTagKeys.FOOT_ARMOR)
            .withTagProperty("bed", ItemTypeTagKeys.BEDS)
            .withTagProperty("anvil", ItemTypeTagKeys.ANVIL)
            .withTagProperty("arrow", ItemTypeTagKeys.ARROWS)
            .withTagProperty("banner", ItemTypeTagKeys.BANNERS)
            .withTagProperty("beacon_payment", ItemTypeTagKeys.BEACON_PAYMENT_ITEMS)
            .withTagProperty("boat", ItemTypeTagKeys.BOATS)
            .withTagProperty("chest_boat", ItemTypeTagKeys.CHEST_BOATS)
            .withTagProperty("bookshelf_book", ItemTypeTagKeys.BOOKSHELF_BOOKS)
            .withTagProperty("lectern_book", ItemTypeTagKeys.LECTERN_BOOKS)
            .withTagProperty("bundle", ItemTypeTagKeys.BUNDLES)
            .withTagProperty("button", ItemTypeTagKeys.BUTTONS)
            .withTagProperty("candles", ItemTypeTagKeys.CANDLES)
            .withTagProperty("coal", ItemTypeTagKeys.COALS)
            .withTagProperty("compass", ItemTypeTagKeys.COMPASSES)
            .withTagProperty("dirt", ItemTypeTagKeys.DIRT)
            .withTagProperty("door", ItemTypeTagKeys.DOORS)
            .withTagProperty("dyeable", ItemTypeTagKeys.DYEABLE)
            .withTagProperty("egg", ItemTypeTagKeys.EGGS)
            .withTagProperty("fence", ItemTypeTagKeys.FENCES)
            .withTagProperty("fence_gate", ItemTypeTagKeys.FENCE_GATES)
            .withTagProperty("fish", ItemTypeTagKeys.FISHES)
            .withTagProperty("flower", ItemTypeTagKeys.FLOWERS)
            .withTagProperty("small_flower", ItemTypeTagKeys.SMALL_FLOWERS)
            .withTagProperty("leaf", ItemTypeTagKeys.LEAVES)
            .withTagProperty("logs", ItemTypeTagKeys.LOGS)
            .withTagProperty("burnable_logs", ItemTypeTagKeys.LOGS_THAT_BURN)
            .withTagProperty("meat", ItemTypeTagKeys.MEAT)
            .withTagProperty("non_burning_wood", ItemTypeTagKeys.NON_FLAMMABLE_WOOD)
            .withTagProperty("piglin_loved", ItemTypeTagKeys.PIGLIN_LOVED)
            .withTagProperty("planks", ItemTypeTagKeys.PLANKS)
            .withTagProperty("rails", ItemTypeTagKeys.RAILS)
            .withTagProperty("sand", ItemTypeTagKeys.SAND)
            .withTagProperty("sapling", ItemTypeTagKeys.SAPLINGS)
            .withTagProperty("shulker_box", ItemTypeTagKeys.SHULKER_BOXES)
            .withTagProperty("sign", ItemTypeTagKeys.SIGNS)
            .withTagProperty("hanging_signs", ItemTypeTagKeys.HANGING_SIGNS)
            .withTagProperty("skull", ItemTypeTagKeys.SKULLS)
            .withTagProperty("slab", ItemTypeTagKeys.SLABS)
            .withTagProperty("stair", ItemTypeTagKeys.STAIRS)
            .withTagProperty("stone_bricks", ItemTypeTagKeys.STONE_BRICKS)
            .withTagProperty("stone_crafting_materials", ItemTypeTagKeys.STONE_CRAFTING_MATERIALS)
            .withTagProperty("terracotta", ItemTypeTagKeys.TERRACOTTA)
            .withTagProperty("trapdoor", ItemTypeTagKeys.TRAPDOORS)
            .withTagProperty("trim_material", ItemTypeTagKeys.TRIM_MATERIALS)
            .withTagProperty("wall", ItemTypeTagKeys.WALLS)
            .withTagProperty("wooden_button", ItemTypeTagKeys.WOODEN_BUTTONS)
            .withTagProperty("wooden_door", ItemTypeTagKeys.WOODEN_DOORS)
            .withTagProperty("wooden_fence", ItemTypeTagKeys.WOODEN_FENCES)
            .withTagProperty("wooden_pressure_plate", ItemTypeTagKeys.WOODEN_PRESSURE_PLATES)
            .withTagProperty("wooden_slab", ItemTypeTagKeys.WOODEN_SLABS)
            .withTagProperty("wooden_stairs", ItemTypeTagKeys.WOODEN_STAIRS)
            .withTagProperty("wooden_trapdoor", ItemTypeTagKeys.WOODEN_TRAPDOORS)
            .withTagProperty("wool", ItemTypeTagKeys.WOOL)
            .withTagProperty("wool_carpet", ItemTypeTagKeys.WOOL_CARPETS)
            .withProperty("block_item", ItemType::hasBlockType)
            .withProperty("associated_block", i -> i.hasBlockType() ? i.getBlockType().key() : null)
            .withProperty("max_durability", ItemType::getMaxDurability)
            .withProperty("max_stack_size", ItemType::getMaxStackSize)
            .withProperty("rarity", ItemType::getItemRarity)
            .withProperty("food", ItemType::isEdible)
            .withProperty("fuel", ItemType::isFuel)
            .withProperty("compostable", ItemType::isCompostable)
            .withProperty("compost_chance", ItemType::getCompostChance);
    public static final NamedCollection<BlockType> BLOCK = new NamedCollection.RegistryCollection<>(RegistryKey.BLOCK)
            .withTagProperty("air", BlockTypeTagKeys.AIR)
            .withTagProperty("valid_spawn", BlockTypeTagKeys.VALID_SPAWN)
            .withTagProperty("wither_immune", BlockTypeTagKeys.WITHER_IMMUNE)
            .withProperty("has_item", BlockType::hasItemType)
            .withProperty("associated_item", bt -> bt.hasItemType() ? bt.getItemType().key() : null)
            .withProperty("blast_resistance", BlockType::getBlastResistance)
            .withProperty("hardness", BlockType::getHardness)
            .withProperty("slipperiness", BlockType::getSlipperiness)
            .withProperty("collision", BlockType::hasCollision)
            .withProperty("gravity", BlockType::hasGravity)
            .withProperty("burnable", BlockType::isBurnable)
            .withProperty("flammable", BlockType::isFlammable)
            .withProperty("occluding", BlockType::isOccluding)
            .withProperty("solid", BlockType::isSolid)
            .withProperty("block_entity", bt -> TileState.class.isAssignableFrom(bt.createBlockData().createBlockState().getClass()))
            .withProperty("bisected", bt -> checkBlockData(Bisected.class, bt))
            .withProperty("directional", bt -> checkBlockData(Directional.class, bt))
            .withProperty("powerable", bt -> checkBlockData(Powerable.class, bt))
            .withProperty("fine_powerable", bt -> checkBlockData(AnaloguePowerable.class, bt))
            .withProperty("ageable", bt -> checkBlockData(Ageable.class, bt))
            .withProperty("randomly_ticking", bt -> bt.createBlockData().isRandomlyTicked())
            .withProperty("light_emission", bt -> bt.createBlockData().getLightEmission())
            .withProperty("piston_behavior", bt -> bt.createBlockData().getPistonMoveReaction())
            .withDefaultFilter(ConditionGroup.of(ConditionGroup.Mode.AND,
                    Condition.invert(Condition.ofStaticEquals(placeholder("air"), Boolean.TRUE.toString())),
                    Condition.invert(Condition.ofStaticEquals(placeholder("wither_immune"), Boolean.TRUE.toString()))
            ));
    public static final NamedCollection<Biome> BIOME = new NamedCollection.RegistryCollection<>(RegistryKey.BIOME);
    public static final NamedCollection<Structure> STRUCTURE = new NamedCollection.RegistryCollection<>(RegistryKey.STRUCTURE);

    private static <T> @Nullable T ofPlayer(@NotNull OfflinePlayer player, @NotNull Function<Player, T> func) {
        return Optional.ofNullable(player.getPlayer()).map(func).orElse(null);
    }

    private static boolean checkBlockData(@NotNull Class<? extends BlockData> cls, @NotNull BlockType type) {
        return cls.isAssignableFrom(type.createBlockData().getClass());
    }

    private static ActionPlaceholder.PlaceholderFunction placeholder(@NotNull String id) {
        return ActionPlaceholder.PlaceholderFunction.of((obj, ctx) -> ctx.scopeStack()
                .getPlaceholders().stream()
                .filter(p -> p.name().equals(id))
                .findAny().map(ActionPlaceholder::function)
                .map(f -> f.execute(obj, ctx)).orElseThrow());
    }

    public static @Nullable NamedCollection<?> fromName(@NotNull String name) {
        try {
            Field field = Collections.class.getDeclaredField(name.toUpperCase());
            if (field.getType() != NamedCollection.class
                    || !Modifier.isPublic(field.getModifiers())
                    || !Modifier.isStatic(field.getModifiers())) return null;
            return (NamedCollection<?>) field.get(null);
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }
}
