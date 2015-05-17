package com.gsm_ccs_client;

import java.util.UUID;

public class UUIDGenerator {
	/**
	 * Creates an unique message ID.
	 * <p>
	 * If unable to create an ID, it will return a null string
	 */
	public static String generate()
	{
		String uniqueUUID = "";
		UUID uuid = UUID.randomUUID();
		uniqueUUID = String.valueOf(uuid);
		
		return uniqueUUID;
	}
}
