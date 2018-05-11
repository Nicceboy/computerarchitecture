# README #

This Repository is about to present custom implementation of example provided on University of Oulu, by Antti Juustila, in the course 'Software Architecture'.
Original repository can be found [here.](https://bitbucket.org/anttijuu/keywords)

Repository contains Application that presents a Server, which supports plugins. Plugins can be anything, what can make you to track something special.
Eg. As example plugin, here is Plugin vesion of DirectoryWatcher presented in course example.
This enables you to follow changes on filesystems of Server. User is able to get notifications from server, once she/he has connected there with TCP and added something to track. 

By following the API of KeywordPlugin presented in this repo, you are able to produce plugin which is able to follow anything you are able to implement with by the limits of API.

We have provided example console client SampleClient, which works on any Java environment.

Client can be anything, if it supports the protocol below, and uses TCP/IP protocol as well:

### Json Protocols ###

**Client to server Json:**

 Current version
* Number of WordsForModule object lists is amount of modules
* Each module has own list of targets, clear individual trackables

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
Command can be 1 or 2
 1. Change trackables to be followed
 2. Get trackables what server has recorded from you

ExtraInfo is info needed for different module (eg. imaginary Twitter module will need Url, Directory needs filepath)


**Server to client Json:**

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

ResponseType can be 1, 2, 3 and 4
 1. Change succeed
 2. Giving detailed list of words in watch list
 3. Trackable found
 4. Error
 