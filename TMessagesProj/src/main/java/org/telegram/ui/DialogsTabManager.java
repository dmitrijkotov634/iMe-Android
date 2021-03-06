package org.telegram.ui;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import org.telegram.messenger.MessagesController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@SuppressLint("ApplySharedPref")
public class DialogsTabManager {

    // Все вкладки
    public List<DialogsTab> allTabs = new ArrayList<>();
    // Активные вкладки (DialogsActivity.TabsMode.ALL)
    private ArrayList<DialogsTab> activeTabs = new ArrayList<>();
    // Активные вкладки (DialogsActivity.TabsMode.MINIMAL)
    private ArrayList<DialogsTab> activeTabsMinimal = new ArrayList<>();

    // Вкладки, которые не должны отображаться в режиме MINIMAL
    private Set<DialogsActivity.Tab> extraTabs =
        new HashSet<>(Arrays.asList(DialogsActivity.Tab.CHANNELS, DialogsActivity.Tab.FOLDERS, DialogsActivity.Tab.MANAGEMENT));

    private static final String POSITION_KEY = "_Position";
    private static final String STATE_KEY = "_Enabled";

    private static volatile DialogsTabManager Instance = null;

    public static DialogsTabManager getInstance() {
        DialogsTabManager localInstance = Instance;
        if (localInstance == null) {
            synchronized (DialogsTabManager.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new DialogsTabManager();
                }
            }
        }
        return localInstance;
    }

    private DialogsTabManager() {
        fetchStoredTabsData();
    }

    private void fetchStoredTabsData() {
        TreeSet<DialogsTab> sortedTabs = new TreeSet<>();
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        for (int index = 0; index < DialogsActivity.Tab.values().length; index++) {
            DialogsActivity.Tab tab = DialogsActivity.Tab.values()[index];
            int position = preferences.getInt(buildPositionKey(tab), index);
            boolean isEnabled = preferences.getBoolean(buildStateKey(tab), tab != DialogsActivity.Tab.BOTS);
            DialogsTab tabState = new DialogsTab(tab, isEnabled, position);
            sortedTabs.add(tabState);
        }

        allTabs.clear();
        allTabs.addAll(sortedTabs);
        refreshActiveTabs();
    }

    public void refreshActiveTabs() {
        activeTabs.clear();
        activeTabsMinimal.clear();
        for (DialogsTab tab : allTabs) {
            if (tab.isEnabled()) {
                activeTabs.add(tab);
                if (!extraTabs.contains(tab.getType()) || tab.getType() == DialogsActivity.Tab.ALL) {
                    activeTabsMinimal.add(tab);
                }
            }
        }

        if (activeTabs.isEmpty()) {
            activeTabs.add(new DialogsTab(DialogsActivity.Tab.ALL, true, 0));
        }

        if (activeTabsMinimal.isEmpty()) {
            activeTabsMinimal.add(new DialogsTab(DialogsActivity.Tab.ALL, true, 0));
        }
    }

    public void storeCurrentTabsData() {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        for (int index = 0; index < allTabs.size(); index++) {
            DialogsTab tab = allTabs.get(index);
            editor.putInt(buildPositionKey(tab.getType()), index);
            editor.putBoolean(buildStateKey(tab.getType()), tab.isEnabled());
        }
        editor.commit();
    }

    DialogsActivity.Tab getTabByPosition(int position, DialogsActivity.TabsMode mode) {
        switch (mode) {
            case SINGLE:
                return DialogsActivity.Tab.ALL;
            case MINIMAL:
                return activeTabsMinimal.get(position).getType();
            default:
                return activeTabs.get(position).getType();

        }
    }

    int getPositionByTab(DialogsActivity.Tab tab, DialogsActivity.TabsMode mode) {
        if (mode == DialogsActivity.TabsMode.SINGLE) {
            return 0;
        }

        ArrayList<DialogsTab> targetArray;
        if (mode == DialogsActivity.TabsMode.MINIMAL) {
            targetArray = activeTabsMinimal;
        } else {
            targetArray = activeTabs;
        }

        int position = 0;

        for (int a = 0; a < targetArray.size(); a++) {
            DialogsTab dialogsTab = targetArray.get(a);
            if (dialogsTab.getType() == tab) {
                position = a;
                break;
            }
        }

        return position;
    }

    int getAvailableTabsCount(DialogsActivity.TabsMode mode) {
        switch (mode) {
            case SINGLE:
                return 1;
            case MINIMAL:
                return activeTabsMinimal.size();
            default:
                return activeTabs.size();
        }
    }

    private String buildPositionKey(DialogsActivity.Tab tab) {
        return tab.name().toLowerCase() + POSITION_KEY;
    }

    private String buildStateKey(DialogsActivity.Tab tab) {
        return tab.name().toLowerCase() + STATE_KEY;
    }
}
