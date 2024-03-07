# Eluvio Deeplink Library for AndroidTV

## Installation
1. Copy [Eluvio.kt](Eluvio.kt) into your project.
2. Add this to your `AndroidManifest.xml`, outside the `<application>` tag:
```xml
    <queries>
        <package android:name="app.eluvio.wallet"/>
        <package android:name="com.android.vending"/>
        <package android:name="com.amazon.venezia"/>
    </queries>
```

** Gradle library coming soon **

## Usage
Call `Eluvio.launchSkuDeeplink(context, marketplaceId, sku)` to launch a specific item.  
If the Eluvio Media Wallet app is not installed, it'll open the the relevant store page for the app (only Google Play and Amazon Appstore are currently supported).

