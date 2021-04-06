# Tiddloid Lite

![avatar](img/Tiddloid.png)

&ensp;&ensp;&ensp;&ensp;&ensp;[![CoolApk](https://www.coolapk.com/static/img/icon.png)](https://www.coolapk.com/apk/top.donmor.tiddloidlite)&ensp;&ensp;&ensp;&ensp;<img src="img/qr.png" width="72" height="72" alt="QrCode"/>

Tiddloid Lite, a lightweight version of [Tiddloid](https://github.com/donmor/Tiddloid), is an app to work with locally stored TiddlyWikis. Once have some ideas, you can immediately write them down and save it in a tiddler, and sync the Wiki to your other devices so that you can access these ideas anywhere.

<img src="img/img01.png" width="360" height="640" alt="01"/>&emsp;&emsp;<img src="img/img02.png" width="360" height="640" alt="02"/>

See https://tiddlywiki.com/ for more details of TiddlyWiki.

### Features

* Create new Wikis with latest template
* Import existing Wikis from file providers, supporting local and cloud.
* TiddlyWiki detection
* Saving TiddlyWikis to the source file by clicking the Save button
* Creating shortcuts to existing wiki on desktop
* Tiddler/file export and import(Lollipop and above)

### Please notice (1.2 and above)

* Tiddloid/Tiddloid Lite now supports wiki list data importing/exporting. 
    * To import: Copy `data.json` to `INTERNAL/Android/data/top.donmor.tiddloidlite/files/` before running for the first time. If you have previously run the program, clear the data in `Settings/Apps` and copy the file.
    * To export: Create a tiddler with: `<a href="javascript:window.twi.exportDB()">export</a>` and click the link.
* Now supports Applying theme color to the window or hiding the toolbar on loaded depending on configurations inside wiki.
    * Apply theme color: Check `Control Panel/Appearance/Client Tweaks/Tiddloid/Apply theme color to system bars` (in future versions) or create the tiddler: `$:/config/Tiddloid/ApplyTheme` with `yes`.
    * Hide toolbar: Check `Control Panel/Appearance/Client Tweaks/Tiddloid/Hide toolbar on loading complete` (in future versions) or create the tiddler: `$:/config/Tiddloid/HideToolbar` with `yes`.
* TiddlyWiki 5.1.23 has dropped support for Android 5.1 and below (WebView 39). Another thing is that JSON behaves differently between 5.0+ and 4.4, causing random wiki list order bug on KitKat devices. We recommend you to use Tiddloid/Tiddloid Lite on Oreo (8.0) and above to enable all features.

### License

This app is under GPL v2 license,
1. Commercial use
2. Modification
3. Distribution
4. Private use

is permitted UNDER THESE CONDITIONS:
1. Provide this license and copyright notice
2. State changes
3. Disclose source under
4. Same license.

This app is made possible by these open source projects:
* Json - https://json.org/ ,
* Jsoup - https://jsoup.org/ ,

License files provided in /lic.

### Localization

The localization of this app is currently provided by Google Translation. Pull requests are welcomed if you have better localization.
