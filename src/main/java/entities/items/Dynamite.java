package entities.items;

import static audio.AudioMaster.SoundCategory.EXPLOSION;
import static audio.AudioMaster.SoundCategory.FUSE;

import audio.Source;
import engine.models.RawModel;
import engine.models.TexturedModel;
import engine.particles.systems.Explosion;
import engine.particles.systems.Fire;
import engine.particles.systems.Shockwave;
import engine.particles.systems.Smoke;
import engine.render.Loader;
import engine.render.objconverter.ObjFileLoader;
import engine.textures.ModelTexture;
import entities.NetPlayer;
import entities.blocks.Block;
import entities.blocks.BlockMaster;
import entities.light.Light;
import entities.light.LightMaster;
import game.Game;
import game.NetPlayerMaster;
import game.map.GameMap;
import game.stages.Playing;
import java.util.Random;
import net.packets.block.PacketBlockDamage;
import net.packets.items.PacketItemUsed;
import org.joml.Vector3f;

/** A bundle of dynamite that can damage blocks or the player. */
@SuppressWarnings("FieldCanBeLocal") // We want settings on top even if it could be local
public class Dynamite extends Item {

  private static final int DYNAMITE_DAMAGE_DISTANCE_SQ =
      4 * 4 * GameMap.getDim() * GameMap.getDim();

  private static TexturedModel preloadedModel;
  private final float gravity = 20;
  private final float fuseTimer = 3f;
  private final float explosionTime = .5f;
  private final float totalEffectsTime = 2.5f;
  private final float explosionRange = 15;
  private final float maximumDamage = 50;
  private final Fire particleFuse;
  private final Explosion particleExplosion;
  private final Explosion particleShrapnel;
  private final Smoke particleSmoke;
  private final Shockwave particleShockwave;
  private float time;
  private boolean active;
  private boolean exploded;
  private Light flash;
  private int itemId;
  private Source fuseSound;

  /** Extended Constructor for Dynamite. Don't use directly. Use the Item Master to create items. */
  private Dynamite(Vector3f position, float rotX, float rotY, float rotZ, float scale) {
    super(ItemMaster.ItemTypes.DYNAMITE, getPreloadedModel(), position, rotX, rotY, rotZ, scale);

    time = 0;
    active = false;
    exploded = false;

    // Generate Fuse Effect
    particleFuse = new Fire(70, 1, .01f, 1, 3);
    particleFuse.setDirection(new Vector3f(0, 1, 0), 0.1f);
    particleFuse.setLifeError(.2f);
    particleFuse.setSpeedError(.1f);
    particleFuse.setScaleError(.5f);
    particleFuse.randomizeRotation();

    /* Generate Fancy Particle Effects for an explosion */
    // Generate Explosion Effect
    particleExplosion = new Explosion(200, 5, 0, 1f, 10);
    particleExplosion.setScaleError(.4f);
    particleExplosion.setSpeedError(.3f);
    particleExplosion.setLifeError(.2f);

    // Generate Shrapnel Effect
    particleShrapnel = new Explosion(1200, 70, 0, 1.5f, 1f);
    particleShrapnel.setScaleError(.2f);
    particleShrapnel.setLifeError(.5f);
    particleShrapnel.setSpeedError(.3f);

    // Generate Smoke Effect
    particleSmoke = new Smoke(40, 5, -.1f, 6f, 20f);
    particleSmoke.setScaleError(.2f);
    particleSmoke.setLifeError(.8f);
    particleSmoke.setSpeedError(.3f);
    particleSmoke.randomizeRotation();

    // Generate Shockwave effect
    // We can vary pps by graphic setting, but it looks very nice with a lot of particles!

    particleShockwave = new Shockwave(2000, 30, 0f, .8f, 7f);
    particleShockwave.setScaleError(.1f);
    particleShockwave.setLifeError(.1f);
    particleShockwave.setDirection(new Vector3f(1, 0, 0), 0);
    Random rnd = new Random(); // Give the shockwave a slight random tilt
    particleShockwave.setRotationAxis(
        new Vector3f(rnd.nextFloat() * .4f - .2f, 1, rnd.nextFloat() * .4f - .2f), 0);
  }

  /**
   * Constructor for the dynamite. Don't use directly. Use the Item Master to create items.
   *
   * @param position position to spawn the dynamite
   */
  Dynamite(Vector3f position) {
    this(position, 0, 0, 0, 1);
  }

  /**
   * Preload model.
   *
   * @param loader passed by Item Master
   */
  public static void init(Loader loader) {
    RawModel rawDynamite = loader.loadToVao(ObjFileLoader.loadObj("dynamite"));
    setPreloadedModel(
        new TexturedModel(rawDynamite, new ModelTexture(loader.loadTexture("dynamite"))));
  }

