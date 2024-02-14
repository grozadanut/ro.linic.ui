This plugin is a rewrite of `org.eclipse.equinox.p2.ui` plugin adapted to work in a pure e4 application, thus dependencies to `org.eclipse.ui` were removed.

# Differences to consider

Notable differences from `org.eclipse.equinox.p2.ui` include:
1. **ProvUIActivator** was migrated to **ProvUIAddon**. The imageRegistry and provisioningUI were removed as fields and moved into the context.
2. **ProvUI.isUpdateManagerInstallerPresent()** hardcoded to return false, as we don't have the `org.eclipse.ui.update.findAndInstallUpdates` command from **org.eclipse.ui.ide**
3. **ProvUI.INSTALLATION_DIALOG** command id removed, as it points to **InstallationDialog** from **org.eclipse.ui.workbench** which is not available.
4. **InstalledSoftwarePage** was not migrated.
5. **RevertProfilePage** was not migrated.

# How to use

- add `ro.linic.ui.p2` as a dependency in your plugin.xml
- register `ro.linic.ui.p2.ui.addons.ProvUIAddon` as an addon in your **application.e4xmi** or **fragment.e4xmi**
- create a handler for your **Install New Software...** menu item and use `ro.linic.ui.p2.handlers.InstallNewSoftwareHandler` as the handler class; if you wish you can also use **icons/obj/iu_obj.png** as the icon for the menu item
- *optional*: you can also create a menu item for **Check for Updates** and use `ro.linic.ui.p2.handlers.UpdateHandler` as the handler class; use **icons/obj/iu_update_obj.png** as the icon