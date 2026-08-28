package org.geyser.extension.nightvision;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.bedrock.SessionJoinEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class NightVisionExtension implements Extension {

    private boolean enabled = true;

    @Subscribe
    public void onGeyserPostInitialize(GeyserPostInitializeEvent event) {
        // 加载配置文件
        Path configPath = dataFolder().resolve("config.properties");
        enabled = loadConfig(configPath.toFile());
        this.logger().info("NightVisionExtension 已加载！启用状态: " + enabled);
    }

    @Subscribe
    public void onSessionJoin(SessionJoinEvent event) {
        if (!enabled) return;

        GeyserConnection connection = event.connection();
        String command = "effect give " + connection.bedrockUsername() + " night_vision infinite 0 true";
        connection.sendCommand(command);
        this.logger().info("已为玩家 " + connection.bedrockUsername() + " 应用夜视效果");
    }

    private boolean loadConfig(File configFile) {
        if (!configFile.exists()) {
            // 生成默认配置
            saveDefaultConfig(configFile);
            return true; // 默认启用
        }

        Properties props = new Properties();
        try (FileInputStream input = new FileInputStream(configFile)) {
            props.load(input);
            return Boolean.parseBoolean(props.getProperty("enabled", "true"));
        } catch (IOException e) {
            this.logger().warn("读取配置失败，使用默认启用状态");
            return true;
        }
    }

    private void saveDefaultConfig(File configFile) {
        try {
            Files.createDirectories(configFile.getParentFile().toPath());
            Properties props = new Properties();
            props.setProperty("enabled", "true");
            try (FileOutputStream output = new FileOutputStream(configFile)) {
                props.store(output, "NightVision Extension - 设置为 false 以禁用夜视效果");
            }
        } catch (IOException e) {
            this.logger().warn("无法保存默认配置文件: " + e.getMessage());
        }
    }
}