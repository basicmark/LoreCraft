package io.github.basicmark.lorecraft.core.wands;

import io.github.basicmark.lorecraft.LoreCraft;
import io.github.basicmark.lorecraft.LoreCraftAction;
import io.github.basicmark.lorecraft.LoreCraftBlockPlaceEvent;
import io.github.basicmark.lorecraft.LoreCraftItem;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_8_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftArrow;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;


/*
 * Other wands
 * wand of smelting :- Smelts block in the AoE (Circler AoE)
 * 
 * Alters:
 * An alter allows advance items to be created. Use of entities that
 * can't be picked up. Right clicking on an alter stone will place
 * the item on it, left will remove it. Would need to store the creation
 * of the alter so its state can be written to a config file and its
 * "inventory" can be stored to.
 * 
 * 
 * Mages robes :- Consumes XP before taking damage
 * Creation of potion effects within a specific range (like becons).
 * Could get potion effects from brewed potions?
 * Would be good to have a helper to create a structure. Build it and
 * then select the blocks that make up the alter, these are then checked
 * when a player interacts with the alter to see if its complete.
 */

public class WandOfEnlightenment implements LoreCraftAction, Listener {
	LoreCraft loreCraft;
	JavaPlugin plugin;
	LoreCraftItem item;
	ItemStack torch;
	
