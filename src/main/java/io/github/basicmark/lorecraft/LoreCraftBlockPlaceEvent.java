package io.github.basicmark.lorecraft;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class LoreCraftBlockPlaceEvent extends BlockPlaceEvent {
	public LoreCraftBlockPlaceEvent(Block placedBlock,
			BlockState replacedBlockState, Block placedAgainst,
			ItemStack itemInHand, Player thePlayer, boolean canBuild) {
		super(placedBlock, replacedBlockState, placedAgainst, itemInHand, thePlayer,
				canBuild);
	}
}
