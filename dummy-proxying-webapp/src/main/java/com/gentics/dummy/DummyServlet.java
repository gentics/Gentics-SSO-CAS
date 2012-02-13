package com.gentics.dummy;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jasig.cas.client.util.AssertionHolder;
import org.jasig.cas.client.validation.Assertion;

/**
 * Servlet implementation class DummyServlet
 */
public class DummyServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public DummyServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	public final String BACKEND_SERVICE_URL = "https://mybackend/backend/DummyServlet";

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		PrintWriter pw = new PrintWriter(response.getWriter());
		response.setStatus(200);

		String encodedBackendServiceUrl = URLEncoder.encode(BACKEND_SERVICE_URL, "UTF-8");
		String casServerUrlPrefix = this.getServletContext().getInitParameter("casServerUrlPrefix");
		pw.write("\ncasServerUrlPrefix: " + casServerUrlPrefix);

		pw.write("Hello World - This is the proxying application speaking (Frontend)\n");
		if (request.getUserPrincipal() != null) {
			pw.write("Request.getUserPrincipal().getName(): " + request.getUserPrincipal().getName() + "\n");
		}
		pw.write("Request.getRemoteUser(): " + request.getRemoteUser() + "\n");

		for (Cookie cookie : request.getCookies()) {
			pw.write("Cookie - Name: " + cookie.getName() + " Domain: " + cookie.getDomain() + " Path: " + cookie.getPath() + " Value: "
					+ cookie.getValue() + "\n");
		}

		Assertion assertion = AssertionHolder.getAssertion();
		if (assertion != null) {

			pw.write("\n");
			pw.write("Assertion: " + assertion.getPrincipal().getName());

			for (Object key : assertion.getPrincipal().getAttributes().keySet()) {
				String value = (String) assertion.getPrincipal().getAttributes().get(key);
				pw.write("\nAttribute: " + key + " " + value);
			}

			// Fetch the proxy ticket for our service url
			String proxyTicketId = assertion.getPrincipal().getProxyTicketFor(BACKEND_SERVICE_URL);
			pw.write("\n\nProxyTicket for: " + BACKEND_SERVICE_URL + " : " + proxyTicketId);

			URL backendRequestURL = new URL(BACKEND_SERVICE_URL + "?ticket=" + proxyTicketId);
			pw.write("\n\n\nResponse of [" + backendRequestURL.toExternalForm() + "]: " + getResponse(backendRequestURL));

		} else {
			pw.write("Assertion is null\n");
		}

		// pw.write("\n\n--------------------------------------");
		//
		// pw.write("\nReponse from backend Service DummyServlet [" + backendServlet + "]");
		// pw.write("\nReponse: " + getResponse(new URL(backendServlet)));

	}

	public String getResponse(URL url) throws HttpException, IOException {

		HttpClient client = new HttpClient();
		HttpMethod method = new GetMethod(url.toExternalForm());
		method.setFollowRedirects(true);
		int statusCode = client.executeMethod(method);
//		if (statusCode != HttpStatus.SC_OK) {
//			System.err.println("Method failed: " + method.getStatusLine());
//		}

		return method.getResponseBodyAsString();

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setStatus(200);
		PrintWriter pw = new PrintWriter(response.getWriter());
		pw.write("Hello World");
	}

}
