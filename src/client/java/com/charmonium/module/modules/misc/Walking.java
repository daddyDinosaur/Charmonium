package com.charmonium.module.modules.misc;

import com.charmonium.Charmonium;
import com.charmonium.CharmoniumClient;
import com.charmonium.event.events.Render3DEvent;
import com.charmonium.event.events.TickEvent;
import com.charmonium.event.listeners.Render3DListener;
import com.charmonium.event.listeners.TickListener;
import com.charmonium.module.Category;
import com.charmonium.module.Module;
import com.charmonium.settings.types.*;
import com.charmonium.utils.blocks.BlockUtils;
import com.charmonium.utils.misc.KeyBindUtils;
import com.charmonium.utils.pathfinding.AStarPathFinder;
import com.charmonium.utils.pathfinding.BlockNodeClass;
import com.charmonium.utils.pathfinding.PathFinderConfig;
import com.charmonium.utils.pathfinding.Walker;
import com.charmonium.utils.render.Color;
import com.charmonium.utils.render.Render3D;
import com.charmonium.utils.rotation.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.*;

import static com.charmonium.CharmoniumClient.mc;

public class Walking extends Module implements TickListener, Render3DListener {
    private BlockPos targetPosition;
    private List<BlockPos> currentPath = new ArrayList<>();
    private List<Vec3d> pathVec = new ArrayList<>();
    private boolean isWalking = false;

    // Settings
    private final ColorSetting color = ColorSetting.builder()
            .id("walker_color").displayName("Path Color")
            .defaultValue(new Color(0, 1f, 1f, 0.3f)).build();

    private final FloatSetting lineThickness = FloatSetting.builder()
            .id("walker_linethickness").displayName("Line Thickness")
            .defaultValue(2f).minValue(0f).maxValue(5f).build();

    private final FloatSetting rotationTime = FloatSetting.builder()
            .id("walker_rotationtime").displayName("Rotation Time")
            .defaultValue(500f).minValue(0f).build();

    public Walking() {
        super("Walker");
        setCategory(Category.of("Pathfinding"));
        setDescription("Walks along a calculated path");
        addSettings(color, lineThickness, rotationTime);
    }

    @Override
    public void onEnable() {
        if (mc.player == null || targetPosition == null) return;

        BlockPos startPos = new BlockPos(
                (int) Math.floor(mc.player.getX()),
                (int) Math.floor(mc.player.getY()),
                (int) Math.floor(mc.player.getZ())
        );

        PathFinderConfig config = new PathFinderConfig(10000, startPos, targetPosition);
        AStarPathFinder pathFinder = new AStarPathFinder(config);
        List<BlockNodeClass> path = pathFinder.findPath();

        if (path != null && !path.isEmpty()) {
            currentPath = new ArrayList<>(path.stream()
                    .map(BlockNodeClass::getBlockPos)
                    .toList());
            pathVec = new ArrayList<>(pathFinder.fromClassToVec(path));
            isWalking = true;
            Charmonium.getInstance().eventManager.AddListener(TickListener.class, this);
            Charmonium.getInstance().eventManager.AddListener(Render3DListener.class, this);
            CharmoniumClient.sendMessage("Path found! Starting walk...");

            Walker walker = new Walker();
            walker.run(pathVec, true, false, 2);
        } else {
            CharmoniumClient.sendMessage("No path found to the given coordinates.");
            currentPath.clear();
            //disable();
        }
    }

    @Override
    public void onToggle() {

    }

    @Override
    public void onDisable() {
        isWalking = false;
        pathVec.clear();
        currentPath.clear();
        KeyBindUtils.stopMovement();
        Charmonium.getInstance().eventManager.RemoveListener(TickListener.class, this);
        Charmonium.getInstance().eventManager.RemoveListener(Render3DListener.class, this);
    }

    @Override
    public void onTick(TickEvent.Pre event) {

    }

    @Override
    public void onTick(TickEvent.Post event) {
    }

    @Override
    public void onRender(Render3DEvent event) {
        for (BlockPos pos : currentPath) {
            Box box = new Box(pos.getX() + 0.1, pos.getY(), pos.getZ() + 0.1,
                    pos.getX() + 0.9, pos.getY() + 0.1, pos.getZ() + 0.9);
            Render3D.draw3DBox(event.GetMatrix(), event.getCamera(), box,
                    color.getValue(), lineThickness.getValue());
        }
    }

    public void setTargetPosition(BlockPos targetPosition) {
        this.targetPosition = targetPosition;
    }
}