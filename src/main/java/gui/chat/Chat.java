package gui.chat;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;

import engine.io.InputHandler;
import engine.render.Loader;
import engine.render.fontmeshcreator.FontType;
import engine.render.fontrendering.TextMaster;
import gui.GuiTexture;
import java.util.ArrayList;
import java.util.List;
import net.packets.chat.PacketChatMessageToServer;
import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * The Chat Window Overlay in the Game
 *
 * <p>In its normal state it is transparent. With ENTER it can be activated to type a message.
 * Pressing ENTER while a message is typed will send the message. Pressing ENTER while no message is
 * typed will fade out the chat.
 *
 * <p>This class is only a mock up and not functional yet, except for the fading and positioning.
 */
public class Chat {

  private static final float ALPHA_OFF = .1f;
  private static final float ALPHA_ON = .8f;

  private boolean enabled;
  private String chatText;
  private GuiTexture chatGui;
  private float alpha;

  private FontType font;
  private ChatText guiText;
  private Vector3f textColour;

  private float maxLineLength;
  private int maxLines;

  private List<ChatText> messages;
  private int msgSize;
  private List<String> text;
  private int textSize;

  /**
   * Initialize Chat, only needs to be called once on game init.
   *
   * @param loader main loader
   */
  public Chat(Loader loader, int maxLines, float maxLineLength) {
    this.maxLines = maxLines;
    this.maxLineLength = maxLineLength;
    enabled = false;
    alpha = ALPHA_OFF;

    // Load the background image of the chat and set rendering parameters
    chatGui =
        new GuiTexture(
            loader.loadTexture("chat"),
            new Vector2f(-.6f, -.6f),
            new Vector2f(.4f, .4f),
            ALPHA_OFF);
    chatText = "";

    // Load font and text properties for all messages
    font = new FontType(loader, "verdanaAsciiEx");
    textColour = new Vector3f(1f, 1f, 1f);
    guiText =
        new ChatText(
            chatText,
            1,
            new Vector3f(textColour.x, textColour.y, textColour.z),
            alpha,
            font,
            new Vector2f(.06f, .91f),
            maxLineLength,
            false,
            false);

    messages = new ArrayList<>();
    text = new ArrayList<>();
    msgSize = 0;
  }

  /**
   * Method to check user input which calls the keyboardInputHandler
   *
   * <p>Called every frame. Reads chat input and toggles chat window text input handler
   */
  public void checkInputs() {
    if (InputHandler.isKeyPressed(GLFW_KEY_ENTER)) {
      if (chatText.length() > 0 && enabled) {

        if (chatText.startsWith("@")) {
          int wisperId = game.NetPlayerMaster.getClientIdForWhisper(chatText);
          if (-1 == wisperId) {
            text.add("Username ist ungültig");
          } else if (-2 == wisperId) {
            chatText = chatText.substring(4);
            System.out.println(chatText);
            PacketChatMessageToServer broadcostmessage =
                new PacketChatMessageToServer(
                    "(to all from "
                        + game.Game.getActivePlayer().getUsername()
                        + ") "
                        + chatText
                        + "║-1");
            broadcostmessage.sendToServer();

          } else {
            String userName = game.NetPlayerMaster.getNetPlayerById(wisperId).getUsername();
            chatText = chatText.substring(userName.length() + 1);
            //            System.out.println("chatText");
            //
            //            System.out.println("wisperId");
            //            System.out.println(wisperId);
            PacketChatMessageToServer sendMessage =
                new PacketChatMessageToServer("(wispered)" + chatText + "║" + wisperId);
            sendMessage.sendToServer();

            PacketChatMessageToServer sendMessage2 =
                new PacketChatMessageToServer(
                    "(wispered to "
                        + userName
                        + ")"
                        + chatText
                        + "║"
                        + game.Game.getActivePlayer().getClientId());
            sendMessage2.sendToServer();
          }
        } else {

          PacketChatMessageToServer sendMessage = new PacketChatMessageToServer(chatText + "║0");
          sendMessage.sendToServer();
        }

        //        sendMessage();
        chatText = "";
        InputHandler.resetInputString();
      } else {
        if (enabled) {
          setEnabled(false);
          InputHandler.readInputOff();
        } else {
          setEnabled(true);
          InputHandler.readInputOn();
        }
        InputHandler.resetInputString();
      }
    }

    if (messages.size() != text.size()) {
      addChatText();
    }

    updateAlpha();

    if (!enabled) {
      return;
    }

    String newChatText = chatText;

    newChatText = InputHandler.getInputString();

    if (!chatText.equals(newChatText)) {
      chatText = newChatText;
      updateGuiText();
    }
    arrangeMessages();
  }

