package io.github.basicmark.lorecraft;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.bukkit.Bukkit;

public class LoreCraftActionLoader {
	@SuppressWarnings("resource")
	static LoreCraftAction loadLoreCraftActionClass(File classFile, String name) {
		ClassLoader loader = null;
		LoreCraftAction item = null;
		if (classFile != null) {
			try {
				Bukkit.getLogger().info("Loading from " + classFile);
				loader = new URLClassLoader(new URL[] { classFile.toURI().toURL() },
						LoreCraftAction.class.getClassLoader());
			} catch (MalformedURLException ex) {
				Bukkit.getLogger().info("Failed to create class loader for " + name);
				return null;
			}
		}

		try {
			Class<?> clazz;
			if (classFile != null) {
				/* Load the class from an external source */
				if (loader != null) {
					clazz = loader.loadClass(name);
				} else {
					Bukkit.getLogger().info("External class required but no loader present!");
					return null;
				}
			} else {
				/* The class is built-in */
				clazz = Class.forName(name);
			}
			Object object = clazz.newInstance();
			if (!(object instanceof LoreCraftAction)) {
				Bukkit.getLogger().info("Not a LoreCraftItem: " + clazz.getSimpleName());
			} else {
				item = (LoreCraftAction) object;
			}
		} catch (Exception ex) {
			Bukkit.getLogger().info("Failed to load action class " + name);
			ex.printStackTrace();
		}
		return item;
	}
}
