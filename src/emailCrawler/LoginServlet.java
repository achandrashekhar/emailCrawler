package emailCrawler;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

//import org.apache.commons.lang3.StringEscapeUtils;

/** 
 * A servlet that handles user Login. doGet() method displays an HTML form with a button and
 * two textfields: one for the username, one for the password.
 * doPost() processes the form: if the username is not taken, it adds user info to the database.
 *
 */
@SuppressWarnings("serial")
public class LoginServlet extends BaseServlet {
	
	ReadingEmail readEmailObject = new ReadingEmail();


	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		HttpSession session = request.getSession();
		String uname = (String) session.getAttribute("username");
		
		if(uname!=null){
			String date = getDate();
			String url = "/showspendings?month="+date;
			url = response.encodeRedirectURL(url);
			response.sendRedirect(url);
		}
		prepareResponse("Logging in ", response,request);

		PrintWriter out = response.getWriter();
		
		 /*  first, get and initialize an engine  */
        VelocityEngine ve = new VelocityEngine();
        ve.init();
        /*  next, get the Template  */
        Template t = ve.getTemplate( "HTML_PAGES/LoginForm2.html" );
        /*  create a context and add data */
        VelocityContext context = new VelocityContext();
  
        t.merge( context, out );

		//displayForm(out); 
		finishResponse(response,request);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		String emailId = (String) request.getAttribute("newuser");
		String password = (String) request.getAttribute("newpass");
		prepareResponse("show spendings", response, request);
		//this will get current default month
		String currentMonth = getDate();
		readEmailObject.loginUser(emailId, password, request, response, currentMonth);
		
	}
	
	protected String getDate() {
		String format = "MMM";
		DateFormat dateFormat = new SimpleDateFormat(format);
		return dateFormat.format(Calendar.getInstance().getTime());
	}

	
}