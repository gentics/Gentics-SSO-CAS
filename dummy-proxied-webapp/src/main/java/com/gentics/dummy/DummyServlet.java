package com.gentics.dummy;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setStatus(200);
		PrintWriter pw = new PrintWriter(response.getWriter());
		pw.write("Hello World - This is the proxied application speaking (Backend)\n");
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
			for(Object key : assertion.getPrincipal().getAttributes().keySet()) {
				String value = (String)assertion.getPrincipal().getAttributes().get(key);
				pw.write("Attribute: " + key  + " " +value + "\n" );	
			}
		} else {
			pw.write("Assertion is null\n");
		}

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
