package io.github.basicmark.lorecraft;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class LoreCraftObjectType {
	static final Map<String, Class<? extends LoreCraftObject>> objects = new HashMap<String, Class<? extends LoreCraftObject>>();

	static void register(Class<? extends LoreCraftObject> clazz) {
		objects.put(clazz.getSimpleName().toLowerCase(), clazz);
	}
	
	static LoreCraftObject load(LoreCraft loreCraft, String name) {
		String pluginDir = loreCraft.getDataFolder() + "/" + name + "/";
		File data = new File(pluginDir + "config.yml");

		if (!data.exists() && (loreCraft.getPlugin().getResource("configs/" + name + "/config.yml") != null)) {
			loreCraft.getLogger().info("Failed to load " + name + " as no config information could be found");
		}
		FileConfiguration config = YamlConfiguration.loadConfiguration(data);

	    InputStream defConfigStream = loreCraft.getPlugin().getResource("configs/" + name + "/config.yml");
	    if (defConfigStream != null) {
	        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
	        config.setDefaults(defConfig);
	        config.options().copyDefaults(true);
	        try {
				config.save(data);
			} catch (IOException e) {
				loreCraft.getLogger().info("Failed to save updated config file");
			}
	    }
		
		String type = config.getString("type");
		try {
			if (!objects.containsKey(type)) {
				loreCraft.getLogger().info("Failed to load " + name + " as type '" + type + "' is not known");
				return null;
			}
			return objects.get(type).getConstructor(LoreCraft.class, String.class).newInstance(loreCraft, name);
		} catch (Exception e) {
			loreCraft.getLogger().info("Failed to load " + name + ": " + e.toString());
			
			e.printStackTrace();
		}
		return null;
	}
}
