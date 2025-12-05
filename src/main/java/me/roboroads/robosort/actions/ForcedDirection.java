package me.roboroads.robosort.actions;

import gearth.extensions.parsers.HFloorItem;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import me.roboroads.robosort.Robosort;
import me.roboroads.robosort.data.WiredFurni;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ForcedDirection {
    private static final String ROTATION_VARIABLE_ID = "-122";
    private static final Set<String> EXCEPTIONS = new HashSet<>(Arrays.asList("wf_act_leave_team", "wf_act_join_team", "wf_act_move_to_dir", "wf_act_toggle_to_rnd", "wf_act_control_clock", "wf_cnd_actor_in_team", "wf_cnd_not_in_team"));

    private final Robosort ext;

    public ForcedDirection(Robosort ext) {
        this.ext = ext;
        ext.intercept(HMessage.Direction.TOCLIENT, "ObjectAdd", this::onObjectAdd);
    }

    private void onObjectAdd(HMessage hMessage) {
        if (!ext.forcedDirectionEnabled()) {
            return;
        }

        HFloorItem floorItem = new HFloorItem(hMessage.getPacket());
        String furniClassName = ext.furniDataTools.getFloorItemClassName(floorItem.getTypeId());
        if (!WiredFurni.isWiredFurni(furniClassName)) {
            return;
        }

        if ((ext.getForcedDirection() == Robosort.ForcedDirection.LEFT && EXCEPTIONS.contains(furniClassName)) || (ext.getForcedDirection() == Robosort.ForcedDirection.RIGHT && !EXCEPTIONS.contains(furniClassName))) {
            ext.sendToServer(new HPacket("WiredSetObjectVariableValue", HMessage.Direction.TOSERVER, 0, floorItem.getId(), ROTATION_VARIABLE_ID, 1));
        }
    }
}
