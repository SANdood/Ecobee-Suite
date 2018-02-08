Free Ecobee Suite, version: 1.3.* 
======================================================
Released February 8, 2018
---------------------------------------------

>NOTE: This version of Ecobee Thermostat support and integration for SmartThings has been developed as an extension of the amazing contributions originally created by Sean Stryker (StrykerSKS). This README file is an edited/updated version of Sean's, modified to reflect the enhancements AND the GitHub paths/locations for those wanting to use my version. I have NOT yet updated all the screen shots - these currently reflect the UI of Sean's version, and not mine.

## <a name="top">Table Of Contents</a>
- [Introduction](#intro)
- [Motivation](#motivation)
- [Quick Links](#quicklinks)
- [Features](#features)
	- [Thermostat and Sensor Device User Interfaces](#features-therm-ui)
	- [Ecobee (Connect) SmartApp](#features-connect-sa)
    - [ecobee Routines SmartApp](#features-routines-sa)
    - [ecobee Open Contacts SmartApp](#features-opencontact-sa)
    - [ecobee Smart Circulation SmartApp](#features-smart-circ-sa)
    - [ecobee Smart Room SmartApp](#features-smart-room-sa)
    - [ecobee Smart Switches SmartApp](#features-smart-switches-sa)
    - [ecobee Smart Vents SmartApp](#features-smart-vents-sa)
    - [ecobee Smart Zones SmartApp](#features-smart-zone-sa)
- [Installation](#installation)
  - [Install Device Handlers](#install-device-types)
  - [Install SmartApp in IDE](#install-smartapp)
  - [Install SmartApp on Device](#install-smartapp-phone)
- [Updating](#updating)
- [Troubleshooting](#troubleshooting)
- [Reporting Issues](#reporting-issues)
- [License](#license)

## <a name="intro">Introduction</a>
This document describes the various features related to the Open Source Ecobee (Connect)  SmartApp and the related compoenents. This SmartApp suite and the related Device Handlers are intended to be used with [Ecobee thermostats](http://www.ecobee.com/) with the [SmartThings](https://www.smartthings.com/) platform. 

The following components are part of the solution:
- **Ecobee (Connect) SmartApp**: This SmartApp provides a single interface for Ecobee Authorization, Device Setup (both Thermostats **and** Sensors), Behavioral Settings and even a Debug Dashboard. Additional features can be added over time as well thanks to built in support for Child SmartApps, keeping everything nicely integrated into one app.
- **ecobee Routines Helper SmartApp**: Child app that lets you trigger settings changes on your Ecobee thermostats based on the SmartThings Hello Modes and/or Routines. This version also supports changing SmartThings Mode or executing a Routine when the thermostat enters a new Program or Vacation. Settings include the Ecobee Program (Comfort Settings), Fan Modes and Hold Types. In additional to SmartThings Hello Modes, sunrise/sunset triggers are also support. Multiple instances of the SmartApp are also supported for maximum flexibility.
- **Ecobee Smart Circulation Helper SmartApp**: Child app that adjusts hourly circulation time (fan on minimum minutes) trying to get 2 or more temperature sensors (Ecobee or SmartThings) to within a configurable temperature difference.
- **Ecobee Smart Room Helper SmartApp**: Child app that automates a "Smart Room." Intended for rooms that are normally not used (doors, windows and automated vents closed), this app will Activate a "Smart Room" when the door is left open for a configurable period (defalt 5 minutes). Once active, the room's Ecobee Sensor is added to any of the user-selected default 3 Programs (Home, Away, Sleep), and (optionally) the automated vent (EcoNet or Keene) is opened. If the door is closed, after a configurable number of hours (default 12), it will De-activate the Smart Room, removing the Ecobee sensor from specified Programs and closing the vent. While active, opening a window will temporarily de-activate the room until the window is closed. If motion is detected while the door is closed, the automated de-activation will be cancelled until the next time the door is closed (and no motion is detected. Requires: Ecobee3 Full (not Lite version), Ecobee Temp/Occupancy Sensor and Door contact sensor. Optional: Window contact sensor(s), EcoNet or Keene smart vents.
- **Ecobee Smart Switches Helper SmartApp**: Child app that will turn on/off one or more switches, and/or set the level on one or more dimmers (also works for vents), all based on changes to the Operating State of one or more thermostats. Intended to turn on vent booster fans when HVAC is pushing air, and (optionally) turn them off when they return to idle state. Can also be used to control lights and dimmers; dimmer control can be used to operate vents (see also Smart Vents Helper, below).
- **Ecobee Smart Vents Helper SmartApp**: Child app that will open and close one or more SmartThings-controlled HVAC vents based on room temperature in relation to specified heating/cooling target setpoint, or the setpoint of the specified thermostat.
- **Ecobee Smart Zones Helper SmartApp**: Child app that attempts to synchronize fan on time between two zones. Intended for use on multi-zones HVAC systems; running the fan on all the zones simultaneously could help reduce electricity consumption vs. having each zone run the fan independently.
- **Ecobee Thermostat Device Handler**: This implements the Device Handler for the Ecobee Thermostat functions and attributes. Now includes indicators for current thermostat Program, buttons that allow adding/removing the zone from specific Programs (Home, Away, Sleep only), and indicators that appear when Smart Room is configured for the room.
- **Ecobee Sensor Device Handler**: This implements the Device Handler for the Ecobee Sensor attributes. This is also used to expose the internal sensors on the Thermostat to allow the actual temperature values (instead of only the average) to also be available. This is critically important for some applications such as smart vents.

Here are links to the working version of the repository being developed and maintained by Barry A. Burke [(on GitHub)](https://github.com/SANdood/SmartThingsPublic/tree/StorageAnarchy-Ecobee) [(on SmartThings Community)](https://community.smartthings.com/t/release-free-ecobee-suite-version-1-2-0/94841).

## <a name="motivation">Motivation</a>

I maintain the original intent as defined by Sean:

The intent is to provide an Open Source Licensed ecobee-to-SmartThings implementation that can be used by the SmartThings community of users free of charge and without fear of the device disappearing in the future. This will help ensure accessibility for all users and provide for an easy mechanism for the community to help maintain/drive the functionality.

Let me be clear: this is not a competition. I am not trying to replace or outdo any other version, and I personally have nothing against people making money off of their efforts. I have invested my time in this as Open Source so as to meet my own wants and desires, and I offer the result back to the community with no strings attached.

Please feel free to try any version you want, and pick the one that best fits your own use case and preference.

### Donations

As noted, this work is fully Open Source, and available for use at no charge. However, if you would like to make a donation, you can send it to me at <https://paypal.me/BarryABurke>

## <a name="quicklinks">Quick Links</a>
- README.md (this file): <https://github.com/StrykerSKS/SmartThingsPublic/blob/StrykerSKS-Ecobee3/smartapps/smartthings/ecobee-connect.src/README.md>
- Ecobee (Connect) SmartApp: <https://github.com/SANdood/SmartThingsPublic/tree/StorageAnarchy-Ecobee/smartapps/smartthings/ecobee-connect.src>
- ecobee Routines Child SmartApp: <https://github.com/SANdood/SmartThingsPublic/tree/StorageAnarchy-Ecobee/smartapps/smartthings/ecobee-routines.src>
- ecobee Open Contacts Child SmartApp: <https://github.com/SANdood/SmartThingsPublic/tree/StorageAnarchy-Ecobee/smartapps/smartthings/ecobee-open-contacts.src>
- ecobee Smart Circulation Helper SmartApp: <https://github.com/SANdood/SmartThingsPublic/tree/StorageAnarchy-Ecobee/smartapps/smartthings/ecobee-smart-circulation.src>
- ecobee Smart Room Helper SmartApp: <https://github.com/SANdood/SmartThingsPublic/tree/StorageAnarchy-Ecobee/smartapps/smartthings/ecobee-smart-room.src>
- ecobee Smart Switches Helper SmartApp: <https://github.com/SANdood/SmartThingsPublic/tree/StorageAnarchy-Ecobee/smartapps/smartthings/ecobee-smart-switches.src>
- ecobee Smart Vents Helper SmartApp: <https://github.com/SANdood/SmartThingsPublic/tree/StorageAnarchy-Ecobee/smartapps/smartthings/ecobee-smart-vents.src>
- ecobee Smart Zones Helper SmartApp: <https://github.com/SANdood/SmartThingsPublic/tree/StorageAnarchy-Ecobee/smartapps/smartthings/ecobee-smart-zones.src>
- Ecobee Thermostat Device: <https://github.com/SANdood/SmartThingsPublic/tree/StorageAnarchy-Ecobee/devicetypes/smartthings/ecobee-thermostat.src>
- Ecobee Sensor Device: <https://github.com/SANdood/SmartThingsPublic/tree/StorageAnarchy-Ecobee/devicetypes/smartthings/ecobee-sensor.src>
- SmartThings IDE: <https://graph.api.smartthings.com>


-----------------------------
# <a name="features">Features</a>
## General
This collection of SmartApps and Device Handlers has been designed for simple installation, flexibile configuration options and easy operation. It is also extensible through the use of Child SmartApps that can easily be added to the configuration. **And it fully implements the related [SmartThings Capabilities](http://docs.smartthings.com/en/latest/capabilities-reference.html).**

Key Highlights include:
- **Open Source Implementation!** Free as in beer AND speech. No donations or purchase needed to use.
- Single installation SmartApp, `Ecobee (Connect)` used for installing both Thermostats **and** Sensors. No need for multiple apps just for installation! In fact, the `Ecobee (Connect)` SmartApp is the only SmartApp interface you'll need to access all available functions, including those provided by Child SmartApps (if installed).
- Sophisticated User Interface: Uses custom Ecobee icons throughout the design to provide a more polished look and feel.
- Display of current weather with support for separate day and night icons (just like on the front of your Ecobee thermostat)!
- Robust watchdog handling to minimize API Connectivity issues, but also includes an API Status Tile to quickly identify if there is an ongoing problem. No more guessing if you are still connected or not.
- Included Child SmartApp (`ecobee Routines`) for automating settings changes based on SmartThings Hello Modes being activated (such as through a Routine), and/or automating SmartThings Modes/Routines based on Ecobee Thermostat Program changes (including Vacations).
- Additional Smart Helper Apps to automate ciculation time, coordinate fan-on time between multiple zones, and automate a "Smart Room."
- Full support for both Fahrenheit and Celsius

## <a name="features-therm-ui">Thermostat and Sensor Device User Interfaces</a>
The primary user interface on a day-to-day basis will be two different types of Device Handlers that are shown on the `Things` list under `My Home` in the mobile app. 

### Key Enhancements

<b>Thermostat UI Feature Enhancements</b>
  - The main MultiAttributeTile tile now reflects additional information
    - Background colors match Ecobee3 thermostat colors for Idle (magenta), Heat (orange) & Cool (blue), and Fan Only (green) and Offline (red) are added
    - Displays (Smart Recovery) when heating/cooling in advance of a Program change
    - Displays the setpoint temperature that will initiate a heat/cool demand while idle, and the actual target temp while heating/cooling
    - All temperatures can be configured to display in 0, 1 or 2 decimal positions (with appropriate rounding)
  - Most icons are dynamic and in full color
    - New Operating State icons reflect current stage for multi-stage Heat or Cool, plus humidification/dehumidification (if configured)
    - Hold: Program icons are now "filled" so you can recognize the Hold at a glance
    - Buttons that are inactive due to the current state/mode/program are greyed out
    - Display-only buttons no longer invoke any actions
  - Dynamic "Resume" button
    - Normally displays as Resume (Program) button, to override a current Hold: event
    - While thermostat is in a Vacation event, the button becomes 'Cancel" to allow the cancelation of the Vacation
  - Hold/Vacation Status display
    - Displays when current Hold or Vacation ends
    - If the thermostat loses its connection to the Ecobee Cloud, displays the time that the last valid update was recieved
  - Default Hold handling
    - In addition to Permanent and Temporary holds, now supports hourly holds: 2 hours, 4 hours or Custom Hours. 
    - The default hold type can be configured in Ecobee (Connect) or for each individual thermostat in Thermostat Preferences.
    - Also supports using the thermostat's preference setting (askMe is not currently supported)
  - New Refresh actions
    - If pressed once, will only request the latest updates from the Ecobee Cloud
    - If pressed a second time within a few seconds of the first press completing, will force a full update from the Ecobee Cloud
   
<b>Sensor UI Feature Enhancements</b>
  -	A new multiAttributeTile replaces the old presentation, with motion displayed at the bottom left corner
  - Now displays the parent thermostat current Program within each Sensor device
  - New mini-icons indicate which of the 3 default programs (Home, Away, Sleep) the sensor is included in
    - The sensor can be added or removed from a Program by tapping these mini icons
  - Includes 4 new blank mini-tiles that are utilized by the new Smart Room Helper App
	
<b>SmartThings Integration</b>
  - Messages sent to the Device notification log (found under the Recently tab of a Device) are now optimized, most with associated colored icons 
  - All current Attributes and Capabilities of Thermostat devices are supported
  - Most Ecobee Settings Object variables are are now available as Attributes of a Thermostat (so things like CoRE can see them) 
  - Supports the latest updated Thermostat Attributes released by SmartThings on July 7, 2017
  - Now offers several new API Command interfaces - see `ecobee-thermostat.groovy` for specifics. These include new API commands to:
    - Add a sensor to a Program
    - Delete/Remove a sensor from a Program
    - Delete the current Vacation
    - Change the minimum fan on time for both the current running Program and current Vacation event

<b>Operational Enhancements</b>

  - Operational Efficiency
    - Redesigned to do only lightweight polls of the Ecobee Cloud before requesting updates
    - If updates are available, then only requests those updated objects, and only for the thermostats with updates
    - From the updated Ecobee objects, only the data that has changed is sent to the individual devices
    - As a result of the above, it is possible to run will polling Frequency less than the recommended 3 minutes (as low as 1 minute)
  - Stacked Holds
    
    Ecobee devices support a concent of "stacked" holds - each hold operation can be stacked on top of a prior hold, such that Resuming the current hold merely "pops the stack" to the prior hold. This is a very difficult concept to manage across multiple interfaces, because it is difficult to depict what the result of a "Resume" operation will be at any given point in time.
    
    This implementation avoids the complexity by supporting only a single depth of Hold: events - whenever you execute a Resume you reset the thermostat to run the currently scheduled Program.
        
<b>Ask Alexa Message Queue Support</b>

 - For users of Michael Struck's most awesome Ask Alexa integration for SmartThings <http://thingsthataresmart.wiki/index.php?title=Ask_Alexa>
 	- you can now configure your Ecobee Alerts and Reminders to be sent to Ask Alexa Message Queues. 
 	- This is enabled in a new Ask Alexa preferences page in Ecobee (Connect) where you can specify the target Message Queue(s). 
	- You can also specify an expiration time for Alerts so that they are removed from Ask Alexa after a specified period of time.

NOTE: You will not be able to configure Ask Alexa support until you have fully installed Ask Alexa <b>AND</b> created at least 1 custom Message Queue, as the Primary Queue is being deprecated.

- The current implementation does not support acknowledging of messages directly via Ask Alexa, but when the Alert/Reminder is acknowledged (on the thermostat or in the Ecobee application/web site) they are also removed from the Ask Alexa Message Queue.

- This feature will evolve over the coming months to include more control over what gets sent to Ask Alexa and to support Notifications Only. So stay tuned!

Screenshots of both the `Ecobee Thermostat` and the `Ecobee Sensor` are shown below (new versions).
------------------------------------------------------------------------------------

`Ecobee Thermostat` Device |  `Ecobee Thermostat` Device w/ Annotation
:-------------------------:|:-------------------------:
<img src="https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/UISnapshot-v1.2.0.jpg" border="1" width="250" /> | coming soon

`Ecobee Sensor` Device |  `Ecobee Sensor` Device w/ Annotation
:-------------------------:|:-------------------------:
<img src="https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/UISensor-v1.2.0.png" border="1" width="250" /> | coming soon

## <a name="features-connect-sa">`Ecobee (Connect)` SmartApp</a>
The `Ecobee (Connect)` SmartApp provides a single SmartApp interface for accessing installation, updates, Child SmartApps and even debugging tools. The interface uses dynamic pages to guide the user through the installation process in order simplify the steps as much as possible.

The SmartApp provides the following capabilities:
- Perform Ecobee API Authorization (OAuth)
- Select Thermostats from account to use (dynamic list, so any future Thermostats can easily be added at a later date)
- Select Sensors to use (dynamic list, will only show sensors associated with the previously selected Thermostats)
- Access Child SmartApps (such as `ecobee Routines`)
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



## <a name="features-routines-sa">`ecobee Routines` SmartApp</a>
The `ecobee Routines` SmartApp provides the ability to change the running Program (Comfort Setting) when a SmartThings Mode is changed (for example, by running a Routine) or a Routine is run. 

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

## <a name="features-opencontact-sa">`Open Contacts` SmartApp</a>
The `Open Contacts` SmartApp can detect when one (or more) contact sensors (such as doors and windows) are left open for a configurable amount of time and can automatically turn off the HVAC and/or send a notification when it is detected. 

Updated May 10, 2017 to also support switches, with configurable selection of whether contact open or close, switch on or off will turn off the HVAC. This enhancement allows (for example) automatically turning off the HVAC when the attic fan is running.

<b>Features:</b>
- Change one or multiple thermostats
- Trigger based on one or multiple contact sensors and/or switches
- Configurable delay timers (for trigger and reset)
- Configurable actions: Notify Only, HVAC Only or Both
- Support for Contact Book or simply SMS for notifications
- Temporarily Disable app without having to delete and recreate!

## <a name="features-smart-circ-sa">`Smart Circulation` SmartApp</a>
The `Smart Ciculation' SmartApp will adjust fan mininum on time of an Ecobee thermostat as it tries to get two or more temperature sensors to converge.

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

## <a name="features-smart-room-sa">`Smart Room` SmartApp</a>
The `Smart Room' Helper SmartApp will automate a normally unused room, Activating it one or more of the Doors associated with the Smart Room are left open for a configurable number of minutes, and Deactivating it when all the Doors are closed for a configurable number of hours.

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
    
## <a name="features-smart-switches-sa">`Smart Switches` SmartApp</a>
Smart Switches Helper SmartApp enables activating switches or dimmers based upon the Operating State of a thermostat.

## <a name="features-smart-vents-sa">`Smart Vents` SmartApp</a>
Smart Vents automates one or more vents to reach/maintain a target temperature that you specify or from a specified thermostat. It uses one or more temperature devices, and opens/closes vents based upon current temperature, target temperature and thermostat operating state.

## <a name="features-smart-zone-sa">`Smart Zones` SmartApp</a>
Documentation coming soon.

# <a name="installation">Installation</a>

## General
> **NOTE**: While I have tested this on my 3 location configuration and believe it to be working well, I have not run a full battery of tests that can include all possible corner cases or configurations. It is possible, perhaps even _likely_, that there are still bugs or platform specific issues in this code. If you do run into an issue, the best option is to report it using the [Issues](https://github.com/SANdood/SmartThingsPublic/issues) tab within the GitHub repository. I will do my best to quickly address any issues that are found. 

It is highly recommended that you use the GitHub Integration that SmartThings offers with their IDE. This will make it **much** easier to keep up to date with changes over time. For the general steps needed for setting up GitHub IDE integration, please visit <http://docs.smartthings.com/en/latest/tools-and-ide/github-integration.html> and follow the steps for performing the setup.

## Install Preparation
The first step is to ensure that you <b>delete any existing Ecobee related devices and SmartApps that you may have from other sources</b>. They are likely not compatible with this codebase and are almost certain to cause problems down the road. While it *may* be possible to install this new version over an existing installation of StrykerSKS' version, there are no guarantees that it will work.

If you are not familiar with adding your own custom devices, then be sure to familiarize yourself with the [SmartThings IDE](https://graph.api.smartthings.com/) before you begin the installation process.

You will also need to make sure that you remember your Ecobee username and password. You should login to <http://www.ecobee.com/> now to ensure you have your credentials.

## <a name="install-device-type">Install Device Handlers</a>
Here we will install two (2) different Device Handlers:
- `Ecobee Thermostat`
- `Ecobee Sensor`

Follow the steps for _either_ the GitHub Integration or the Manual method below. Do **not** try to do both methods.

### Install Using GitHub Integration (Recommended Method)
Follow these steps (all within the SmartThings IDE):
- Click on the `My Device Handlers` tab
- Click `Settings`
- Click `Add new repository` and use the following parameters:
  - Owner: `SANdood`
  - Name: `SmartThingsPublic`
  - Branch: `StorageAnarchy-Ecobee`
- Click `Save`
- Click `Update from Repo` and select the repository we just added above
- Find and Select `ecobee-sensor.groovy` and `ecobee-thermostat.groovy`
- Select `Publish`(bottom right of screen near the `Cancel` button)
- Click `Execute Update`
- Note the response at the top. It should be something like "`Updated 0 devices and created 2 new devices, 2 published`"
- Verify that the two devices show up in the list and are marked with Status `Published` (NOTE: You may have to reload the `My Device Handlers` screen for the devices to show up properly.)


### Install Manually from Code
For this method you will need to have one browser window open on GitHub and another on the IDE.

Follow these steps to install the `Ecobee Sensor`:
- [IDE] Click on the `My Device Handlers` tab
- [IDE] Click `New Device Type` (top right corner)
- [IDE] Click `From Code`
- [GitHub] Go to the respository for the Ecobee Sensor: <https://github.com/SANdood/SmartThingsPublic/blob/StorageAnarchy-Ecobee/devicetypes/smartthings/ecobee-sensor.src/ecobee-sensor.groovy>
- [GitHub] Click `Raw`
- [GitHub] Select all of the text in the window (use Ctrl-A if using Windows)
- [GitHub] Copy all of the selected text to the Clipboard (use Ctrl-C if using Windows)
- [IDE] Click inside the text box
- [IDE] Paste all of the previously copied text (use Ctrl-V if using Windows)
- [IDE] Click `Create`
- [IDE] Click `Save`
- [IDE] Click `Publish` --> `For Me`

Follow these steps to install the `Ecobee Thermostat`:
- [IDE] Click on the `My Device Handlers` tab
- [IDE] Click `New Device Type` (top right corner)
- [IDE] Click `From Code`
- [GitHub] Go to the respository for the Ecobee Thermostat: <https://github.com/SANdood/SmartThingsPublic/blob/StorageAnarchy-Ecobee/devicetypes/smartthings/ecobee-thermostat.src/ecobee-thermostat.groovy>
- [GitHub] Click `Raw`
- [GitHub] Select all of the text in the window (use Ctrl-A if using Windows)
- [GitHub] Copy all of the selected text to the Clipboard (use Ctrl-C if using Windows)
- [IDE] Click inside the text box
- [IDE] Paste all of the previously copied text (use Ctrl-V if using Windows)
- [IDE] Click `Create`
- [IDE] Click `Save`
- [IDE] Click `Publish` --> `For Me`


## <a name="install-smartapp">Install SmartApps in IDE</a>
Here we will install the following SmartApps:
- `Ecobee (Connect)`
- `ecobee Open Contacts` (Child SmartApp)
- `ecobee Routines` (Child SmartApp)
- `ecobee Smart Circulation` (Child SmartApp)
- `ecobee Smart Room` (Child SmartApp)
- `ecobee Smart Switches` (Child SmartApp)
- `ecobee Smart Vents` (Child SmartApp)
- `ecobee Smart Zones` (Child SmartApp)

Follow the steps for _either_ the GitHub Integration or the Manual method below. Do **not** try to do both methods.

### Install Using GitHub Integration
Follow these steps to install the `Ecobee (Connect)` SmartApp (all within the SmartThings IDE):
- Click on the `My SmartApps` tab
- Click `Settings`
- Click `Add new repository` and use the following parameters:
  - Owner: `SANdood`
  - Name: `SmartThingsPublic`
  - Branch: `StorageAnarchy-Ecobee`
- Click `Save`
- Click `Update from Repo` and select the repository we just added above
- Find and Select `ecobee-connect.groovy`, `ecobee-routines.groovy`, `ecobee-open-contacts.groovy`, `ecobee-smart-circulation.groovy`, `ecobee-smart-room.groovy`, `ecobee-smart-zones.groovy`
- Select `Publish`(bottom right of screen near the `Cancel` button)
- Click `Execute Update`
- Note the response at the top. It should be something like "`Updated 0 and created 2 SmartApps, 2 published`"
- Verify that the SmartApps shows up in the list and is marked with Status `Published`
- Locate the `Ecobee (Connect)` SmartApp from the list and Click on the `Edit Properties` button to the left of the SmartApp that we just added (looks like pencil on a paper)
- Click on the `OAuth` tab (**NOTE: This is a commonly missed set of steps, but failing to enable OAuth will generate cryptic errors later when you try to use the SmartApp. So please don't skip these steps.**)
- Click `Enable OAuth in Smart App`
- Click `Update` (bottom left of screen)
- Verify that `Updated SmartApp` appears at the top of the screen

### Install Manually from Code
For this method you will need to have one browser window open on GitHub and another on the IDE.

Follow these steps to install the `Ecobee (Connect)` SmartApps:
- [IDE] Click on the `My SmartApps` tab
- [IDE] Click `New SmartApp` (top right corner)
- [IDE] Click `From Code`
- [GitHub] Go to the respository for the `Ecobee (Connect)` SmartApp: <https://github.com/SANdood/SmartThingsPublic/blob/StorageAnarchy-Ecobee/smartapps/smartthings/ecobee-connect.src/ecobee-connect.groovy>
- [GitHub] Click `Raw`
- [GitHub] Select all of the text in the window (use Ctrl-A if using Windows)
- [GitHub] Copy all of the selected text to the Clipboard (use Ctrl-C if using Windows)
- [IDE] Click inside the text box
- [IDE] Paste all of the previously copied text (use Ctrl-V if using Windows)
- [IDE] Click `Create`
- [IDE] Click `Save`
- [IDE] Click `Publish` --> `For Me`
- [IDE] Click on the `My SmartApps` tab
- [IDE] Verify that the SmartApp shows up in the list and is marked with Status `Published`
- [IDE] Click on the `Edit Properties` button to the left of the SmartApp that we just added (looks like pencil on a paper)
- [IDE] Click on the `OAuth` tab
- [IDE] Click `Enable OAuth in Smart App`
- [IDE] Click `Update` (bottom left of screen)
- [IDE] Verify that `Updated SmartApp` appears at the top of the screen

Follow these steps to install the `ecobee Routines` SmartApps:
- [IDE] Click on the `My SmartApps` tab
- [IDE] Click `New SmartApp` (top right corner)
- [IDE] Click `From Code`
- [GitHub] Go to the respository for the `ecobee Open Contacts` SmartApp: <https://github.com/SANdood/SmartThingsPublic/blob/StorageAnarchy-Ecobee/smartapps/smartthings/ecobee-routines.src/ecobee-open-contacts.groovy>
- [GitHub] Click `Raw`
- [GitHub] Select all of the text in the window (use Ctrl-A if using Windows)
- [GitHub] Copy all of the selected text to the Clipboard (use Ctrl-C if using Windows)
- [IDE] Click inside the text box
- [IDE] Paste all of the previously copied text (use Ctrl-V if using Windows)
- [IDE] Click `Create`
- [IDE] Click `Save`
- [IDE] Click `Publish` --> `For Me` (Optional)
- [IDE] Click on the `My SmartApps` tab
- [IDE] Verify that the SmartApp shows up in the list

Repeat the above steps for the rest of the desired Helper SmartApps:
- `Ecobee Routines` SmartApp: <https://github.com/SANdood/SmartThingsPublic/blob/StorageAnarchy-Ecobee/smartapps/smartthings/ecobee-routines.src/ecobee-routines.groovy>
- `ecobee Smart Circulation` SmartApp: <https://github.com/SANdood/SmartThingsPublic/blob/StorageAnarchy-Ecobee/smartapps/smartthings/ecobee-smart-circulation.src/ecobee-smart-circulation.groovy>
- `ecobee Smart Room` SmartApp: <https://github.com/SANdood/SmartThingsPublic/blob/StorageAnarchy-Ecobee/smartapps/smartthings/ecobee-smart-room.src/ecobee-smart-room.groovy>
- `ecobee Smart Switches` SmartApp: <https://github.com/SANdood/SmartThingsPublic/blob/StorageAnarchy-Ecobee/smartapps/smartthings/ecobee-smart-room.src/ecobee-smart-switches.groovy>
- `ecobee Smart Vents` SmartApp: <https://github.com/SANdood/SmartThingsPublic/blob/StorageAnarchy-Ecobee/smartapps/smartthings/ecobee-smart-room.src/ecobee-smart-vents.groovy>
- `ecobee Zones Circulation` SmartApp: <https://github.com/SANdood/SmartThingsPublic/blob/StorageAnarchy-Ecobee/smartapps/smartthings/ecobee-smart-zones.src/ecobee-smart-zones.groovy>

## <a name="install-smartapp-phone">Install and Run `Ecobee (Connect) `SmartApp on Phone/Tablet</a>
> **NOTE**: My primary device is an iPhone 7 Plus, but I have tested on an Android-based tablet as well. Feedback and bug reports are welcome if any issues are found on any platform. There are some known issues with platforms behaving differently due to differences in the SmartThings apps on those platforms. I have tried to work around the issues created recently by the Android 2.3.1 updates where SmartThings broke many fonts and graphical displays.

The SmartApp will guide you through the basic installation and setup process. It includes the following aspects:
- Authentication with Ecobee to allow API Calls for your thermostat(s) (and connected sensors)
- Discover and selection of Thermostats
- Discover and selection of Remote Sensors (if there are any)
- Setup of option features/parameters such as Smart Auto Temp Control, Polling Intervals, etc

Follow these steps for the SmartApp on your mobile device:
- Open the SmartThings app
- Open the `Marketplace`
- Click the `SmartApps` tab
- Select `My Apps` (all the way at the bottom of the list)
- Click `Ecobee (Connect)` (NOTE: If the app simply returns back to the `My Apps` screen try clicking again. If this still does not work after several tries, please verify all install steps from above are completed.)
- Click (as indicated on the screen) to enter your Ecobee Credentials
- Enter your Ecobee Email and Password
- Click `Accept`
- You should receive a message indicating `Your ecobee Account is now connected to SmartThings!`
- Click `Done` (or `Next` depending on your device OS)
- Click `Done` (or `Next` depending on your device OS) again to save the credentials and prepare for the next steps.
- You should receive a small green popup at the top stating "`Ecobee (Connect) is now installed and automating`"
- Go to the `My Home` screen and slect the `SmartApps` tab
- Click on the `Ecobee (Connect)` SmartApp
- Work through the various option screens to select thermostats and sensors. (NOTE: The options are dynamic and will change/appear based on other selections such as selecting a thermostat will reveal the sensors option screen.)
- You can also go into the `Preferences` section to set various preferences such as `Hold Type`, `Smart Auto Temperature`, `Polling Interval`, `Debug Level`, Decimal Precision, and whether to create separate sensor objects for thermostats.
- After making all selections, Click `Done` to save your preferences and exit the SmartApp

At this point, the SmartApp will automatically create all of the new devices, one for each thermostat and sensor. These will show up in your regular `Things` list within the app. 

> **NOTE 1**: It may take a few minutes for the new devices to show up in the list. You should try refreshing the list if they are not there (pull down on the list). In extreme cases, you may have to restart the SmartThings app on your phone to update the list. You should only have to do this once.

<br/>

> **NOTE 2**: If you uninstall the SmartApp it will automatically remove all of the thermostats and sensors that it previously installed. This is necessary (and expected) as those devices are "children" of the SmartApp.

<br/>

> There is currently a lot of debug information that can be generated from the app (which is configurable). If you need to do any kind of troubleshooting, you can see the current information flowing through in the `Live Logging` tab of the SmartThings IDE. You will also need this information if you open an `Issue` since it will be needed to track down what is going on. ** Please ensure that you do not include any personal information from the logs in an `Issue` report. **

-------------------------
## Updating
If you have enabled GitHub integration with the SmartThings IDE, then updates are a breeze. Otherwise the steps are a bit more manual but not too complicated.

### Updating with GitHub Integration
The IDE provides visual cues to alert you that any device types or SmartApps have been updated in their upstream repositories. See the [GitHub/IDE integration guide](http://docs.smartthings.com/en/latest/tools-and-ide/github-integration.html) for more details on the different colors.

Once you have determined that an update is available, follow these steps:
- Login to the SmartThings IDE
- Go to either the `My Device Handlers` or `My SmartApps` tabs to see if there are updates (the color of the item will be purple)
- Click the `Update from Repo` button (top right)
- Select the repository and branch you want to update `SmartThingsPublic (StorageAnarchy-Ecobee)`
- The item should show up in the `Obsolete (updated in GitHub)` column and automatically be selected
- Select `Publish` (bottom right)
- Click `Execute Update` (bottom right)
- You should receive a confirmation message such as this example: `Updated 1 and created 0 SmartApps, 1 published`
- (Optional, but <b>HIGHLY Recommended</b>) Rerun the `Ecobee (Connect)` SmartApp. This seems to eleviate any residual issues that may occur due to the update

You should now be running on the updated code. Be sure that you check for both updates of the SmartApp **and** the Device Type. Updating one but not the other could cause compatibility problems.

### Updating manually (without GitHub Integration)

To update manually, you will need to "cut & paste" the raw code from GitHub into the SmartThings IDE, Save and Publish the code. I will leave it to the reader to work through the full individual steps, but the links to the code are the same as those that were used during the initial install process.

Note that there are EIGHT SmartApps and TWO Device Handlers in this suite. For proper operation, you will need to copy/paste ALL 10 of these files into your IDE, even if you aren't going to use them all. At a minimum, you should also publish the Ecobee (Connect) SmartApp and the two Device Handlers.

-------------------------
## Troubleshooting

| Symptom 	| Possible Solution 	|
|---------	|-------------------	|
| The devices are not showing up in the Things tab after installation    	|  It can take several minutes for things to show up properly. If you don't want to wait then simply kill the SmartThings app and reload it.              	|
| Receive error similar to "error java.lang.NullPointerException: Cannot get property 'authorities' on null object"        	| This indicates that you have not turned on OAuth for the SmartApp. Please review the installation instructions and complete the OAuth steps.                  	|
| "You are not authorized to perform the requested operation."        	|  This indicates that you have not turned on OAuth for the SmartApp. Please review the installation instructions and complete the OAuth steps.                 	|
| Irregular behavior after an update to the SmartApp or Device Handler code| It is possible that after updating the codebase that you may experience strange behavior, including possible app crashes. This seems to be a general issue with updates on SmartThings. Try the following steps: <br/> 1) Re-run the `Ecobee (Connect)` SmartApp to re-initialize the devices <br/> 2) If that does not solve the problem, remove and re-install the SmartApp |
|         	|                   	|
|         	|                   	|

"You are not authorized to perform the requested operation."

### Debug Level in SmartApp
The `Ecobee (Connect)` SmartApp allows the end user to config the Debug Level they wish to use (ranging from 1-5). The higher the level the more debug information is fed into the `Live Logging` on the SmartThings IDE.

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
- Find the `Ecobee (Connect)` SmartApp and click the link


-------------------------
## <a name="reporting-issues">Reporting Issues</a>
All issues or feature requests should be submitted to the latest release thread on the SmartThings Community. For the major release version 1.2.0, please use this thread: https://community.smartthings.com/t/release-free-ecobee-suite-version-1-2-0/94841

## <a name="license">License<a/>

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at:
      http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
