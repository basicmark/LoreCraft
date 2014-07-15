package io.github.basicmark.lorecraft;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class LoreCraftItem extends LoreCraftObject{
	LoreCraft loreCraft;
	Material itemType;
	String itemName;
	String loreName;
	List<String> loreInfo;
	List<String> recipeDesign;
	List<Map<?, ?>> ingredientList;
	HashSet<Enchantment> enchantments;
	LoreCraftAction action;
	ItemStack loreCraftItem;

	public LoreCraftItem(LoreCraft loreCraft, String name) {
		super(loreCraft, name);

		JavaPlugin plugin = loreCraft.getPlugin();
		String pluginDir = plugin.getDataFolder() + "/" + name + "/";
		File data = new File(pluginDir + "config.yml");
		FileConfiguration config = YamlConfiguration.loadConfiguration(data);

		this.loreCraft = loreCraft;
		
		/* Get the basic item information */
		itemType = Material.getMaterial(config.getString("itemtype"));
		itemName = config.getString("name");
		loreName = config.getString("lorename");
		loreInfo = config.getStringList("loreinfo");
		recipeDesign = config.getStringList("recipe");
		ingredientList = config.getMapList("ingredients");

		/* What enchantments does this item support? */
		List<String> enchantmentNames = config.getStringList("enchantments");
		if (enchantmentNames != null) {
			enchantments = new HashSet<Enchantment>();
			for (String enchantmentName: enchantmentNames) {
				enchantments.add(Enchantment.getByName(enchantmentName));
			}
		}	

		/* Create the item based on the data we've just loaded */
		loreCraftItem = new ItemStack(itemType);
		loreCraftItem.setAmount(1);
        ItemMeta meta = loreCraftItem.getItemMeta();
        meta.setDisplayName(itemName);
        List<String> loreText = new ArrayList<String>();
        loreText.add("" + ChatColor.DARK_PURPLE + ChatColor.BOLD + loreName);
        for (String text : loreInfo) {
        	loreText.add("" + ChatColor.LIGHT_PURPLE + ChatColor.ITALIC + text);
        }
        meta.setLore(loreText);
        loreCraftItem.setItemMeta(meta);

		/* Load the class that implements the actions if required and register them*/
		String actionClass = null;
		action = null;
		actionClass = config.getString("actionclass");
		if (actionClass != null) {
			/* Check if this class is built in or not */
			String classLoad = config.getString("classload"); 
			File classFile = null;
			if (classLoad != null) {
				classFile = new File(pluginDir + "/" + classLoad + ".jar");
			}
			action = LoreCraftActionLoader.loadLoreCraftActionClass(classFile, actionClass);
			action.onEnable(loreCraft, this);
		}

		/* Lastly register the crafting recipe */
        ShapedRecipe recipe = new ShapedRecipe(new ItemStack(loreCraftItem));
        recipe.shape(recipeDesign.toArray(new String[0]));
       
		for (Map<?, ?> ingredient : ingredientList) {
			try {
				
				String materialKey = (String) ingredient.keySet().toArray()[0];
				String materialName = (String) ingredient.values().toArray()[0];
				recipe.setIngredient(materialKey.charAt(0), Material.getMaterial(materialName));
			} catch (Exception e) {
				plugin.getServer().getLogger().info("Failed to load recipe for item " + name);
				return;
			}
		}

        plugin.getServer().addRecipe(recipe);
	}
	
	public boolean isItem(ItemStack item) {
		if (item != null) {
			if (item.getType().equals(itemType)) {
				if (item.getItemMeta().getLore() != null) {
					if (item.getItemMeta().getLore().get(0) != null) {
						if (item.getItemMeta().getLore().get(0).equals(loreCraftItem.getItemMeta().getLore().get(0))) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	List<String> getLoreBookPages() {
		List<String> itemPages = new ArrayList<String>();
		String recipe = new String();
		recipe = "" + ChatColor.ITALIC + ChatColor.DARK_PURPLE + itemName + "\n";
		recipe += "" + ChatColor.RESET + ChatColor.BLACK + "+--+--+--+\n";
		for (String line : recipeDesign) {
			/*
			 * Spaces are smaller then other characters so to keep the table aligned replace
			 * them with a white X which doesn't get displayed.
			 */
			line = line .replace(" ", "#");
			String tmp = "|  " + line.charAt(0) + " | " + line.charAt(1) + "  | " + line.charAt(2) + " |\n";
			recipe += tmp.replace("#", "" + ChatColor.WHITE + "X" + ChatColor.BLACK);
			recipe += "" + ChatColor.RESET + ChatColor.BLACK + "+--+--+--+\n";
		}

		for (Map<?, ?> ingredient : ingredientList) {
			String materialKey = (String) ingredient.keySet().toArray()[0];
			String materialName = (String) ingredient.values().toArray()[0];
			recipe += " " + materialKey.charAt(0) + ": " + materialName.toLowerCase().replace("_", " ") + "\n";
		}
				
		itemPages.add(recipe);
		itemPages.addAll(super.getLoreBookPages());
		return itemPages;
	}

	HashSet<Enchantment> compatibleEnchanetments() {
		return enchantments;
	}

	void onDisable() {
		if (action != null) {
			action.onDisable();
		}
	}
}