	public void onEnable(LoreCraft loreCraft, LoreCraftItem item) {
		this.loreCraft = loreCraft;
		this.plugin = loreCraft.getPlugin();
		this.item = item;

		torch = new ItemStack(Material.TORCH,1);
    	plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	public void onDisable() {
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if ((event.getAction() == Action.LEFT_CLICK_AIR) || (event.getAction() == Action.LEFT_CLICK_BLOCK)) {
			Player player = event.getPlayer();
			ItemStack held = player.getItemInHand();
			int eff = held.getEnchantmentLevel(Enchantment.DIG_SPEED);
        	int totalXp = loreCraft.getTotalExp(player, true);
			int xpCost = (6 - eff) * 3;

			if (item.isItem(held) && player.hasPermission(item.getPermissionNode())) {
				if (player.getInventory().containsAtLeast(torch, 1) && (totalXp > xpCost)) {
					int power = held.getEnchantmentLevel(Enchantment.ARROW_DAMAGE);
					if (power > 0) {
						SmallFireball fireball = player.launchProjectile(SmallFireball.class);
						fireball.setMetadata("WOE.power", new FixedMetadataValue(plugin, power));
						fireball.setMetadata("WOE.eff", new FixedMetadataValue(plugin, eff));
					} else {
						Arrow arrow = player.launchProjectile(Arrow.class);
						arrow.setMetadata("WOE.power", new FixedMetadataValue(plugin, 0));
						arrow.setMetadata("WOE.eff", new FixedMetadataValue(plugin, eff));
						arrow.setFireTicks(200);
					}
				} else {
					player.getWorld().playEffect(player.getLocation(), Effect.CLICK1, 0);
				}
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onProjectileHit(ProjectileHitEvent event) {
		Projectile projectile = event.getEntity();
		if (!projectile.hasMetadata("WOE.power")) {
			return;
		}

		if (projectile.getType() == EntityType.SMALL_FIREBALL) {
			int power = projectile.getMetadata("WOE.power").iterator().next().asInt();
			int eff = projectile.getMetadata("WOE.eff").iterator().next().asInt();
			/*
			 * The initial fireball explodes into an "arrow bomb".
			 * Fire a number of arrows (depending on the enchantment)
			 * in a way that create a nice grid-like placement of torches
			 */
			ArrayList<Vector> vects = new ArrayList<Vector>();
			ArrayList<Float> powers = new ArrayList<Float>();
			int level = (power + 1) / 2;
			int spread = 3;
			int count = 0;
			for (int y = -(spread * level); y <= (spread * level); y+= spread) {
				for (int x = -(spread * level); x <= (spread * level); x+= spread) {
					if (count%2 == 1) {
						vects.add(new Vector(x,4,y));
						powers.add((float) ((Math.abs(Math.sqrt((x*x) + (y*y)))*0.08)) + 0.2f);
					}
					count++;
				}
			}

			Iterator<Float> pow = powers.iterator();
			for (Vector vect : vects) {
				World world = projectile.getWorld();
				Location loc = projectile.getLocation();
				Arrow arrow = world.spawnArrow(loc, vect, pow.next(), 0);
				arrow.setMetadata("WOE.power", new FixedMetadataValue(plugin, 0));
				arrow.setMetadata("WOE.eff", new FixedMetadataValue(plugin, eff));
				arrow.setFireTicks(200);
				arrow.setShooter(projectile.getShooter());
				/* FIXME: How do I make it so the arrow can't be picked up? */
			}
		} else if (projectile.getType() == EntityType.ARROW) {
			int eff = projectile.getMetadata("WOE.eff").iterator().next().asInt();
			placeTorch(projectile, eff);

			/* Despawn the entity as it's served it's purpose */
			projectile.remove();
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityCombustByEntity(EntityCombustByEntityEvent event) {
		if (event.getCombuster() != null) {
			if (event.getCombuster().hasMetadata("WOE.power")) {
				/* Ensure our entities don't do any damage */
				event.setCancelled(true);
			}
		}
	}
	
	/* FIXME: Detect entity damage and cancel as above */
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (event.getDamager() != null) {
			if (event.getDamager().hasMetadata("WOE.power")) {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockIgnite(BlockIgniteEvent event) {
		if (event.getIgnitingEntity() != null) {
			if (event.getIgnitingEntity().hasMetadata("WOE.power")) {
				event.setCancelled(true);
			}
		}
	}
	
	private void placeTorch(final Projectile projectile, final int efficiency) {
		World world = projectile.getLocation().getWorld();
		final Block block = world.getBlockAt(projectile.getLocation());
		final ItemStack torch = new ItemStack(Material.TORCH,1);
		final Player player = (Player) projectile.getShooter();

		/* Only replace air blocks with torches */
		if ((player != null) && player.isOnline() && (player.getInventory().containsAtLeast(torch, 1))) {
			
			/* Work out which face the arrow has embedded into */
			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                @SuppressWarnings("deprecation")
				public void run() {
                    try {

                    	net.minecraft.server.v1_8_R1.EntityArrow entityArrow = ((CraftArrow) projectile).getHandle();

                        Field fieldX = net.minecraft.server.v1_8_R1.EntityArrow.class
                                .getDeclaredField("d");
                        Field fieldY = net.minecraft.server.v1_8_R1.EntityArrow.class
                                .getDeclaredField("e");
                        Field fieldZ = net.minecraft.server.v1_8_R1.EntityArrow.class
                                .getDeclaredField("f");

                        fieldX.setAccessible(true);
                        fieldY.setAccessible(true);
                        fieldZ.setAccessible(true);

                        int x = fieldX.getInt(entityArrow);
                        int y = fieldY.getInt(entityArrow);
                        int z = fieldZ.getInt(entityArrow);

                        if (isValidBlock(y)) {
                        	
                        	Block againstBlock = projectile.getWorld().getBlockAt(x, y, z);
                        	BlockState state = againstBlock.getState();
                        	byte data = 0;
                        	BlockFace face = BlockFace.UP;

                        	if ((block.getZ() == z) && (block.getX()-1 == x) && (block.getY() == y)) {
                        		face = BlockFace.EAST;
                        		data = 0x1;
                        	} else if ((block.getZ() == z) && (block.getX()+1 == x) && (block.getY() == y)) {
                        		face = BlockFace.WEST;
                        		data = 0x2;
                        	} else if  ((block.getZ()-1 == z) && (block.getX() == x) && (block.getY() == y)) {
                        		face = BlockFace.SOUTH;
                        		data = 0x3;
                        	} else if ((block.getZ()+1 == z) && (block.getX() == x) && (block.getY() == y)) {
                        		face = BlockFace.NORTH;
                        		data = 0x4;
                        	} else if ((block.getZ() == z) && (block.getX() == x) && (block.getY()-1 == y)) {
                        		face = BlockFace.UP;
                        		data = 0x5;
                        	}

                        	
                        	int totalXp = loreCraft.getTotalExp(player, true);
                			int xpCost = (6 - efficiency) * 3;

                			Block placedBlock = againstBlock.getRelative(face);
                        	net.minecraft.server.v1_8_R1.World mcWorld = ((CraftWorld)placedBlock.getWorld()).getHandle();
                			net.minecraft.server.v1_8_R1.BlockPosition mcBlockPos = new net.minecraft.server.v1_8_R1.BlockPosition(placedBlock.getX(), placedBlock.getY(), placedBlock.getZ());

                			if (!net.minecraft.server.v1_8_R1.Blocks.TORCH.canPlace(mcWorld, mcBlockPos)) {
                        		return;
                        	}
                			
                			BlockState oldState = placedBlock.getState();
                			BlockState newState = placedBlock.getState();
                        	if ((data != 0) && (newState.getType() == Material.AIR) && (totalXp > xpCost)) {
                        		newState.setType(Material.TORCH);
                        		newState.update(true);
                        		newState = block.getState();
                        		//newState.setRawData(data);
                        		newState.update(true);
                        		net.minecraft.server.v1_8_R1.IBlockData mcBlockData = mcWorld.getType(mcBlockPos);
                        		net.minecraft.server.v1_8_R1.Blocks.TORCH.onPlace(mcWorld, mcBlockPos, mcBlockData);

                        		LoreCraftBlockPlaceEvent placeEvent = new LoreCraftBlockPlaceEvent(placedBlock, oldState, againstBlock, torch, player, true);
                        		plugin.getServer().getPluginManager().callEvent(placeEvent);
                        		if (!placeEvent.isCancelled()) {
                        			player.getInventory().removeItem(torch);
    								player.setExp(0);
    								player.setLevel(0);
    								player.setTotalExperience(0);
    								player.giveExp(totalXp - xpCost);
                        		}  else {
        							/* Restore the previous state of the block */
                        			oldState.update(true);
        						}
                        	}
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
		}
	}
	
    // If the arrow hits a mob or player the y coord will be -1
    private boolean isValidBlock(int y) {
        return y != -1;
    }
}
