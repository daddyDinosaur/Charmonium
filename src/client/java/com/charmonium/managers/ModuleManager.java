package com.charmonium.managers;

import com.charmonium.CharmoniumClient;
import com.charmonium.command.commands.CmdChestESP;
import com.charmonium.module.Module;
import com.charmonium.module.modules.misc.Pathfind;
import com.charmonium.module.modules.misc.Walking;
import com.charmonium.module.modules.render.ChestESP;
import com.charmonium.module.modules.render.Fullbright;
import com.charmonium.settings.Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;

public class ModuleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModuleManager.class);
    private final Map<String, Module> modules = new HashMap<>();
    private final List<Module> moduleList = new ArrayList<>();

    public final Fullbright fullbright = new Fullbright();
    public final ChestESP chestesp = new ChestESP();
    public final Pathfind pathfinding = new Pathfind();
    public final Walking walk = new Walking();

    public ModuleManager() {
        registerBuiltinModules();
        registerModuleSettings();
    }

    private void registerBuiltinModules() {
        Arrays.stream(getClass().getDeclaredFields())
                .filter(field -> Module.class.isAssignableFrom(field.getType()))
                .forEach(field -> {
                    try {
                        Module module = (Module) field.get(this);
                        modules.put(module.getName().toLowerCase(), module);
                        moduleList.add(module);
                    } catch (IllegalAccessException e) {
                        LOGGER.error("Module registration failed: {}", e.getMessage());
                    }
                });
    }

    private void registerModuleSettings() {
        moduleList.forEach(module -> {
            for (Setting<?> setting : module.getSettings()) {
                SettingManager.registerSetting(setting);
            }
        });
    }

    public Collection<Module> getModules() {
        return Collections.unmodifiableCollection(moduleList);
    }

    public Module getModule(String name) {
        return modules.get(name.toLowerCase());
    }

    public void toggleModule(String name) {
        Module module = getModule(name);
        if (module != null) {
            module.toggle();
        }
    }
}