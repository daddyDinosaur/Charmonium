package com.charmonium.module.modules.misc;

import com.charmonium.Charmonium;
import com.charmonium.CharmoniumClient;
import com.charmonium.event.events.Render3DEvent;
import com.charmonium.event.listeners.Render3DListener;
import com.charmonium.module.Category;
import com.charmonium.module.Module;
import com.charmonium.settings.types.ColorSetting;
import com.charmonium.settings.types.FloatSetting;
import com.charmonium.utils.pathfinding.AStarPathFinder;
import com.charmonium.utils.pathfinding.BlockNodeClass;
import com.charmonium.utils.pathfinding.PathFinderConfig;
import com.charmonium.utils.blocks.BlockUtils;
import com.charmonium.utils.render.Color;
import com.charmonium.utils.render.Render3D;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;

import static com.charmonium.CharmoniumClient.mc;

public class Pathfind extends Module implements Render3DListener {
    private BlockPos targetPosition;
    private List<BlockPos> currentPath = new ArrayList<>();

    private final ColorSetting color = ColorSetting.builder().id("pathfind_color").displayName("Color")
            .description("Color").defaultValue(new Color(0, 1f, 1f, 0.3f)).build();

    private final FloatSetting lineThickness = FloatSetting.builder().id("pathfind_linethickness")
            .displayName("Line Thickness").description("Adjust the thickness of the pathfind lines").defaultValue(2f)
            .minValue(0f).maxValue(5f).step(0.1f).build();

    public Pathfind() {
        super("Pathfind");
        setCategory(Category.of("Pathfinding"));
        setDescription("Finds a path to a specified block position");

        addSettings(color, lineThickness);
    }

    @Override
    public void onDisable() {
        Charmonium.getInstance().eventManager.RemoveListener(Render3DListener.class, this);
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
            CharmoniumClient.sendMessage("Path found! Length: " + currentPath.size() + " blocks");
            if (!Charmonium.getInstance().eventManager.isListenerRegistered(Render3DListener.class, this)) Charmonium.getInstance().eventManager.AddListener(Render3DListener.class, this);
        } else {
            CharmoniumClient.sendMessage("No path found to the given coordinates.");
            currentPath.clear();
        }
    }

    @Override
    public void onToggle() {

    }

    public void setTargetPosition(BlockPos targetPosition) {
        this.targetPosition = targetPosition;
    }

    @Override
    public void onRender(Render3DEvent event) {
        if (currentPath.isEmpty()) return;

        for (BlockPos pos : currentPath) {
            Box box = new Box(pos.getX() + 0.1, pos.getY(), pos.getZ() + 0.1,
                    pos.getX() + 0.9, pos.getY() + 0.1, pos.getZ() + 0.9);

            Render3D.draw3DBox(event.GetMatrix(), event.getCamera(), box, color.getValue(),
                    lineThickness.getValue());
        }
    }
}
