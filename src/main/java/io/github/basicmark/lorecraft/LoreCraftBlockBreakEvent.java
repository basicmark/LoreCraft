package io.github.basicmark.lorecraft;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

public class LoreCraftBlockBreakEvent extends BlockBreakEvent{
	public LoreCraftBlockBreakEvent(Block theBlock, Player player) {
		super(theBlock, player);
	}
}
