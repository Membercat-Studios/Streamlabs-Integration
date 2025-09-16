package me.Domplanto.streamLabs.action.collection;

import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.tags.*;
import me.Domplanto.streamLabs.condition.Condition;
import me.Domplanto.streamLabs.condition.ConditionGroup;
import me.Domplanto.streamLabs.config.placeholder.ActionPlaceholder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockType;
import org.bukkit.block.TileState;
import org.bukkit.block.data.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.ItemType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

@SuppressWarnings({"unused"})
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
            .withDefaultFilter(ConditionGroup.of(ConditionGroup.Mode.AND, Condition.ofStaticEquals(placeholder("online"), Boolean.TRUE.toString())))
            .create();
    public static final NamedCollection<EntityType> ENTITY_TYPE = new NamedCollection.RegistryCollection<>(RegistryKey.ENTITY_TYPE)
            .withProperty("alive", EntityType::isAlive)
            .withProperty("spawnable", EntityType::isSpawnable)
            .withDefaultFilter(ConditionGroup.of(ConditionGroup.Mode.AND, Condition.ofStaticEquals(placeholder("spawnable"), Boolean.TRUE.toString())))
            .create();
    public static final NamedCollection<Enchantment> ENCHANTMENT = new NamedCollection.RegistryCollection<>(RegistryKey.ENCHANTMENT)
            .withProperty("description", NamedCollection.TypedFunction.ofGeneric(Component.class, Enchantment::description))
            .withProperty("weight", Enchantment::getWeight)
            .withProperty("start_level", Enchantment::getStartLevel)
            .withProperty("max_level", Enchantment::getMaxLevel)
            .withProperty("anvil_cost", Enchantment::getAnvilCost)
            .create();
    public static final NamedCollection<PotionEffectType> STATUS_EFFECT = new NamedCollection.RegistryCollection<>(RegistryKey.MOB_EFFECT)
            .withProperty("instant", PotionEffectType::isInstant)
            .withProperty("category", PotionEffectType::getEffectCategory)
            .withProperty("color", e -> TextColor.color(e.getColor().asARGB()))
            .withProperty("category_color", e -> e.getEffectCategory().getColor())
            .withDefaultFilter(ConditionGroup.of(ConditionGroup.Mode.AND, Condition.ofStaticEquals(placeholder("instant"), Boolean.FALSE.toString())))
            .create();
    public static final NamedCollection<ItemType> ITEM = new NamedCollection.RegistryCollection<>(RegistryKey.ITEM)
            .withProperty("block_item", ItemType::hasBlockType)
            .withProperty("associated_block", i -> i.hasBlockType() ? i.getBlockType().key() : null)
            .withProperty("max_durability", ItemType::getMaxDurability)
            .withProperty("max_stack_size", ItemType::getMaxStackSize)
            .withProperty("rarity", ItemType::getItemRarity)
            .withProperty("food", ItemType::isEdible)
            .withProperty("fuel", ItemType::isFuel)
            .withProperty("compostable", ItemType::isCompostable)
            .withProperty("compost_chance", i -> i.isCompostable() ? i.getCompostChance() : null)
            .create();
    public static final NamedCollection<BlockType> BLOCK = new NamedCollection.RegistryCollection<>(RegistryKey.BLOCK)
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
                    Condition.invert(Condition.ofStaticEquals(placeholder("tag:air"), Boolean.TRUE.toString())),
                    Condition.invert(Condition.ofStaticEquals(placeholder("tag:wither_immune"), Boolean.TRUE.toString()))
            )).create();
    public static final NamedCollection<Biome> BIOME = new NamedCollection.RegistryCollection<>(RegistryKey.BIOME);
    public static final NamedCollection<Structure> STRUCTURE = new NamedCollection.RegistryCollection<>(RegistryKey.STRUCTURE);
    public static final NamedCollection<World> WORLD = new NamedCollection.SimpleCollection<>(s -> s.getWorlds().stream(), World::getName)
            .withProperty("uuid", World::getUID)
            .withProperty("generate_structures", World::canGenerateStructures)
            .withProperty("bonus_chest", World::hasBonusChest)
            .withProperty("ceiling", World::hasCeiling)
            .withProperty("sky_light", World::hasSkyLight)
            .withProperty("animals", World::getAllowAnimals)
            .withProperty("monsters", World::getAllowMonsters)
            .withProperty("chunks", World::getChunkCount)
            .withProperty("coordinate_scale", World::getCoordinateScale)
            .withProperty("difficulty", World::getDifficulty)
            .withProperty("min_height", World::getMinHeight)
            .withProperty("max_height", World::getMaxHeight)
            .withProperty("environment", World::getEnvironment)
            .withProperty("pvp", World::getPVP)
            .withProperty("sea_level", World::getSeaLevel)
            .withProperty("world_border", w -> w.getWorldBorder().getSize())
            .withProperty("seed", World::getSeed)
            .create();
    public static final NamedCollection<Plugin> PLUGIN = new NamedCollection.SimpleCollection<>(s -> Arrays.stream(s.getPluginManager().getPlugins()), Plugin::getName)
            .withNameFunction(pl -> Component.text(pl.getPluginMeta().getDisplayName()))
            .withProperty("enabled", Plugin::isEnabled)
            .withProperty("description", pl -> pl.getPluginMeta().getDescription())
            .withProperty("version", pl -> pl.getPluginMeta().getVersion())
            .withProperty("website", pl -> pl.getPluginMeta().getWebsite())
            .withProperty("authors", pl -> String.join(", ", pl.getPluginMeta().getAuthors()))
            .withProperty("contributors", pl -> String.join(", ", pl.getPluginMeta().getContributors()))
            .withProperty("dependencies", pl -> String.join(", ", pl.getPluginMeta().getPluginDependencies()))
            .withProperty("soft_dependencies", pl -> String.join(", ", pl.getPluginMeta().getPluginSoftDependencies()))
            .withProperty("api_version", pl -> pl.getPluginMeta().getAPIVersion())
            .withDefaultFilter(ConditionGroup.of(ConditionGroup.Mode.AND, Condition.ofStaticEquals(placeholder("enabled"), Boolean.TRUE.toString())))
            .create();

    private static <T> @Nullable T ofPlayer(@NotNull OfflinePlayer player, @NotNull Function<Player, T> func) {
        return Optional.ofNullable(player.getPlayer()).map(func).orElse(null);
    }

    private static boolean checkBlockData(@NotNull Class<? extends BlockData> cls, @NotNull BlockType type) {
        return cls.isAssignableFrom(type.createBlockData().getClass());
    }

    private static ActionPlaceholder.PlaceholderFunction placeholder(@NotNull String id) {
        return ActionPlaceholder.PlaceholderFunction.of(ctx -> ctx.scopeStack()
                .getPlaceholders().stream()
                .filter(p -> p instanceof ActionPlaceholder)
                .filter(p -> p.name().equals(id))
                .findAny().map(p -> ((ActionPlaceholder) p).function())
                .map(f -> f.execute(ctx)).orElseThrow());
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
