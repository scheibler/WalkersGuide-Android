#WalkersGuide-Android


WalkersGuide is a navigational aid primarily intended for blind and visual impaired pedestrians. It
calculates routes and shows nearby points of interest.  The project consists of an Android client
and a server component. The latter performs the route calculation. The map data is provided by
[OpenStreetMap](https://www.openstreetmap.org), a project to create a free map of the world.

This repository contains the client application. It's written in Java and supports the Android
operating system since version 4.1. The application is fully accessible with Android's screen reader
Talkback.

Please visit https://www.walkersguide.org for more information about the project.



## Public transport api credentials

Some public transport providers require api credentials for usage. Put the credentials file into the
following folder if you want to use one of them (create if necessary):

~~~
/sdcard/Android/data/org.walkersguide.android/files/pt_provider_credentials/
~~~

Then create the credentials file (see below). Afterwards close WalkersGuide from the recent apps
screen and restart again. You will find the respective provider in the app settings under "public
transport provider".

For more information visit the [public-transport-enabler website](https://github.com/schildbach/public-transport-enabler).


### Deutsche Bahn

Create `db_provider_api_credentials.json` in the following format:

~~~
{
    "apiAuthorization" : "YOUR_API_AUTHORIZATION_KEY",
    "salt" : "YOUR_SALT"
}
~~~

