package com.solegaonkar.servercommunication;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;

import com.solegaonkar.servercommunication.Action.ActionInvocationListener;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Handle incoming HTTP Requests and forward them to other servers or run local
 * commands as required.
 * 
 * @author vs0016025
 *
 */
public class Communicator {
	private static HashMap<String, Action> methods = new HashMap<>();
	private static final String ConfigFilePath = "ConfigFile.txt";

	/**
	 * If you want to run this stand alone, start here.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		initiate();
	}

	/**
	 * Start here if you want to run this along with an existing Java Application.
	 * Reads the config file and initiates the HTTP Server.
	 * 
	 * @throws Exception
	 */
	public static void initiate() throws Exception {
		readConfig();
		createServer();
	}

	/**
	 * If you are running as a part of a Java application, add a Runnable to be
	 * invoked when the method is invoked. The hook will run within the client
	 * thread.
	 * 
	 * @param methodName
	 * @param hook
	 * @return
	 */
	public static boolean addHook(String methodName, ActionInvocationListener hook) {
		Action a = methods.get(methodName);
		if (a != null) {
			a.setHook(hook);
			return true;
		}
		return false;
	}

	/**
	 * If you want to initiate an action within the Java Code.
	 * 
	 * @param methodName
	 */
	public static void initiateAction(String methodName) {
		Action a = methods.get(methodName);
		if (a != null) {
			a.initiate(null);
		}
	}

	/**
	 * Read the configuration file.
	 * 
	 * @throws Exception
	 */
	private static void readConfig() throws Exception {
		// Create the methods map
		BufferedReader br = new BufferedReader(new FileReader(ConfigFilePath));
		String configLine = "";
		while ((configLine = br.readLine()) != null) {
			String[] config = configLine.split(":::");
			Action a = new Action();
			a.setSourceIp(config[0].trim());
			a.setMethod(config[1].trim());
			a.setTargetIp(config[2].trim());
			a.setTargetMethod(config[3].trim());
			a.setLocalCommand(config[4].trim());
			methods.put(a.getMethod(), a);
		}
		br.close();
	}

	/**
	 * Create the HTTP Server and assign HTTP Haandlers to take care of the required
	 * functionality
	 * 
	 * @throws Exception
	 */
	private static void createServer() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
		for (String key : methods.keySet()) {
			server.createContext("/" + methods.get(key).getMethod(), new CustomHandler(methods.get(key)));
		}
		server.setExecutor(null); // creates a default executor
		server.start();
	}

	/**
	 * Custom HTTP Handler to take care of an action specified in the config file
	 * 
	 * @author vs0016025
	 *
	 */
	static class CustomHandler implements HttpHandler {
		private Action action;

		public CustomHandler(Action action) {
			super();
			this.action = action;
		}

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			action.initiate(exchange);
		}
	}

	/**
	 * Action corresponds to a record in the config file. It defines the source and
	 * target IP Addresses as well as the source and target methods along with any
	 * local command that needs to be run.
	 * 
	 * @author vs0016025
	 *
	 */
}
