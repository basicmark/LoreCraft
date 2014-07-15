package io.github.basicmark.lorecraft;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public abstract class LoreCraftObject {
	private String permissionNode;
	private String chapterName;
	private List<String> pages;

	public LoreCraftObject(LoreCraft loreCraft, String name) {
		String pluginDir = loreCraft.getDataFolder() + "/" + name + "/";
		File data = new File(pluginDir + "config.yml");
		FileConfiguration config = YamlConfiguration.loadConfiguration(data);

		/* Get the name of the permission node for this object */
		permissionNode = config.getString("permissionnode");
		
		/* Get the items information for the LoreCrafters guide */
		chapterName = config.getString("chaptername");
		List<String> raw_pages = config.getStringList("pages");
		pages = new ArrayList<String>();
		for (String raw_page : raw_pages) {
			pages.add(raw_page.replace('&', ChatColor.COLOR_CHAR));
		}
	}

	String getChapterName() {
		return chapterName;
	}

	List<String> getLoreBookPages() {
		return pages;
	}

	public String getPermissionNode() {
		return "lorecraft." + permissionNode;
	}
	
	void onDisable() {
	}
}
