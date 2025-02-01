# WalkersGuide-Android


WalkersGuide is a navigational aid primarily intended for blind and visual impaired pedestrians. It
calculates routes and shows nearby points of interest.  The project consists of an Android client
and a server component. The latter performs the route calculation. The map data is provided by
[OpenStreetMap](https://www.openstreetmap.org), a project to create a free map of the world.

This repository contains the client application. It's written in Java and supports the Android
operating system since version 5.1.

The application is fully compatible with the screen reader Talkback.

Please visit https://www.walkersguide.org for more information about the project.



## Build and install

You need the [Android SDK](https://developer.android.com/studio) to build this app.

```
git clone https://github.com/scheibler/WalkersGuide-Android
cd WalkersGuide-Android

# checkout the public transport-enabler library submodule which is required as a project dependency
# for more information about the library visit https://github.com/schildbach/public-transport-enabler
git submodule update --init --recursive

# create the .apk for the debug build types
./gradlew assembleDebug

# open the app on an attached Android device with usb debugging enabled
./gradlew openProdDebug
```



## Unit tests

```
./gradlew test
```

Please have a look into `app/build/reports/tests/testProdDebugUnitTest/index.html` for results.

