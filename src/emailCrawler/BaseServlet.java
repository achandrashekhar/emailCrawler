package emailCrawler;


import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

/**
 * Provides base functionality to all servlets in this example. Original author:
 * Prof. Engle
 *
 * @see RegisterServer
 */
@SuppressWarnings("serial")
public class BaseServlet extends HttpServlet {
	

	protected void prepareResponse(String title, HttpServletResponse response, HttpServletRequest request) {
		try {
			HttpSession session = request.getSession();
			String uname = (String) session.getAttribute("username");
			PrintWriter writer = response.getWriter();

			writer.println("<!DOCTYPE html>");
			writer.println("<html>");
			writer.println("<head>");
			writer.println("\t<title>" + title + "</title>");
			writer.print("<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\" integrity=\"sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u\" "
					+ "crossorigin=\"anonymous\">");
			
			writer.print("<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css\""
					+ "	integrity=\"sha384-rHyoN1iRsVXV4nD0JutlnGaslCJuC7uwjduW9SVrLvRYooPp2bWYgmgJQIXwl/Sp\" crossorigin=\"anonymous\">");
			
			writer.print("<script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js\" integrity=\"sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa\" "
					+ "crossorigin=\"anonymous\"></script>");
			
			writer.print("<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.1.1/jquery.min.js\"></script>");
			writer.println("</head>");
			writer.println("<body>");
			
			 /*  first, get and initialize an engine  */
	        VelocityEngine ve = new VelocityEngine();
	        ve.init();
	        /*  next, get the Template  */
	        Template t = ve.getTemplate( "HTML_PAGES/navigation.html" );
	        /*  create a context and add data */
	        VelocityContext context = new VelocityContext();
	        if(uname == null){
	        	uname = "guest";
	        	   context.put("login-logout", "login");
	        	   context.put("userName", uname);
	        	   context.put("link", "login");
	        	
	        }
	        else {
	        	
	        	context.put("login-logout", "logout");
	        	   context.put("userName", uname);
	        	   context.put("link", "logout");
	        }
	     
	        t.merge( context, writer );
			
			
		} catch (IOException ex) {
			System.out.println("IOException while preparing the response: " + ex);
			return;
		}
	}
	
//	protected void prepareResponse2(String title, HttpServletResponse response) {
//		try {
//			
//			PrintWriter writer = response.getWriter();
//
//			writer.println("<!DOCTYPE html>");
//			writer.println("<html>");
//			writer.println("<head>");
//			writer.println("\t<title>" + title + "</title>");
//			writer.println("</head>");
//			writer.println("<body>");
//			
//			writer.println();
//		} catch (IOException ex) {
//			System.out.println("IOException while preparing the response: " + ex);
//			return;
//		}
//	}

	protected void finishResponse(HttpServletResponse response,HttpServletRequest request) {
		HttpSession session = request.getSession();
		String uname = (String) session.getAttribute("username");
		try {
			PrintWriter writer = response.getWriter();

			writer.println();
			writer.println("<p> </p>");
			writer.println("<p style=\"font-size: 10pt; font-style: italic;\">");
			writer.println("Last updated at " + getDate());
			writer.println("</p>");
			String date = (String) session.getAttribute("lastLogin");
			writer.println("<p style=\"font-size: 10pt; font-style: italic;\">");
			writer.println("Last logged in on " + date);
			writer.println("</p>");
			writer.println("</body>");
			writer.println("</html>");

			writer.flush();

			response.setStatus(HttpServletResponse.SC_OK);
			response.flushBuffer();
		} catch (IOException ex) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
	}

	protected String getDate() {
		String format = "hh:mm a 'on' EEE, MMM dd, yyyy";
		DateFormat dateFormat = new SimpleDateFormat(format);
		return dateFormat.format(Calendar.getInstance().getTime());
	}

	/**
	 * Return a cookie map from the cookies in the request
	 * 
	 * @param request
	 * @return
	 */
	protected Map<String, String> getCookieMap(HttpServletRequest request) {
		HashMap<String, String> map = new HashMap<String, String>();

		Cookie[] cookies = request.getCookies();

		if (cookies != null) {
			for (Cookie cookie : cookies) {
				map.put(cookie.getName(), cookie.getValue());
			}
		}

		return map;
	}

	/**
	 * Clear cookies
	 * 
	 * @param request
	 * @param response
	 */
	protected void clearCookies(HttpServletRequest request, HttpServletResponse response) {
		Cookie[] cookies = request.getCookies();

		if (cookies == null) {
			return;
		}

		for (Cookie cookie : cookies) {
			cookie.setValue("");
			cookie.setMaxAge(0);
			response.addCookie(cookie);
		}
	}

	protected void clearCookie(String cookieName, HttpServletResponse response) {
		Cookie cookie = new Cookie(cookieName, null);
		cookie.setMaxAge(0);
		response.addCookie(cookie);
	}

	

	
}