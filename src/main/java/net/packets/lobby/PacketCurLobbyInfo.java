package net.packets.lobby;

import game.Game;
import game.LobbyPlayerEntry;
import game.NetPlayerMaster;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import net.ServerLogic;
import net.lobbyhandling.Lobby;
import net.packets.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A packed that is send from the server to the client, which contains the names of all clients that
 * are in the lobby. Packet-Code: LOBCI
 *
 * @author Sebastian Schlachter
 */
public class PacketCurLobbyInfo extends Packet {

  private static final Logger logger = LoggerFactory.getLogger(PacketCurLobbyInfo.class);
  private String info;
  private String[] infoArray;
  CopyOnWriteArrayList<LobbyPlayerEntry> catalog = new CopyOnWriteArrayList<>();

  /**
   * Constructor that is used by the Server to build the Packet with a lobby id.
   *
   * @param clientId clientId of the the receiver.
   * @param lobbyId the lobby id to build the info string for
   */
  public PacketCurLobbyInfo(int clientId, int lobbyId) {
    // server builds
    super(PacketTypes.CUR_LOBBY_INFO);
    setClientId(clientId);
    Lobby lobby = ServerLogic.getLobbyList().getLobby(lobbyId);
    if (lobby == null) {
      addError("Lobby doesn't exist");
      setData(createErrorMessage());
      return;
    }
    info = "OK║" + lobby.getLobbyName() + "║" + lobby.getPlayerNamesIdsReadies();
    setData(info);
    infoArray = info.split("║"); // necessary since infoArray is not really used on the Server side,
    // but needed in validate
    validate();
  }

  /**
   * Constructor that is used by the Client to build the Packet, after receiving the Command LOBCI.
   *
   * @param data A single String that begins with "OK║" and contains the names of all clients that
   *     are in the lobby of the receiver. (names are separated by "║") In the case that an error
   *     occurred before, the String is an errormessage and does not begin with "OK║". {@link
   *     PacketCurLobbyInfo#info} gets set to equal data. The variable data gets split at the
   *     positions of "║". Every substring gets then saved in to the Array {@code infoArray}.
   */
  public PacketCurLobbyInfo(String data) {
    // client builds
    super(PacketTypes.CUR_LOBBY_INFO);
    setData(data);
    info = getData();
    if (data == null) {
      addError("Invalid Data.");
      return;
    }
    infoArray = data.split("║");
    validate();
  }

  /**
   * Validation method to check the data that has, or will be send in this packet. Checks if {@code
   * data} is not null. Checks for every element of the Array {@code infoArray}, that it consists of
   * extendet ASCII Characters. In the case of an error it gets added with {@link
   * Packet#addError(String)}.
   */
  @Override
  public void validate() {
    if (info != null) {
      for (String s : infoArray) {
        if (!isExtendedAscii(s)) {
          return;
        }
      }
    }
    if ((infoArray.length - 2) % 3 != 0) {
      addError("Invalid number of arguments.");
    }
  }

  /**
   * Method that lets the Client react to the receiving of this packet. Check for errors in
   * validate. If {@code in[0]} equals "OK" the names of the clients get printed. Else in the case
   * of an error only the error message gets printed.
   */
  @Override
  public void processData() {
    if (hasErrors()) { // Errors ClientSide
      String s = createErrorMessage();
      logger.error(s);
    } else if (infoArray[0].equals("OK")) { // No Errors ServerSide
      for (int i = 2; i < infoArray.length; i += 3) {
        boolean isReady = infoArray[i + 2].equals("true");
        catalog.add(new LobbyPlayerEntry(infoArray[i + 1], isReady));
      }
      Game.setLobbyPlayerCatalog(catalog);
      // Game Logic updates

      // Add missing players and create list of present players
      NetPlayerMaster.setLobbyname(infoArray[1]);
      ArrayList<Integer> presentIds = new ArrayList<>();
      for (int i = 2; i < infoArray.length; i += 3) {
        try {
          int id = Integer.parseInt(infoArray[i]);
          if (id == Game.getActivePlayer().getClientId()) {
            continue;
          }
          presentIds.add(id);
          NetPlayerMaster.addPlayer(id, infoArray[i + 1]);
        } catch (NumberFormatException e) {
          logger.error(
              "Invalid client ID received from server. ID: "
                  + infoArray[i]
                  + ", Username: "
                  + infoArray[i + 1]);
          addError("Number incorrect.");
        } catch (NullPointerException ignored) {
          // This is a network only client and no game is running, or the game has not loaded yet
        }
      }
      // Check if we need to remove a player
      if (presentIds.size() < NetPlayerMaster.getIds().size()) {
        NetPlayerMaster.removeMissing(presentIds);
      }

    } else { // Errors ServerSide
      logger.error(infoArray[0]);
    }
  }
}
