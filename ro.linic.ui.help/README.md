# What is this?

This project helps developers bring help content to the users of the app in the form of tutorials(basically html).

# Why?

This project was created so we have a unified platform for providing instructions to the users about how to use the app. The idea is to put the html instructions next to the code, in the repository, so the developers can easily create the tutorials and the platform will then automatically present them to the end users.

# How this works?

Each plugin that wishes to provide help content to end users will create a bunch of html files containing tutorials and put them in a special folder inside the plugin and include these folders in the binary build. This plugin will then take these html files during runtime and concatenate them into a single html and show this html to the user in an embedded browser.

# Localization

Localization is supported for the html files based on the user's locale. You can name the html files using standard naming used in resourse bundles, eg: `tutorial.html`(default language), `tutorial_ro.html`(ro language), `tutorial_hu_HU.html`(hu language, HU country). In this way you can customize different tutorials for different languages. The rules are:

1. If the user locale is `ro` and there are the following files present:
    - tutorial.html
    - tutorial_ro.html (this will be shown to the user)
    - tutorial_hu.html
2. If the user locale is `ro` and there are the following files present:
    - tutorial.html (this will be shown to the user)
    - tutorial_hu.html
3. If the user locale is `en` and there are the following files present:
    - tutorial_ro.html
    - tutorial_hu.html
    
Nothing will be shown to the user.

# How to use

There are 2 types of help content you can provide:

1. Tutorials about how to use the application
2. Release notes containing changes from the previous version

## Tutorials

These are general tutorials about how to use the application, they are generally long standing. They can be replayed multiple times by the user. There is a menu item from which the user can browse through them, but also the app could decide to show them when a certain part of the app is opened for the first time.

These files are searched in the root of the project inside the `help` folder. You can also organize the files into subfolders, they are searched recursively.

The files should have the following structure:
- `[name][_locale].html`; **name** can be anything, but only one file with the same name will be selected for different locales

Example:
- help
    - intro.html
    - intro_ro.html
    - intro_hu.html
    - how-to-update.html
    - pos
        - pos.html
        - dude-driver_ro.html
        
A user with `ro` locale will see: intro_ro.html, how-to-update.html, pos.html, dude-driver_ro.html. <br>
A user with `en` locale will see: intro.html, how-to-update.html, pos.html.

## Release notes

These contain noteworthy changes when a user updates from a version to the next. It is assumed that the user updates sequentially, so we don't handle here the case where the user skips a version(eg.: updating from 1.0.0 to 1.0.3, skipping 1.0.2), thus we only show the most recent changes(eg.: changes in 1.0.3).

These html tutorials are more ephemeral and are usually only shown to the user once, typically after an app update, although the user could choose to see them later if he wishes.

These files are searched in the root of the project inside the `help-release` folder. You can also organize the files into subfolders, they are searched recursively.

The files should have the following structure:
- `[version][_locale].html`; **locale** is optional; the minor and major in the **version** should be separated with `-` instead of `.`(eg.: 1-0-1, NOT 1.0.1)

Example:
- help-release
    - 1-0-1.html
    - 1-0-1_ro.html
    - 1-0-1_hu.html
    - 1-0-0.html
    - old
        - 0-0-2_ro.html
        - 0-0-1.html
        
A user with `ro` locale that just updated to 1.0.1 will see: 1-0-1_ro.html <br>
A user with `en` locale that just updated to 1.0.1 will see: 1-0-1.html <br>
A user with `en` locale that just updated to 0.0.2 will see nothing. Note that if no html files where found when updating, the browser will not be shown at all.