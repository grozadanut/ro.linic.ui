# What is this?

This project extends the help system available in the RCP platform.

# Why?

The help system in Eclipse RCP is implemented in eclipse 3.x version, depending on `org.eclipse.ui`. We migrated the UI help to also work in a pure e4 application and also added some custom features for presenting release notes.

# How to use

There are 2 types of help content you can provide:

1. Basic help content(the one specified by eclipse extension points)
2. Release notes containing changes from the previous version(new custom feature)

## Release notes

These contain noteworthy changes when a user updates from a version to the next. It is assumed that the user updates sequentially, so we don't handle here the case where the user skips a version(eg.: updating from 1.0.0 to 1.0.3, skipping 1.0.2), thus we only show the most recent changes(eg.: changes in 1.0.3).

These html tutorials are only shown to the user once, typically after an app update, although the user could choose to see them later if he wishes, in the Help Browser.

Contributing release notes is the same as contributing help content; you have to add the html files using the help extension point, but to be picked up and shown after an update they need to be added to the update topic, see below.

The files should be added to a topic with the name:
- `[bundleSymbolicName]/[version].html`; the minor and major in the **version** should be separated with `-` instead of `.`(eg.: 1-0-1, NOT 1.0.1)

Example:
- ro.linic.ui.help
    - 1-0-1.html
    - 1-0-0.html
    - 0-0-1.html
        
A user that just updated to 1.0.1 will see: 1-0-1.html <br>
A user that just updated to 0.0.2 will see nothing. Note that if no html files where found when updating, the browser will not be shown at all.