package game.map;

import entities.blocks.BlockMaster;
import entities.blocks.DirtBlock;
import entities.blocks.GoldBlock;
import entities.blocks.StoneBlock;
import entities.items.ItemMaster;

import java.util.Random;

import entities.items.ItemServerState;
import net.ServerLogic;
import net.packets.items.PacketSpawnItem;
import org.joml.Vector3f;

public class ServerBlock {

  private BlockMaster.BlockTypes type;
  private float hardness;
  private int gridX;
  private int gridY;
  private final int gridZ = Map.getSize();

  ServerBlock(BlockMaster.BlockTypes type, int gridX, int gridY) {
    this.gridX = gridX;
    this.gridY = gridY;
    this.type = type;
    getHardness();
  }

  public BlockMaster.BlockTypes getType() {
    return type;
  }

  public float getHardness() {
    return hardness;
  }

  /**
   * Damage a block.
   *
   * @param damage damage to deal to a block
   */
  public void damageBlock(float damage, int clientId) {
    hardness -= damage;
    if (hardness < 0) {
      if (this.type == BlockMaster.BlockTypes.QMARK) {
        onQmarkDestroy(clientId);
      }
      this.type = BlockMaster.BlockTypes.AIR;
    }
  }

  @Override
  public String toString() {
    return getType().toString();
  }

  private void onQmarkDestroy(int clientId) {

    Random random = new Random(1);
    int r = random.nextInt(4);
    if (r == 0) {
      PacketSpawnItem packetSpawnItem =
          new PacketSpawnItem(
              ItemMaster.ItemTypes.DYNAMITE, new Vector3f(gridX, gridY, gridZ), clientId);
      packetSpawnItem.sendToLobby(ServerLogic.getLobbyForClient(clientId).getLobbyId());
    } else if (r == 1) {
      PacketSpawnItem packetSpawnItem =
          new PacketSpawnItem(
              ItemMaster.ItemTypes.HEART, new Vector3f(gridX, gridY, gridZ), clientId);
      packetSpawnItem.sendToLobby(ServerLogic.getLobbyForClient(clientId).getLobbyId());
    } else if (r == 2) {
      PacketSpawnItem packetSpawnItem =
          new PacketSpawnItem(
              ItemMaster.ItemTypes.STAR, new Vector3f(gridX, gridY, gridZ), clientId);
      packetSpawnItem.sendToLobby(ServerLogic.getLobbyForClient(clientId).getLobbyId());
    } else if (r == 3) {
      PacketSpawnItem packetSpawnItem =
          new PacketSpawnItem(
              ItemMaster.ItemTypes.ICE, new Vector3f(gridX, gridY, gridZ), clientId);
      packetSpawnItem.sendToLobby(ServerLogic.getLobbyForClient(clientId).getLobbyId());
    }
  }
}
