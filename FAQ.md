Free Universal Ecobee Suite FAQ 
======================================================

### During initial authentication, When I hit save, I get a Red Box error “Device still in use. Remove from any SmartApps or Dashboards.” 
### After completing authentication, I get the error "Your are not authorized to perform the requested operation." when I try to exit out of Ecobee Suite Manager.

During authentication, ES Manager creates 2 temporary devices to verify that the device drivers are properly installed, and it tries to delete them immediately afterwards. Occasionally the delete fails, especially if you are running Health Check (the devices register as 'in use' during health checks). 

To clear this up in SmartThings, simply go into the IDE, click `My Devices` at the top of the page, and then find and delete the two “test” devices - one thermostat and one sensor. Then go back and re-open Ecobee Suite Manager on your mobile device, and it should proceed…

On Hubutat, you can delete the "test" devices from the "Devices" page which is accessed from the left menu pane of the Hubitat web interface.

### How do I report a problem with Ecobee Suite?
I prefer to have issues reported using the current [RELEASE] threads on either [hubitat.community.com](https://community.hubitat.com/t/release-universal-ecobee-suite-version-1-7/15813) or [smartthings.community.com](https://community.smartthings.com/t/release-universal-ecobee-suite-version-1-7/163929).

When reporting an issue, I require:
* A description of the problem, including the operation that didn't work, or caused the issue
* The **Version Numbers** for the following:
  - **Ecobee Suite Manager**
  - **Ecobee Suite Thermostat**
  - And if your problem is related to a Helper app, the **Ecobee Suite XXXX Helper**
* A snapshot of Live Logging for each of the above *at the time the problem occured.* The log entries are designed to help us both understand what is going on. Note that most problems are related to a failure or error condition somewhere, so look for the keywords 'error' and 'warn' in the Live Log. 

Send all of the above to me in the appropriate forum - ideally in a PM because the logs do reveal certain "secrets" about your environment.

### (ST) My EcoSensor and EcoTherm devices all show as 'Unavailable' on SmartThings

I generally advise ES users to turn off Health Check. SmartThings never officially released support for 3rd party applications using Health Check, and they seem to continuously change the implementation so that reverse-engineered support for it sometimes fails for no apparent reason.

That said, the code does implement the latest known Health Check mechanism, so this problem may no longer appear.

### Ecobee Suite doesn't support Ecobee Switches - why not?

Ecobee hasn't released an official API for switches, and the current SmartThings-supplied support uses polling to update the switch status. Since the API is not released, I don't feel it appropriate to copy the SmartThings implementation.

### (ST) How do I use Ecobee Switches and Ecobee Suite at the same location?

Easy - just install the SmartThings native support and enable the switch support. You can run both Ecobee Suite and the native SmartThings (or Hubitat) support at the same location/hub.

Note that since the polling frequency is five minutes, the switch state in SmartThings lags far behind the actual switch state (up to 5 minutes). Until Ecobee releases an asynchronous API for the switches, response time is a little too slow for my liking.

### (ST) Does Ecobee Suite work with the new Samsung Connect/SmartThings mobile app?

Yes - as of about June 1, 2019, everything seems to be working correctly in the new App. The thermostat and sensor devices don't display the custom UI of Ecobee Suite, but you do get the ability to control the basic features of them in the new UI. All of the Helper apps appear to function correctly as well.

Some users report that they cannot install using the new app, but after installing with SmartThings Classic, the thermostat and sensor devices appear correctly.

Note that the sensor devices in the new app appear to show the motion sensor as the default device instead of the temperature. I have been unable to find a way to change this for Ecobee Suite.