  private static TexturedModel getPreloadedModel() {
    return preloadedModel;
  }

  private static void setPreloadedModel(TexturedModel preloadedModel) {
    Dynamite.preloadedModel = preloadedModel;
  }

  /**
   * Called every frame. Updates all the animations for the Dynamite and decides which particle
   * systems will fire. Takes care of entities.collision and removes itself when done with
   * everything.
   */
  @Override
  public void update() {

    // Skip if dynamite is being placed or otherwise inactive
    if (!active) {
      return;
    }

    // Update the fuse time
    time += Game.dt();

    /*
    Case 1: Dynamite is about to blow -> play fuse animation, check for entities,
    collision and update position if falling.
    Dynamite can only move if it is in this phase, the explosion will be stationary.
    */
    if (time < fuseTimer) {
      boolean collision = false;
      for (Block block : BlockMaster.getBlocks()) {
        if (collidesWith(block)) {
          collision = true;
          break;
        }
      }
      if (!collision) {
        increasePosition(0, (float) -(gravity * Game.dt()), 0);
      }

      float offset = getBbox().getDimY() * 2 * (fuseTimer - time) / fuseTimer;
      particleFuse.generateParticles(new Vector3f(0, offset, 0).add(getPosition()));
      if (fuseSound == null) {
        fuseSound = new Source(FUSE);
      }

      if (!fuseSound.isPlaying()) {
        fuseSound.playIndex(0);
      }
      /*
      Case 2: Explosion is finished, remove the object
       */
    } else if (time >= fuseTimer && time <= fuseTimer + explosionTime) {
      explode();
      particleExplosion.generateParticles(getPosition());
      particleSmoke.generateParticles(getPosition());
      if (time <= fuseTimer + 0.1) {
        particleShrapnel.generateParticles(getPosition());
        particleShockwave.generateParticles(getPosition());
      }
    }

    if (time >= fuseTimer + totalEffectsTime) {
      setDestroyed(true); // Remove Object
      if (flash != null) {
        flash.setDestroyed(true);
      }
    } else if (time >= fuseTimer + .3f) {
      float scaleBrightness = (float) (1 - Game.dt() * 5);
      flash.setBrightness(flash.getBrightness() * scaleBrightness);
    } else if (time > fuseTimer) {
      float scaleBrightness = (float) (1 + Game.dt() * 10);
      flash.setBrightness(flash.getBrightness() * scaleBrightness);
    }
  }

  /** Damage the blocks in range of the explosion and hide the dynamite. */
  private void explode() {
    if (exploded) {
      //      if (Game.getActivePlayer().getFuseIsPlaying()){
      //        Game.getActivePlayer().setFuseSoundOff();
      //      }

      return;
    }
    exploded = true;
    new Source(EXPLOSION).playRandom();
    setScale(new Vector3f()); // Hide the model, but keep the object for the explosion effect to
    flash =
        LightMaster.generateLight(
            LightMaster.LightTypes.FLASH, getPosition(), new Vector3f(1, 1, 1));
    flash.setBrightness(10);

    // Deal damage to the active player if too close (4 blocks (6 units) distance squared is 576)
    if (getPosition().distanceSquared(Game.getActivePlayer().getPosition())
        <= DYNAMITE_DAMAGE_DISTANCE_SQ) {
      Playing.showDamageTakenOverlay();
      // Send to server to inform of damage
      Game.getActivePlayer().informServerOfLifeChange(-1);
    }

    // Inform server is a NetPlayer is damaged
    for (Integer id : NetPlayerMaster.getIds()) {
      NetPlayer netPlayer = NetPlayerMaster.getNetPlayerById(id);
      if (getPosition().distanceSquared(netPlayer.getPosition()) <= DYNAMITE_DAMAGE_DISTANCE_SQ) {
        // Send to server to inform of damage
        netPlayer.informServerOfLifeChange(-1);
      }
    }

    // Deal Damage if dynamite is owned
    if (!isOwned()) {
      return;
    } else {
      PacketItemUsed packetItemUsed = new PacketItemUsed(itemId);
      packetItemUsed.sendToServer();
    }

    for (Block block : BlockMaster.getBlocks()) {
      float distance = block.get2dDistanceFrom(getPositionXy());
      if (distance < explosionRange) {
        // Damage blocks inverse to distance (closer = more damage)
        // block.increaseDamage(1 / distance * maximumDamage);
        if (Game.isConnectedToServer()) {
          new PacketBlockDamage(block.getGridX(), block.getGridY(), 1 / distance * maximumDamage)
              .sendToServer();
        }
      }
    }
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public int getItemId() {
    return itemId;
  }

  public void setItemId(int itemId) {
    this.itemId = itemId;
  }
}
