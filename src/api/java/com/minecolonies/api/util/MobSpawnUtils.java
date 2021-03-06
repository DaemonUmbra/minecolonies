package com.minecolonies.api.util;

import com.google.common.collect.Multimap;
import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.MinecoloniesAPIProxy;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesMob;
import com.minecolonies.api.entity.mobs.IArcherMobEntity;
import com.minecolonies.api.entity.mobs.barbarians.AbstractEntityBarbarian;
import com.minecolonies.api.entity.mobs.barbarians.IChiefBarbarianEntity;
import com.minecolonies.api.entity.mobs.barbarians.IMeleeBarbarianEntity;
import com.minecolonies.api.entity.mobs.pirates.ICaptainPirateEntity;
import com.minecolonies.api.entity.mobs.pirates.IPirateEntity;
import com.minecolonies.api.entity.mobs.util.MobEventsUtils;
import com.minecolonies.api.items.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Random;
import java.util.stream.IntStream;

import static com.minecolonies.api.util.constant.RaiderConstants.*;

public final class MobSpawnUtils
{

    private MobSpawnUtils()
    {
        throw new IllegalStateException("Tried to initialize: MobSpawnUtils but this is a Utility class.");
    }

    /**
     * Sets up the mob ai for a minecolonies mob.
     * Calls into the api to get the required ai tasks from the registry and loads the tasks.
     *
     * @param mob The mob to set the AI Tasks on.
     */
    public static void setupMobAi(final AbstractEntityMinecoloniesMob mob)
    {
        final Multimap<Integer, Goal> aiTasks = IMinecoloniesAPI.getInstance().getMobAIRegistry().getEntityAiTasksForMobs(mob);
        aiTasks.keySet().forEach(priority -> aiTasks.get(priority).forEach(task -> mob.goalSelector.addGoal(priority, task)));

        final Multimap<Integer, Goal> aiTargetTasks = IMinecoloniesAPI.getInstance().getMobAIRegistry().getEntityAiTargetTasksForMobs(mob);
        aiTargetTasks.keySet().forEach(priority -> aiTargetTasks.get(priority).forEach(task -> mob.targetSelector.addGoal(priority, task)));
    }

    /**
     * Set mob attributes.
     *
     * @param mob The mob to set the attributes on.
     * @param colony    The colony that the mob is attacking.
     */
    public static void setMobAttributes(final LivingEntity mob, final IColony colony)
    {
        mob.getAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(FOLLOW_RANGE);
        mob.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(MOVEMENT_SPEED);

        final double attackDamage = MinecoloniesAPIProxy.getInstance().getConfig().getCommon().barbarianHordeDifficulty.get() >= 10 ? ATTACK_DAMAGE * 2 : ATTACK_DAMAGE;
        mob.getAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(attackDamage);
        if (mob instanceof IChiefBarbarianEntity)
        {
            final double chiefArmor =MinecoloniesAPIProxy.getInstance().getConfig().getCommon().barbarianHordeDifficulty.get() > 5 ? CHIEF_ARMOR * 2 : CHIEF_ARMOR;
            mob.getAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(chiefArmor);
        }
        else
        {
            final double armor = MinecoloniesAPIProxy.getInstance().getConfig().getCommon().barbarianHordeDifficulty.get() * ARMOR;
            mob.getAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(armor);
        }
        mob.getAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(getHealthBasedOnRaidLevel(colony));
        mob.setHealth(mob.getMaxHealth());
    }

    /**
     * Sets the entity's health based on the raidLevel
     *
     * @return returns the health in the form of a double
     */
    private static double getHealthBasedOnRaidLevel(final IColony colony)
    {
        if (colony != null)
        {
            final int raidLevel = (int) (MobEventsUtils.getColonyRaidLevel(colony) * BARBARIAN_HEALTH_MULTIPLIER);
            return Math.max(BARBARIAN_BASE_HEALTH, (BARBARIAN_BASE_HEALTH + raidLevel) * ((double)MinecoloniesAPIProxy.getInstance().getConfig().getCommon().barbarianHordeDifficulty.get() * 0.1));
        }
        return BARBARIAN_BASE_HEALTH;
    }

