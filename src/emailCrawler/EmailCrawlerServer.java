package emailCrawler;

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
