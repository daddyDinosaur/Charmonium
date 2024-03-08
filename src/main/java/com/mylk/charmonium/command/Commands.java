package com.mylk.charmonium.command;

import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.config.aotv.AOTVWaypointsStructs;
import com.mylk.charmonium.config.page.AOTVWaypointsPage;
import com.mylk.charmonium.util.BlockUtils;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Commands implements ICommand {
    public static ArrayList<String> aliases = new ArrayList<>(Arrays.asList("char", "charmonium"));

    @Override
    public String getCommandName() {
        return "char";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/char";
    }

    @Override
    public List<String> getCommandAliases() {
        return aliases;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            Charmonium.config.openGui();            return;
        }

        switch (args[0]) {
            case "add": {
                try {
                    if (Charmonium.aotvWaypoints.getSelectedRoute() == null) return;

//                    Charmonium.sendMessage("Player Loc:" + Charmonium.mc.thePlayer.getPosition().down());
//                    Charmonium.sendMessage("Selected Route:" + Charmonium.aotvWaypoints.getSelectedRoute());

                    boolean added = Charmonium.aotvWaypoints.addCoord(Charmonium.aotvWaypoints.getSelectedRoute(), new AOTVWaypointsStructs.Waypoint(String.valueOf(Charmonium.aotvWaypoints.getSelectedRoute().waypoints.size()), BlockUtils.fromVecToBP(Charmonium.mc.thePlayer.getPositionVector().subtract(0, 1, 0))));
                    if (added)
                        Charmonium.sendMessage("AOTV Waypoints - Added current position (" + Math.floor(Charmonium.mc.thePlayer.getPositionVector().xCoord) + ", " + Math.floor(Charmonium.mc.thePlayer.getPositionVector().yCoord) + ", " + Math.floor(Charmonium.mc.thePlayer.getPositionVector().zCoord) + ") to selected waypoint list");
                    else
                        Charmonium.sendMessage("AOTV Waypoints - This waypoint already exists!");
                    AOTVWaypointsStructs.SaveWaypoints();
                    AOTVWaypointsPage.redrawRoutes();
                }catch (NumberFormatException e){
                    Charmonium.sendMessage("AOTV Error: " + e.getMessage());
                    return;
                }
                break;
            }
            case "remove": {
                try {
                    AOTVWaypointsStructs.Waypoint waypointToDelete = null;
                    if (Charmonium.aotvWaypoints.getSelectedRoute() == null) return;
                    for (AOTVWaypointsStructs.Waypoint waypoint : Charmonium.aotvWaypoints.getSelectedRoute().waypoints) {
                        if (BlockUtils.fromVecToBP(Charmonium.mc.thePlayer.getPositionVector().subtract(0, 1, 0)).equals(new BlockPos(waypoint.x, waypoint.y, waypoint.z))) {
                            waypointToDelete = waypoint;
                        }
                    }
                    if (waypointToDelete != null) {
                        Charmonium.aotvWaypoints.removeCoord(Charmonium.aotvWaypoints.getSelectedRoute(), waypointToDelete);
                        Charmonium.sendMessage("AOTV Waypoints - Removed current position (" + Math.floor(Charmonium.mc.thePlayer.getPositionVector().xCoord) + ", " + Math.floor(Charmonium.mc.thePlayer.getPositionVector().yCoord) + ", " + Math.floor(Charmonium.mc.thePlayer.getPositionVector().zCoord) + ") from selected waypoint list");
                        AOTVWaypointsStructs.SaveWaypoints();
                        AOTVWaypointsPage.redrawRoutes();
                    } else {
                        Charmonium.sendMessage("AOTV Waypoints - No waypoint found at your current position");
                    }
                }catch (NumberFormatException e){
                    Charmonium.sendMessage("AOTV Error: " + e.getMessage());
                    return;
                }
                break;
            }
            case "removeall": {
                //
                break;
            }
            default: {
                Charmonium.config.openGui();
                break;
            }
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return null;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(@NotNull ICommand o) {
        return 0;
    }
}
