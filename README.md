# README #
### Json Protocols ###

**Client to server Json:**
```javascript
{
  "Command": 1,
  "WordsForModule": [
    {
      "WordsAndModulePair": [
        {
          "Keywords": [
            "Word1",
            "Word2"
          ],
          "Modules": [
            {
              "ModuleName": "Twitter",
              "ExtraInfo": "OmenaTwitterZ"
            },
            {
              "ModuleName": "Directory",
              "ExtraInfo": "C:/User/Directory"
            }
          ]
        }
      ]
    }
  ]
}
```
Command can be 1, 2 or 3
 1. Add word to watch list
 2. Remove word from watch list
 3. Give words in watch list

ExtraInfo is info needed for different module (Twitter module will need Url, Directory needs filepath)

**Server to client Json:**

```javascript
{
  "ResponseType": 1,
  "ModuleList": [
    {
      "ModuleName": "Twitter",
      "ModuleDesc": "This module will search/watch words found in twitter from specific username",
      "ModuleUsage": "This module needs twitter username",
      "WatchList": [
        {
          "ModuleTarget": "OmenatwitterZ",
          "Words": [
            "Omena",
            "Appelsiini"
          ]
        },
        {
          "ModuleTarget": "AppelsiinitwitterZ",
          "Words": [
            "Päärynä",
            "Kiiwi"
          ]
        }
      ]
    },
    {
      "ModuleName": "Directory",
      "ModuleDesc": "This module will search/watch directory for words in either name of file or in the content of file from specific directory",
      "ModukeUsage": "This module needs filepath to directory",
      "WatchList": [
        {
          "ModuleTarget": "C:/User/Directory",
          "Words": [
            "Päärynä",
            "Kiiwi"
          ]
        },
        {
          "ModuleTarget": "C:/Paula/Documents",
          "Words": [
            "Hevonen",
            "Koira"
          ]
        }
      ]
    }
  ]
}
```

ResponseType can be 1, 2 and 3
 1. Added words to watch list
 2. Removed words from watch list
 3. Giving detailed list of words in watch list

### How do I get set up? ###

1. Clone the project from bitbucket.
    * Dependencies
        * Uses https://code.google.com/archive/p/json-simple/ on Client API and service to create/parse JSON.
        * Download the json-simple-1.1.1.jar to project root directory before building and running the Client or Server.
2. Build the service and the client API by running the buildall.sh script on the root directory of the project.
    * If on Windows, use the buildall.cmd and/or build.cmd files in subdirectories for building.
	* You can also build the components separately in each subdirectory where you find the build.sh (build.cmd) script.
	* After this, you will have KeywordServer.jar and KeywordClient.jar as the binary components.
3. buildall.sh (.cmd) script also copies the KeywordClient.jar and json-simple-1.1.1.jar to the Android app *jars* directory to be used then building the Android app. 
4. Open Android Studio and open the WordWatch project from there. The Android app in WordWatch directory assumes Android API level 25 but works on API level 23 and perhaps also above. Best to use API level 25.
5. Add the two .jar files to the project:
    * Right-click the app module > Open Module Settings > Dependencies > Add the two jar files from the WordWatch/libs directory as Implementation dependencies.
5. Build the Android app.

Now you can run the service and then the Android client on the Android emulator to test if everything works:

1. Run the service using the start.sh (start.cmd on Win) script provided in the KeywordServer directory. It will launch the service in the KeywordServer.jar component.
2. Run the Android app in the Android emulator.
3. When the app starts, select the menu, go to Settings > General and change the address of the service to 10.0.2.2. Also, change the path to match some path on your disk which the service can access.
4. Then you should be able to connect to the service using the Connect button on the app.
5. Add keywords to watch using the text box and button. Words appear on the list.
6. Create a test document using a text editor on the directory of the path. Add some text, and especially some of the keywords you wanted to watch. Save your document.
7. Soon (there is a delay) you should see a notification on the Android app that the keyword was used.

Service uses the port 10000 to listen to incoming connection attempts. If it is reserved for other use, you will need to change the port in the KeywordServer.java, line 55 (currently).

If you run the Android app on a real device, make sure to follow these steps:

1. Check the IP address of the machine where the service runs, using your system settings app.
2. Enter this IP address to the WordWatch Android app settings as the service address.
3. Make sure the service and the Android app are on the *same local network*, or that the service IP address is reachable from the network the Android device is using.

Firewalls may prevent the connection between the devices. If you can edit your firewall settings, open the port 10000 the service is using to accept incoming connections.

### Contribution guidelines ###

* TBD
* Antti Juustila
