package com.shinybunny.cmdplus.commands.modifyitem;

public class ModifyContext {

    private ModifyItemCommand.InventoryType inventoryType;
    private ModifyItemCommand.SlotFinderType finderType;

    public ModifyContext(ModifyItemCommand.InventoryType inventoryType, ModifyItemCommand.SlotFinderType finderType) {
        this.inventoryType = inventoryType;
        this.finderType = finderType;
    }

    public ModifyItemCommand.InventoryType getInventoryType() {
        return inventoryType;
    }

    public ModifyItemCommand.SlotFinderType getFinderType() {
        return finderType;
    }
}