    /**
     * Sets up and spawns the Barbarian entities of choice
     *  @param entityToSpawn  The entity which should be spawned
     * @param numberOfSpawns The number of times the entity should be spawned
     * @param spawnLocation  the location at which to spawn the entity
     * @param world          the world in which the colony and entity are
     * @param colony the colony to spawn them close to.
     */
    public static void spawn(
      final EntityType entityToSpawn,
      final int numberOfSpawns,
      final BlockPos spawnLocation,
      final World world,
      final IColony colony)
    {
        if (spawnLocation != null && entityToSpawn != null && world != null)
        {

            final int x = spawnLocation.getX();
            final int y = BlockPosUtil.getFloor(spawnLocation, world).getY();
            final int z = spawnLocation.getZ();

            IntStream.range(0, numberOfSpawns).forEach(theInteger ->
            {


                final AbstractEntityBarbarian entity = (AbstractEntityBarbarian) entityToSpawn.create(world);

                if (entity != null)
                {
                    setEquipment(entity);
                    entity.setPositionAndRotation(x, y + 1.0, z, (float) MathHelper.wrapDegrees(world.rand.nextDouble() * WHOLE_CIRCLE), 0.0F);
                    CompatibilityUtils.addEntity(world, entity);
                    entity.setColony(colony);
                }
            });
        }
    }

    /**
     * Set the equipment of a certain mob.
     * @param mob the equipment to set up.
     */
    public static void setEquipment(final AbstractEntityMinecoloniesMob mob)
    {
        if (mob instanceof IMeleeBarbarianEntity)
        {
            mob.setItemStackToSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.STONE_AXE));
        }
        else if (mob instanceof IArcherMobEntity)
        {
            mob.setItemStackToSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.BOW));
        }
        else if (mob instanceof IChiefBarbarianEntity)
        {
            mob.setItemStackToSlot(EquipmentSlotType.MAINHAND, new ItemStack(ModItems.chiefSword));
            mob.setItemStackToSlot(EquipmentSlotType.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
            mob.setItemStackToSlot(EquipmentSlotType.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
            mob.setItemStackToSlot(EquipmentSlotType.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
            mob.setItemStackToSlot(EquipmentSlotType.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));
        }
        else if (mob instanceof IPirateEntity)
        {
            mob.setItemStackToSlot(EquipmentSlotType.MAINHAND, new ItemStack(ModItems.scimitar));
            if (mob instanceof ICaptainPirateEntity)
            {
                if (new Random().nextBoolean())
                {
                    mob.setItemStackToSlot(EquipmentSlotType.HEAD, new ItemStack(ModItems.pirateHelmet_1));
                    mob.setItemStackToSlot(EquipmentSlotType.CHEST, new ItemStack(ModItems.pirateChest_1));
                    mob.setItemStackToSlot(EquipmentSlotType.LEGS, new ItemStack(ModItems.pirateLegs_1));
                    mob.setItemStackToSlot(EquipmentSlotType.FEET, new ItemStack(ModItems.pirateBoots_1));
                }
                else
                {
                    mob.setItemStackToSlot(EquipmentSlotType.HEAD, new ItemStack(ModItems.pirateHelmet_2));
                    mob.setItemStackToSlot(EquipmentSlotType.CHEST, new ItemStack(ModItems.pirateChest_2));
                    mob.setItemStackToSlot(EquipmentSlotType.LEGS, new ItemStack(ModItems.pirateLegs_2));
                    mob.setItemStackToSlot(EquipmentSlotType.FEET, new ItemStack(ModItems.pirateBoots_2));
                }
            }
        }
    }
}