  /**
   * Clear text box on send.
   *
   * @param text text to remove
   * @return an empty text
   */
  private ChatText clearChatText(ChatText text) {
    TextMaster.removeText(text);
    return new ChatText(
        "",
        text.getFontSize(),
        text.getColour(),
        text.getAlpha(),
        text.getFont(),
        text.getPosition(),
        text.getMaxLineSize(),
        text.isCentered(),
        text.isSent());
  }

  /**
   * Mock up. Add timestamp and user to message. Adds message to a message handler (currently just a
   * list) Clears the message.
   */
  private void sendMessage() {
    ChatText messageText =
        new ChatText(
            guiText.getTime() + chatText,
            .7f,
            textColour,
            alpha,
            font,
            new Vector2f(.06f, .91f),
            maxLineLength,
            false,
            false);
    guiText = clearChatText(guiText);
    messages.add(messageText);
    chatText = "";
  }

  /**
   * TEMPORARY MOCK UP.!!
   *
   * <p>This should get its own class later to handle network messages Maybe validate first if there
   * are invalid messages
   *
   * <p>Check if messages changed so we don't have to create them every frame
   */
  public void arrangeMessages() {
    if (messages.size() != msgSize) { // Something changed
      float posY = .88f;
      float posX = .03f;
      int currentLines = 0;

      for (int i = messages.size() - 1; i >= 0; i--) {
        int lines = messages.get(i).getNumberOfLines();
        currentLines += lines;
        posY -= .02f * lines;
        messages.get(i).setPosition(new Vector2f(posX, posY));
        if (currentLines >= maxLines + 1) {
          messages.get(i).remove();
        }
        if (currentLines > maxLines + 1) {
          break;
        }
      }
      // for (ChatText message : messages) {
      //  message.setPosition(new Vector2f(posX, posY));
      //  posY += .02f * message.getNumberOfLines();
      // }
      msgSize = messages.size(); // Update size so we can detect further changes
    }
  }

  /** We need to fully recreate and render if even a single letter changes. */
  private void updateGuiText() {
    // guiText.setTextString(chatText); // doesn't work, we need to reload the texture and
    // create a new text
    TextMaster.removeText(guiText);
    guiText =
        new ChatText(
            chatText, 1, textColour, alpha, font, new Vector2f(.06f, .91f), 1f, false, false);
  }

  /** Chat fading. */
  private void updateAlpha() {
    if (enabled && alpha < ALPHA_ON) {
      alpha += .02f;

    } else if (!enabled && alpha > ALPHA_OFF) {
      alpha -= .025f;
    }
    chatGui.setAlpha(alpha);
    guiText.setAlpha(alpha);
    for (ChatText message : messages) {
      message.setAlpha(alpha);
    }
  }

  private void toggleChat() {
    setEnabled(!enabled);
  }

  public boolean isEnabled() {
    return enabled;
  }

  private void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  // public String getChatText() {
  //  return chatText;
  // }
  //
  // public void setChatText(String chatText) {
  //  this.chatText = chatText;
  // }
  //
  public GuiTexture getChatGui() {
    return chatGui;
  }

  public void addText(String stringText) {
    text.add(stringText);
    textSize++;
  }

  public void addChatText() {

    ChatText messageText =
        new ChatText(
            text.get(text.size() - 1),
            .7f,
            textColour,
            alpha,
            font,
            new Vector2f(.06f, .91f),
            maxLineLength,
            false,
            false);
    guiText = clearChatText(guiText);
    messages.add(messageText);
  }
}
