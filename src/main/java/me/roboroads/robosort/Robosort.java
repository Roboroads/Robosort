package me.roboroads.robosort;

import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.parsers.HFloorItem;
import gearth.misc.Cacher;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.collections.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import me.roboroads.robosort.data.WiredBoxType;
import me.roboroads.robosort.data.WiredFurni;
import me.roboroads.robosort.furnidata.FurniDataTools;
import me.roboroads.robosort.state.*;
import me.roboroads.robosort.util.Mover;
import me.roboroads.robosort.util.Util;

import java.io.File;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ExtensionInfo(
  Title = "Robosort",
  Description = "Automatically sort your wired stacks.",
  // %%VERSION%% will be replaced by the GitHub Actions workflow
  Version = "%%VERSION%%",
  Author = "Roboroads"
)
public class Robosort extends ExtensionForm {
    public CheckBox commandsEnabledCheckbox;
    public CheckBox sortOnActionEnabledCheckbox;
    public ListView<WiredBoxType> sortOrderListView;

    private ObservableList<WiredBoxType> sortOrder;

    public WiredState wiredState;
    public FloorPlanState floorPlanState;
    public FurniDataTools furniDataTools;
    public RoomPermissionState roomPermissionState;
    public Mover mover;

    private CommandState commandState = CommandState.IDLE;
    private int commandArgument = 0;

    private LocalDateTime furniRemoved;
    private LocalDateTime furniAdded;
    private LocalDateTime furniMoved;

    @Override
    protected void initExtension() {
        onConnect((host, i, s1, s2, hClient) -> furniDataTools = new FurniDataTools(host));
        wiredState = WiredState.getInstance(this);
        mover = Mover.getInstance(this);
        roomPermissionState = RoomPermissionState.getInstance(this);
        floorPlanState = FloorPlanState.getInstance(this);

        furniRemoved = LocalDateTime.now().minusSeconds(1);
        furniAdded = LocalDateTime.now().minusSeconds(1);
        furniMoved = LocalDateTime.now().minusSeconds(1);

        initializeCache();
        initializeSortOrderListView();

        // For commands
        intercept(HMessage.Direction.TOSERVER, "Chat", this::handleChat);
        intercept(HMessage.Direction.TOSERVER, "ClickFurni", this::handleClickFurni);
        intercept(HMessage.Direction.TOCLIENT, "CloseConnection", m -> commandState = CommandState.IDLE);
        intercept(HMessage.Direction.TOSERVER, "Quit", m -> commandState = CommandState.IDLE);
        intercept(HMessage.Direction.TOCLIENT, "RoomReady", m -> commandState = CommandState.IDLE);

        // For sort on action
        intercept(HMessage.Direction.TOSERVER, "PlaceObject", m -> furniAdded = LocalDateTime.now());
        intercept(HMessage.Direction.TOSERVER, "BuildersClubPlaceRoomItem", m -> furniAdded = LocalDateTime.now());
        intercept(HMessage.Direction.TOSERVER, "MoveObject", m -> furniMoved = LocalDateTime.now());
        intercept(HMessage.Direction.TOSERVER, "PickupObject", m -> furniRemoved = LocalDateTime.now());
        intercept(HMessage.Direction.TOCLIENT, "ObjectAdd", m -> handleObjectAddAndUpdate(m, furniAdded));
        intercept(HMessage.Direction.TOCLIENT, "ObjectRemove", this::handleObjectRemove);
        intercept(HMessage.Direction.TOCLIENT, "ObjectUpdate", m -> handleObjectAddAndUpdate(m, furniMoved));
    }

    private void initializeSortOrderListView() {
        // Get all options from cache, add missing values and set as observable list
        List<WiredBoxType> cachedSortOrder = Optional.ofNullable(Cacher.getList("sortOrder")).orElse(new ArrayList<>())
          .stream().map(o -> WiredBoxType.valueOf((String) o)).collect(Collectors.toList());
        // Add missing values
        WiredBoxType.defaultValues().stream().filter(o -> !cachedSortOrder.contains(o)).forEach(cachedSortOrder::add);

        // Reverse the list to make it more intuitive
        sortOrder = FXCollections.observableArrayList(Util.reverse(cachedSortOrder));
        sortOrderListView.setItems(sortOrder);

        // Save the sort order to cache when it changes, save it in un-reversed order
        sortOrder.addListener((ListChangeListener<? super WiredBoxType>) o -> Cacher.put("sortOrder", Util.reverse(sortOrder)));

        // make sort box as big as the items
        sortOrderListView.setPrefHeight(sortOrder.size() * 24 + 4);

        // Enable drag and drop for reordering
        sortOrderListView.setCellFactory(lv -> {
            ListCell<WiredBoxType> cell = new ListCell<WiredBoxType>() {
                @Override
                protected void updateItem(WiredBoxType item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item.toString());
                }
            };

            cell.setOnDragDetected(event -> {
                if (!cell.isEmpty()) {
                    Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(Integer.toString(cell.getIndex()));
                    db.setContent(content);
                    event.consume();
                }
            });

            cell.setOnDragOver(event -> {
                if (event.getGestureSource() != cell &&
                  event.getDragboard().hasString()) {
                    event.acceptTransferModes(TransferMode.MOVE);
                    event.consume();
                }
            });

            cell.setOnDragEntered(event -> {
                if (event.getGestureSource() != cell &&
                  event.getDragboard().hasString()) {
                    cell.setOpacity(0.3);
                }
            });

            cell.setOnDragExited(event -> {
                if (event.getGestureSource() != cell &&
                  event.getDragboard().hasString()) {
                    cell.setOpacity(1);
                }
            });

            cell.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;

                if (db.hasString()) {
                    int draggedIndex = Integer.parseInt(db.getString());
                    int thisIndex = cell.getIndex();

                    WiredBoxType item = sortOrder.remove(draggedIndex);
                    sortOrder.add(thisIndex, item);

                    sortOrderListView.getSelectionModel().select(thisIndex);

                    success = true;
                    event.consume();
                }
                event.setDropCompleted(success);
            });

