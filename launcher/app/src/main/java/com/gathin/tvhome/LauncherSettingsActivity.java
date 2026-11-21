package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Remote-friendly settings owned by the launcher; system settings stay one row away. */
public final class LauncherSettingsActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));   // locale
    }

    static final String EXTRA_REORDER = "com.gathin.tvhome.extra.REORDER_FROM_SETTINGS";
    private SharedPreferences choices;
    private final TextView[] columnChoices = new TextView[5];
    private final TextView[] styleChoices = new TextView[Config.TILE_STYLES.length];
    private String tileStyle;
    private TextView namesChoice;
    private boolean names;
    private boolean fixedDock;
    private String backdrop;
    private final TextView[] backdropChoices = new TextView[2];
    private static final int PICK_PICTURE_REQUEST = 41;
    private final TextView[] dockModeChoices = new TextView[2];
    /** The choices as they were when this screen opened; put back unless Save is pressed. */
    private java.util.Map<String, ?> snapshot;
    private boolean saved;
    /** A settings page opened from here (Default home, TV settings): leaving for it is not leaving unsaved. */
    private boolean openingPage;
    private final TextView[] rowChoices = new TextView[4];
    private static final int[] DOCK_OPACITIES = {100, 70, 50, 30};
    private final TextView[] dockChoices = new TextView[DOCK_OPACITIES.length];
    private static final String[] CLOCK_FORMATS = {"system", "12", "24"};
    private final TextView[] clockChoices = new TextView[CLOCK_FORMATS.length];
    private TextView wallpaperChoice;
    private TextView animationsChoice;
    private TextView advancedChoice;
    private TextView followThemeChoice;
    private TextView openOnStartChoice;
    private int columns;
    private int rowsBelowDock;
    private int dockOpacity;
    private String clockFormat;
    private boolean animations;
    private boolean advancedEffects;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        Config config = Config.load(this);
        View reorderChoice = findViewById(R.id.reorderChoice);
        reorderChoice.setVisibility(config.launcherSettingsButton ? View.VISIBLE : View.GONE);
        reorderChoice.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_REORDER, true));
            finish();
        });
        columns = config.columns;
        rowsBelowDock = config.rowsBelowDock;
        dockOpacity = config.dockOpacity;
        clockFormat = config.clockFormat;
        tileStyle = config.tileStyle;
        names = config.alwaysShowNames;
        fixedDock = config.singleRow;
        backdrop = choices.getString(Config.BACKDROP_KEY, "app");
        snapshot = new java.util.HashMap<>(choices.getAll());
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;

        int[] ids = {R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7, R.id.columns8};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + 4;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                changed();
                refresh();
            });
        }
        int[] rowIds = {R.id.rows0, R.id.rows1, R.id.rows2, R.id.rows3};
        for (int i = 0; i < rowIds.length; i++) {
            final int value = i;
            rowChoices[i] = findViewById(rowIds[i]);
            rowChoices[i].setOnClickListener(v -> {
                if (rowsBelowDock == value) return;
                rowsBelowDock = value;
                choices.edit().putInt(Config.ROWS_BELOW_DOCK_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] dockIds = {R.id.dock100, R.id.dock70, R.id.dock50, R.id.dock30};
        for (int i = 0; i < dockIds.length; i++) {
            final int value = DOCK_OPACITIES[i];
            dockChoices[i] = findViewById(dockIds[i]);
            dockChoices[i].setOnClickListener(v -> {
                if (dockOpacity == value) return;
                dockOpacity = value;
                choices.edit().putInt(Config.DOCK_OPACITY_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] clockIds = {R.id.clockSystem, R.id.clock12, R.id.clock24};
        for (int i = 0; i < clockIds.length; i++) {
            final String value = CLOCK_FORMATS[i];
            clockChoices[i] = findViewById(clockIds[i]);
            clockChoices[i].setOnClickListener(v -> {
                if (value.equals(clockFormat)) return;
                clockFormat = value;
                choices.edit().putString(Config.CLOCK_FORMAT_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] styleIds = {R.id.styleDefault, R.id.styleTinted, R.id.styleClear, R.id.styleDark};
        for (int i = 0; i < styleIds.length; i++) {
            final String value = Config.TILE_STYLES[i];
            styleChoices[i] = findViewById(styleIds[i]);
            styleChoices[i].setOnClickListener(v -> {
                if (value.equals(tileStyle)) return;
                tileStyle = value;
                choices.edit().putString(Config.TILE_STYLE_KEY, value).apply();
                changed();
                refresh();
            });
        }
        // Mirror text alignment without globally opting the launcher into RTL. Numeric rows
        // still need explicit ordering in Android's no-supportsRtl compatibility mode.
        if (getResources().getBoolean(R.bool.rtl_menu_bar))
            findViewById(android.R.id.content).setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if (getResources().getBoolean(R.bool.rtl_app_grid)) {
            reverseRow(columnChoices);
            reverseRow(rowChoices);
            reverseRow(dockChoices);
            reverseRow(clockChoices);
            reverseRow(styleChoices);
        }
        int[] dockModeIds = {R.id.dockFixed, R.id.dockScroll};
        for (int i = 0; i < dockModeIds.length; i++) {
            final boolean value = i == 0;
            dockModeChoices[i] = findViewById(dockModeIds[i]);
            dockModeChoices[i].setOnClickListener(v -> {
                if (fixedDock == value) return;
                fixedDock = value;
                choices.edit().putBoolean(Config.FIXED_DOCK_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] backdropIds = {R.id.backdropApp, R.id.backdropWallpaper};
        for (int i = 0; i < backdropIds.length; i++) {
            final String value = i == 0 ? "app" : "wallpaper";
            backdropChoices[i] = findViewById(backdropIds[i]);
            backdropChoices[i].setOnClickListener(v -> {
                if (!value.equals(backdrop)) {
                    backdrop = value;
                    choices.edit().putString(Config.BACKDROP_KEY, value).apply();
                    changed();
                    refresh();
                }
                if ("wallpaper".equals(value)) showWallpaperSources();
            });
        }
        findViewById(R.id.saveChoice).setOnClickListener(v -> {
            saved = true;
            setResult(RESULT_OK);
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
            finish();
        });
        namesChoice = findViewById(R.id.namesChoice);
        namesChoice.setOnClickListener(v -> {
            names = !names;
            choices.edit().putBoolean(Config.NAMES_KEY, names).apply();
            changed();
            refresh();
        });
        wallpaperChoice = findViewById(R.id.wallpaperChoice);
        wallpaperChoice.setOnClickListener(v -> showWallpaperPicker());
        animationsChoice = findViewById(R.id.animationsChoice);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            changed();
            refresh();
        });
        advancedChoice = findViewById(R.id.advancedChoice);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (dialog, which) -> setAdvanced(true))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        openOnStartChoice = findViewById(R.id.openOnStartChoice);
        if (config.openOnStartChoice) {
            openOnStartChoice.setVisibility(View.VISIBLE);
            openOnStartChoice.setOnClickListener(v -> showOpenOnStartPicker());
        }
        followThemeChoice = findViewById(R.id.followThemeChoice);
        followThemeChoice.setOnClickListener(v -> {
            if (!hasOnTvChoices()) return;
            choices.edit().remove("columns").remove(Config.ROWS_BELOW_DOCK_KEY).remove("animations")
                    .remove("advanced_effects").remove(Config.WALLPAPER_CHOICE_KEY)
                    .remove(Config.DOCK_OPACITY_KEY).remove(Config.CLOCK_FORMAT_KEY).remove(Config.TILE_STYLE_KEY).remove(Config.NAMES_KEY).remove(OpenOnStart.KEY).commit();
            Config current = Config.load(this);
            columns = current.columns;
            rowsBelowDock = current.rowsBelowDock;
            dockOpacity = current.dockOpacity;
            clockFormat = current.clockFormat;
            tileStyle = current.tileStyle;
            names = current.alwaysShowNames;
            fixedDock = current.singleRow;
            backdrop = "app";
            animations = current.animations;
            advancedEffects = current.animations && current.advancedEffects;
            changed();
            refresh();
        });
        findViewById(R.id.defaultHomeChoice).setOnClickListener(v ->
                open(new Intent(this, HomeSettingsActivity.class)));
        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(SystemSettings.intent(this, Settings.ACTION_SETTINGS)));
        refresh();
        if (config.launcherSettingsButton) reorderChoice.requestFocus();
        else columnChoices[Math.max(0, Math.min(columns - 4, 4))].requestFocus();
    }

    private void reverseRow(TextView[] views) {
        LinearLayout row = (LinearLayout) views[0].getParent();
        row.removeAllViews();
        for (int i = views.length - 1; i >= 0; i--) row.addView(views[i]);
    }

    private void showWallpaperPicker() {
        File dir = Config.dir(this);
        List<File> images = new java.util.ArrayList<>(WallpaperRotation.images(
                dir == null ? null : new File(dir, "wallpapers")));
        // A theme with a single wallpaper.jpg offers that picture too (Config maps the choice back to it).
        File single = dir == null ? null : new File(dir, Config.WALLPAPER_NAME);
        if (single != null && single.isFile()) images.add(0, single);
        String[] labels = new String[images.size() + 1];
        for (int i = 0; i < images.size(); i++) labels[i] = images.get(i).getName();
        labels[images.size()] = getString(R.string.wallpaper_none);
        String current = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        int checked = "none".equals(current) ? images.size()
                : indexOf(images, current);
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == images.size()) {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, "none").apply();
                    } else {
                        backdrop = "wallpaper";
                                choices.edit().putString(Config.BACKDROP_KEY, backdrop).putString(Config.WALLPAPER_CHOICE_KEY,
                                images.get(which).getName()).apply();
                    }
                    changed();
                    Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNeutralButton(R.string.wallpaper_theme_default, (dialog, which) -> {
                    choices.edit().remove(Config.WALLPAPER_CHOICE_KEY).apply();
                    changed();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Where a custom wallpaper comes from: the TV's wallpaper collection, a picture already on the TV (Downloads, a USB
     * drive) through Android's photo picker, or Google Images / Google Drive in the TV's browser, to save a picture
     * that the photo picker then offers. The launcher itself never goes online.
     */
    private void showWallpaperSources() {
        String[] items = {getString(R.string.wp_tv_wallpapers), getString(R.string.wp_pictures),
                getString(R.string.wp_google_images), getString(R.string.wp_google_drive)};
        new AlertDialog.Builder(this)
                .setTitle(R.string.wp_sources_title)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) showWallpaperPicker();
                    else if (which == 1) pickPicture();
                    else openWeb(which == 2 ? "https://images.google.com" : "https://drive.google.com");
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void pickPicture() {
        Intent pick = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
        try {
            openingPage = true;
            startActivityForResult(pick, PICK_PICTURE_REQUEST);
        } catch (ActivityNotFoundException | SecurityException e) {
            openingPage = false;
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }

    private void openWeb(String url) {
        Toast.makeText(this, R.string.wp_web_hint, Toast.LENGTH_LONG).show();
        open(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)));
    }

    /** Copies the picked picture into the wallpapers collection and makes it the wallpaper. */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_PICTURE_REQUEST || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        File dir = Config.dir(this);
        if (dir == null) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
            return;
        }
        File folder = new File(dir, "wallpapers");
        if (!folder.isDirectory() && !folder.mkdirs()) return;
        String name = "picked-" + System.currentTimeMillis() + ".jpg";
        File out = new File(folder, name);
        try (java.io.InputStream in = getContentResolver().openInputStream(data.getData());
             java.io.OutputStream os = new java.io.FileOutputStream(out)) {
            if (in == null) throw new java.io.IOException("no stream");
            byte[] buf = new byte[64 * 1024];
            for (int n; (n = in.read(buf)) > 0; ) os.write(buf, 0, n);
        } catch (java.io.IOException | RuntimeException e) {
            //noinspection ResultOfMethodCallIgnored
            out.delete();
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
            return;
        }
        backdrop = "wallpaper";
        choices.edit().putString(Config.BACKDROP_KEY, backdrop).putString(Config.WALLPAPER_CHOICE_KEY, name).apply();
        changed();
        Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
        refresh();
    }

    /** Off, then every app with a TV entry point by name. Saved at once; it is read on the next boot. */
    private void showOpenOnStartPicker() {
        PackageManager pm = getPackageManager();
        List<String[]> apps = new ArrayList<>();
        Intent tv = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(tv, 0)) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            boolean seen = false;
            for (String[] a : apps) seen |= a[1].equals(pkg);
            if (!seen) apps.add(new String[]{r.loadLabel(pm).toString(), pkg});
        }
        Collections.sort(apps, (a, b) -> a[0].compareToIgnoreCase(b[0]));
        String[] labels = new String[apps.size() + 1];
        labels[0] = getString(R.string.off);
        String current = choices.getString(OpenOnStart.KEY, "");
        int checked = 0;
        for (int i = 0; i < apps.size(); i++) {
            labels[i + 1] = apps.get(i)[0];
            if (apps.get(i)[1].equals(current)) checked = i + 1;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.open_on_start_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) choices.edit().remove(OpenOnStart.KEY).apply();
                    else {
                        OpenOnStart.skipThisBoot(this);
                        choices.edit().putString(OpenOnStart.KEY, apps.get(which - 1)[1]).apply();
                    }
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** The picked app's name, its package when it is gone, or Off. */
    private String openOnStartLabel() {
        String pkg = choices.getString(OpenOnStart.KEY, "");
        if (pkg.isEmpty()) return getString(R.string.off);
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private static int indexOf(List<File> images, String name) {
        if (name == null) return -1;
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getName().equals(name)) return i;
        }
        return -1;
    }

    /**
     * A choice was saved. Home rebuilds when it next resumes because the change count moved, whether these
     * settings are left with Back or with the remote's Home button (which cancels the activity result).
     */
    private void changed() {
        setResult(RESULT_OK);
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        changed();
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + 4);
        }
        for (int i = 0; i < rowChoices.length; i++) {
            rowChoices[i].setSelected(rowsBelowDock == i);
        }
        for (int i = 0; i < dockChoices.length; i++) {
            dockChoices[i].setSelected(dockOpacity == DOCK_OPACITIES[i]);
        }
        for (int i = 0; i < styleChoices.length; i++) {
            styleChoices[i].setSelected(Config.TILE_STYLES[i].equals(tileStyle));
        }
        for (int i = 0; i < clockChoices.length; i++) {
            clockChoices[i].setSelected(CLOCK_FORMATS[i].equals(clockFormat));
        }
        backdropChoices[0].setSelected(!"wallpaper".equals(backdrop));
        backdropChoices[1].setSelected("wallpaper".equals(backdrop));
        dockModeChoices[0].setSelected(fixedDock);
        dockModeChoices[1].setSelected(!fixedDock);
        namesChoice.setText(getString(R.string.names_state, getString(names ? R.string.on : R.string.off)));
        animationsChoice.setText(getString(R.string.animations_state,
                getString(animations ? R.string.on : R.string.off)));
        advancedChoice.setText(getString(R.string.advanced_effects_state,
                getString(advancedEffects ? R.string.on : R.string.off)));
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.5f);
        String wallpaperCurrent = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        wallpaperChoice.setText(getString(R.string.wallpaper_state,
                "none".equals(wallpaperCurrent) ? getString(R.string.wallpaper_none)
                        : wallpaperCurrent != null ? wallpaperCurrent
                        : getString(R.string.wallpaper_theme_default)));
        openOnStartChoice.setText(getString(R.string.open_on_start_state, openOnStartLabel()));
        followThemeChoice.setSelected(!hasOnTvChoices());
    }

    private boolean hasOnTvChoices() {
        return choices.contains("columns") || choices.contains(Config.ROWS_BELOW_DOCK_KEY)
                || choices.contains("animations") || choices.contains("advanced_effects")
                || choices.contains(Config.WALLPAPER_CHOICE_KEY) || choices.contains(Config.DOCK_OPACITY_KEY)
                || choices.contains(Config.CLOCK_FORMAT_KEY) || choices.contains(Config.TILE_STYLE_KEY) || choices.contains(Config.NAMES_KEY) || choices.contains(Config.FIXED_DOCK_KEY) || choices.contains(Config.BACKDROP_KEY)
                || choices.contains(OpenOnStart.KEY);
    }

    private void open(Intent intent) {
        try {
            openingPage = true;
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        openingPage = false;
    }

    /** Leaving without Save (Back, or the remote's Home button) puts every choice back as it was. */
    @Override
    protected void onPause() {
        super.onPause();
        if (saved || openingPage || snapshot == null) return;
        SharedPreferences.Editor e = choices.edit().clear();
        for (java.util.Map.Entry<String, ?> en : snapshot.entrySet()) {
            Object v = en.getValue();
            if (v instanceof Boolean) e.putBoolean(en.getKey(), (Boolean) v);
            else if (v instanceof Integer) e.putInt(en.getKey(), (Integer) v);
            else if (v instanceof Long) e.putLong(en.getKey(), (Long) v);
            else if (v instanceof Float) e.putFloat(en.getKey(), (Float) v);
            else if (v instanceof String) e.putString(en.getKey(), (String) v);
            else if (v instanceof java.util.Set) {
                @SuppressWarnings("unchecked") java.util.Set<String> s = (java.util.Set<String>) v;
                e.putStringSet(en.getKey(), s);
            }
        }
        e.commit();
        setResult(RESULT_CANCELED);
    }
}
