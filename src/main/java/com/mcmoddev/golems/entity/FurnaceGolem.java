package com.mcmoddev.golems.entity;

import java.util.EnumSet;

import com.mcmoddev.golems.entity.base.GolemBase;
import com.mcmoddev.golems.items.ItemBedrockGolem;
import com.mcmoddev.golems.util.GolemNames;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

public final class FurnaceGolem extends GolemBase {

  private static final DataParameter<Integer> FUEL = EntityDataManager.createKey(FurnaceGolem.class, DataSerializers.VARINT);
  private static final String KEY_FUEL = "FuelRemaining";

  private static final ResourceLocation LIT = makeTexture(GolemNames.FURNACE_GOLEM + "/lit");
  private static final ResourceLocation UNLIT = makeTexture(GolemNames.FURNACE_GOLEM + "/unlit");

  public static final String FUEL_FACTOR = "Burn Time";
  public static final int MAX_FUEL = 102400;
  public final int fuelBurnFactor;
  // true if the golem has been inert for more than one tick
  protected boolean inertLastTick = false;

  public FurnaceGolem(final EntityType<? extends GolemBase> entityType, final World world) {
    super(entityType, world);
    fuelBurnFactor = Math.max(1, getConfigInt(FUEL_FACTOR));
  }

  @Override
  protected void registerData() {
    super.registerData();
    this.getDataManager().register(FUEL, Integer.valueOf(0));
  }

  @Override
  protected void registerGoals() {
    super.registerGoals();
    this.goalSelector.addGoal(0, new InertGoal(this));
    this.goalSelector.addGoal(1, new UseFuelGoal(this));
    this.goalSelector.addGoal(1, new TemptGoal(this, 0.7D, Ingredient.fromTag(ItemTags.COALS), false));
  }

  @Override
  public void livingTick() {
    super.livingTick();
    if (this.world.isRemote && rand.nextInt(10) == 0) {
      // particle effects
      final double pMotion = 0.03D;
      world.addParticle(this.hasFuel() ? ParticleTypes.FLAME : ParticleTypes.SMOKE,
          this.posX + world.rand.nextDouble() * 0.4D - 0.2D + this.getMotion().getX() * 8,
          this.posY + world.rand.nextDouble() * 0.5D + this.getHeight() / 2.0D,
          this.posZ + world.rand.nextDouble() * 0.4D - 0.2D + this.getMotion().getZ() * 8, 
          world.rand.nextGaussian() * pMotion,
          world.rand.nextDouble() * pMotion, 
          world.rand.nextGaussian() * pMotion);
    }
    // update inert tick
    if(!inertLastTick && !hasFuel()) {
      inertLastTick = true;
    }
  }

  @Override
  public void readAdditional(final CompoundNBT tag) {
    super.readAdditional(tag);
    this.setFuel(tag.getInt(KEY_FUEL));
  }

  @Override
  public void writeAdditional(final CompoundNBT tag) {
    super.writeAdditional(tag);
    tag.putInt(KEY_FUEL, getFuel());
  }

  @Override
  protected boolean processInteract(final PlayerEntity player, final Hand hand) {
    // allow player to add fuel to the golem by clicking on them with a fuel item
    ItemStack stack = player.getHeldItem(hand);
    int burnTime = ForgeHooks.getBurnTime(stack) * (player.isSneaking() ? stack.getCount() : 1);
    if (burnTime > 0 && (getFuel() + burnTime) <= MAX_FUEL) {
      if (player.isSneaking()) {
        // take entire ItemStack
        this.addFuel(burnTime * stack.getCount());
        stack = stack.getContainerItem();
      } else {
        // take one item from ItemStack
        this.addFuel(burnTime);
        if (stack.getCount() > 1) {
          stack.shrink(1);
        } else {
          stack = stack.getContainerItem();
        }
      }
      // update the player's held item
      player.setHeldItem(hand, stack);
      // add particles
      ItemBedrockGolem.spawnParticles(this.world, this.posX, this.posY + this.getHeight() / 2.0D, this.posZ, 0.05D, ParticleTypes.FLAME, 10);
      return true;
    }

    // allow player to remove burn time by using a water bucket
    if (stack.getItem() == Items.WATER_BUCKET && stack.getCount() == 1) {
      this.setFuel(0);
      player.setHeldItem(hand, stack.getContainerItem());
      ItemBedrockGolem.spawnParticles(this.world, this.posX, this.posY + this.getHeight() / 2.0D, this.posZ, 0.1D, ParticleTypes.LARGE_SMOKE, 15);
      return true;
    }

    return super.processInteract(player, hand);
  }

  @Override
  public ResourceLocation getTexture() {
    return hasFuel() ? LIT : UNLIT;
  }

  public boolean hasFuel() {
    return getFuel() > 0;
  }

  /** @return the current fuel level as a number **/
  public int getFuel() {
    return this.getDataManager().get(FUEL).intValue();
  }

  /** @return a number between 0.0 and 1.0 to indicate fuel level **/
  public float getFuelPercentage() {
    return (float) getFuel() / (float) MAX_FUEL;
  }

  public void setFuel(final int fuel) {
    if (getFuel() != fuel) {
      this.getDataManager().set(FUEL, Integer.valueOf(fuel));
    }
  }

  public void addFuel(final int toAdd) {
    if (toAdd != 0) {
      this.getDataManager().set(FUEL, getFuel() + toAdd);
    }
  }

  class UseFuelGoal extends Goal {

    private final FurnaceGolem golem;

    protected UseFuelGoal(final FurnaceGolem entity) {
      super();
      golem = entity;
    }

    @Override
    public boolean shouldExecute() {
      // only uses fuel every X ticks
      return golem.isServerWorld() && golem.getFuel() > 0 && golem.ticksExisted % golem.fuelBurnFactor == 0;
    }

    @Override
    public boolean shouldContinueExecuting() {
      return false;
    }

    @Override
    public void startExecuting() {
      golem.addFuel(-1);
    }
  }

  class InertGoal extends Goal {

    private final FurnaceGolem golem;
    private Vec3d lookVec;
    private float rotateYaw;
    private float rotatePitch;

    protected InertGoal(final FurnaceGolem entity) {
      super();
      this.setMutexFlags(EnumSet.of(Flag.JUMP, Flag.LOOK, Flag.MOVE, Flag.TARGET));
      golem = entity;
      lookVec = golem.getLookVec();
      rotateYaw = golem.rotationYaw;
      rotatePitch = golem.rotationPitch;
    }

    @Override
    public boolean shouldExecute() {
      return !golem.hasFuel();
    }

    @Override
    public boolean shouldContinueExecuting() {
      return false;
    }

    @Override
    public void startExecuting() {
      tick();
    }

    @Override
    public void tick() {
      // freeze the golem and some AI tasks
      golem.setMotion(golem.getMotion().mul(0, 1.0D, 0));
      golem.setMoveForward(0F);
      golem.setMoveStrafing(0F);
      golem.moveController.setMoveTo(golem.posX, golem.posY, golem.posZ, 0.1D);
      golem.setJumping(false);
      golem.setAttackTarget(null);
      golem.setRevengeTarget(null);
      golem.getNavigator().clearPath();
      // update look vec if the golem just barely ran out of fuel
      if(!golem.inertLastTick) {
        lookVec = golem.getLookVec();//.rotateYaw(Math.toRadians(-15D));
        rotateYaw = golem.rotationYaw;
        rotatePitch = golem.rotationPitch;
      }
      // freeze look vec and turn direction
      golem.getLookController().setLookPosition(lookVec);
      golem.setRotation(rotateYaw, rotatePitch);
    }
  }
}
