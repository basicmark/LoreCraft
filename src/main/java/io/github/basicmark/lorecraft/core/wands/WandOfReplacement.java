package io.github.basicmark.lorecraft.core.wands;

import io.github.basicmark.lorecraft.LoreCraft;
import io.github.basicmark.lorecraft.LoreCraftAction;
import io.github.basicmark.lorecraft.LoreCraftBlockBreakEvent;
import io.github.basicmark.lorecraft.LoreCraftBlockPlaceEvent;
import io.github.basicmark.lorecraft.LoreCraftItem;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public class WandOfReplacement implements LoreCraftAction, Listener {
	private static EnumSet<Material> whiteList = EnumSet.of(
			Material.STONE,
			Material.DIRT,
			Material.COBBLESTONE,
			Material.WOOD,
			Material.LOG,
			Material.GLASS,
			Material.LAPIS_BLOCK,
			Material.SANDSTONE,
			Material.WOOL,
			Material.GOLD_BLOCK,
			Material.IRON_BLOCK,
			Material.BRICK,
			Material.BOOKSHELF,
			Material.MOSSY_COBBLESTONE,
			Material.DIAMOND_BLOCK,
			Material.SOIL,
			Material.SNOW,
			Material.ICE,
			Material.SNOW_BLOCK,
			Material.CLAY,
			Material.PUMPKIN,
			Material.NETHERRACK,
			Material.SOUL_SAND,
			Material.GLOWSTONE,
			Material.JACK_O_LANTERN,
			Material.STAINED_GLASS,
			Material.SMOOTH_BRICK,
			Material.MELON_BLOCK,
			Material.NETHER_BRICK,
			Material.REDSTONE_LAMP_OFF,
			Material.EMERALD_BLOCK,
			Material.REDSTONE_BLOCK,
			Material.QUARTZ_BLOCK,
			Material.STAINED_CLAY,
			Material.LOG_2,
			Material.HAY_BLOCK,
			Material.HARD_CLAY,
			Material.COAL_BLOCK,
			Material.PACKED_ICE,
			Material.GRASS,
			Material.MYCEL,
			Material.PURPUR_BLOCK,
			Material.PURPUR_PILLAR,
			Material.END_BRICKS,
			Material.BONE_BLOCK,
			Material.MAGMA,
			Material.NETHER_WART_BLOCK,
			Material.RED_NETHER_BRICK);
	private HashMap<Player, ReplacementPlayerData> replaceData;
	LoreCraft loreCraft;
	JavaPlugin plugin;
	LoreCraftItem item;

	public void onEnable(LoreCraft loreCraft, LoreCraftItem item) {
		this.loreCraft = loreCraft;
		this.plugin = loreCraft.getPlugin();
		this.item = item;

		replaceData = new HashMap<Player, ReplacementPlayerData>();
        /* Check if players are already online */
    	for(Player player : plugin.getServer().getOnlinePlayers()) {
    		replaceData.put((Player) player, new ReplacementPlayerData(plugin, player, 2));
    	}
    	
    	plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	public void onDisable() {
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		replaceData.put(player, new ReplacementPlayerData(plugin, player, 2));
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		replaceData.remove(player);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerInteract(PlayerInteractEvent event) {
		Block clickedBlock = event.getClickedBlock();
		BlockFace clickedFace = event.getBlockFace();
		Player player = event.getPlayer();

		if (((event.getAction() == Action.RIGHT_CLICK_BLOCK) || (event.getAction() == Action.LEFT_CLICK_BLOCK)) && player.hasPermission("lorecraft.wandofreplacement")) {
			PlayerInventory inventory = player.getInventory();
			ItemStack heldItem = inventory.getItem(inventory.getHeldItemSlot());

			/* Check the item and the lore title are the same */
			if (item.isItem(heldItem)) {
				ReplacementPlayerData playerData = replaceData.get(player);

				if (playerData.isActive()) {
					event.setCancelled(true);
					return;
				}

				if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
					/* Right click selects the block to replace with */
					playerData.setReplaceItem(clickedBlock);
				} else {
					playerData.replace(clickedBlock, clickedFace, player.isSneaking());
				}
				event.setCancelled(true);
			}
		}
	}
	
	private class ReplacementPlayerData implements Runnable {
		JavaPlugin plugin;
		Player player;
		Block blockArray[];
		int radius;
		ItemStack replacementItem;
		ItemStack original;
		BlockFace clickedFace;
		BlockFace directions[];
		boolean active;
		int replaceAmount;
		int index;
		int xpCost;

		ReplacementPlayerData(JavaPlugin plugin, Player player, int radius) {
			this.plugin = plugin;
			this.player = player;
			//this.radius = radius;
			blockArray = new Block[15*15];
			directions = new BlockFace[4];
			active = false;
			index = 0;
			replacementItem = null;
		}
		
		void setReplaceItem(Block block) {
			BlockState state = block.getState();

			if (whiteList.contains(state.getType())) {
				replacementItem = state.getData().toItemStack(1);
			} else {
				/* An bad block was selected so clear the selection */
				player.getWorld().playEffect(player.getLocation(), Effect.CLICK1, 0);
				replacementItem = null;
			}
		}
		
		boolean replace(Block start, BlockFace face, boolean single) {
			BlockState state = start.getState();

			if (whiteList.contains(state.getType()) && (replacementItem != null)) {
				original = state.getData().toItemStack(1);
			} else {
				player.getWorld().playEffect(player.getLocation(), Effect.CLICK1, 0);
				return false;
			}
			this.clickedFace = face;

			PlayerInventory inv = player.getInventory();
			xpCost = 6 - inv.getItem(inv.getHeldItemSlot()).getEnchantmentLevel(Enchantment.DIG_SPEED);
			if (xpCost <= 0) {
				xpCost = 1;
			}
			
			radius = 2 + inv.getItem(inv.getHeldItemSlot()).getEnchantmentLevel(Enchantment.ARROW_DAMAGE);
			
			switch (clickedFace) {
			case UP:
				directions[0] = BlockFace.EAST;
				directions[1] = BlockFace.SOUTH;
				directions[2] = BlockFace.WEST;
				directions[3] = BlockFace.NORTH;
				break;
			case DOWN:
				directions[0] = BlockFace.NORTH;
				directions[1] = BlockFace.WEST;
				directions[2] = BlockFace.SOUTH;
				directions[3] = BlockFace.EAST;
				break;
			case EAST:
				directions[0] = BlockFace.NORTH;
				directions[1] = BlockFace.DOWN;
				directions[2] = BlockFace.SOUTH;
				directions[3] = BlockFace.UP;
				break;
			case WEST:
				directions[0] = BlockFace.SOUTH;
				directions[1] = BlockFace.DOWN;
				directions[2] = BlockFace.NORTH;
				directions[3] = BlockFace.UP;
				break;
			case NORTH:
				directions[0] = BlockFace.WEST;
				directions[1] = BlockFace.DOWN;
				directions[2] = BlockFace.EAST;
				directions[3] = BlockFace.UP;
				break;
			case SOUTH:
				directions[0] = BlockFace.EAST;
				directions[1] = BlockFace.DOWN;
				directions[2] = BlockFace.WEST;
				directions[3] = BlockFace.UP;
				break;
			default:
				player.sendMessage("I don't know how to do that :(");
				return true;
			}
			/* Walk the blocks in the pattern in which we want to replace them */
			int i=0;
			blockArray[i++] = start;
			if (!single) {
				Block radiusStart = start.getRelative(directions[3]);
				for (int r=1;r<radius+1;r++) {
					BlockFace direction = directions[0];
					Block blockWalker = radiusStart;
					for (int c=0;c<(((r * 2) + 1) * 4)-4;c++) {
						blockArray[i++] = blockWalker;
						if (c == (r)) {
							direction = directions[1];
						} else if (c == (r*3)) {
							direction = directions[2];
						} else if (c == (r*5)) {
							direction = directions[3];
						} else if (c == (r*7)) {
							direction = directions[0];
						}
						blockWalker = blockWalker.getRelative(direction);
					}
					radiusStart = radiusStart.getRelative(directions[3]);
				}
			}
			
			/* Start the replacement */
			replaceAmount = i;
			active = true;
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, this, 1);
			
			return true;
		}
		
		boolean isActive() {
			return active;
		}

		@SuppressWarnings("deprecation")
		@Override
		public void run() {
			boolean cont = true;
			int totalXp = loreCraft.getTotalExp(player, true);

			/* Check the player have enough items and XP before doing the swap */
			if (player.getInventory().containsAtLeast(replacementItem, 1) && (totalXp >= xpCost)) {
				Block block = blockArray[index];
				BlockState state = block.getState();

				if (state.getType().equals(original.getType()) && state.getData().equals(original.getData())) {
					LoreCraftBlockBreakEvent event = new LoreCraftBlockBreakEvent(block, player);
					plugin.getServer().getPluginManager().callEvent(event);

					if (!event.isCancelled()) {
						/* The removal went ok so we can replace the block */
						LoreCraftBlockPlaceEvent placeEvent;
						Block placedAgainst;
						if (index == 0) {
							placedAgainst = block.getRelative(BlockFace.DOWN);
						} else {
							placedAgainst = blockArray[index - 1];
						}

						BlockState newState = block.getState();
						newState.setType(replacementItem.getType());
						newState.setData(replacementItem.getData());
						newState.update(true);

						placeEvent = new LoreCraftBlockPlaceEvent(block, state, placedAgainst, replacementItem, player, true);
						plugin.getServer().getPluginManager().callEvent(placeEvent);
						if (!event.isCancelled()) {
							block.getWorld().playEffect(block.getRelative(clickedFace).getLocation(),Effect.ENDER_SIGNAL, 0);
							block.getWorld().playEffect(block.getRelative(clickedFace).getLocation(),Effect.STEP_SOUND, original.getTypeId()); 

							/* Take away one item and give them back the other */
							player.getInventory().removeItem(replacementItem);
							HashMap<Integer,ItemStack> remainingItems = player.getInventory().addItem(original);
							if (!remainingItems.isEmpty()) {
								/* There should only ever be one item but better safe then sorry! */
								Iterator<ItemStack> isi = remainingItems.values().iterator();
								while (isi.hasNext()) {
									ItemStack dropItem = isi.next();
									player.getWorld().dropItemNaturally(player.getLocation(), dropItem);
								}
							}
							player.updateInventory();

							/* The more blocks that get replaced in one go the cheaper each replacement becomes */
							int modifier = (index/25)+1;
							if ((index % modifier) == 0) {
								player.setExp(0);
								player.setLevel(0);
								player.setTotalExperience(0);
								player.giveExp(totalXp - xpCost);
							}
						} else {
							/* Restore the previous state of the block */
							state.update(true);
						}
					}
				}
			} else {
				player.getWorld().playEffect(player.getLocation(), Effect.EXTINGUISH, 0);
				cont = false;
			}

			index++;
			if ((index < replaceAmount) && cont) {
				/* More work to do */
				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, this, 1);
			} else {
				/* Finished the replacement so reset our state */
				active = false;
				index = 0;
			}
		}
	}
}
