package com.mylk.charmonium.event;

import lombok.Getter;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Getter
public class GuiContainerEvent extends Event {
    private final Container container;
    private final GuiContainer gui;

    public GuiContainerEvent(Container container, GuiContainer gui) {
        this.container = container;
        this.gui = gui;
    }

    @Cancelable
    public static class DrawSlotEvent extends GuiContainerEvent {
        private Slot slot;

        public DrawSlotEvent(Container container, GuiContainer gui, Slot slot) {
            super(container, gui);
            this.slot = slot;
        }

        public Slot getSlot() {
            return slot;
        }

        public void setSlot(Slot slot) {
            this.slot = slot;
        }
    }

    @Cancelable
    public static class SlotClickEvent extends GuiContainerEvent {
        private Slot slot;
        private int slotId;

        public SlotClickEvent(Container container, GuiContainer gui, Slot slot, int slotId) {
            super(container, gui);
            this.slot = slot;
            this.slotId = slotId;
        }

        public Slot getSlot() {
            return slot;
        }

        public void setSlot(Slot slot) {
            this.slot = slot;
        }

        public int getSlotId() {
            return slotId;
        }

        public void setSlotId(int slotId) {
            this.slotId = slotId;
        }
    }
}
