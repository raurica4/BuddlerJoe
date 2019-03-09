package net.packets;

import net.ServerLogic;
import net.ServerPlayerList;

public class PacketGetName extends Packet{

    private int clientId;
    private String username;
    private int getClientId;
    private ServerPlayerList playerList;

    public PacketGetName(int clientId, String data) {
        super(PacketTypes.GET_NAME);
        setClientId(clientId);
        setData(data);

        if(!validate()){
            setPacketId(PacketTypes.GET_NAME);
            return;
        }

    }

    /**
     * Method to validate the data which is forwarded to the package to ensure its safety.
     ** @return True or false to determine the further action of the class.
     */

    //TODO: Exceptions!


    public boolean validate(){
        //TODO: Write the validation method
        return true;
    }

    public void processData(){

    }

    @Override
    public String getData() {
        return this.toString();
    }

    @Override
    public String toString() {
        return "PacketGetName{" +
                "clientId=" + clientId +
                ", username='" + username + '\'' +
                ", getClientId=" + getClientId +
                '}';
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public int getGetClientId() {
        return getClientId;
    }

    public void setGetClientId(int getClientId) {
        this.getClientId = getClientId;
    }

    public ServerPlayerList getPlayerList() {
        return playerList;
    }

    public void setPlayerList(ServerPlayerList playerList) {
        this.playerList = playerList;
    }
}
