package com.mylk.charmonium.command;

import com.mylk.charmonium.Charmonium;
import com.mylk.charmonium.config.aotv.AOTVWaypointsStructs;
import com.mylk.charmonium.config.page.AOTVWaypointsPage;
import com.mylk.charmonium.feature.impl.brush;
import com.mylk.charmonium.pathfinding.main.AStarPathFinderImproved;
import com.mylk.charmonium.pathfinding.utils.BlockNodeClass;
import com.mylk.charmonium.pathfinding.utils.PathFinderConfig;
import com.mylk.charmonium.pathfinding.walker.WalkerMain;
import com.mylk.charmonium.pathfinding.walker.WalkerMainImproved;
import com.mylk.charmonium.util.BlockUtils;
import com.mylk.charmonium.util.charHelpers.TunnelUtils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Commands implements ICommand {
    public static ArrayList<String> aliases = new ArrayList<>(Arrays.asList("char", "charmonium"));

    @Override
    public String getCommandName() {
        return "char";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/char [help|add|remove|removeall|setblock <blockID>|deleteblocks|removeblock]";
    }

    @Override
    public List<String> getCommandAliases() {
        return aliases;
    }

    public static List<BlockPos> currentPath = new ArrayList<>();

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            Charmonium.config.openGui();
            return;
        }

        switch (args[0]) {
            case "help": {
                Charmonium.sendMessage("Usage: /char [help|add|remove|removeall|setblock <blockID>|removeallblocks|removeblock]");
                break;
            }
            case "highlight": {
                try {
                    TunnelUtils.getAllBlocks();
                    currentPath = TunnelUtils.possibleBreaks.stream()
                            .map(BlockPos::new)
                            .collect(Collectors.toList());

                    if (!currentPath.isEmpty()) {
                        Charmonium.sendMessage("Highlighted " + currentPath.size() + " possible blocks.");
                    } else {
                        Charmonium.sendMessage("No blocks to highlight found.");
                    }
                } catch (Exception e) {
                    Charmonium.sendMessage("Highlight Error: " + e.getMessage());
                }
                break;
            }
            case "pathfind": {
                try {
                    if (args.length < 3) {
                        Charmonium.sendMessage("Usage: /char pathfind <endX> <endY> <endZ>");
                        return;
                    }

                    BlockPos startPos = new BlockPos(
                            Math.floor(Charmonium.mc.thePlayer.getPositionVector().xCoord),
                            Math.floor(Charmonium.mc.thePlayer.getPositionVector().yCoord),
                            Math.floor(Charmonium.mc.thePlayer.getPositionVector().zCoord)
                    );
                    BlockPos endPos = new BlockPos(
                            Integer.parseInt(args[1]),
                            Integer.parseInt(args[2]),
                            Integer.parseInt(args[3])
                    );

                    PathFinderConfig config = new PathFinderConfig(10000, startPos, endPos);
                    AStarPathFinderImproved pathFinder = new AStarPathFinderImproved(config);
                    List<BlockNodeClass> path = pathFinder.findPath();
                    List<Vec3> pathA;
                    pathA = pathFinder.fromClassToVec(path);

                    if (pathA != null) {
                        Charmonium.sendMessage("Path found! Length: " + path.size() + " blocks");
                        currentPath = path.stream()
                                .map(node -> node.blockPos)
                                .collect(Collectors.toList());

                        WalkerMainImproved walker = new WalkerMainImproved();

                        walker.run(pathA, true, false, 2);

                    } else {
                        Charmonium.sendMessage("No path found to the given coordinates.");
                        currentPath.clear();
                    }
                } catch (NumberFormatException e) {
                    Charmonium.sendMessage("Invalid coordinates: " + e.getMessage());
                }
                break;
            }
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
            case "setblock": {
                try {
                    if (args.length < 2) {
                        Charmonium.sendMessage("Usage: /char setblock <blockID>");
                        return;
                    }

                    int blockID = Integer.parseInt(args[1]);
                    IBlockState blockState = Block.getBlockById(blockID).getDefaultState();

                    brush.setBlockInFrontOfPlayer(blockState, args[2].equalsIgnoreCase("true"));
                    Charmonium.sendMessage("Block placed in front of the player.");
                } catch (NumberFormatException e) {
                    Charmonium.sendMessage("Invalid block ID: " + e.getMessage());
                }
                break;
            }
            case "removeallblocks": {
                brush brushObj = new brush();
                brushObj.deletePlacedBlocks();
                Charmonium.sendMessage("All placed blocks have been deleted.");
                break;
            }
            case "removeblock": {
                brush.removeBlockInFrontOfPlayer();
                Charmonium.sendMessage("Block in front of the player has been removed.");
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
