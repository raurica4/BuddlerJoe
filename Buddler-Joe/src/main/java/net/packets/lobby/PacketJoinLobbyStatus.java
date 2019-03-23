package net.packets.lobby;

import net.playerhandling.ServerPlayerList;
import net.ServerLogic;
import net.packets.Packet;

public class PacketJoinLobbyStatus extends Packet{

    private String status;


    /**
     * Package to inform the client over the result of the lobby-join attempt
     * @param clientId to find the player in the list
     * @param data a String with "code;lobbyId"
     */

    public PacketJoinLobbyStatus(int clientId, String data) {
        //server builds
        super(Packet.PacketTypes.JOIN_LOBBY_STATUS);
        setData(data);
        setClientId(clientId);
        validate();
    }

    public PacketJoinLobbyStatus(String data) {
        //client builds
        super(Packet.PacketTypes.JOIN_LOBBY_STATUS);
        setData(data);
        status = getData();
        validate();
    }



    @Override
    public void validate() {
        if(status != null) {
            isExtendedAscii(status);
        }else{
            addError("No Status found.");
        }
    }

    @Override
    public void processData() {
        if(hasErrors()){//Errors on Client
            System.out.println(createErrorMessage());
        }else if(status.startsWith("OK")){
            System.out.println("Successfully joined lobby");
        }else{//Errors on Server
            System.out.println(status);
        }
    }
}
