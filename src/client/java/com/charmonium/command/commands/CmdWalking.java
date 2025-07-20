package com.charmonium.command.commands;

import com.charmonium.Charmonium;
import com.charmonium.CharmoniumClient;
import com.charmonium.command.Command;
import com.charmonium.command.InvalidSyntaxException;
import com.charmonium.module.modules.misc.Walking;
import net.minecraft.util.math.BlockPos;

public class CmdWalking extends Command {
    public CmdWalking() {
        super("walk", "Walks to specified coordinates", "<x> <y> <z>");
    }

    @Override
    public void runCommand(String[] parameters) throws InvalidSyntaxException {
        if (parameters.length != 3) throw new InvalidSyntaxException(this);

        Walking module = Charmonium.getInstance().moduleManager.walk;

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
            CharmoniumClient.sendMessage("Invalid coordinates! Usage: /char walk <x> <y> <z>");
        }
    }

    @Override
    public String[] getAutocorrect(String previousParameter) {
        return new String[]{"x y z"};
    }
}