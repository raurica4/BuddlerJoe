package entities.blocks.debris;

import entities.blocks.Block;
import game.Game;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class DebrisMaster {

  private static final float SIZE_MIN = .1f;
  private static final float SIZE_MAX = .6f;
  // That's definitely how this is spelled
  private static final List<Debris> debrises = new CopyOnWriteArrayList<>();
  static Random random;

  public static void init() {
    random = new Random();
  }

  /**
   * Generate debris from a destroyed block. Debris will be randomized and depend on volume and
   * density of base block.
   *
   * @param block the block that was destroyed
   */
  public static void generateDebris(Block block) {

    float blockMass = (float) Math.pow(block.getDim(), 3);
    float debrisMass = 0;
    while (debrisMass < blockMass / 12f) {
      float size = Math.max(SIZE_MIN, random.nextFloat() * SIZE_MAX);
      debrisMass += Math.pow(size, 3);
      Debris debris = new Debris(block, size);
      Game.addEntity(debris);
      debrises.add(debris);
    }
  }

  /**
   * Should be called every frame in the main loop. Update all debris entities and remove the ones
   * that are past their life length
   */
  public static void update() {
    for (Debris d : debrises) {
      d.update();
      if (d.getElapsedTime() >= d.getLifeLength()) {
        Game.removeEntity(d);
        debrises.remove(d);
      }
    }
  }
}
