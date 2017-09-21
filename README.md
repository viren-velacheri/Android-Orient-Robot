## Mindstorm robot controlled by an Android smartphone.

Demonstrates using the **magnetic orientation sensor**
in an Android smartphone to steer the robot in a straight line.

Android smartphone communicates via Bluetooth
with IOIO (https://github.com/ytai/ioio/wiki) board.
#### IOIO Board
![IOIO](/images/ioio.png  "IOIO")

The IOIO board essentially allows the phone to control the pins of a pic micro controller.
In Arduino fashion the micro controller pins can be used as digital or analog input/outputs.
Pins can also be configured as PWM (Pulse Width Modulation) outputs.

So, the IOIO board provides PWM outputs controlled by the smartphone, which are sent to
an L298 dual H-Bridge motor controller. Differential steering by varying power to left and right wheels using PWM motor control.

#### L298 dual H-Bridge DC motor controller
![motor](/images/l298.png "H-Bridge")

### Video of Robot in Action
<a href="http://www.youtube.com/watch?feature=player_embedded&v=7GzuBxc2jFg" target="_blank"><img src="http://img.youtube.com/vi/7GzuBxc2jFg/0.jpg"
alt="youtube video" width="480" height="360" border="10" /></a>

### [Link to main source file (MainActivity.java)](/src/app/ioio/orientcontrol/MainActivity.java)

#### IOIO Board Notes

Got *geeeetech* IOIO board from China. Cost $20, came with female and male headers.

IOIO board is bare so had to solder the female headers to IOIO board.
Powered on IOIO board with 8.65 v DC from home made power supply. Red led turns on.

##### Software:
IOIO was released 2011 with an eclipse based android developers kit. Had old version of eclipse, ran into problems trying to update ADT plug-in. Decided to re-install.
As of May 2015, Google had removed the links to download an eclipse based kit. Searched stack-overflow and found some old links. The last release was dated 2014-07-02 (July 2014).

Found an article on migrating IOIO to Android Studio, but given challenges in just ‘importing’ and existing project into Android Studio, gave it a pass. Also Android Studio has a new build system called ‘gradle’.

Downloaded and installed, for some reason SDK manager would not start, tracked it down to a problem with the find_java.bat program under tools/lib. For some reason it wasn’t copied over. To start SDK manager, android.bat runs which in turn calls find_java.bat. After wasting an hour trying to patch android.bat, but still getting errors with the SDK manager starting but not displaying the API levels. Finally deleted the eclipse install and re-extracted. This time everything worked. Installed SDK 4.1 with Google APIs.

From IOIO wiki, downloaded zip file the sample application and libraries. Downloaded the latest (as of May 2015) IOIOApp005.zip and extracted the files.
Start eclipse
IOIO wiki, getting started doesn’t have a detailed guide, they have a link to one in Sparkfun.
Choose import option, DO NOT COPY into workspace. (copying resulted in libraries not building).
First import IOIOLib. Will say something about target android-7 not found.
Select project → Properties → Android. Choose Android 4.1 as target. (when the ADT couldn’t be updated, eclipse wouldn’t show any targets). Keep project selected, clean build. With the build-automatically option selected. Show now build
Note: IOIOLibPC would not build, but I ignored since I was only interested in Android.
Do the same for IOIOAccessory. However now under properties select Google API. If Google API not available, go into SDK manager and install. Project clean and auto-build.
Then IOIOBTLib, set Android property to Android 4.1 target, clean and auto-build.

Now import  the sample HelloIOIO application. Do the same target gymnastics, should build, with libraries built above auto-selected.

Enable USB debugging on phone, and upload APK. Select RunAs→ Android Application.
Now disconnect from PC.
Disable USB debugging on phone. (this wasn’t on the sparkfun tutorial).
On IOIO board, turn trimpot all the way to the right, make sure power supply can supply enough current. The phone should begin charging.
Connect to IOIO board. Toast message popped up saying firmware on IOIO board (401) not compatible with IOIOLib 005.
Note: I got to this point only after trying numerous times to connect with USB debugging enabled. Phone would say connecting as installer and then eventually connect the IOIO board as a media device. The HelloIOIO app would not work (no button press to light the led).
After numerous Google searches, found one where Ytai (original developer) said make sure you have USB debugging turned off. That’s when I got a pop up asking me if I wanted to connect to IOIO as an accessory (for which I chose the option to do so always).

I then tried building the HelloIOIO app with IOIOApp004.zip libraries. because I selected the copy into workspace option while importing, I couldn’t build. Then having recalled that I had built the 005 version I went back to it, wouldn’t build until finally I discovered the option not to copy into workspace.

Anyway decided to update the firmware to support IOIOLib005.

From IOIO wiki, downloaded IOIODude, just extract the zip file to a directory.
Download the firmware image and copy to same directory.
Followed link to device driver for IOIO (windows), opened a text file. Copied contents into an IOIO.inf file and saved to desktop.

As per instructions, disconnected power supply to IOIO and connected boot pin to gnd and then connected a smartphone USB cable between IOIO and PC. PC recognized new device but failed to install a driver. Went into device manager, selected update and chose desktop as directory to look for driver. Windows installed IOIO driver , and showed IOIO OTG COM7.

Followed instructions running IOIOdude referencing the serial port COM7 found in previous step.
Successfully updated application firmware.

Now connected phone to IOIO board, and HelloIOIO works as expected.

Tried HelloIOIOService. Found that if service was accessing IO, HelloIOIO could not. Sometimes had to use task manager to kill either app or service to get it working.

##### Things to do:

- [x] Order NXT sockets from mindsensor.
- [x] Order L298 based H-Bridge motor controllers.
- [ ] IOIO project in Android Studio
- [x] OpenCV install

##### References

http://mitchtech.net/category/tutorials/ioio/
Projects from mitchtech use deprecated classes and methods (2012)

https://www.sparkfun.com/news/789

IOIO tutorial
https://www.sparkfun.com/tutorials/280

Looper and Handler (used by IOIO)
http://mindtherobot.com/blog/159/android-guts-intro-to-loopers-and-handlers/

http://www.socsci.uci.edu/~jkrichma/ABR/index.html

https://groups.google.com/forum/?hl=en#!forum/android-based-robotics
