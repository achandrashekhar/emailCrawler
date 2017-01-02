package emailCrawler;

import org.apache.velocity.app.VelocityEngine;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;



public class EmailCrawlerServer {
	
	private static int PORT = 8080;

	public static void main(String[] args) {
		Server server = new Server(PORT);

		
		ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
		servletContextHandler.addServlet(LoginServlet.class,"/login");
		servletContextHandler.addServlet(LoginServlet.class,"/");
		servletContextHandler.addServlet(ShowSpendingsServlet.class,"/showspendings");
		// initialize velocity
        VelocityEngine velocity = new VelocityEngine();
		velocity.init();
		
        servletContextHandler.setContextPath("/");
        servletContextHandler.setAttribute("templateEngine", velocity);
		server.setHandler(servletContextHandler);
		try {
			server.start();
			server.join();

		} catch (Exception ex) {
			System.out.println("An exception occurred while running the server. ");
			System.exit(-1);
		}
	}

}
