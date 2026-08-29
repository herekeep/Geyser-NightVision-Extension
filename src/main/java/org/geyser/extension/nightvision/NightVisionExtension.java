package org.geyser.extension.nightvision;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.bedrock.SessionJoinEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent;
import org.geysermc.geyser.api.extension.Extension;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NightVisionExtension implements Extension {

    private boolean enabled = true;
    private int delaySeconds = 120;
    private ScheduledExecutorService scheduler;

    @Subscribe
    public void onGeyserPostInitialize(GeyserPostInitializeEvent event) {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        Path configPath = dataFolder().resolve("config.properties");
        loadConfig(configPath.toFile());
        this.logger().info("NightVisionExtension 已加载！启用状态: " + enabled + "，延迟时间: " + delaySeconds + " 秒");
    }

    @Subscribe
    public void onSessionJoin(SessionJoinEvent event) {
        if (!enabled) return;

        GeyserConnection connection = event.connection();

        // 使用独立的调度器延迟执行命令
        scheduler.schedule(() -> {
            String command = "effect give " + connection.bedrockUsername() + " night_vision infinite 0 true";
            connection.sendCommand(command);
            this.logger().info("已延迟 " + delaySeconds + " 秒为玩家 " + connection.bedrockUsername() + " 应用夜视效果");
        }, delaySeconds, TimeUnit.SECONDS);
    }

    @Subscribe
    public void onGeyserShutdown(GeyserShutdownEvent event) {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void loadConfig(File configFile) {
        if (!configFile.exists()) {
            saveDefaultConfig(configFile);
            return;
        }

        Properties props = new Properties();
        try (FileInputStream input = new FileInputStream(configFile)) {
            props.load(input);
            enabled = Boolean.parseBoolean(props.getProperty("enabled", "true"));
            try {
                delaySeconds = Integer.parseInt(props.getProperty("delay", "120"));
                if (delaySeconds < 0) delaySeconds = 0;
            } catch (NumberFormatException e) {
                delaySeconds = 120;
            }
        } catch (IOException e) {
            this.logger().error("读取配置失败，使用默认值");
        }
    }

    private void saveDefaultConfig(File configFile) {
        try {
            Files.createDirectories(configFile.getParentFile().toPath());
            Properties props = new Properties();
            props.setProperty("enabled", "true");
            props.setProperty("delay", "120");
            try (FileOutputStream output = new FileOutputStream(configFile)) {
                props.store(output, "NightVision Extension Configuration\nenabled: true/false 是否启用\ndelay: 延迟秒数（例如 120）");
            }
        } catch (IOException e) {
            this.logger().error("无法保存默认配置文件: " + e.getMessage());
        }
    }
}
