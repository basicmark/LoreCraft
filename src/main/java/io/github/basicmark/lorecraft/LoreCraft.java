package io.github.basicmark.lorecraft;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.server.v1_7_R3.ContainerAnvil;
import net.minecraft.server.v1_7_R3.ContainerAnvilInventory;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_7_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R3.inventory.CraftInventoryAnvil;
import org.bukkit.craftbukkit.v1_7_R3.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class LoreCraft extends JavaPlugin implements Listener {
	static {
		LoreCraftObjectType.register(LoreCraftItem.class);
		LoreCraftObjectType.register(LoreCraftInformation.class);
	}

	private ItemStack loreCraftersGuide;
	private List<LoreCraftObject> loreCraftObjects;
	private List<String> introduction;
	private Random random;
	
	public void loadConfig() {
		List<String> objectNames;
		FileConfiguration config = getConfig();
		loreCraftObjects = new ArrayList<LoreCraftObject>();
		
		List<String> raw_introduction = config.getStringList("introduction");
		introduction = new ArrayList<String>();
		for (String raw_page : raw_introduction) {
			introduction.add(raw_page.replace('&', ChatColor.COLOR_CHAR));
		}
		
		objectNames = config.getStringList("objects");
		for (String name : objectNames) {
			LoreCraftObject ob = LoreCraftObjectType.load(this, name);
			loreCraftObjects.add(ob);
			this.getServer().getLogger().info("Loaded LoreCraft object: " + name);
		}
	}
	
	public void onEnable(){
		getLogger().info("Enabling lorecraft");

		random = new Random();
		
		// Create/load the config file
		saveDefaultConfig();
		loadConfig();
		getServer().getPluginManager().registerEvents(this, this);
		
        /* Create the LoreCrafters guide */
        loreCraftersGuide = new ItemStack(Material.WRITTEN_BOOK);
        loreCraftersGuide.setAmount(1);
        BookMeta meta = (BookMeta) loreCraftersGuide.getItemMeta();
        meta.setDisplayName("LoreCrafters guide");
        List<String> loreText = new ArrayList<String>();
        loreText.add("" + ChatColor.DARK_PURPLE + ChatColor.BOLD + "The LoreCrafters guide");
        loreText.add("" + ChatColor.LIGHT_PURPLE + ChatColor.ITALIC + "This book is your guide into the");
        loreText.add("" + ChatColor.LIGHT_PURPLE + ChatColor.ITALIC + "mystical world of Lore and magic.");
        loreText.add("" + ChatColor.LIGHT_PURPLE + ChatColor.ITALIC + "Be sure to always have it to hand");
        loreText.add("" + ChatColor.LIGHT_PURPLE + ChatColor.ITALIC + "as you never know when a new");
        loreText.add("" + ChatColor.LIGHT_PURPLE + ChatColor.ITALIC + "discorvery may be made!");
        meta.setLore(loreText);
        meta.setAuthor(ChatColor.MAGIC + "Arch-Mage Markus");
        loreCraftersGuide.setItemMeta(meta);

        ShapedRecipe recipe = new ShapedRecipe(new ItemStack(loreCraftersGuide));
        recipe.shape(new String[] {" E ", "IBF", " S "});
        recipe.setIngredient('E', Material.EYE_OF_ENDER);
        recipe.setIngredient('I', Material.INK_SACK);
        recipe.setIngredient('B', Material.BOOK);
        recipe.setIngredient('F', Material.FEATHER);
        recipe.setIngredient('S', Material.SAPLING);
        getServer().addRecipe(recipe);
	}

	public void onDisable(){
		getLogger().info("Disabling lorecraft");
		for (LoreCraftObject obj : loreCraftObjects) {
			obj.onDisable();
		}
	}
	
	public boolean isLoreCraftersGuide(ItemStack item) {
		if (item.getType().equals((Material.WRITTEN_BOOK))) {
			BookMeta meta = (BookMeta) item.getItemMeta();
			if (meta != null) {
				List<String> loreText = item.getItemMeta().getLore();
				if ((loreText != null) && (!loreText.isEmpty())) {
					String loreName = loreText.get(0);
					if ((loreName != null) && loreName.equals(loreCraftersGuide.getItemMeta().getLore().get(0))) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerInteract(PlayerInteractEvent event) {
		ItemStack clickedItem = event.getItem();
		if ((clickedItem != null) && isLoreCraftersGuide(clickedItem)) {
			Player player = event.getPlayer();
			BookMeta meta = (BookMeta) clickedItem.getItemMeta();
			List<String> customPages = new ArrayList<String>();

			/*
			 * Different players may have different permissions, create the book
			 * according to the items/research they have access to
			 */

			/* First the introduction */
			customPages.addAll(introduction);

			/* Next the table of contents */
			String contents = new String();
			for (LoreCraftObject object : loreCraftObjects) {
				if (player.hasPermission(object.getPermissionNode())) {
					contents = contents + "§0- " + object.getChapterName() + "\n";
				}
			}
			customPages.add(contents);
			
			/* And finally the contents */
			for (LoreCraftObject object : loreCraftObjects) {
				if (player.hasPermission(object.getPermissionNode())) {
					customPages.addAll(object.getLoreBookPages());
				}
			}

			meta.setPages(customPages);
			if (!meta.equals(clickedItem.getItemMeta())) {
				player.getWorld().strikeLightningEffect(player.getLocation());
				player.sendMessage("" + ChatColor.GOLD + ChatColor.ITALIC + "Your LoreCraft guide tremors with power, new discoveries have been made.");
				player.sendMessage("The pages can not settle until you close the book, you must reopen it to see the new findings.");
			}
			clickedItem.setItemMeta(meta);
		}
	}

	public JavaPlugin getPlugin() {
		return this;
	}

	private int increaseLevel(Enchantment enchantment, int level) {
		if (enchantment.equals(Enchantment.DIG_SPEED)) {
			if (level >= 5)
				return 5;
			return level + 1;
		}
		if (enchantment.equals(Enchantment.ARROW_DAMAGE)) {
			if (level >= 5) {
				return 5;
			}

			return level + 1;
		}
		return level;
	}
	
	private int getRepairCostAndEnchant(ItemStack item, Map<Enchantment, Integer> enchantments) {
		int cost = 0;

		/* This isn't following the same method minecraft itself uses but keeps things simple */
		for (Enchantment enchant : enchantments.keySet()) {
			if (item.containsEnchantment(enchant)) {
				Bukkit.getLogger().info("Checking existing enchant " + enchant);
				/* We already have this enchantment, can we use the new enchantment */
				int ench_level = enchantments.get(enchant);
				int item_level = item.getEnchantmentLevel(enchant);

				if (ench_level >= item_level) {
					int change_level = ench_level - item_level;

					if (change_level != 0) {
						/* The new enchantment has a higher level so just replace the old one */
						item.addUnsafeEnchantment(enchant, ench_level);
						cost += ench_level * 2;
					} else {
						/* The new enchantment is the same level as the old one, can we combine and level up? */
						int new_level = increaseLevel(enchant, ench_level);

						if (new_level != ench_level) {
							item.addUnsafeEnchantment(enchant, new_level);
							cost += new_level * 2;
						}
					}
				}
			} else {
				/* This is a new enchantment so just add it */
				int level = enchantments.get(enchant);
				item.addUnsafeEnchantment(enchant, level);
				cost += level;
			}
		}
		return cost;
	}
	
	/*
	 * Handle enchanting items and give the player a fright if they try to enchant their lorecraft item via the
	 * enchanting table ;)
	 */
	@EventHandler(ignoreCancelled=true, priority=EventPriority.HIGHEST)
	public void onInventoryClick(final InventoryClickEvent event) {
		this.getServer().getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                if (event.getInventory() instanceof AnvilInventory) {
                    Player player = (Player) event.getWhoClicked();
                    AnvilInventory ai = (AnvilInventory) event.getInventory();
                    ItemStack first = ai.getItem(0);
                    ItemStack second = ai.getItem(1);
                    net.minecraft.server.v1_7_R3.ItemStack nmsResult = ((CraftInventoryAnvil)ai).getResultInventory().getItem(0);
                    ItemStack result = nmsResult == null ? null : CraftItemStack.asCraftMirror(nmsResult);
                    Map<Enchantment, Integer> resultEnchantments = new HashMap<Enchantment, Integer>();
                    LoreCraftItem loreItem = null;

                    if (first != null && second != null && result == null) {
            			for (LoreCraftObject loreObject: loreCraftObjects) {
            				if (loreObject instanceof LoreCraftItem) {
            					loreItem = (LoreCraftItem) loreObject;
            					if (loreItem.isItem(first)) {
            						break;
            					}
            				}
            			}
			
                    	if ((loreItem !=null) && player.hasPermission(loreItem.getPermissionNode()) && second.getType().equals(Material.ENCHANTED_BOOK)) {
                    		EnchantmentStorageMeta meta = (EnchantmentStorageMeta) second.getItemMeta();

                    		if (meta != null && meta.hasStoredEnchants()) {
                    			Set<Enchantment> compatible = new HashSet<Enchantment>(meta.getStoredEnchants().keySet());
                    			compatible.retainAll(loreItem.compatibleEnchanetments());

                    			if (!compatible.isEmpty()) {
                    				result = first.clone();
                    				for (Enchantment enchant : compatible) {
                    					Integer level = meta.getStoredEnchantLevel(enchant);
                    					resultEnchantments.put(enchant, level);
                    				}
                				}
                    		}
                    	}

                    	if (!resultEnchantments.isEmpty()) {
                    		ContainerAnvilInventory nmsInv = (ContainerAnvilInventory) ((CraftInventoryAnvil) ai).getInventory();
                    		try {
                    			Field containerField = ContainerAnvilInventory.class.getDeclaredField("a");
                    			containerField.setAccessible(true);
                    			ContainerAnvil anvil = (ContainerAnvil) containerField.get(nmsInv);
                    			int cost = getRepairCostAndEnchant(result, resultEnchantments);
                    			if (cost != 0)
                    			{
                    				anvil.a = cost;	
                    				((CraftInventoryAnvil)ai).getResultInventory().setItem(0, CraftItemStack.asNMSCopy(result));
                    				((CraftPlayer) event.getWhoClicked()).getHandle().setContainerData(anvil, 0, anvil.a);
                    			}
                    		} catch (Exception e) {
                    			e.printStackTrace();
                    		}
                    	}
                    }
                }

                if (event.getInventory() instanceof EnchantingInventory) {
                	Player player = (Player) event.getWhoClicked();
                	EnchantingInventory inv = (EnchantingInventory) event.getInventory();
                	ItemStack item = inv.getItem();
                	LoreCraftItem loreItem = null;

                	for (LoreCraftObject loreObject: loreCraftObjects) {
        				if (loreObject instanceof LoreCraftItem) {
        					loreItem = (LoreCraftItem) loreObject;
        					if (loreItem.isItem(item)) {
        						break;
        					}
        					loreItem = null;
        				}
        			}
                	
                	if ((item != null) && (loreItem != null)) {
                		if (random.nextInt(20) == 0) {
                			player.getWorld().strikeLightningEffect(player.getLocation());
                		} else {
                			player.getWorld().playEffect(player.getLocation(), Effect.GHAST_SHRIEK, 10);
                		}
                		event.getView().close();
                	}
                }
            }
        }, 0);
	}
	
	/*
	 * The following functions are taken from:
	 * 
	 * https://github.com/feildmaster/ControlORBle/blob/master/src/main/java/com/feildmaster/lib/expeditor/Editor.java
	 * 
	 * This is because the Minecraft XP doesn't seem to work in a sane way. If recalcTotalExp isn't called
	 * then after enchanting the players total XP would be that before the enchantment as it seems to only
	 * update the total XP on some events and enchanting is not one of them :( 
	 */
	public int getExp(Player player) {
		return (int) (getExpToLevel(player) * player.getExp());
	}

	public int getTotalExp(Player player, boolean recalc) {
		if (recalc) {
			recalcTotalExp(player);
		}
		return player.getTotalExperience();
	}

	public int getExpToLevel(Player player) {
		return player.getExpToLevel();
	}

	public int getExpToLevel(int level) {
		return level >= 30 ? 62 + (level - 30) * 7 : (level >= 15 ? 17 + (level - 15) * 3 : 17);
	}
	
	private void recalcTotalExp(Player player) {
		int total = getExp(player);
		for (int i = 0; i < player.getLevel(); i++) {
			total += getExpToLevel(i);
		}
		player.setTotalExperience(total);
	}
}

