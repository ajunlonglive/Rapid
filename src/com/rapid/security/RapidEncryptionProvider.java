package com.rapid.security;

import com.rapid.utils.Encryption.EncryptionProvider;

public class RapidEncryptionProvider implements EncryptionProvider {
	
	private static final char[] PASSWORD = "rapiddesktop123".toCharArray();
    private static final byte[] SALT = {
        (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
        (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
    };

	@Override
	public char[] getPassword() {
		return PASSWORD;
	}

	@Override
	public byte[] getSalt() {
		return SALT;
	}

}