package com.mylk.charmonium.feature.impl.charMods.terminals;

import lombok.Data;
import net.minecraft.inventory.Slot;

import java.util.List;

@Data
public class TerminalSolution {
    private List<Slot> currSlots;
    private List<Slot> nextSlots;
}
