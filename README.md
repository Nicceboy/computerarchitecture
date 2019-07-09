# README #

This Repository is about to present custom implementation of example provided on University of Oulu, by Antti Juustila, in the course 'Software Architecture' in the past. Looks like course has new topic with other programming language nowdays, and this source code can be made public.

Original repository can be found [here.](https://bitbucket.org/anttijuu/keywords)

Repository contains Application that presents a Server, which supports plugins. Plugins can be anything, what can make you to track something special.
Eg. As example plugin, here is Plugin vesion of DirectoryWatcher presented in course example.
This enables you to follow changes on filesystems of Server. User is able to get notifications from server, once she/he has connected there with TCP and added something to track.

**These modules can be ignored**
* WordWatcher
* KeywordClient

No changes on code in them. WordWatcher has some changes for styling.

By following the API of KeywordPlugin presented in this repo, you are able to produce plugin which is able to follow anything you are able to implement with by the limits of API.

We have provided example console client SampleClient, which works on any Java environment.

Client can be anything, if it supports the protocol below, and uses TCP/IP protocol as well:

### Json Protocols ###

**Client to server Json:**

 Current version
* Each WordsForModule object presents one module what server is supporting
* Each module has own list of targets, and trackables for each target. Trackable could be keyword for e example.
* ExtraInfo represents they way to describe or identify the target from other targets (eg. imaginary Twitter module will need profile name for example, Directory needs filepath)

Command can be 1 or 2
 1. Change trackables to be followed
 2. Get trackables what server has recorded from you

```javascript
{
  "Command": 1,
  "WordsForModule": [
    {
      "ModuleName": "Twitter",
      "TrackableAndTargetPair": [
        {
          "TrackablesToAdd": [
            "Word1",
            "Word2"
          ],
          "TrackablesToRemove": [
            "Word3",
            "Word4"
          ],
          "ExtraInfo": "OmenaTwitterZ"
        }
      ]
    }
  ]
}
```

**Server to client Json:**

Server lists information of each module to client, giving a detailed description and usage of module for client.
Current followed trackables for each followed target will represented for client.


ResponseType can be 1, 2, 3 and 4
 1. Change succeed
 2. Giving detailed list of words in watch list
 3. Trackable found
 4. Error

```javascript
{
	"ResponseType": 1,
	"AdditionalInfo": "Error on adding",
	"ModuleList": [
		{
			"ModuleName": "Twitter",
			"ModuleDesc": "This module will search/watch words found in twitter from specific username",
			"ModuleUsage": "This module needs twitter username",
			"WatchList": [
				{
					"ModuleTarget": "OmenatwitterZ",
					"Trackables": [
						"Omena",
						"Appelsiini"
					]
				},
				{
					"ModuleTarget": "AppelsiinitwitterZ",
					"Trackables": [
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
					"Trackables": [
						"Päärynä",
						"Kiiwi"
					]
				},
				{
					"ModuleTarget": "C:/Paula/Documents",
					"Trackables": [
						"Hevonen",
						"Koira"
					]
				}
			]
		}
	],
	"NotificationContent": {
		"ModuleName": "Twitter",
		"ModuleTarget": "Paukku",
		"Trackables": [
			"Hevonen",
			"Koira"
		]
	}
}
```


## API for making plugins

Source file for plugin API can be found from [here.](/KeywordPlugin/src/org/keyword/plugin/KeywordPlugin.java)

Plugins have to implement interface to be eligible to work as plugin.
Class FailedToDoPluginThing is not necessary, as class defined in interface is enough for functionality.

## Clients?
Any client which will follow the JSON protocol above, and is using TCP/IP connection  is able to use server for tracking things.

## Building
buildall scripts will build all sources to path [build](/build).
ExampleServer and ExampleClient are ready  to go.
Libs/plugins have been created/copied to correct folders.

.sh script is not tested, but theorically should work..
