package me.roboroads.robosort;

import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.misc.Cacher;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import me.roboroads.robosort.actions.SortOnAction;
import me.roboroads.robosort.commands.CommandHandler;
import me.roboroads.robosort.commands.DownCommand;
import me.roboroads.robosort.commands.SortCommand;
import me.roboroads.robosort.commands.UpCommand;
import me.roboroads.robosort.data.WiredBoxType;
import me.roboroads.robosort.furnidata.FurniDataTools;
import me.roboroads.robosort.state.FloorPlanState;
import me.roboroads.robosort.state.RoomPermissionState;
import me.roboroads.robosort.state.WiredState;
import me.roboroads.robosort.util.HabboUtil;
import me.roboroads.robosort.util.Mover;
import me.roboroads.robosort.util.Util;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ExtensionInfo(
  Title = "Robosort", Description = "Automatically sort your wired stacks.",
  // %%VERSION%% will be replaced by the GitHub Actions workflow
  Version = "%%VERSION%%", Author = "Roboroads"
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

    @Override
    protected void initExtension() {
        onConnect((host, i, s1, s2, hClient) -> furniDataTools = new FurniDataTools(host));
        wiredState = WiredState.I(this);
        mover = Mover.I(this);
        roomPermissionState = RoomPermissionState.I(this);
        floorPlanState = FloorPlanState.I(this);

        // Initialize shared utility singleton
        HabboUtil.init(this);

        initializeCache();
        initializeSortOrderListView();

        // Initialize new handlers (they register their own interceptions)
        new CommandHandler(this, Arrays.asList(new SortCommand(this), new UpCommand(this), new DownCommand(this)));
        new SortOnAction(this);
    }

    private void initializeSortOrderListView() {
        // Get all options from cache, add missing values and set as observable list
        List<WiredBoxType> cachedSortOrder = Optional.ofNullable(Cacher.getList("sortOrder")).orElse(new ArrayList<>()).stream().map(o -> WiredBoxType.valueOf((String) o)).collect(Collectors.toList());
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
                if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                    event.acceptTransferModes(TransferMode.MOVE);
                    event.consume();
                }
            });

            cell.setOnDragEntered(event -> {
                if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                    cell.setOpacity(0.3);
                }
            });

            cell.setOnDragExited(event -> {
                if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
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

    public boolean commandsEnabled() {
        return commandsEnabledCheckbox.isSelected();
    }

    public boolean sortOnActionEnabled() {
        return sortOnActionEnabledCheckbox.isSelected();
    }
}