            return cell;
        });
    }

    private void initializeCache() {
        File extDir = null;
        try {
            extDir = (new File(Robosort.class.getProtectionDomain().getCodeSource().getLocation().toURI())).getParentFile();
            if (extDir.getName().equals("Extensions")) {
                extDir = extDir.getParentFile();
            }
        } catch (URISyntaxException ignored) {
        }

        Cacher.setCacheDir(extDir + File.separator + "Cache");
    }

    //<editor-fold desc="Command handling">
    private void handleChat(HMessage hMessage) {
        String text = hMessage.getPacket().readString();
        String[] command = text.split(" ");

        List<String> availableCommands;
        if (commandState == CommandState.IDLE) {
            availableCommands = new ArrayList<>(Arrays.asList(":sort", ":up", ":down"));
        } else {
            availableCommands = new ArrayList<>(Collections.singletonList(":abort"));
        }

        if (!availableCommands.contains(command[0]) || !checkCanMove(true)) {
            return;
        }

        hMessage.setBlocked(true);
        if (commandState == CommandState.IDLE) {
            switch (command[0]) {
                case ":sort":
                    commandState = CommandState.SORT_COMMAND;
                    sendChat("Click on a box in the wired stack you want to sort");
                    return;
                case ":up":
                    commandArgument = text.matches("^:up \\d+$") ? Integer.parseInt(text.split(" ")[1]) : 1;
                    commandState = CommandState.UP_COMMAND;
                    sendChat("Click on the box that you want to move up " + commandArgument + " position(s).");
                    return;
                case ":down":
                    commandArgument = text.matches("^:down \\d+$") ? Integer.parseInt(text.split(" ")[1]) : 1;
                    commandState = CommandState.DOWN_COMMAND;
                    sendChat("Click on a box that you want to move down " + commandArgument + " position(s).");
            }
        } else if (text.equals(":abort")) {
            commandState = CommandState.IDLE;
            sendChat("Aborted");
        }
    }

    private void handleClickFurni(HMessage hMessage) {
        if (!commandsEnabled() || !checkCanMove(false) || commandState == CommandState.IDLE) {
            return;
        }

        int furniId = hMessage.getPacket().readInteger();
        WiredFurni wiredFurni = wiredState.getCurrent(furniId);
        if (wiredFurni == null) {
            return;
        }

        switch (commandState) {
            case SORT_COMMAND:
                sort(wiredFurni.floorItem.getTile().getX(), wiredFurni.floorItem.getTile().getY());
                break;
            case UP_COMMAND:
                moveUp(wiredFurni, commandArgument);
                break;
            case DOWN_COMMAND:
                moveDown(wiredFurni, commandArgument);
                break;
        }

        hMessage.setBlocked(true);
        commandState = CommandState.IDLE;
    }

    private boolean commandsEnabled() {
        return commandsEnabledCheckbox.isSelected();
    }
    //</editor-fold>

    //<editor-fold desc="Sort on action">
    private void handleObjectAddAndUpdate(HMessage hMessage, LocalDateTime lastAction) {
        long msDiff = Duration.between(lastAction, LocalDateTime.now()).toMillis();
        if (sortOnActionEnabled() && checkCanMove(false) && msDiff < 500) {
            HFloorItem floorItem = new HFloorItem(hMessage.getPacket());
            String furniClassName = furniDataTools.getFloorItemClassName(floorItem.getTypeId());

            if (WiredFurni.isWiredFurni(furniClassName)) {
                new Timer().schedule(
                  new TimerTask() {
                      @Override
                      public void run() {
                          sort(floorItem.getTile().getX(), floorItem.getTile().getY());

                          WiredFurni previousPosition = wiredState.getPrevious(floorItem.getId());
                          if (previousPosition != null
                            && (floorItem.getTile().getX() != previousPosition.floorItem.getTile().getX()
                                  || floorItem.getTile().getY() != previousPosition.floorItem.getTile().getY())) {
                              sort(previousPosition.floorItem.getTile().getX(), previousPosition.floorItem.getTile().getY());
                          }
                      }
                  }, 10
                );
            }
        }
    }

    private boolean sortOnActionEnabled() {
        return sortOnActionEnabledCheckbox.isSelected();
    }

    private void handleObjectRemove(HMessage hMessage) {
        long msDiff = Duration.between(furniRemoved, LocalDateTime.now()).toMillis();
        if (sortOnActionEnabled() && checkCanMove(false) && msDiff < 500) {
            HPacket packet = hMessage.getPacket();
            int furniId = Integer.parseInt(packet.readString());
            WiredFurni wiredFurni = wiredState.get(furniId);
            if (wiredFurni != null) {
                new Timer().schedule(
                  new TimerTask() {
                      @Override
                      public void run() {
                          sort(wiredFurni.floorItem.getTile().getX(), wiredFurni.floorItem.getTile().getY());
                      }
                  }, 10
                );
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="Utilities">
    private boolean checkCanMove(boolean callOut) {
        if (!floorPlanState.isReady()) {
            if (callOut) {
                sendChat("Floorplan is not loaded - re-enter the room to load it.");
            }
            return false;
        }

        if (!wiredState.isReady()) {
            if (callOut) {
                sendChat("There are no wired boxes in the room - re-enter the room if you think that's false.");
            }
            return false;
        }

        if (!furniDataTools.isReady()) {
            if (callOut) {
                sendChat("Furnidata is not loaded, wait a minute or restart Habbo.");
            }
            return false;
        }

        if (!roomPermissionState.canModifyWired()) {
            if (callOut) {
                sendChat("You are not allowed to modify wired here.");
            }
            return false;
        }

        return true;
    }

    public void sendChat(String text) {
        sendToClient(new HPacket("Chat", HMessage.Direction.TOCLIENT, -1, "[ROBOSORT] " + text, 0, 1, 0, -1));
    }
    //</editor-fold>

    //<editor-fold desc="Functionality">
    private void sort(int x, int y) {
        List<WiredBoxType> unreversedSortOrder = Util.reverse(sortOrder);
        List<WiredFurni> stackState = wiredState.wiredOnTile(x, y).stream()
          .sorted(Comparator.comparingInt(wiredFurni -> unreversedSortOrder.indexOf(wiredFurni.wiredBoxType)))
          .collect(Collectors.toList());

        int currentAltitude = floorPlanState.getTileHeight(x, y) * 100;
        for (WiredFurni wiredFurni : stackState) {
            int currentZ = (int) (wiredFurni.floorItem.getTile().getZ() * 100);
            if (Math.abs(currentZ - currentAltitude) > 1) { // Floating point precision makes sometimes 1 unit difference, we can ignore that
                mover.queue(wiredFurni.floorItem.getId(), currentAltitude);
            }
            currentAltitude += wiredFurni.height;
        }
    }

    private void moveUp(WiredFurni wiredFurni, int amount) {
        List<WiredFurni> stackState = wiredState.wiredOnTile(wiredFurni.floorItem.getTile().getX(), wiredFurni.floorItem.getTile().getY())
          .stream().filter(i -> i.wiredBoxType == wiredFurni.wiredBoxType)
          .collect(Collectors.toList());

        int index = stackState.indexOf(wiredFurni);
        List<WiredFurni> movingBoxes = stackState.subList(index, Math.min(index + amount + 1, stackState.size()));

        if (movingBoxes.size() < 2) {
            sendChat("There's no boxes of the same type above this one. Aborting.");
            return;
        }

        for (int i = 0; i < movingBoxes.size(); i++) {
            WiredFurni movingBox = movingBoxes.get(i);
            double newZ = movingBoxes.get((i == 0 ? (movingBoxes.size() - 1) : (i - 1))).floorItem.getTile().getZ();
            mover.queue(movingBox.floorItem.getId(), (int) (newZ * 100));
        }
    }

    private void moveDown(WiredFurni wiredFurni, int amount) {
        List<WiredFurni> stackState = wiredState.wiredOnTile(wiredFurni.floorItem.getTile().getX(), wiredFurni.floorItem.getTile().getY())
          .stream().filter(i -> i.wiredBoxType == wiredFurni.wiredBoxType)
          .collect(Collectors.toList());

        int index = stackState.indexOf(wiredFurni);
        List<WiredFurni> movingBoxes = stackState.subList(Math.max(0, index - amount), index + 1);

        if (movingBoxes.size() < 2) {
            sendChat("There's no boxes of the same type below this one. Please try again.");
            return;
        }

        for (int i = movingBoxes.size() - 1; i >= 0; i--) {
            WiredFurni movingBox = movingBoxes.get(i);
            double newZ = movingBoxes.get((i == movingBoxes.size() - 1 ? 0 : i + 1)).floorItem.getTile().getZ();
            mover.queue(movingBox.floorItem.getId(), (int) (newZ * 100));
        }
    }
    //</editor-fold>

    enum CommandState {
        IDLE,
        SORT_COMMAND,
        UP_COMMAND,
        DOWN_COMMAND,
    }
}
