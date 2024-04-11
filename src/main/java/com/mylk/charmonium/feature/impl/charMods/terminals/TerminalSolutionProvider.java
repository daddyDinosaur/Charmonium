package com.mylk.charmonium.feature.impl.charMods.terminals;

import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;

import java.util.List;

public interface TerminalSolutionProvider {
    TerminalSolution provideSolution(ContainerChest chest, List<Slot> clicked);
    boolean isApplicable(ContainerChest chest);
}
