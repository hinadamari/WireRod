/*
 * @author     ucchy
 * @license    GPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fish;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

/**
 * @author ucchy
 * ワイヤロッドプラグイン
 */
public class WireRod extends JavaPlugin implements Listener {

    private static final String NAME = "フックショット";
    private static final String DISPLAY_NAME =
    		ChatColor.WHITE + NAME;
    private static final int DEFAULT_LEVEL = 2;
    private static final int MAX_LEVEL = 20;
    private static final int HOOK_LAUNCH_SPEED = 2;

    private int level;
    private int speed;
    private ItemStack item;

    /**
     * プラグインが有効になったときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    public void onEnable(){

    	saveDefaultConfig();
        loadConfigDatas();

        getServer().getPluginManager().registerEvents(this, this);

        item = new ItemStack(Material.FISHING_ROD, 1);
        ItemMeta wirerodMeta = item.getItemMeta();
        wirerodMeta.setDisplayName(DISPLAY_NAME);
        item.setItemMeta(wirerodMeta);

        this.addRecipe(); // レシピ追加
    }

    /**
     * 設定情報の読み込み処理
     */
    private void loadConfigDatas() {

        FileConfiguration config = getConfig();
        level = config.getInt("defaultLevel", DEFAULT_LEVEL);
        speed = config.getInt("speed", HOOK_LAUNCH_SPEED);

    }

    /**
     * コマンドが実行されたときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command,
            String label, String[] args) {

        if ( args.length <= 0 ) {
            return false;
        }

        if ( args[0].equalsIgnoreCase("reload") ) {

            if (!sender.hasPermission("wirerod.reload")) {
            	sender.sendMessage(ChatColor.RED + "You don't have permission wirerod.reload.");
            	return true;
            }

            // コンフィグ再読込
			this.reloadConfig();
			this.loadConfigDatas();
			sender.sendMessage(ChatColor.GREEN + "WireRod Reloaded!");

            return true;

        } else if ( args[0].equalsIgnoreCase("get") ) {

            if ( !(sender instanceof Player) ) {
                sender.sendMessage(ChatColor.RED + "This command can only use in game.");
                return true;
            }

            if (!sender.hasPermission("wirerod.get")) {
            	sender.sendMessage(ChatColor.RED + "You don't have permission wirerod.get.");
            	return true;
            }

            Player player = (Player)sender;

            int level = this.level;
            if ( args.length >= 2 && args[1].matches("^[0-9]+$") ) {
                level = Integer.parseInt(args[1]);
            }

            giveWirerod(player, level);

            return true;

        } else if ( args.length >= 2 && args[0].equalsIgnoreCase("give") ) {

        	if (!sender.hasPermission("wirerod.give")) {
            	sender.sendMessage(ChatColor.RED + "You don't have permission wirerod.give.");
            	return true;
            }

            Player player = getServer().getPlayerExact(args[1]);
            if ( player == null ) {
                sender.sendMessage(ChatColor.RED + "Player " + args[1] + " was not found.");
                return true;
            }

            int level = this.level;
            if ( args.length >= 3 && args[2].matches("^[0-9]+$") ) {
                level = Integer.parseInt(args[2]);
            }

            giveWirerod(player, level);

            return true;
        }

        return false;
    }

    /**
     * Shooterのレシピ追加
     */
    private void addRecipe() {

    	ShapedRecipe recipe1 = new ShapedRecipe(getWirerod(1));
        recipe1.shape("  B", " BC", "B C");
        recipe1.setIngredient('B', Material.STICK);
        recipe1.setIngredient('C', Material.LEATHER);
        getServer().addRecipe(recipe1);

        ShapedRecipe recipe = new ShapedRecipe(getWirerod(2));
        recipe.shape("  B", " BC", "B C");
        recipe.setIngredient('B', Material.STICK);
        recipe.setIngredient('C', Material.NETHER_BRICK_ITEM);
        getServer().addRecipe(recipe);

        ShapedRecipe recipe3 = new ShapedRecipe(getWirerod(3));
        recipe3.shape("  B", " BC", "B C");
        recipe3.setIngredient('B', Material.STICK);
        recipe3.setIngredient('C', Material.IRON_INGOT);
        getServer().addRecipe(recipe3);

    }

    /**
     * 指定したプレイヤーに、指定したレベルのWirerodを与える
     * @param player プレイヤー
     * @param level レベル
     */
    private void giveWirerod(Player player, int level) {

        if ( level < 1 ) {
            level = 1;
        } else if ( level > MAX_LEVEL ) {
            level = MAX_LEVEL;
        }

        player.getInventory().addItem(getWirerod(level));
    }

