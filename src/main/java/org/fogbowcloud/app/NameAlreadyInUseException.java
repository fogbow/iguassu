package org.fogbowcloud.app;

public class NameAlreadyInUseException extends Exception {
	
	private static final long serialVersionUID = -7478338187106923555L;

	public NameAlreadyInUseException(String message) {
		super(message);
	}
}
