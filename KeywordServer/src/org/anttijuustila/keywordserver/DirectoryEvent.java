package org.anttijuustila.keywordserver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Observer;

class DirectoryEvent {
	public enum Event { ECreated, EModified }

	public Event event;
	public String fileName;
	public DirectoryEvent(Event e, String file) {
		event = e;
		fileName = file;
	}
}
