package com.solegaonkar.servercommunication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import com.sun.net.httpserver.HttpExchange;

/**
 * The task to be performed on a local or external request
 * 
 * @author vs0016025
 *
 */
public class Action implements Runnable {
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
	 * @return the sourceIp
	 */
	public String getSourceIp() {
		return sourceIp;
	}

	/**
	 * @param sourceIp
	 *            the sourceIp to set
	 */
	public void setSourceIp(String sourceIp) {
		this.sourceIp = sourceIp;
	}

	/**
	 * @return the targetIp
	 */
	public String getTargetIp() {
		return targetIp;
	}

	/**
	 * @param targetIp
	 *            the targetIp to set
	 */
	public void setTargetIp(String targetIp) {
		this.targetIp = targetIp;
	}

	/**
	 * @return the method
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * @param method
	 *            the method to set
	 */
	public void setMethod(String method) {
		this.method = method;
	}

	/**
	 * @return the targetMethod
	 */
	public String getTargetMethod() {
		return targetMethod;
	}

	/**
	 * @param targetMethod
	 *            the targetMethod to set
	 */
	public void setTargetMethod(String targetMethod) {
		this.targetMethod = targetMethod;
	}

	/**
	 * @return the localCommand
	 */
	public String getLocalCommand() {
		return localCommand;
	}

	/**
	 * @param localCommand
	 *            the localCommand to set
	 */
	public void setLocalCommand(String localCommand) {
		this.localCommand = localCommand;
	}

	/**
	 * @return the hook
	 */
	public ActionInvocationListener getHook() {
		return hook;
	}

	/**
	 * @param hook
	 *            the hook to set
	 */
	public void setHook(ActionInvocationListener hook) {
		this.hook = hook;
	}

	/**
	 * @return the exchange
	 */
	public HttpExchange getExchange() {
		return exchange;
	}

	/**
	 * @param exchange
	 *            the exchange to set
	 */
	public void setExchange(HttpExchange exchange) {
		this.exchange = exchange;
	}

	public static interface ActionInvocationListener {
		public void run(ArrayList<String> parameters);
	}

}
