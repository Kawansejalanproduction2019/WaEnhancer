package com.wmods.wppenhacer.xposed.features.others;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import com.wmods.wppenhacer.BuildConfig;
import com.wmods.wppenhacer.xposed.core.Feature;
import com.wmods.wppenhacer.xposed.core.WppCore;
import com.wmods.wppenhacer.xposed.utils.DesignUtils;
import com.wmods.wppenhacer.xposed.utils.ResId;
import com.wmods.wppenhacer.xposed.utils.Utils;
import java.util.HashSet;
import java.util.LinkedHashSet;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;

public class MenuHome extends Feature {

    public static HashSet<HomeMenuItem> menuItems = new LinkedHashSet<>();

    public MenuHome(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        hookMenu();
        var action = prefs.getBoolean("buttonaction", true);
        menuItems.add((menu, activity) -> InsertRestartButton(menu, activity, action));
        menuItems.add((menu, activity) -> InsertDNDOption(menu, activity, action));
        menuItems.add((menu, activity) -> InsertGhostModeOption(menu, activity, action));
        menuItems.add((menu, activity) -> InsertFreezeLastSeenOption(menu, activity, action));
        menuItems.add((menu, activity) -> InsertScheduleOption(menu, activity, action));
        menuItems.add(this::InsertOpenWae);
    }

    private void InsertScheduleOption(Menu menu, Activity activity, boolean newSettings) {
        var iconDraw = DesignUtils.getDrawableByName("ic_privacy");
        iconDraw.setTint(newSettings ? DesignUtils.getPrimaryTextColor() : 0xff8696a0);
        var itemMenu = menu.add(0, 0, 0, "Jadwalkan Pesan").setIcon(iconDraw);
        itemMenu.setOnMenuItemClickListener(item -> {
            Intent intent = new Intent();
            intent.setClassName(BuildConfig.APPLICATION_ID, "com.wmods.wppenhacer.activities.SchedulerActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            return true;
        });
    }

    private void InsertOpenWae(Menu menu, Activity activity) {
        var waeMenu = prefs.getBoolean("open_wae", true);
        if (!waeMenu) return;
        var itemMenu = menu.add(0, 0, 9999, " " + activity.getString(ResId.string.app_name));
        var iconDraw = DesignUtils.getDrawableByName("ic_settings");
        iconDraw.setTint(0xff8696a0);
        itemMenu.setIcon(iconDraw);
        itemMenu.setOnMenuItemClickListener(item -> {
            Intent intent = activity.getPackageManager().getLaunchIntentForPackage(BuildConfig.APPLICATION_ID);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            return true;
        });
    }

    private void InsertGhostModeOption(Menu menu, Activity activity, boolean newSettings) {
        var ghostmode = WppCore.getPrivBoolean("ghostmode", false);
        var itemMenu = menu.add(0, 0, 0, ResId.string.ghost_mode);
        var iconDraw = activity.getDrawable(ghostmode ? ResId.drawable.ghost_enabled : ResId.drawable.ghost_disabled);
        if (iconDraw != null) {
            iconDraw.setTint(newSettings ? DesignUtils.getPrimaryTextColor() : 0xff8696a0);
            itemMenu.setIcon(iconDraw);
        }
        itemMenu.setOnMenuItemClickListener(item -> {
            WppCore.setPrivBoolean("ghostmode", !ghostmode);
            Utils.doRestart(activity);
            return true;
        });
    }

    private void InsertRestartButton(Menu menu, Activity activity, boolean newSettings) {
        var iconDraw = activity.getDrawable(ResId.drawable.refresh);
        iconDraw.setTint(newSettings ? DesignUtils.getPrimaryTextColor() : 0xff8696a0);
        menu.add(0, 0, 0, ResId.string.restart_whatsapp).setIcon(iconDraw).setOnMenuItemClickListener(item -> {
            Utils.doRestart(activity);
            return true;
        });
    }

    private void InsertDNDOption(Menu menu, Activity activity, boolean newSettings) {
        var dndmode = WppCore.getPrivBoolean("dndmode", false);
        var item = menu.add(0, 0, 0, activity.getString(ResId.string.dnd_mode_title));
        var drawable = Utils.getApplication().getDrawable(dndmode ? ResId.drawable.airplane_enabled : ResId.drawable.airplane_disabled);
        if (drawable != null) {
            drawable.setTint(newSettings ? DesignUtils.getPrimaryTextColor() : 0xff8696a0);
            item.setIcon(drawable);
        }
        item.setOnMenuItemClickListener(menuItem -> {
            WppCore.setPrivBoolean("dndmode", !dndmode);
            Utils.doRestart(activity);
            return true;
        });
    }

    private void InsertFreezeLastSeenOption(Menu menu, Activity activity, boolean newSettings) {
        var freezelastseen = WppCore.getPrivBoolean("freezelastseen", false);
        MenuItem item = menu.add(0, 0, 0, activity.getString(ResId.string.freezelastseen_title));
        var drawable = Utils.getApplication().getDrawable(freezelastseen ? ResId.drawable.eye_disabled : ResId.drawable.eye_enabled);
        if (drawable != null) {
            drawable.setTint(newSettings ? DesignUtils.getPrimaryTextColor() : 0xff8696a0);
            item.setIcon(drawable);
        }
        item.setOnMenuItemClickListener(menuItem -> {
            WppCore.setPrivBoolean("freezelastseen", !freezelastseen);
            Utils.doRestart(activity);
            return true;
        });
    }

    private void hookMenu() {
        XposedHelpers.findAndHookMethod(WppCore.getHomeActivityClass(classLoader), "onCreateOptionsMenu", Menu.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var menu = (Menu) param.args[0];
                var activity = (Activity) param.thisObject;
                for (var menuItem : MenuHome.menuItems) {
                    menuItem.addMenu(menu, activity);
                }
            }
        });
    }

    @NonNull
    @Override
    public String getPluginName() { return "Menu Home"; }

    public interface HomeMenuItem { void addMenu(Menu menu, Activity activity); }
}