    /**
     * Wirerodの取得
     * @param level
     * @return
     */
    private ItemStack getWirerod(int level) {

    	ItemStack shooter = this.item.clone();

    	ItemMeta wirerodMeta = shooter.getItemMeta();
    	ArrayList<String> lore = new ArrayList<String>();
        lore.add(ChatColor.BLUE + "Level: " + ChatColor.WHITE +  level);
        wirerodMeta.setLore(lore);
        shooter.setItemMeta(wirerodMeta);

        return shooter;

    }

    /**
     * Wirerodの針を投げたり、針がかかったときに呼び出されるメソッド
     * @param event
     */
    @EventHandler
    public void onHook(PlayerFishEvent event) {

        final Player player = event.getPlayer();
        final Fish hook = event.getHook();

        if (!player.hasPermission("wirerod.action")) return;

        if ( player.getItemInHand() == null ||
                player.getItemInHand().getType() == Material.AIR ||
                !player.getItemInHand().getItemMeta().hasDisplayName() ||
                !player.getItemInHand().getItemMeta().getDisplayName().equals(DISPLAY_NAME) ) {
            return;
        }

        if ( event.getState() == State.FISHING ) {
            // 針を投げるときの処理

            // 針の速度を上げる
            hook.setVelocity(hook.getVelocity().multiply(speed));

        } else if ( event.getState() == State.CAUGHT_ENTITY ||
                event.getState() == State.IN_GROUND ) {
            // 針をひっぱるときの処理

        	// ひっかかっているのは自分なら、1ダメージ(0.5ハート)を与える
        	if ( event.getCaught() != null &&
                    event.getCaught().equals(player) ) {
                player.damage(1);
                return;
            }

            // レベルを取得
            ItemStack rod = player.getItemInHand();
            double level = 1;
            for (String lore : rod.getItemMeta().getLore()) {
            	String[] lores = ChatColor.stripColor(lore).split(" ");
            	if (lores[0].equals("Level:")) level = Double.valueOf(lores[1]);
            }

            // 針がかかった場所に向かって飛び出す
            Location loc1 = hook.getLocation();
            Location loc2 = player.getEyeLocation();
            Vector vector = new Vector(
                    loc1.getX()-loc2.getX(),
                    loc1.getY()-loc2.getY(),
                    loc1.getZ()-loc2.getZ());
            Vector vec = vector.normalize().multiply((level + 1) / 2);
            if (((Entity) player).isOnGround()) {
            	// 斜め移動時、地面との摩擦でスピードを落とさないように
            	double x, y, y2, z;
            	x = vec.getX() * 100;
            	y = vec.getY() * 100;
            	z = vec.getZ() * 100;
            	y2 = Math.signum(y) * Math.min(Math.abs(y) * (1.3 - (Math.abs(50 - Math.abs(y)) / 167)), Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2)));
            	double div = Math.sqrt((Math.pow(x, 2) + Math.pow(z, 2)) / (Math.pow(x, 2) + Math.pow(y, 2) - Math.pow(y2, 2) + Math.pow(z, 2))) * 100;
            	vec = new Vector(x / div, y2 / 100, z / div).multiply((level + 1) / 2);
            	player.teleport(player.getLocation().add(0, 0.15, 0));
            }
            player.setVelocity(vec);
            player.setFallDistance(-1000F);
            player.playSound(player.getLocation(), Sound.GHAST_FIREBALL, 1, 1);
        }
    }

    /**
     * Wirerodの針が、地面やブロック、MOBに刺さったときに呼び出されるメソッド
     * @param event
     */
    @EventHandler
    public void onHit(ProjectileHitEvent event) {

        Projectile projectile = event.getEntity();

        if ( projectile.getType() != EntityType.FISHING_HOOK) return;

        LivingEntity shooter = projectile.getShooter();

        if ( shooter == null || !(shooter instanceof Player) ) return;

        Player player = (Player)shooter;

        if (!player.hasPermission("wirerod.action")) return;

        if ( player.getItemInHand() == null ||
                player.getItemInHand().getType() == Material.AIR ||
                !player.getItemInHand().getItemMeta().hasDisplayName() ||
                !player.getItemInHand().getItemMeta().getDisplayName().equals(DISPLAY_NAME) ) {
            return;
        }

        // 音を出す
        player.playSound(player.getEyeLocation(), Sound.ARROW_HIT, 1, (float)0.5);
    }

}
