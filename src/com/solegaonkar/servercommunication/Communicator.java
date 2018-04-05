package com.solegaonkar.servercommunication;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

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
			a.hook = hook;
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
			a.sourceIp = config[0].trim();
			a.method = config[1].trim();
			a.targetIp = config[2].trim();
			a.targetMethod = config[3].trim();
			a.localCommand = config[4].trim();
			methods.put(a.method, a);
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
			server.createContext("/" + methods.get(key).method, new CustomHandler(methods.get(key)));
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
	static class Action implements Runnable {
		private String sourceIp;
		private String targetIp;
		private String method;
		private String targetMethod;
		private String localCommand;
		private ActionInvocationListener hook;

		private HttpExchange exchange;

		/**
		 * Initiate the action in a new thread.
		 * 
		 * @param exchange
		 */
		public void initiate(HttpExchange exchange) {
			this.exchange = exchange;
			new Thread(this).start();
		}

		/**
		 * Execute the configured action. Invoke the HTTP Request / Local Command if
		 * configured.
		 */
		public void run() {
			try {
				if (exchange == null || sourceIp.equals(exchange.getRemoteAddress().getHostString())) {
					ArrayList<String> parameters = getParameters();
					forwardHttpRequest(parameters);
					invokeHook(parameters);
					runLocalCommand(parameters);
				}
				respond();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void respond() throws IOException {
			if (exchange != null) {
				String response = "Response";
				exchange.sendResponseHeaders(200, response.length());
				OutputStream os = exchange.getResponseBody();
				os.write(response.getBytes());
				os.close();
			}
		}

		/**
		 * If the action has a hook, invoke it in the same thread.
		 */
		private void invokeHook(ArrayList<String> parameters) {
			if (hook != null) {
				hook.run(parameters);
			}
		}

		/**
		 * Run the local command if configured.
		 * 
		 * @param parameters
		 * @throws IOException
		 */
		private void runLocalCommand(ArrayList<String> parameters) throws IOException {
			if (localCommand.length() > 0) {
				StringBuilder sb = new StringBuilder();
				sb.append(localCommand);
				sb.append(" ");
				for (int i = 1; i < parameters.size(); i++) {
					sb.append(parameters.get(i));
					sb.append(" ");
				}
				Runtime.getRuntime().exec(sb.toString());
			}
		}

		/**
		 * Forward to HTTP Request if configured
		 * 
		 * @param parameters
		 * @throws Exception
		 */
		private void forwardHttpRequest(ArrayList<String> parameters) throws Exception {
			if (targetIp.length() > 0) {
				URL url = new URL("http://" + targetIp + "/" + targetMethod + parameters.get(0));
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");
				connection.connect();
				new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
				connection.disconnect();
			}
		}

		/**
		 * Get parameters of the input HTTP Request
		 * 
		 * @return
		 */
		private ArrayList<String> getParameters() {
			ArrayList<String> parameters = new ArrayList<>();
			String[] ext = exchange.getRequestURI().toString().split("\\?");
			if (ext.length > 1) {
				parameters.add("?" + ext[1]);
				for (String x : ext[1].split("&")) {
					parameters.add(x);
				}
			} else {
				parameters.add("");
			}
			return parameters;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "Action [sourceIp=" + sourceIp + ", targetIp=" + targetIp + ", method=" + method + ", targetMethod="
					+ targetMethod + ", localCommand=" + localCommand + "]";
		}

		/**
		 * @param hook
		 *            the hook to set
		 */
		public void setHook(ActionInvocationListener hook) {
			this.hook = hook;
		}
	}
	
	public interface ActionInvocationListener {
		public void run(ArrayList<String> parameters);
	}
}
