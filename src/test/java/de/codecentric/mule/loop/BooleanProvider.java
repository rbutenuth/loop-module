package de.codecentric.mule.loop;

/**
 * Return size times <code>true</code>, then <code>false</code>.
 */
public class BooleanProvider {
	private int remaining;
	
	public BooleanProvider(int size) {
		remaining = size;
	}
	
	public synchronized boolean fetch() {
		if (remaining > 0) {
			remaining--;
			return true;
		} else {
			return false;
		}
	}
}
