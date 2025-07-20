package com.charmonium.command.commands;

import com.charmonium.Charmonium;
import com.charmonium.CharmoniumClient;
import com.charmonium.command.Command;
import com.charmonium.command.InvalidSyntaxException;
import com.charmonium.module.modules.misc.Pathfind;
import com.charmonium.utils.misc.KeyBindUtils;
import com.charmonium.utils.rotation.Rotation;
import net.minecraft.util.math.BlockPos;

public class CmdPathfind extends Command {
    public CmdPathfind() {
        super("pathfind", "Finds a path to specified coordinates", "<x> <y> <z>");
    }

    @Override
    public void runCommand(String[] parameters) throws InvalidSyntaxException {
        Charmonium.getInstance().rotationManager.easeTo(new Rotation(45.0f, -30.0f), 2500);

        if (parameters.length != 3) {
            //Charmonium.getInstance().rotationManager.easeTo(new Rotation(45.0f, -30.0f), 2500);
            throw new InvalidSyntaxException(this);
        }

        //mc.options.forwardKey.setPressed(false);

        Pathfind module = Charmonium.getInstance().moduleManager.pathfinding;

        try {
            BlockPos targetPos = new BlockPos(
                    Integer.parseInt(parameters[0]),
                    Integer.parseInt(parameters[1]),
                    Integer.parseInt(parameters[2])
            );

            module.setTargetPosition(targetPos);
            module.state.setValue(true);

        } catch (NumberFormatException e) {
            module.state.setValue(false);
            CharmoniumClient.sendMessage("Invalid coordinates! Usage: /char pathfind <x> <y> <z>");
        }
    }

    @Override
    public String[] getAutocorrect(String previousParameter) {
        return new String[]{"x y z"};
    }
}
