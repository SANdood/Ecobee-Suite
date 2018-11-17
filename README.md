Free Ecobee Suite, version: 1.6.1* 
======================================================
Latest: Version 1.6.1* Released August 7, 2018 at 4:00pm EDT

***NOTE: When updating/installing, you MUST include ALL of the Suite's components***

## Highlights
The most significant changes in the August 2018 update to 1.6.1* include the following:

***The Reservation System have been totally re-implemented as of the 1.6.10 release - now part of Ecobee Suite Manager instead of Thermostats***

#### General changes to the Ecobee Suite
- New **Android Optimizations** - Via a new preference setting in Ecobee Suite Manager (under Preferences), users can now specify the type of mobile device they are using (iOS or Android). Where SmartThings operates differently on these devices, the code now tries to optimize for the platform. Primary differences are Heat/Cool **At** and Heat/Cool **To** on iOS (but not Android), and OperatingState 'Off' on iOS (but not Android). Default is 'iOS', so if you have an Android, please go change the preference setting. *Added to Ecobee Suite Manager v1.6.13 and Ecobee Thermostat v1.6.15*
- New **Multiple SMS numbers Notifications** - SmartThings has notified users that the Contact Book will no longer be supported as of Monday, July 30, 2018. The recommended alternative is to send SMS messages to users, or to send Push messages to ALL users. This update enables you to configure multiple phone numbers (separated by ';') as the target for SMS messages (if you want directed notifications).
- New **Reservations** - Through a new internal reservation system, Helper Apps can coordinate their changes to ensure that another Helper doesn't override an intended function (*e.g.,* changing the Mode to Off, or changing fan circulation time). See below for more information. 
- Most Helper SmartApps can now select **Ecobee Suite Thermostats Only**, so to ensure proper coordination between the Suite's devices and SmartApps
#### Ecobee Suite Manager
- **Re-implemented Reservations** 1.6.10 moves the entire reservation system into ESM, where it probably should have been since the beginning.
- **Asynchronous HTTP** While not user visible, this change significantly improves the performance and reliability of API calls to the Ecobee servers.
- **Additional Weather Info** Another internal enhancement, now provides more info from the Ecobee weather report to enable the new Dew Point feature of Ecobee Suite Smart Mode.
##### ecobee Suite Contacts & Switches
- Now supports **Reservations** for Thermostat Mode changes to/from 'Off'
- Now supports **Multiple SMS Notifications**
- Now supports *Ecobee Suite Thermostats Only**
##### ecobee Suite Quiet Time
- Now supports **Reservations** for both Mode and Circulation time changes
- Now supports **Ecobee Suite Thermostats Only**
- Notification setup has been removed, because Notifications has not been implemented yet
##### ecobee Suite Mode/Routine/Program
- Now supports **Multiple SMS Notifications**
- Now supports **Ecobee Suite Thermostats Only**
##### ecobee Suite Smart Circulation
- Now supports **Reservations** for Circulation time changes
- Now supports **Ecobee Suite Thermostats Only**
##### ecobee Suite Smart Mode
- New!! **Dew Point Override** - You can now configure Smart Mode to ***not*** turn off the thermostat if the exterior Dew Point is above a certain temperature (e.g., 64°F), even if the target temperature is observed.
- New!! **Weather Data Sources** have been expanded to include SmartThings Weather Stations, in addition to the standard Ecobee weather info and the SmartThings/WeatherUnderground data. Users of my [MeteoWeather Weather Station](https://github.com/SANdood/MeteoWeather) can even access their local weather station (if they are using a [Meteobridge](https://www.meteobridge.com/wiki/index.php/Home) device).
- Now supports **Reservations** for Thermostat Mode changes to/from 'Off'
- Now supports **Multiple SMS Notifications**
- Now supports **Ecobee Suite Thermostats Only**
##### ecobee Suite Smart Room
- Now supports **Multiple SMS Notifications**
##### ecobee Suite Smart Switches, Smart Vents, Smart Zones
- Now support **Ecobee Suite Thermostats Only**

## <a name="top">Table Of Contents</a>
- [Introduction](#intro)
  - [Motivation](#motivation)
  - [Donations](#donations)
  - [What's New](#whazzup)
- [Installation](#installation)
  - [Upgrading from Prior Releases](#upgrading)
  - [Installation Preparation](#install-prep)
  - [Installing Using GitHub Integration](#github-install)
	  - [Installing Device Handlers](#github-devices)
	  - [Installing SmartApps](#github-smartapps)
	  - [Enabling oAuth](#github-oauth)
  - [Installing Manually from Code](#manual-install)
	  - [Installing Device Handlers](#manual-devices)
	  - [Installing Ecobee Suite Manager](#manual-manager)
	  - [Endabling oAuth](#manual-oauth)
	  - [Installing Helper SmartApps](#install-helpers)
  - [Finalizing Installation on SmartThings Mobile](#install-mobile)
  - [Updating the Code](#updating)
	  - [Updating using GitHub](#github-updates)
	  - [Updating Manually from Code](#manual-updates)
- [Features](#features)
  - [General Overview](#general)
  - [Thermostat and Sensor Device User Interfaces](#features-therm-ui)
	  - [Thermostat User Interface](#thermostat)
	  - [Sensor User Interface](#sensor)
	  - [SmartThings Integration](#smartthings)
	  - [Operational Enhancements](#operational)
  - [Screenshots](#screenshots)
  - [Ecobee Suite Manager SmartApp](#features-manager-sa)
  - [ecobee Suite Contacts & Switches Helper](#features-opencontact-sa) ***Updated!***
  - [ecobee Suite Quiet Time Helper](#features-quiet-time-sa) ***New!***
	  - [Using a Virtual Switch with Quiet Time](#qt-virtual-switch)
  - [ecobee Suite Mode/Routine/Program Helper](#features-routines-sa)
  - [ecobee Suite Smart Circulation Helper](#features-smart-circ-sa) ***Updated!***
  - [ecobee Suite Smart Mode](#smart-mode-sa)
  - [ecobee Suite Smart Room SmartApp](#features-smart-room-sa)
  - [ecobee Suite Smart Switches SmartApp](#features-smart-switches-sa)
  - [ecobee Suite Smart Vents SmartApp](#features-smart-vents-sa)
  - [ecobee Suite Smart Zones SmartApp](#features-smart-zone-sa) ***Updated!***
- [Troubleshooting](#troubleshooting)
  - [Reporting Issues](#reporting-issues)
- - [Quick Links](#quicklinks)
- [License](#license)
- [Integrating With Ecobee Suite Devices](#integration)
  - [Supported Device Attributes](#attributes)
  - [Supported Commands](#commands)
	  - [Changing Programs](#changeprogram)
	  - [Changing Thermostat Modes](#changingmode)
	  - [Changing Thermostat Fan Modes](#changingfan)
	  - [Complex Commands Requiring Arguments](#complex)
  - [Reservations](#reservations)

## <a name="intro">Introduction</a>
This document describes the various features related to the Ecobee Suite of Device Handlers and supporting Helper SmartApps for Ecobee thermostats and sensors. 

The following components are part of the Suite:
#### SmartApps

- **Ecobee Suite Manager SmartApp**: This SmartApp provides a single interface for Ecobee Installation, Authorization, Device Setup (both Thermostats **and** Sensors), Behavioral Settings and even a Debug Dashboard. Additional features can be added over time as well thanks to built in support for Child SmartApps, keeping everything nicely integrated into one app. 

   ***IMPORTANT**: This is the "parent" SmartApp to the entire Ecobee Suite - **users should not install any instances of the other SmartApps or devices**, neither on their mobile devices nor in the IDE.*
- **ecobee Suite Contacts & Switches**:

- **ecobee Suite Mode/Routine/Program**: Child Helper SmartApp that lets you trigger settings changes on your Ecobee thermostats based on the SmartThings Hello Modes and/or Routines. This version also supports changing SmartThings Mode or executing a Routine when the thermostat enters a new Program or Vacation. Settings include the Ecobee Program (Comfort Settings), Fan Modes and Hold Types. In additional to SmartThings Hello Modes, sunrise/sunset triggers are also support. Multiple instances of this SmartApp are also supported for maximum flexibility.

- **ecobee Suite Quiet Time**: ***New*** Child Helper SmartApp that will turn off your selection of the fan, circulation, humidifier, dehumidifier and even the whole HVAC whenever a speficied switch (real or virtual) is turned on or off (selectable). Reverses the actions when the switch is reset.

- **ecobee Suite Smart Circulation**: Child Helper SmartApp that adjusts hourly circulation time (fan on minimum minutes) trying to get (and keep) 2 or more rooms (temperature sensors, either Ecobee or SmartThings) to within a configurable temperature difference.

- **ecobee Suite Smart Mode**: ***Enhanced*** Child Helper SmartApp that will automatically change the thermostat(s) Mode (Auto, Cool, Heat, Off, etc.) based on an external (outdoor) weather temperature.

- **ecobee Suite Smart Room**: Child Helper SmartApp that automates a "Smart Room." Intended for rooms that are normally not used (doors, windows and automated vents closed), this app will Activate a "Smart Room" when the door is left open for a configurable period (defalt 5 minutes). Once active, the room's Ecobee Sensor is added to any of the user-selected default 3 Programs (Home, Away, Sleep), and (optionally) the automated vent (EcoNet or Keene) is opened. If the door is closed, after a configurable number of hours (default 12), it will De-activate the Smart Room, removing the Ecobee sensor from specified Programs and closing the vent. While active, opening a window will temporarily de-activate the room until the window is closed. If motion is detected while the door is closed, the automated de-activation will be cancelled until the next time the door is closed (and no motion is detected. Requires: Ecobee thermostat(s) with Ecobee Temp/Occupancy Sensor and Door contact sensor. Optional: Window contact sensor(s), EcoNet or Keene smart vents.

- **ecobee Suite Smart Switch/Dimmer/Vent**: Child Helper SmartApp that will turn on/off one or more switches, and/or set the level on one or more dimmers (also works for vents), all based on changes to the Operating State of one or more thermostats. Intended to turn on vent booster fans when HVAC is pushing air, and (optionally) turn them off when they return to idle state. Can also be used to control lights and dimmers; dimmer control can be used to operate vents (see also Smart Vents Helper, below).

- **ecobee Suite Smart Vents**: Child Helper SmartApp that will open and close one or more SmartThings-controlled HVAC vents based on room temperature in relation to specified heating/cooling target setpoint, or the setpoint of the specified thermostat.

- **ecobee Suite Smart Zone**: Child Helper SmartApp that attempts to synchronize fan on time between two zones. Intended for use on multi-zones HVAC systems; running the fan on all the zones simultaneously could help reduce electricity consumption vs. having each zone run the fan independently.

#### Device Handlers

- **Ecobee Thermostat**: The Device Handler for the Ecobee Thermostat functions and attributes. Presents a much more detailed view of the thermostat/HVAC system status, including heat/cool stages, humidification & dehumidification status, fan circulation control, and active/inactive icons for many modes and states. Designed using icons representative of the Ecobee web and mobile apps. Also supports a broad suite of extended attributes and commands to allow WebCOrE and SmartThings apps to monitor and control the thermostat, allowing customization and integration within a users' Smart Home. 

- **Ecobee Sensor**: This implements the Device Handler for the Ecobee Sensor attributes. Includes indicators for current thermostat Program, buttons that allow adding/removing the zone from specific Programs (Home, Away, Sleep only), and indicators that appear when Smart Room is configured for the room. This is also used to expose the internal sensors on the Thermostat to allow the actual temperature values (instead of only the average) to also be available. This is critically important for some applications such as smart vents.

Here are links to the working version of the repository being developed and maintained by Barry A. Burke [(on GitHub)](https://github.com/SANdood/Ecobee-Suite) and [(on SmartThings Community)](https://community.smartthings.com/t/release-updated-ecobee-suite-v1-4-0-free/118597).

--------------------------------
### <a name="motivation">Motivation</a>

I maintain the original intent as was previously defined by Sean:

The intent is to provide an Open Source DTH and SmartApps for Ecobee thermostats that can be used by the SmartThings community of users ***free of charge*** and ***without fear of the device disappearing in the future***. This will help ensure accessibility for all users and provide for an easy mechanism for the community to help maintain/drive the functionality.

Let me be clear: this is not a competition. I am not trying to replace or outdo any other version, and I personally have nothing against people making money off of their efforts. I have invested my time in this as Open Source so as to meet my own wants and desires, and I offer the result back to the community with no strings attached.

Please feel free to try any version you want, and pick the one that best fits your own use case and preference.

If you like this version and are so inclined, post a brief message of support on the SmartThings Community link above. And if you **don't** like it, or have any problems with the installation or operation of the suite, please also post on the link above. I strive for total transparency, and your use of this Suite does not require you to post positive reviews, nor are you prohibited in any way from posting negative reviews. 

-------------------------
### <a name="donations">Donations</a>

This work is fully Open Source, and available for use at no charge.

While not required, I do humbly accept donations. If you would like to make an *optional* donation, I will be most grateful. You can make donations to me on PayPal at <https://paypal.me/BarryABurke>

-------------------------------
## <a name="whazzup">What's New</a>

The most significant enhancement with the release of 1.4.* (and continued with 1.5.*) is the separation from the SmartThings Ecobee support, as well as from all prior versions of both @StrykerSKS' and my own Ecobee integration. Users of these prior versions should review the [Upgrading from Prior Releases](#upgrading) section below for more IMPORTANT information before installing this version.

#### *Note that release 1.5.\* CAN be installed on top of an existing 1.4.\* installation.*

### Key enhancements in release 1.5.0
- ***Significantly improved handling of API connection errors.*** While I cannot yet guarantee non-stop operation during all SmartThings and/or Ecobee Cloud issues, the code now silently recovers from almost all error conditions;
- Addition of the new [Quiet Time](#quiet-time-sa) Helper SmartApp, to automatically turn off/adjust HVAC settings when a specified (real or virtual) switch is turned off or on.
- Enhanced Thermostat UI, with the addition of
  - Sliders switched from horizontal to vertical orientation, to reduce screen space and make room for 2 more sliders (humidity setpoint and dehumidity setpoint)
  - Now clearly displays the heat/cool setpoint offsets, the Heat/Cool At and the Heat/Cool To setpoints, as well as the heat/cool minimum differential so that it is clearer how each number is applied.
- A cornucopia of internal performance and UI display optimizations to improve both initial installation and operational performance and improve the overall User Experience (UX).

-------------------------
# <a name="installation">Installation</a>

## General

It is highly recommended that you use the GitHub Integration that SmartThings offers with their IDE. This will make it **much** easier for you to keep up to date with changes over time. For the general steps needed for setting up GitHub IDE integration, please visit <http://docs.smartthings.com/en/latest/tools-and-ide/github-integration.html> and follow the steps for performing the setup.

## <a name="upgrading">Upgrading from Prior Releases</a>

### First time users of Ecobee Suite
Please proceed with the [installation instructions](#install-prep) below.

### Users of version 1.4.\*

If you are running my Ecobee Suite version 1.4.\*, you can simply install version 1.5.* on top of your existing release, just like you would any minor release of 1.4.\*. So, you too can Proceed with the [installation instructions](#install-prep) below.

### Releases prior to 1.4.\*
Since both the namespace and the name of the SmartApps & Devices has changed in 1.4 version, users of earlier versions will find that this release will install *alongside* instead of replacing their current installation. This means that you will be creating a completely new set of thermostat and sensor devices in SmartThings. And while you probably do not want to run two completely separate-but-similar devices, you may want to keep the old one around until you are satisfied that you have recreated your entire setup (including Helper SmartApps) within this new Suite.

There is **one key recommendation** if you want to run both an old and this new version: you should first change the polling frequency of your existing installation to 5 minutes or longer. Both versions will be polling the same API from your username, and excess polling may cause undesired timeouts in one or both instances.

Proceed with the [installation instructions](#install-prep) below, then return here.

Once you have successfully recreated your devices and supporting Helper SmartApps in the new Ecobee Suite, you can then proceed with removing your old support (should you choose to do so). It is **extremely IMPORTANT** that you follow these steps to remove your old implementation - *failure to do so may require you to solicit assistance from SmartThings Support to completely remove the old support.*

1. Using the old Ecobee (Connect) on your mobile device, remove all of the Helper SmartApps, one by one, and then exit out of Ecobee (Connect)
2. Select each of your old Sensor devices one by one, then tap the "SmartApps" tab to verify that each Sensor is not used by any existing SmartApps. If they are, remove them from these SmartApps (you'll probably want to replace them with the "new" Sensor devices)
3. Back in the old Ecobee (Connect), go to **Sensors** page, and de-select ALL of the sensors listed there. Save/Done back out of Ecobee (Connect), and all of the Sensor devices *should* be deleted automatically. Don't worry if they're not, though...it doesn't always work, but you can still proceed...
4. Select each of your old Thermostat devices, one by one, then tap the "SmartApps" tab, and verify that these also are not used by any SmartApps.
5. Back into Ecobee (Connect), and this time select the **Thermostats** page, de-select ALL of the thermostats listed there, and exit back out of Ecobee (Connect).
6. Finally, back into Ecobee (Connect) one last time, scroll down and go to the **Remove this instance** page, tap the big red Remove button and confirm. 

At this point, you should have totally removed all the old support, and (hopefully) have a fully functional Ecobee Suite v1.3.* installation.

If any devices or SmartApps fail to remove ***after following the above steps***, you can try to manually go into each device/SmartApp on your movile and remove them from within the DTH and/or SmartApp itself. You can also try to manually delete them from within your IDE. If neither of these work, you'll probably have to [email SmartThings Support](mailto://support@smartthings.com).

## <a name="install-prep">Install Preparation</a>

If you are not familiar with adding your own custom devices, then be sure to familiarize yourself with the [SmartThings IDE](https://graph.api.smartthings.com/) before you begin the installation process.

You will also need to make sure that you have your Ecobee username and password handy. You should login to <http://www.ecobee.com/> now to ensure you have your credentials.

## <a name="github-install">Installing Using GitHub Integration (Recommended Method)</a>
If this is your first time installing this new Ecobee Suite (versions 1.3.* and later) first follow these steps to link your IDE with the new Ecobee Suite repository (note: this is a new repository, different from the one used for prior versions - everyone will be required to perform these steps once to get to the new version).
1. Login to the IDE at [ide.smartthings.com](http://ide.smartthings.com)
2. Click on **`My Locations`** at the top of the page
3. Click on the name of the location that you want to install to
4. Click on the **`My Device Handlers`** tab
5.  Click **`Settings`**
6.  Click **`Add new repository`** and use the following parameters:
    - Owner: **`SANdood`**
    - Name: **`Ecobee-Suite`** *(note that the hyphen is required)*
    - Branch: **`master`**
7. Click **`Save`**

### <a name="github-devices">Installing Ecobee Suite Device Handlers</a>
Once the Ecobee Suite repository is connected to your IDE, use the GitHub integration to install the current version into your workspace. In the IDE:

8. Click **`Update from Repo`** and select the **`Ecobee-Suite`** repository we just added
9. Find and Select **`ecobee-suite-sensor.groovy`** and **`ecobee-suite-thermostat.groovy`**
10. Select **`Publish`**(bottom right of screen near the **`Cancel`** button)
11. Click **`Execute Update`**
	- Note the response at the top of the **`My Devices Handlers`** page. It should be something like "**`Updated 0 devices and created 2 new devices, 2 published`**"
	- Verify that the two devices show up in the list and are marked with Status **`Published`** (NOTE: You may have to reload the **`My Device Handlers`** screen for the devices to show up properly.)

Once completed, you can delete the OLD device handlers if you prefer. To do so, you can select the Edit Properties icon next to each device and select Delete.

### <a name="github-smartapps">Installing Ecobee Suite SmartApps</a>
Once you have both of the Ecobee Suite Device Handlers added and published in your IDE, it is time to add the Ecobee Suite SmartApps.

12. Click on the **`My SmartApps`** tab
13. Click **`Update from Repo`** and select the **`**Ecobee-Suite**`** repository we added earlier
14. Select the checkboxes next to **`ecobee-suite-manager.groovy`**, **`ecobee-suite-routines.groovy`**, **`ecobee-suite-open-contacts.groovy`**, **`ecobee-suite-smart-circulation.groovy`**, **`ecobee-suite-quiet-time.groovy`**, **`ecobee-suite-smart-mode.groovy`**, **`ecobee-suite-smart-room.groovy`**, **`ecobee-suite-smart-switches.groovy`**, **`ecobee-suite-smart-vents.groovy`**, and **`ecobee-suite-smart-zones.groovy`** (all 10 SmartApps listed)
15. Select **`Publish`**(bottom right of screen near the `Cancel` button)
16. Click **`Execute Update`**
	- Again, note the response at the top of the My SmartApps page. It should be something like "**`Updated 0 and created 10 SmartApps, 10 published`**"
	- Verify that all 10 of the SmartApps show up in the list and are marked with Status **`Published`**

### <a name="github-oauth">Enabling oAuth for `Ecobee Suite Manager`</a>
Finally, we must enable oAuth for the connector SmartApp as follows:

**NOTE: This is the most commonly missed set of steps, but failing to enable OAuth will generate cryptic errors later when you try to use the SmartApp. *So please don't skip these steps.***

17. Locate the **`Ecobee Suite Manager`** SmartApp from the list and Click on the **`Edit Properties`** button to the left of the SmartApp that we just added (looks like pencil on a paper)
18. Click on the **`oAuth`** tab (
19. Click **`Enable oAuth in Smart App`**
20. Click **`Update`** (bottom left of screen)
21. Verify that **`Updated SmartApp`** appears at the top of the screen

That's it - you are now all set to skip down to [install the Ecobee Suite from your mobile device](#install-mobile).

**REMINDER:** The entire Ecobee Suite is installed using the Ecobee Suite Manager SmartApp. You should not attempt to install the individual devices or SmartApps directly - this will undoubtedly result in a failed installation, and you may require assistance from SmartThings support to recover if you don't follow these instructions.

Once completed, you can delete the OLD SmartApps if you prefer. To do so, you can select the Edit Properties icon next to each of the old SmartApps (the ones NOT in the "sandood" namespace) and select Delete.

## <a name="manual-install">Installing Manually from Code</a>
For this method it is recommended that you have one browser window open on GitHub and another on the IDE. If you have used the GitHub installation method above, you can skip this section and [proceed to completing the installation on your mobile device](#install-mobile).

### <a name="manual-devices">Installing Ecobee Suite Device Handlers Manually</a>
Follow these steps to install the **`Ecobee Sensor`** Device Handler:
1. Login to the IDE at [ide.smartthings.com](http://ide.smartthings.com)
2. [IDE] Click on **`My Locations`** at the top of the page
3. [IDE]Click on the name of the location that you want to install to
4. [IDE]Click on the **`My Device Handlers`** tab
5. [IDE] Click **`New Device Type`** (top right corner)
6. [IDE] Click **`From Code`**
7. [GitHub] Go to the [raw source code for the Ecobee Sensor](https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/devicetypes/sandood/ecobee-suite-sensor.src/ecobee-suite-sensor.groovy) *(click the hyperlink)*
8. [GitHub] Select all of the text in the window (use Ctrl-A if using Windows)
9. [GitHub] Copy all of the selected text to the Clipboard (use Ctrl-C if using Windows)
10. [IDE] Click inside the text box
11. [IDE] Paste all of the previously copied text (use Ctrl-V if using Windows)
12. [IDE] Click **`Create`**
13. [IDE] Click **`Save`**
14. [IDE] Click **`Publish`** --> **`For Me`**

Follow these steps to install the **`Ecobee Thermostat`** Device Handler:
15. [IDE] Click on the **`My Device Handlers`** tab
16. [IDE] Click **`New Device Type`** (top right corner)
17. [IDE] Click **`From Code`**
18. [GitHub] Go to the [raw source code for the Ecobee Thermostat](https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/devicetypes/sandood/ecobee-suite-thermostat.src/ecobee-suite-thermostat.groovy) *(click the hyperlink)
19. [GitHub] Select all of the text in the window (use Ctrl-A if using Windows)
20. [GitHub] Copy all of the selected text to the Clipboard (use Ctrl-C if using Windows)
21. [IDE] Click inside the text box
22. [IDE] Paste all of the previously copied text (use Ctrl-V if using Windows)
23. [IDE] Click **`Create`**
24. [IDE] Click **`Save`**
25. [IDE] Click **`Publish`** --> **`For Me`**

Once completed, you can delete the OLD device handlers if you prefer. To do so, you can select the Edit Properties icon next to each device and select Delete.

### <a name="manual-manager">Installing `Ecobee Suite Manager` Manually from Code</a>
Again, it is recommended to have one browser window open on GitHub and another on the IDE.

Follow these steps to add the **`Ecobee Suite Manager`** SmartApp to your IDE:
1. [IDE] Click on the **`My SmartApps`** tab
2. [IDE] Click **`New SmartApp`** (top right corner)
3. [IDE] Click **`From Code`**
4. [GitHub] Go to the [raw source code for the Ecobee Suite Manager SmartApp](https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-manager.src/ecobee-suite-manager.groovy)
6. [GitHub] Select all of the text in the window (use Ctrl-A if using Windows)
7. [GitHub] Copy all of the selected text to the Clipboard (use Ctrl-C if using Windows)
8. [IDE] Click inside the text box
9. [IDE] Paste all of the previously copied text (use Ctrl-V if using Windows)
10. [IDE] Click **`Create`**
11. [IDE] Click **`Save`**
12. [IDE] Click **`Publish`** --> **`For Me`**

### <a name="manual-oauth">Enabling oAuth for `Ecobee Suite Manager`</a>
Finally, we must enable oAuth for the connector SmartApp as follows:

**NOTE: This is the most commonly missed set of steps, but failing to enable OAuth will generate cryptic errors later when you try to use the SmartApp. *So please don't skip these steps.***

13. [IDE] Click on the **`My SmartApps`** tab
14. [IDE] Verify that the SmartApp shows up in the list and is marked with Status `Published`
15. [IDE] Click on the **`Edit Properties`** button to the left of the SmartApp that we just added (looks like pencil on a paper)
16. [IDE] Click on the **`oAuth`** tab
17. [IDE] Click **`Enable oAuth in Smart App`**
18. [IDE] Click **`Update`** (bottom left of screen)
19. [IDE] Verify that **`Updated SmartApp`** appears at the top of the screen

### <a name="manual-helpers">Installing the Ecobee Suite Helper SmartApps Manually from Code</a>
Follow these steps to add the Helper SmartApps to your IDE, one at a time (links to Raw Source Code are provided below):

**NOTE:** It is mandatory that you add ALL of the available Helper SmartApps, or **`Ecobee Suite Manager`** will fail with an error.
20. [IDE] Click on the **`My SmartApps`** tab
21. [IDE] Click **`New SmartApp`** (top right corner)
22. [IDE] Click **`From Code`**
23. [GitHub] Go to the raw source code for each of the Helper SmartApps, one at a time (links provided below)
24. [GitHub] Select all of the text in the window (use Ctrl-A if using Windows)
25. [GitHub] Copy all of the selected text to the Clipboard (use Ctrl-C if using Windows)
26. [IDE] Click inside the text box
27. [IDE] Paste all of the previously copied text (use Ctrl-V if using Windows)
28. [IDE] Click **`Create`**
29. [IDE] Click **`Save`**
30. [IDE] Click **`Publish`** --> **`For Me`** (Optional)
31. [IDE] Click on the **`My SmartApps`** tab
32. [IDE] Verify that the SmartApp shows up in the list

Repeat the above steps (20-32) for the each of the available Helper SmartApps (click each link for the raw source code):
A. [`ecobee Suite Open Contacts`](https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-open-contacts.src/ecobee-suite-open-contacts.groovy)
B. [`ecobee Suite Routines`](https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-quiet-time.src/ecobee-suite-quiet-time.groovy)
C. [`ecobee Suite Routines`](https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-routines.src/ecobee-suite-routines.groovy) 
D. [`ecobee Suite Smart Circulation`](https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-smart-circulation.src/ecobee-suite-smart-circulation.groovy) 
E. [`ecobee Suite Smart Mode`](https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-smart-mode.src/ecobee-suite-smart-mode.groovy) 
F. [`ecobee Suite Smart Room`](https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-smart-room.src/ecobee-suite-smart-room.groovy)
G. [`ecobee Suite Smart Switches`](https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-smart-switches.src/ecobee-suite-smart-switches.groovy)
H. [`ecobee Suite Smart Vents`](https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-smart-vents.src/ecobee-suite-smart-vents.groovy)
I. [`ecobee Suite Smart Zones`](https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/smartapps/sandood/ecobee-suite-smart-zones.src/ecobee-suite-smart-zones.groovy)

Once completed, you can delete the OLD SmartApps (the ones that appear under the `SmartThings` namespace) if you prefer. To do so, you can select the Edit Properties icon next to each of the old SmartApps (the ones NOT in the "sandood" namespace) and select Delete.

## <a name="install-mobile">Finalizing Installation on SmartThings Mobile</a>

The final steps for installing the Ecobee Suite Device Handlers and SmartApps is completed entirely using the Ecobee Suite Manager SmartApp. The SmartApp will guide you through the basic installation and setup process. It includes the following aspects:
- Authentication with Ecobee to allow API Calls for your thermostat(s) (and connected sensors)
- Discovery, selection and creation of Thermostat devices
- Discovery, selection and creation of Remote Sensor devices (if there are any)
- Setup of optional features/parameters such as Smart Auto Temp Control, Polling Intervals, etc
- Installation and configuration of Helper SmartApps (as desired)

Follow these steps for the SmartApp on your mobile device:
1. Open the SmartThings app and login to your Location
2. Open the **`Marketplace`** tab
3. Open the **`SmartApps`** tab
4. Scroll down and select **`My Apps`** (at the bottom of the list)
5. Scroll to open the **`Ecobee Suite Manager`** SmartApp.
(**NOTE:** If you see "Ecobee (Connect)" in this list, you did not remove it from your IDE. This is not a problem - you can use both. Just remember that this new Ecobee Suite uses **`Ecobee Suite Manager`**, and not the old version.
6. Select the Ecobee API Authorization page (as indicated on the screen) and then the ecobee Account Authorization page to enter your Ecobee Credentials
7. Enter your Ecobee Email and Password, and then press the green LOG IN button
8. On the next page, Click **`Accept`** to allow SmartThings to connect to your Ecobee account
9. You should receive a message indicating **`Your ecobee Account is now connected to SmartThings!`**
10. Click **`Done`** (or **`Next`** depending on your device OS)
11. Click **`Done`** (or **`Next`** depending on your device OS) again to save the credentials and exit out of Ecobee Suite Manager 
You should receive a small green popup at the top stating "**`Ecobee Suite Manager is now installed and automating`**"
12. In the SmartThings mobile app, go to the **`Automation`** screen and select the **`SmartApps`** tab
13. Select the the **`Ecobee Suite Manager`** SmartApp that you just installed
14. Select the Ecobee Thermostat devices you want to connect from your account
15. Select the Ecobee Sensors you want to connect (if any)
(NOTE: The options are dynamic and will change/appear based on other selections. E.g., you won't see any Thermostats to select if your Ecobee account login failed; you won't see any sensors to choose until you select the thermostats they are connected to).
16. You can also go into the **`Preferences`** section to set various preferences such as **`Hold Type`**, **`Smart Auto Temperature`**, **`Polling Interval`**, **`Debug Level`**, **`Decimal Precision`**, and whether to create separate sensor objects for thermostats.]  - 
17. After making all selections, Click **`Done`** to save your preferences and exit the SmartApp

At this point, the SmartApp will automatically create all of the new devices, one for each thermostat and sensor. These will show up in your regular **`Things`** list within the app. 

Using the default settings is fine, but some of the more advanced features will require you to change the default settings for Ecobee Suite. My recommended settings are:
- **Hold Type:** If not specified, this will default to the setting on the thermostat itself. Some of the helper SmartApps allow you to customize this for specific operations.
- **Polling Interval:** 1 minute if you are using Helper SmartApps to react to thermostat conditions (like changing to heating/cooling, or running a specific Program/Climate); otherwise 2-5 minutes might be sufficient.
- **Decimal Precision:** One of the reasons I created this in the first place is to get more precision out of the thermostat. If you set this to 1 decimal position, you'll understand better how the thermostat is reacting to your environment, and when "72°" is really "72.4°".
- **Debug Level:** I tend to run with this set to 2 because I've optimized this level to provide the most basic of operational status in the Live Logging monitor.
- 

> **NOTE 1**: It may take a few minutes for the new devices to show up in the list and for them to populate their displays. You should try refreshing the list if they are not there (pull down on the list). In extreme cases, you may have to restart the SmartThings app on your mobile device to update the list. You should only have to do this once.<br/>

> **NOTE 2**: If you uninstall the SmartApp it will automatically remove all of the thermostats and sensors that it previously installed. This is necessary (and expected) as those devices are "children" of the SmartApp.<br/>


-------------------------
## <a name="updating">Updating the Code</a>
If you have enabled GitHub integration with the SmartThings IDE, then updates are a breeze. Otherwise the steps are a bit more manual but not too complicated.

### <a name="github-updates">Updating with GitHub Integration</a>
The IDE provides visual cues to alert you that any device types or SmartApps have been updated in their upstream repositories. See the [GitHub/IDE integration guide](http://docs.smartthings.com/en/latest/tools-and-ide/github-integration.html) for more details on the different colors.

Once you have determined that an update is available, follow these steps:
1. Login to the SmartThings IDE
2. Go to either the **`My Device Handlers`** or **`My SmartApps`** tabs to see if there are updates (the color of the item will be purple)
3. Click the **`Update from Repo`** button (top right)
4. Select the **`Ecobee-Suite`** repository
5. Select ALL of items in the **`Obsolete (updated in GitHub)`** column
6. Select **`Publish`** (bottom right)
7. Click **`Execute Update`** (bottom right)
	- You should receive a confirmation message such as this example: **`Updated 1 and created 0 SmartApps, 1 published`**
	- (Optional, but <b>HIGHLY Recommended</b>) Rerun the **`Ecobee Suite Manager`** SmartApp. This seems to alleviate any residual issues that may occur due to the update

You should now be running on the updated code. Be sure that you check for both updates of the SmartApp **and** the Device Type. Updating one but not the other could cause compatibility problems.

### <a name="manual-updates">Updating manually</a>

To update manually, you will need to "cut & paste" the raw code from GitHub into the SmartThings IDE, Save and Publish the code. I will leave it to the reader to work through the full individual steps, but the links to the code are the same as those that were used during the initial install process.

Note that there are EIGHT SmartApps and TWO Device Handlers in this suite. For proper operation, you will need to copy/paste ALL 10 of these files into your IDE, even if you aren't going to use them all. At a minimum, you should also publish the Ecobee Suite Manager SmartApp and the two Device Handlers.

--------------------------
## <a name="features">Features</a>
### <a name="general">General</a>
This collection of SmartApps and Device Handlers has been designed for simple installation, flexibile configuration options and easy operation. It is also extensible through the use of Child SmartApps that can easily be added to the configuration. **And it fully implements the related [SmartThings Capabilities](http://docs.smartthings.com/en/latest/capabilities-reference.html).**

Key Highlights include:
- **Open Source Implementation!** Free as in beer AND speech. No donations or purchase needed to use (but if you insist, see [donations](#donations).
- Single installation SmartApp, **`Ecobee Suite Manager`** used for installing both Thermostats **and** Sensors. No need for multiple apps just for installation! In fact, the **`Ecobee Suite Manager`** SmartApp is the only SmartApp interface you'll need to access all available functions, including those provided by Child SmartApps (if installed).
- Sophisticated User Interface: Uses custom Ecobee icons throughout the design to provide a more polished look and feel.
- Display of current weather with support for separate day and night icons (just like on the front of your Ecobee thermostat)!
- Robust watchdog handling to minimize API Connectivity issues, but also includes an API Status Tile to quickly identify if there is an ongoing problem. No more guessing if you are still connected or not.
- Included Helper SmartApp (**`ecobee Suite Routines`**) for automating thermostat settings based on SmartThings Hello Modes being activated (such as through a Routine), and/or automating SmartThings Modes/Routines based on Ecobee Thermostat Program changes (including Vacations).
- Additional Smart Helper Apps to automate ciculation time, coordinate fan-on time between multiple zones, and automate a "Smart Room."
- Full support for both Fahrenheit and Celsius

### <a name="features-therm-ui">Thermostat and Sensor Device User Interfaces</a>
The primary user interface on a day-to-day basis will be two different types of Device Handlers that are shown on the **`Things`** list under **`My Home`** in the mobile app. 

#### <a name="thermostat">Thermostat UI</a>
  - The main MultiAttributeTile tile now reflects additional information
    - Background colors match Ecobee3 thermostat colors for Idle (magenta), Heat (orange) & Cool (blue), and Fan Only (green) and Offline (red) are added
    - Displays (Smart Recovery) when heating/cooling in advance of a Program change
    - Displays the setpoint temperature that will initiate a heat/cool demand while idle, and the actual target temp while heating/cooling. 
      - On iOS, the call-for-heat temperature will be indicated by "Heating **at** XX.X°" in the large tile (while idle), and the target temperature (while heating) "Heating **to** YY.Y°". Same for Cool and Auto modes
      - Android mistakenly says "Heating **at** XX.X" for both modes; bug reports have been filed to get this corrected (again).
    - All temperatures can be configured to display in 0, 1 or 2 decimal positions (with appropriate rounding)
  - Most icons are dynamic and in full color
    - New Operating State icons reflect current stage for multi-stage Heat or Cool, plus humidification/dehumidification (if configured)
    - Hold: Program icons are now "filled" when in a Hold event so you can recognize the Hold at a glance
    - Buttons that are inactive due to the current state/mode/program are greyed out
    - Display-only buttons do not invoke any actions
  - Dynamic "Resume" button
    - Normally displays as Resume (Program) button, to override a current Hold: event
    - While thermostat is in a Vacation event, the button becomes 'Cancel" to allow the cancelation of the Vacation
  - Hold/Vacation Status display
    - Displays when current Hold or Vacation ends
    - If the thermostat loses its connection to the Ecobee Cloud, displays the time that the last valid update was recieved
  - Default Hold handling
    - In addition to Permanent and Temporary holds, now supports hourly holds: 2 hours, 4 hours or Custom Hours.
    - The default hold type can be configured in Ecobee Suite Manager or for each individual thermostat in Thermostat Preferences.
    - Also supports using the thermostat's preference setting (askMe is not currently supported)
  - New Refresh actions
    - If pressed once, will only request the latest updates from the Ecobee Cloud
    - If pressed a second time within a few seconds of the first press completing, will force a full update from the Ecobee Cloud
   
#### <a neme="sensor">Sensor UI</a>
  -	A new multiAttributeTile replaces the old presentation, with motion displayed at the bottom left corner
  - Now displays the parent thermostat current Program within each Sensor device
  - New mini-icons indicate which of the 3 default programs (Home, Away, Sleep) the sensor is included in
    - The sensor can be added or removed from a Program by tapping these mini icons
  - Includes 4 new blank mini-tiles that are utilized by the new Smart Room Helper App
	
#### <a name="smartthings">SmartThings Integration</a>
  - Messages sent to the Device notification log (found under the Recently tab of a Device) are now optimized, most with associated colored icons 
  - All current Attributes and Capabilities of Thermostat devices are supported
  - Most Ecobee Settings Object variables are are now available as Attributes of a Thermostat (so things like WebCoRE can see and use them) 
  - Supports the latest updated Thermostat Attributes released by SmartThings on July 7, 2017
  - Now offers several new API Command interfaces - see **`ecobee-thermostat.groovy`** for specifics. These include new API commands to:
    - Add a sensor to a Program
    - Delete/Remove a sensor from a Program
    - Delete the current Vacation
    - Change the minimum fan on time for both the current running Program and current Vacation event

#### <a name="operational">Operational Enhancements</a>

  - **Operational Efficiency**
    - Designed to do only lightweight polls of the Ecobee Cloud before requesting updates
    - If updates are available, then only requests those updated objects, and only for the thermostats with updates
    - From the updated Ecobee objects, only the data that has changed is sent to the individual devices
    - As a result of the above, it is possible to run with polling Frequency less than the recommended 3 minutes (as low as 1 minute)
<br>

  - **Timeout Handling**
Releases prior to 1.3.0 did not handle connection timeouts to the Ecobee servers very well. That is a gross understatement, because the "handling" was limited to sending notifications every hour nagging that the user needed to reauthorize with Ecobee.<br>
Beginning with 1.3.0, this Ecobee Suite will silently retry connection timeouts without sending any notifications (it will report warnings in Live Logging). Usually such timeouts are transient, and the side effect is only a delayed update. For lengthy outages, the code *should automatically reconnect* once the SmartThings to Ecobee linkage is restored.<br>
Thus, if you receive a notification that you need to re-authorize, you probably really do need to reauthorize.

  - **Stacked Holds**
Ecobee devices support a concent of "stacked" holds - each hold operation can be stacked on top of a prior hold, such that Resuming the current hold merely "pops the stack" to the prior hold. This is a very difficult concept to manage across multiple interfaces, because it is difficult to depict what the result of a "Resume" operation will be at any given point in time.<br>
This implementation avoids the complexity by supporting only a single depth of Hold: events - whenever you execute a Resume you reset the thermostat to run the currently scheduled Program.
        
<b>Ask Alexa Message Queue Support</b>

 - For users of Michael Struck's most awesome Ask Alexa integration for SmartThings <http://thingsthataresmart.wiki/index.php?title=Ask_Alexa>
 	- you can now configure your Ecobee Alerts and Reminders to be sent to Ask Alexa Message Queues. 
 	- This is enabled in a new Ask Alexa preferences page in Ecobee Suite Manager where you can specify the target Message Queue(s). 
	- You can also specify an expiration time for Alerts so that they are removed from Ask Alexa after a specified period of time.

NOTE: You will not be able to configure Ask Alexa support until you have fully installed Ask Alexa <b>AND</b> created at least 1 custom Message Queue, as the Primary Queue is being deprecated.

- The current implementation does not support acknowledging of messages directly via Ask Alexa, but when the Alert/Reminder is acknowledged (on the thermostat or in the Ecobee application/web site) they are also removed from the Ask Alexa Message Queue.

- This feature will evolve over the coming months to include more control over what gets sent to Ask Alexa and to support Notifications Only. So stay tuned!

## <a name="screenshots">Screenshots</a> 


Ecobee Thermostat v1.5.0 |  Ecobee Thermostat Device v1.4.0 w/Annotation
:-------------------------:|:-------------------------:
<img src="https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/images/v1.5.0%20Tstat.png" border="1" height="1200" /> | <img src="https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/images/v1.3.0-Tstat-annotated.png" border="1" height="1200" />

Ecobee Sensor Device |  <sp> 
:-------------------------:|:-------------------------:
<img src="https://raw.githubusercontent.com/SANdood/Ecobee-Suite/master/images/v1.3.0-sensor.jpg" border="1" width="295" /> | 

### Annotation Notes
- **Button** Executes labeled action/state when pressed (unless grey)|
- **Cycler** Displays current state, toggles to next state when pressed|
- **Slider** Opens SmartThings Slider control (limited to whole numbers only)|
- **Current Temperature** Decimal value of current Temperature display on Thermostat (note: may be an average of the associated sensors)
- **Up / Down Setpoint buttons** will be slow to respond, but will not actually change the setpoint until 5 seconds after last button pressed (configurable delay in Ecobee Suite (Control) Preferences). Operates in 1° increments in Fahrenheit, and 0.5° increments in Celsius
- **Thermostat Operating State** describes the current activity, or the temperature at which a demand for heat/cool will be made (heating/cooling/fan only/idle/pending heat/pending cool)
- **Refresh Button** Tap once to request a changes-only poll to the Ecobee API, tap again immeditately after the "Bee" icon reappears to force a poll of ***all*** the Ecobee API
- **Equipment Operating State** more detailed state of operations, including heat/cool stages, humidifier/dehumidifier, Smart Recovery, emergency/aux heat, etc.
- **Hold/Vacation Status** displays when current hold or vacation is scheduled to end. Occaisionally reports other status of interest
- **Thermostat Date & Time** date and time as of the last completed poll of the Ecobee API
- **Command Center** Most every tile below this is active, unless the tile is greyed out. Tiles are greyed if the action is not currently allowed because of some state (e.g., cannot change the Program when the thermostat is in Vacation mode).


## <a name="features-manager-sa">`Ecobee Suite Manager` SmartApp</a>
The **`Ecobee Suite Manager`** SmartApp provides a single SmartApp interface for accessing installation, updates, Child SmartApps and even debugging tools. The interface uses dynamic pages to guide the user through the installation process in order simplify the steps as much as possible.

The SmartApp provides the following capabilities:
- Perform Ecobee API Authorization (OAuth)
- Select Thermostats from account to use (dynamic list, so any future Thermostats can easily be added at a later date)
- Select Sensors to use (dynamic list, will only show sensors associated with the previously selected Thermostats)
- Access Child SmartApps (such as **`ecobee Suite Routines`**)
- Set various Preferences:
  - Set default Hold Type ("Until Next Program", "Until I Change", "2 Hours", "4 Hours", "Specified Hours" and "Thermostat Default")
  - Allow changes to temperature setpoint via arrows when in auto mode ("Smart Auto Temperature Adjust")
  - Polling Interval
  - Debugging Level
  - Adjustable Decimal precision for displaying temperatures
  - Include Thermostats as a separate Ecobee Sensor (useful in order to expose the true temperature reading and not just the average temperature shown on the thermostat, e.g. for Smart Vent input)
  - Monitor external devices to drive additional polling and watchdog events
  - Delay timer value after pressing setpoint arrows (to allow multiple arror presses before calling the Ecobee APIs)
- Set Ask Alexa Message Queue integration preferences
- Select Polling and Watchdog Devices (if enabled in Preferences)
- Debug Dashboard (if Debug Level is set to 5)


## <a name="features-opencontact-sa">`Contacts & Switches` Helper</a> *Updated!*
The `ecobee Suite Open Contacts` SmartApp can detect when one (or more) contact sensors (such as doors and windows) are left open for a configurable amount of time and can automatically turn off the HVAC and/or send a notification when it is detected. 

This Helper also supports switches/dimmers/vents, with configurable selection of whether contact open or close, switch on or off will turn off the HVAC. This enhancement allows (for example) automatically turning off the HVAC when the attic fan is running.

Beginning with Ecobee Suite release 1.5.*, this Helper now also integrates with the new Quiet Time Helper SmartApp - instead of turning off the HVAC, this Helper can turn on/off the Virtual Switch that a Quiet Time Helper instance uses as its trigger. Quiet Time offers additional flexibility over the actions taken on the HVAC system (see documentation below).

<b>Features:</b>
- Change one or multiple thermostats
- Trigger based on one or multiple contact sensors and/or switches
- Configurable delay timers (for trigger and reset)
- Configurable actions: Notify Only, HVAC Only or Both
- Support for Contact Book or simply SMS for notifications
- **New!** Integrates with the Quiet Time Helper (via Virtual Switch) 
- Temporarily Disable app without having to delete and recreate!


## <a name="features-quiet-time-sa">`Quiet Time` Helper</a> *New!*
The `ecobee Suite Quiet Time` Helper SmartApp allows you to automatically turn off or change some or all of the following HVAC system settings, based on the (configurable) off/on state of a specific (real or virtual) Switch:
- Turn Off HVAC altogether
- Change the HVAC Mode (auto, cool, heat)
- Turn off the HVAC Fan
- Adjust the heating/cooling setpoints
- Turn off the HVAC's humidifier
- Turn off the HVAC's dehumidifier

When the aforementioned Quiet Time switch state changes back to (on/off), some or all of the above changes are reverted.

#### <a name="qt-virtual-switch">Using a Virtual Switch with Quiet Time</a>
While `Quiet Time` can be triggered by any SmartThings Switch device, it is perhaps most useful to trigger it using a Virtual Switch. SmartThings now provides a standard `Virtual Device Creator` in the **SmartApps Marketplace**, int the "+ More" section. So you simply need to install and run this to create a Virtual Switch (I suggest naming it `Quiet Time`). Then simply configure this Helper to invoke Quiet Time actions when the virtual switch is turned on, and you're all set. Then add turning on this Virtual Switch to your Watch TV routine, or as an action for your SmartThings-integrated Harmony remote.

If you have Alexa (or Google Home), you even say "Alexa, turn on Quiet Time" and smile when your HVAC shifts into peaceful existence!

N.B., The use case for this new Helper came from a user whose HVAC system is rather noisy when the humidifier runs - enough so that it was hard to hear the TV on "movie night." He asked for the ability to automatically turn off the humidifier when the TV was turned on; from that was born the notion of this new Quiet Time Helper (and the ability to control the humidifier/dehumidifier). I'm sure there are several other use cases that can leverage `Quiet Time.`

## <a name="features-routines-sa">`Mode/Routine/Program` Helper</a>
The `ecobee Suite Routines` Helper SmartApp provides the ability to change the running Program (Comfort Setting) when a SmartThings Mode is changed (for example, by running a Routine) or a Routine is run. 

<b>Features:</b>
- Change one or multiple thermostats
- Trigger based on SmartThings Location Mode Change or SmartThings Routine Execution
  - Choose any (including custom) Ecobee Programs to switch to. Or can even choose to Resume Program instead
  - Change the Fan Mode (Optional)
  - Set the Fan Minimum Runtime (Optional)
  - Disable a Vacation hold (Optional)
- Trigger based on Program Change on one or more thermostats
  - Change the Location Mode or execute a Routine when the thermostat Program Changes - including Vacations!
- Temporarily Disable app without having to delete and recreate!



## <a name="features-smart-circ-sa">`Smart Circulation` Helper</a> *Updated!*
The `Ecobee Suite Smart Ciculation` Helper SmartApp will adjust fan mininum on time of an Ecobee thermostat as it tries to get two or more temperature sensors to converge.

Beginning with Ecobee Suite release 1.5.*, Smart Circulation can be configured to disable circulation altogether (sets `fanMinOnTime` to 0) when the specified (Quiet Time) (real or virtual) Switch is turned off/on. If you want Quiet Time to really stop everything, then you need to enable this feature, otherwise Smart Circulation will override Quiet Time by setting the minimum on time to a non-zero number.

<b>Features:</b>
- Monitor/synchronize two or more temperature sensors (Ecobee or any SmartThings temperature sensor)
- Configurable maximum/target temperature delta
- Configurable minimum fan minutes on per hour
- Configurable maximum fan minutes on per hour
- Configurable minutes +/- per adjustment
- Configurable adjustment frequency (in minutes)
- Option to override fan minutes per hour during Vacation hold
- Enable during specific Location Modes or when thermostat is in specific Program
- Temporarily Disable app without having to delete and recreate!

## <a name="smart-mode-sa">`Smart Mode` Helper</a>
This (new) Helper will automatically change Ecobee Thermostats' Mode based on external temperature changes. Generally, the intention is to run Cool or Heat only modes when the outside temperatures might otherwise cause cycling between Heat/Cool if "Auto" were selected. Can also be used to turn the HVAC off when outdoor temperatures warrant leaving the windows open.

**Features**
* Choose from only the Modes that are configured on the thermostat (Auto, Aux_Heat, Cool, Heat, Off) 
* Choose from 4 possible temperature sources:
  * Zip Code (SmartThings Location or manually specified Zip Code) - uses SmartThings embedded support for WeatherUnderground data
  * Any SmartThings Temperature Sensor
  * The Ecobee-supplied outdoor Weather Temperature (note, this is notoriously bad data for most people, unless you live within a couple of miles of the weather sources Ecobee uses)
  * A nearby WeatherUnderground Personal Weather Station (pws) (the app will help locate nearby pws's)
* Optionally deliver Notifications whenever the thermostat(s) mode is changed
* Use a single instance (below, above and between temperatures), or create multiple instances

Many thanks to @JustinL for the original idea, the starting code base, and beta testing for this new Smart Mode Helper.

## <a name="features-smart-room-sa">`Smart Room` Helper</a>
The `ecobee Suite Smart Room` Helper SmartApp will automate a normally unused room, Activating it one or more of the Doors associated with the Smart Room are left open for a configurable number of minutes, and Deactivating it when all the Doors are closed for a configurable number of hours.

<b>Requirements:</b>
  - Ecobee thermostat with Sensor support
  - Ecobee Sensor in the room
  - One or more Doors, each with a contact sensor
  
<b>Optional:</b>
  - SmartThings-controlled vent running stock device drivers
    - EcoNet Vent
    - Keene Smart Vent
  - One or more Windows with contact sensor(s)
  
<b>Features:</b>
  - Specify one or more Ecobee Sensors that are in the Smart Room
    - Configure which of the 3 default ecobee Thermostat Programs (Home, Away, Sleep) the Sensor should be added to when Activated
    - Configure which of these Programs it should be added to when Deactivated
  - Specify one or more Door contact sensors to monitor
    - Configurable number of minutes a Door being open will Activate the Smart Room
    - Configurable number of hours all Doors being closed will Deactivate the Smart Room
  - Optionally specify one or more Window contact sensors to monitor
    - If open while room is Activated, temporarily Deactivate the room
  - Optionally specify additional motion sensors within the Smart Room
    - If motion is detected while a Smart Room is Activated and all the Doors are closed *and* the Deactivate monitor is disabled (monitor will be re-enabled the next time the door closes)
  - Optionally controls specified SmartThings-controlled vents
    - Open vent(s) when Smart Room is activated, and close them when Deactivated
    - Supports EcoNet and Keene vents using SmartThings stock DTH only
    - Optionally configure a minimum vent closed level (to protect over-pressure HVAC situation)
  - Optionally notify on Activations via Push, using Contact List or default Push
  - Temporarily Disable app without having to delete and recreate!
    
## <a name="features-smart-switches-sa">`Smart Switches/Dimmer/Vent` Helper</a>
`ecobee Suite Smart Switches` Helper SmartApp enables activating switches or dimmers based upon the Operating State of a thermostat.

## <a name="features-smart-vents-sa">`Smart Vents` Helper</a>
`ecobee Suite Smart Vents` automates one or more vents to reach/maintain a target temperature that you specify or from a specified thermostat. It uses one or more temperature devices, and opens/closes vents based upon current temperature, target temperature and thermostat operating state.

## <a name="features-smart-zone-sa">`Smart Zones` Helper</a> *Updated!*
`ecobee Suite Smart Zones` Provides functionality to allow one or more "slave" zones to leverage the operating state of a "master" zone. This is designed for multi-zoned HVAC systems with only a single air handler. In this configuration, running only 1 zone (e.g., to cool) can cause the other zone's temperature to increase a few 10ths of a degree. This Helper seeks to address that by turning on the Slave zones' fan, thereby allowing some of the cooled (or heated) air into the slave zone.
- Slaves can be configured to turn on the fan any time the Master thermostat is running 'fan only'. 
- Slaves can be configured to *leech* (share) the heat/cool when the Master is heating and/or cooling. Specifically,
  - If the Master is cooling and the Slave zone's current temperature is higher than the 'Cool To' setpoint but lower than the 'Cool At' setpoint, the Slave's fan will be turned on. 
  - When the Master returns to 'idle' or 'fan only', the Slave(s) will turn off their fans (return them to 'auto'). 
  - The Slave will also turn off its fan once the Slave thermostat's temperature reaches the 'Cool To' setpoint.

In most cases, the Slave will properly reset any program Holds that are in effect when it starts 'leeching'.


-------------------------
## Troubleshooting

| Symptom 	| Possible Solution 	|
|---------	|-------------------	|
| The devices are not showing up in the Things tab after installation    	|  It can take several minutes for things to show up properly. If you don't want to wait then simply kill the SmartThings app and reload it.              	|
| Receive error similar to "error java.lang.NullPointerException: Cannot get property 'authorities' on null object"        	| This indicates that you have not turned on OAuth for the SmartApp. Please review the installation instructions and complete the OAuth steps.                  	|
| "You are not authorized to perform the requested operation."        	|  This indicates that you have not turned on OAuth for the SmartApp. Please review the installation instructions and complete the OAuth steps.                 	|
| Irregular behavior after an update to the SmartApp or Device Handler code| It is possible that after updating the codebase that you may experience strange behavior, including possible app crashes. This seems to be a general issue with updates on SmartThings. Try the following steps: <br/> 1) Re-run the `Ecobee Suite Manager` SmartApp to re-initialize the devices <br/> 2) If that does not solve the problem, remove and re-install the SmartApp |
| physicalgraph.exception.ConflictException: Device still in use. Remove from any SmartApps or Dashboards, then try again @line -1 (doCall)	| The initial installation failed for some reason, and it was unable to delete the temporary devices it creates. Try removing everything that you can again, and then go to the on-line IDE. There, list your devices, and delete ANY that have “Ecobee-Suite” (or any form of that text) in their device ID. Then rerun Ecobee Suite Manager from the SmartApps Marketplace.                  	|
|         	|                   	|


### Debug Level in SmartApp
The `Ecobee Suite Manager` SmartApp allows the end user to config the Debug Level they wish to use (ranging from 1-5). The higher the level the more debug information is fed into the `Live Logging` on the SmartThings IDE.

Also, if a user chooses Debug Level 5, then a new `Debug Dashboard` will appear within the SmartApp. This dashboard gives direct access to various state information of the app as well as a few helper functions that can be used to manaually trigger actions that are normally timer based.

### Live Logging on IDE
The `Live Logging` feature on the SmartThings IDE is an essential tool in the debugging process of any issues that may be encountered.

To access the `Live Logging` feature, follow these steps:
- Go the SmartThings IDE (<https://graph.api.smartthings.com/>) and log in
- click `Live Logging`

### Installed SmartApps Info on IDE
The SmartThings IDE also provides helpful insights related to the current state of any SmartApp running on the system. To access this information, follow the follwing steps:
- Go the SmartThings IDE (<https://graph.api.smartthings.com/>) and log in
- Click `My Locations` (select your location if you have more than one)
- Scroll down and click `List SmartApps`
- Find the `Ecobee Suite Manager` SmartApp and click the link

### <a name="quicklinks">Quick Links</a>
- README.md (this file): <https://github.com/SANdood/Ecobee-Suite/blob/master/README.md>
- SmartThings IDE: <https://graph.api.smartthings.com>


----------------------------------------------

## <a name="reporting-issues">Reporting Issues</a>
All issues or feature requests should be submitted to the latest release thread on the SmartThings Community. For the major release version 1.5.0, please use this thread: https://community.smartthings.com/t/release-free-ecobee-suite-version-1-5/128146

If you have complaints, please email them to me directly at either [storageanarchy@gmail.com](mailto://storageanarchy@gmail.com) or to @storageanarchy on the SmartThings Community.

------------------------------------------------------------------

## <a name="license">License<a/>

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at:
      http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

----------------------------------------------------

## <a name="integration">Integrating With Ecobee Suite Devices</a>
The Ecobee Suite's thermostat devices can be leveraged by other SmartApps and by WebCoRE automations in a variety of ways. The provided Ecobee Thermostat DTH implements the following SmartThings capability sets completely (click each capability for the SmartThings Developer Documentation describing each capability):

- [Actuator](https://smartthings.developer.samsung.com/develop/api-ref/capabilities.html#Actuator)
- Health Check
- [Motion Sensor](https://smartthings.developer.samsung.com/develop/api-ref/capabilities.html#Motion-Sensor)
- [Refresh](https://smartthings.developer.samsung.com/develop/api-ref/capabilities.html#Refresh)
- [Relative Humidity Measurement](https://smartthings.developer.samsung.com/develop/api-ref/capabilities.html#Relative-Humidity-Measurement)
- [Sensor](https://smartthings.developer.samsung.com/develop/api-ref/capabilities.html#Sensor)
- [Temperature Measurement](https://smartthings.developer.samsung.com/develop/api-ref/capabilities.html#Temperature-Measurement)
- [Thermostat](https://smartthings.developer.samsung.com/develop/api-ref/capabilities.html#Thermostat)
- [Thermostat Cooling Setpoint](https://smartthings.developer.samsung.com/develop/api-ref/capabilities.html#Thermostat-Cooling-Setpoint)
- [Thermostat Fan Mode](https://smartthings.developer.samsung.com/develop/api-ref/capabilities.html#Thermostat-Fan-Mode)
- [Thermostat Heating Setpoint](https://smartthings.developer.samsung.com/develop/api-ref/capabilities.html#Thermostat-Heating-Setpoint)
- [Thermostat Mode](https://smartthings.developer.samsung.com/develop/api-ref/capabilities.html#Thermostat-Mode)
- [Thermostat Operating State](https://smartthings.developer.samsung.com/develop/api-ref/capabilities.html#Thermostat-Operating-State)
- [Thermostat Setpoint](https://smartthings.developer.samsung.com/develop/api-ref/capabilities.html#Thermostat-Setpoint)

It is thus possible to subscribe to state changes associated with these capabilities, and to send commands to the devices using the standard command sets these capabilities define.

Because the current SmartThings defined capabilities do not fully expose ALL of the native capabilities of Ecobee thermostats, several additional attributes (states) and commands have been added. 

-----------------------------

### <a name="attributes">Annotated List of Supported Device Attributes (States)</a>
Here is the complete list of attributes (states) that the DTH maintains and exposes:
    
    temperature: 70.5			// Temp shown on thermostat
    humidity: 44				// Current humidity at thermostat
    motion: inactive			// Thermostat motion/presence detector
    heatingSetpoint: 68.5			// Actual temp that demand for heat will be made
    coolingSetpoint: 74.5			// Ditto - demand for cool
    thermostatSetpoint: 71.5		// Heat/Cool setpoint (average of the 2 if in Auto)
    thermostatMode: auto			// See supportedThermostatModes
    thermostatFanMode: auto			// See supportedThermostatFanModes
    thermostatOperatingState: idle		// Current Operating State (heat, cool, idle, fan only, offline)
    coolingSetpointRange: [65.0, 92.0]	// Valid cooling range
    heatingSetpointRange: [45.0, 78.0]	// Valid heating range
    supportedThermostatFanModes:		// Supported fan modes 
        [on, auto, circulate, off]
    supportedThermostatModes:		// Supported thermostat modes (based on actual stat configuration)
        [off, heat, auto, cool]		// Heat Pump also has auxheat
    checkInterval: 960			// For SmartThings Health Check
    temperatureScale: F			// F or C, based on thermostat configuration
    thermostatStatus: Setpoint updating...	// Recent status message
    apiConnected: full			// Ecobee Cloud connection status
    ecobeeConnected: true			// Is thermostat communicating with Ecobee Cloud servers
    currentProgramName: Hold: Home		// Detailed display of current program
    currentProgramId: home			// Internal ID of current program
    currentProgram: Home			// Real name of current program
    scheduledProgramName: Away		// Detailed display of scheduled program
    scheduledProgramId: away		// Scheduled program ID
    scheduledProgram: Away			// Scheduled program name
    weatherSymbol: 2			// Coded icon number for the weather tile
    debugEventFromParent: setDehumiditySetpoint (60) - Succeeded	// Internal debug LOG entry
    timeOfDay: day				// Day or Night (for weather icons)
    lastPoll: Succeeded			// Status of last poll to Ecobee Cloud 
    equipmentStatus: idle			// Detailed status of Ecobee-controlled equipment
    humiditySetpoint: 50			// Target humidity (if hasHumidifier == true)
    dehumiditySetpoint: 60			// Target dehumification level (if hasDehumidifier or dehumidifyWithAC)
    weatherTemperature: 63.6		// Outside temperature from Ecobee
    decimalPrecision: 1			// Number of decimal places to display
    temperatureDisplay: 70.5°		// Temperature displayed in main thermostat tile
    equipmentOperatingState: idle		// Detailed operating state for UI Tile 
    coolMode: true				// Thermostat configured for HVAC Cooling
    heatMode: true				// Thermostat configured for HVAC Heating
    autoMode: true				// HVAC supports Auto
    heatStages: 2				// Number of HVAC Heating stages
    coolStages: 1				// Number of HVAC Cooling stages
    hasHeatPump: false			// HVAC is a Heat Pump
    hasForcedAir: true			// HVAC is a forced air system
    hasElectric: false			// HVAC is Electric (Resistive heat)
    hasBoiler: false			// HVAC is a forced hot water or steam system
    hasHumidifier: true			// Ecobee is controlling a HVAC Humidifier
    hasDehumidifier: true			// Ecobee is controlling a HVAC dehumidifier, or is using AC to dehumidify
    humidifierMode: off			// Current mode of humidifier (off, on, auto)
    dehumidifierMode: on			// Current mode of dehumidifer (off, on)
    auxHeatMode: false			// HVAC has auxiliary heating (usually for Heat Pumps)
    heatRangeHigh: 78.0			// Max heatingSetpoint
    heatRangeLow: 45.0			// Min heatingSetpoint
    coolRangeHigh: 92.0			// Max coolingSetpoint
    coolRangeLow: 65.0			// Min coolingSetpoint
    heatingSetpointMin: 45.0		// Ditto above (SmartThings changed the attribute names)
    heatingSetpointMax: 78.0		// Ditto
    coolingSetpointMin: 65.0		// Ditto
    coolingSetpointMax: 92.0		// Ditto
    heatAtSetpoint: 68.5			// Temperature at which demand for heat is made
    coolAtSetpoint: 74.5			// Ditto... demand for cool
    heatingSetpointDisplay: 69.0		// Actual setpoint display (matches thermostat)
    coolingSetpointDisplay: 74.0		// Ditto
    heatRange: (45..78)			// Yet another version of the Min/Max heating setpoints
    coolRange: (65..92)			// Ditto
    thermostatHold: hold			// Type of hold thermostat is in currently (hold, auto, null)
    holdStatus: Hold ends today at 6:00pm	// Text displayed on UI Tile
    heatDifferential: 0.5			// Degrees below heatingSetpoint for heat demand (configured at thermostat)
    coolDifferential: 0.5			// Degrees above coolingSetpoint for cool demand
    heatCoolMinDelta: 4.0			// Minimum difference between heating & cooling setpoints (enforced by code)
    fanMinOnTime: 0				// Minutes per hour fan should run (heat time + cool time + fan only time)
    programsList: ["Home", "Sleep", "Awake", "Away"]	// Configured program names
    thermostatOperatingStateDisplay: idle	// Keyword for text at bottom of main UI tile
    thermostatFanModeDisplay: auto		// Fan mode that is displayed in UI (off, on, auto, circulate)
    humiditySetpointDisplay: 50- 60		// Humidity/dehumidity setpoints displayed in UI
    thermostatTime: 2018-06-06 12:57:45	// Thermostat's time as reported in last poll of Ecobee Cloud
    statHoldAction: nextPeriod		// Default Hold type configured at thermostat
    lastHoldType: nextTransition		// Actual hold type used for current hold
    lastOpState: heating			// The previous thing that the thermostatOperatingState reported
    
---------------------------------------------------

### <a name="commands">Supported Commands</a>

The complete set of thermostat DTH commands that can be called programmatically is:

#### <a name="changeprogram">Changing Programs</a>
- **home** - change to the "Home" program
- **present** - change to the "Home" program (for compatibility with Nest smart thermostats)
- **asleep** - change to the "Sleep" program
- **night** - change to the "Sleep" program
- **away** - change to the "Away" program
- **wakeup** - change to the "Wakeup" program
- **awake** - change to the "Awake" program
- **resumeProgram** - return to the regularly scheduled program

Calling any of the above will initiate a Hold for the requested program for the currently specified duration (Permanent, Until I Change, 2 Hours, 4 Hours, etc.). If the duration is Until I Change (temporary) and the requested program is the same as the current program, resumeProgram is effected instead to return to the scheduled program.

#### <a name="changemode">Changing Thermostat Modes</a>
- **off** - turns off the thermostat/HVAC. Note, however, if the Fan Minimum On Time is not zero when this is called, the HVAC will turn off, but the fan will remain in circulate mode. Thus, to turn the HVAC *completely* off, you should first call **fanOff**, then **off**
- **auto** - puts HVAC into Auto mode, where either Heating or Cooling can be supplied, based upon the internal temperature and the heat/cool setpoint values
- **cool** - puts HVAC into Cooling (only) mode
- **heat** - puts HVAC into Heating (only) mode
- **emergencyHeat** - for HVAC systems supporting auxiliary heat (usually heat-pump systems), turns on  auxiliary heating
- **emergency** - ditto emergencyHeat
- **auxHeatOnly** - ditto emergencyHeat

#### <a name="changefan">Changing Fan Operating Modes</a>
- **fanOff** - turns the fan completely off. If Fan Minimum On Time is non-zero, it will be reset to zero
- **fanAuto** - sets the fan to operate "on-demand" whenever heating or cooling
- **fanCirculate** - same as fanAuto, except the Fan Minimum On Time is non-zero
- **fanOn** - runs the fan continuously

#### <a name="complex">Complex Commands Requiring Arguments</a>
The following command entry points require multiple arguments, and so are most optimally utilized by SmartApps that can construct the required arguments, including WebCoRE (link-to-example-here). If you want to try using them, I suggest you review the DTH code directly to understand the required argument structure.

- **setCoolingSetpoint** - change the cooling setpoint (will create a Temperature Hold); specify temperature in F or C, based on how you have configured the Ecobee Suite and Thermostat (both should agree)
- **setHeatingSetpoint** - ditto, but for heating setpoint
- **setThermostatProgram** - use to change to custom Programs/Climates other than the ones defined above
- **setFanMinOnTime** - used to set a custom number of minutes for the Fan Minimum On Time circulation mode
- **setVacationFanMinOnTime** - ditto, but for a vacation
- **setHumidifierMode**	- set humidification mode
- **setHumiditySetpoint** - set humidification target (cannot change if humidifierMode == 'auto')
- **setDehumidifierMode** - ditto for dehumidification
- **setDehumiditySetpoint**	- ditto for dehumidification
- **scheduleVacation** - define a vacation programmatically 
- **deleteVacation** - delete a previously defined vacation
- **cancelVacation** - cancel the currently active vacation (if any)

### <a name="reservations">Reservations</a>

Version 1.6.00 introduced a new ***internal use only*** Reservations System to the Suite. While this *may* one day be opened to external SmartApps or WeBCoRE pistons, for now it is a closed system.

To satisfy your curiousity:

The design intent is to enable certain types of coordination between the various Helper SmartApps. For example, if the Smart Mode helper decides to turn off the HVAC system (setting the thermostatMode to 'off'), then we probably don't want the Switches & Contacts helper turning the HVAC back on. 

To accomplish this, Smart Mode will create a 'modeOff' reservation with each configured thermostat, signifying that it has interest in keeping the HVAC system turned off. Should the Contacts & Switches Helper also decide that the HVAC system should be turned off, it too takes out a' 'modeOff' reservation.

Given multiple such 'modeOff' reservations, neither Helper will turn the HVAC back on until ALL of the reservations are removed.

Operationally, reservations work much like semaphores. A process wishing to turn off the HVAC should (in pseudo-code):
    1. makeReservation(stat.Id, app.Id, 'modeOff')
    2. stat.off()

And when it wants to turn the HVAC back on:
1. if ((reservationCount - myReservation) == 0)		// No reservations except my own
2. stat.auto()
3. cancelReservation(stat.id, app.id, 'modeOff')

This order is important, because the entire SmartThings world is asynchronous. You want to be sure to grab the reservation before you turn off the device, and then you want to wait until you have turned the device back on AFTER you turn it on.

  [Note: the revised implementation of Reservations in v1.6.10 and later uses atomicState variables to hold the reservations, explicitly to minimize race conditions. That said, I'm sure that they can still occur.]

The Reservation System implements the following commands/entry points:
- **void makeReservation(stat.id, child.id, type)** Makes a new reservation of `type` for the calling SmartApp (`childId`). If the caller already has a reservation, a new one is not created, and no error is flagged.
- **void cancelReservation(stat.id, child.id, type)** Cancels/deletes a reservation for the `client.Id` of the specified `type`. If no reservation exists of this `type`` for `child.id`, it is silently ignored.
- **Boolean haveReservation(stat.id, child.id, type)** check to see if app.id holds a reservation
- **Boolean anyReservations(stat.id, type)** check to see if there are ANY reservations for `type`
- **Integer countReservations((stat.id, type)** Returns the number of current reservations of `type`
- **List getReservations(stat.id, type)** returns a list of the `child.ids` (aka `app.Ids`)
- **List getGuestList(stat.id, type)** returns a list of the NAMES for each

Where:
- `stat.id` is the Ecobee Thermostat ID number (getDeviceId(device.deviceNetworkId))
- `child.Id` is the SmartThings application ID (app.Id)
- `type` is any arbitrary identity for the class/type of reservation. Used by the code so far are **`modeOff`**, **`fanOff`**, **`circOff`**, **`vacaCircOff`**.
