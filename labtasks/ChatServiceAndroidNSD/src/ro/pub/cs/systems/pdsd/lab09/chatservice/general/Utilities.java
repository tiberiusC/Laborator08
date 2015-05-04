package ro.pub.cs.systems.pdsd.lab09.chatservice.general;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

import android.util.Log;

public class Utilities {
	
	public static BufferedReader getReader(Socket socket) {
		try {
			return new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException ioException) {
			Log.e(Constants.TAG, "An exception has occured: " + ioException.getMessage());
			if (Constants.DEBUG) {
				ioException.printStackTrace();
			}
		}
		return null;
	}
	
	public static PrintWriter getWriter(Socket socket) {
		try {
			return new PrintWriter(new BufferedOutputStream(socket.getOutputStream()));
		} catch (IOException ioException) {
			Log.e(Constants.TAG, "An exception has occured: " + ioException.getMessage());
			if (Constants.DEBUG) {
				ioException.printStackTrace();
			}
		}
		return null;
	}
	
	public static String generateIdentifier(int length) {
		StringBuffer result = new StringBuffer("-");
		
		Random random = new Random();
		for (int k = 0; k < length; k++) {
			result.append((char)(Constants.FIRST_LETTER + random.nextInt(Constants.ALPHABET_LENGTH)));
		}
		return result.toString();
	}

}
