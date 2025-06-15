package com.dev.mythiccore.utils;

import com.dev.mythiccore.MythicCore;
import com.dev.mythiccore.visuals.ASTDamageIndicators;
import io.lumine.mythic.lib.UtilityMethods;
import io.lumine.mythic.lib.api.event.IndicatorDisplayEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static String colorize(String s) {
        if (s == null || s.equals(""))
            return "";
        Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher match = pattern.matcher(s);
        while (match.find()) {
            String hexColor = s.substring(match.start(), match.end());
            s = s.replace(hexColor, net.md_5.bungee.api.ChatColor.of(hexColor).toString());
            match = pattern.matcher(s);
        }

        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String[] splitTextAndNumber(String input) {
        String regex = "(?<=(\\d([.]\\d+)?))(?=[A-Za-z]+)";
        return input.split(regex);
    }

    public static String Format(Double input, String pattern) {
        DecimalFormat df = new DecimalFormat(pattern);
        return df.format(input);
    }

    public static void displayIndicator(String text, Entity entity) {
        if (!(entity instanceof Player) || !UtilityMethods.isVanished((Player)entity)) {
            ConfigurationSection config = MythicCore.getInstance().getConfig().getConfigurationSection("Indicators");
            ASTDamageIndicators indicators = new ASTDamageIndicators(config);
            assert config != null;
            String format = config.getString("shield-attack-format");
            assert format != null;
            double a = new Random().nextDouble() * Math.PI * 2.0;

            Bukkit.getScheduler().runTask(MythicCore.getInstance(), ()->indicators.displayIndicator(entity, indicators.computeFormat(0, false, text, null), new Vector(Math.cos(a), 0.0, Math.sin(a)), IndicatorDisplayEvent.IndicatorType.DAMAGE));
        }
    }

    public static float getMantissa(float number, int exponent) {
        return number / (float) Math.pow(10, exponent);
    }

    public static int getExponent(double number) {
        return (int) Math.floor(Math.log10(Math.abs(number)));
    }

    public static String progressBar(double percentage, int progressBarLength, char symbol, double split) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Invalid progress values.");
        }

        int completedLength = (int) (progressBarLength * percentage / 100);

        StringBuilder progressBar = new StringBuilder();
        int a = 1;
        for (int i = 0; i < progressBarLength; i++) {
            if (i < completedLength) {
                progressBar.append(symbol);
            } else {
                progressBar.append(ChatColor.WHITE).append(symbol);
            }
            if ((int)Math.floor(progressBarLength / split) * a == i+1) {
                progressBar.append("-");
                a++;
            }
        }

        return String.format("%s", progressBar.substring(0, progressBar.length()-1));
    }

    public static double randomNumber(double min, double max) {
        if (min > max) {
            throw new IllegalArgumentException("Min cannot be greater than max");
        }
        Random random = new Random();
        return min + (max - min) * random.nextDouble();
    }

    public static void generateParticles(Particle particle, double radius, int points, double speed, Location center, double xRotationAngle, double yRotationAngle) {

        Bukkit.getScheduler().runTaskAsynchronously(MythicCore.getInstance(), ()->{
            double xRotationRadians = Math.toRadians(xRotationAngle);
            double yRotationRadians = Math.toRadians(yRotationAngle);

            for (int i = 0; i < points; i++) {
                double x = radius * Math.cos(i);
                double y = radius * Math.sin(i);
                double z = 0;

                // Apply X-axis rotation
                Vector rotatedX = new Vector(x, y, z).rotateAroundX(xRotationRadians);

                // Apply Y-axis rotation
                Vector rotatedXY = rotatedX.clone().rotateAroundY(yRotationRadians);

                Objects.requireNonNull(center.getWorld()).spawnParticle(
                        particle, // particle
                        center, // location
                        0, // count
                        rotatedXY.getX(), rotatedXY.getY(), rotatedXY.getZ(),
                        speed, // speed
                        null, // Object: data
                        true // force
                );
            }
        });
    }

    public static String formatNumber(double number) {
        if (number >= 1000000) {
            double millions = number / 1000000.0;
            return String.format("%.1fM", millions);
        } else if (number >= 1000) {
            double thousands = number / 1000.0;
            return String.format("%.2fk", thousands);
        } else {
            return String.format("%.2f", number);
        }
    }

    public static List<String> tabComplete(String a, List<String> arg) {
        List<String> matches = new ArrayList<>();
        String search = a.toLowerCase(Locale.ROOT);
        for (String s : arg) {
            if (s.toLowerCase(Locale.ROOT).startsWith(search)) {
                matches.add(s);
            }
        }
        return matches;
    }
}
