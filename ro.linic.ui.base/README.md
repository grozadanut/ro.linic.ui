# Purpose

The purpose of this plugin is to provide the base functionalities used by other Linic plugins, such as: update functionality, language localization, global progress monitor, global eclipse fixes, default menu items...

# How to use

You just need to make sure this plugin is added as a dependency to your product. This usually is done in two ways:

1. Add this plugin as a direct dependency in your product file
2. Add this plugin to a feature project and add that feature as a `root` dependency in your product file(preferred way)

**NOTE** Option 1 is discouraged because if you use the p2 update functionality, whenever you update this plugin you will need to update the whole product in order to update this plugin. With option 2 you can update the feature separately from the product, whenever you need to push updates in the containing plugins.