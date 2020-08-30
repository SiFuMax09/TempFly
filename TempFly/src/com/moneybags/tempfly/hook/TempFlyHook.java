package com.moneybags.tempfly.hook;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import com.moneybags.tempfly.TempFly;
import com.moneybags.tempfly.fly.RequirementProvider;
import com.moneybags.tempfly.hook.HookManager.Genre;
import com.moneybags.tempfly.util.Console;
import com.moneybags.tempfly.util.data.Files;
import com.moneybags.tempfly.util.data.Reloadable;
import com.moneybags.tempfly.util.data.DataFileHolder;

public abstract class TempFlyHook implements RequirementProvider, Reloadable, DataFileHolder {
	
	protected final TempFly tempfly;
	private String target;
	private boolean enabled;
	
	private File dataf;
	private FileConfiguration data;
	
	private FileConfiguration hookConfig;
	
	public TempFlyHook(TempFly tempfly) {
		this.target = getPluginName();
		this.tempfly = tempfly;
		if (Bukkit.getPluginManager().getPlugin(target) == null) {
			return;
		}
		
		Console.info("Attempting to initialize (" + target + ") hook...");
		try { initializeFiles(); } catch (Exception e) {
			Console.severe("An error occured while trying to initilize the (" + target + ") hook.");
			e.printStackTrace();
			return;
		}
		initializeHook();
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		if (enabled) {
			try {tempfly.getHookManager().registerHook(this);} catch (IllegalArgumentException e) {
				e.printStackTrace();
				setEnabled(false);
			}
			return;
		}
		tempfly.getHookManager().unregisterHook(this);
	}

	
	protected boolean initializeFiles() throws Exception {
		Console.debug("--<[ Initializing hook files...");
		File hookConfigf = new File(tempfly.getDataFolder() + File.separator + getGenre().getDirectory(), getConfigName() + ".yml");
	    if (!hookConfigf.exists()) {
	    	Console.info("Config for ( "+ target + ") hook does not exist, creating...");
	    	hookConfigf.getParentFile().mkdirs();
	    	Files.createConfig(tempfly.getResource(getEmbeddedConfigName() + ".yml"), hookConfigf);
	    }
	    
	    hookConfig = new YamlConfiguration();
    	hookConfig.load(hookConfigf);
		if (!hookConfig.getBoolean("enable_hook")) {
			Console.info("(" + target + ") hook is disabled, skipping...");
			return false;
		}
		initializeData();
		return true;
	}
	
	public void initializeData() throws IOException, InvalidConfigurationException, SQLException {
		Console.debug("--<[ Initializing hook data...");
		Connection connection;
		if ((connection = tempfly.getDataBridge().getConnection()) == null) {
			File hookDataf = new File(tempfly.getDataFolder() + File.separator + getGenre().getDirectory() + File.separator + getHookedPlugin() + "_data.yml");
			Console.debug("--<[ Sql connection null, using YAML: " + hookDataf.getName());
			Console.debug("--<[ Path: " + hookDataf.getAbsolutePath());
			Console.debug("--<[ Exists?: " + hookDataf.exists());
		    if (!hookDataf.exists()) {
		    	Console.debug("--<[ Creating new file: " + hookDataf.getName());
		    	hookDataf.getParentFile().mkdirs();
		    	hookDataf.createNewFile();
		    }
		    FileConfiguration hookData = new YamlConfiguration();
		    hookData.load(hookDataf);
		    
		    setDataFile(hookDataf);
		    setDataConfiguration(hookData);
		} else {
			DatabaseMetaData meta = connection.getMetaData();
			/**
			ResultSet results = meta.getTables(null, null, table.getSqlTable(), null);
			if (!results.next()) {
				PreparedStatement statement = connection.prepareStatement("create table if not exists " + table.getSqlTable());
				statement.executeQuery();
			}
			*/
		}
	}
	
	public TempFly getTempFly() {
		return tempfly;
	}
	
	public String getHookedPlugin() {
		return target;
	}
	
	public FileConfiguration getConfig() {
		return hookConfig;
	}
	
	@Override
	public void onTempflyReload() {
		enabled = false;
		try { initializeFiles(); } catch (Exception e) {
			Console.severe("An error occured while trying to initilize the (" + target + ") hook.");
			e.printStackTrace();
			return;
		}
	}
	
	public abstract Genre getGenre();
	
	public abstract String getPluginName();
	
	public abstract String getConfigName();
	
	public abstract String getEmbeddedConfigName();
	
	/**
	 * Called from within the TempFlyHook constructor as a means of constructing the class structure in
	 * a reversed mannor allowing the child classes to finish their initialization first. 
	 * This will not be called if there is an error while setting up the tempfly hook.
	 * 
	 * @return false if there is a problem while initializing the hook.
	 */
	public abstract boolean initializeHook();

	@Override
	public File getDataFile() {
		return dataf;
	}

	@Override
	public FileConfiguration getDataConfiguration() {
		return data;
	}

	@Override
	public void setDataFile(File file) {
		this.dataf = file;
	}

	@Override
	public void setDataConfiguration(FileConfiguration data) {
		this.data = data;
	}
	
	@Override
	public void saveData() {
		try { data.save(dataf); } catch (Exception e) {e.printStackTrace();}
	}
}
