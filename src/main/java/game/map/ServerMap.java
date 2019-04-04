package game.map;

import entities.blocks.BlockMaster;
import java.util.ArrayList;
import java.util.Random;
import net.ServerLogic;
import net.packets.block.PacketBlockDamage;

public class ServerMap extends Map<ServerBlock> {

  /**
   * Generate a new map for the Server.
   *
   * @param width number of blocks on the horizontal
   * @param height number of blocks on the vertical = depth
   * @param seed random seed
   */
  public ServerMap(int width, int height, long seed) {
    super(width, height, seed);
    blocks = new ServerBlock[width][height];
    generateMap();
    checkFallingBlocks();
  }

  @Override
  void generateMap() {
    Random rng = new Random(seed);
    float[][] noiseMap = generateNoiseMap(rng);

    /* Threshold function
     */
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int gridX = x * Map.getDim();
        int gridY = y * Map.getDim();
        if (noiseMap[x][y] < thresholds[0]) {
          blocks[x][y] = new ServerBlock(BlockMaster.BlockTypes.STONE, gridX, gridY); // Air
        } else {
          if (rng.nextFloat() < .02f) {
            blocks[x][y] = new ServerBlock(BlockMaster.BlockTypes.GOLD, gridX, gridY); // Gold: 1 in 40 chance
          } else if (rng.nextFloat() < .01f) {
            blocks[x][y] =
                new ServerBlock(BlockMaster.BlockTypes.QMARK, gridX, gridY); // Item Block: 1 in 50 chance
          } else if (noiseMap[x][y] < thresholds[1]) {
            blocks[x][y] = new ServerBlock(BlockMaster.BlockTypes.DIRT, gridX, gridY); // Dirt
          } else {
            blocks[x][y] = new ServerBlock(BlockMaster.BlockTypes.AIR, gridX, gridY); // Stone
          }
        }
      }
    }
  }

  /**
   * Check and move falling blocks. This will send packets in the future. For now client side
   * handles the blocks themselves.
   */
  public void checkFallingBlocks() {
    boolean done;
    do {
      done = true;
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          if (blocks[x][y].getType() == BlockMaster.BlockTypes.STONE
              && y + 1 < height
              && blocks[x][y + 1].getType() == BlockMaster.BlockTypes.AIR) {
            blocks[x][y + 1] = blocks[x][y];
            blocks[x][y] = new ServerBlock(BlockMaster.BlockTypes.AIR, x, y);
            done = false;
          }
        }
      }
    } while (!done);
  }

  /**
   * Deal damage to a block. Gets called from a client packet.
   *
   * @see net.packets.block.PacketBlockDamage
   * @param posX X position of the block
   * @param posY Y position of the block
   * @param damage damage to deal to the block
   */
  @Override
  public void damageBlock(int clientId, int posX, int posY, float damage) {
    if (blocks[posX][posY] != null) {
      blocks[posX][posY].damageBlock(damage);
      checkFallingBlocks();
      int lobId = ServerLogic.getLobbyForClient(clientId).getLobbyId();
      if (lobId > 0) {
        new PacketBlockDamage(clientId, posX, posY, damage).sendToLobby(lobId);
      }
    }
  }

  /**
   * Create a random test map. Use this to develop a good mapping algorithm.
   *
   * @param args nothing
   */
  public static void main(String[] args) {
    ServerMap testMap = new ServerMap(100, 110, System.currentTimeMillis());
    System.out.println(testMap);
  }

  /**
   * Creates a String that describes every block of the map. This string can be sent over the
   * network protocol with {@link net.packets.map.PacketBroadcastMap}
   *
   * @return transferable string representation of the map
   */
  public String toPacketString() {
    StringBuilder sb = new StringBuilder();
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        sb.append(blocks[x][y].getType().getId());
      }
      if (y < height - 1) {
        sb.append("║");
      }
    }
    return sb.toString();
  }

  /**
   * Generate a damage packet for each damaged block to update a newly generated map.
   *
   * @return an array list of damage packets to update the map
   */
  public ArrayList<PacketBlockDamage> getDamagePackets() {
    ArrayList<PacketBlockDamage> packets = new ArrayList<>();
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        float damage = blocks[x][y].getBaseHardness() - blocks[x][y].getHardness();
        if (damage > 0) {
          packets.add(new PacketBlockDamage(0, x, y, damage));
        }
      }
    }
    return packets;
  }
}
