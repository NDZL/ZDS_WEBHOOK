package com.ndzl.tomcat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.imageio.ImageIO;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;



@WebServlet("/sserelay")
public class SseRelay extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SseRelay() {
        super();
        // TODO Auto-generated constructor stub
    }
    

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {

		
		String body = request.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);	
		
		if(body==null || body.length()==0) return;
		
		/*commented on sep.30, 2021 for performance reasons
		PrintWriter writer = response.getWriter();

		dump(getTimeStamp()+"::"+body+"\n");
 
		writer.close();
		*/

		//clq.add(body);
		queueBySession.forEach( (s, q) -> q.add(body) );

	}
	
	String getTimeStamp() {
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss zzz", Locale.ITALY);
        return "" + sdf2.format( Date.from( LocalDateTime.now().atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.of("CET")).toInstant() ));
	}
	
	//Object abc = new Object();
    //static String globalBody="hello";
	Integer count=100;
	//static ConcurrentLinkedQueue<String> clq = new ConcurrentLinkedQueue<String>();
	static TreeMap<String, ConcurrentLinkedQueue<String>> queueBySession = new TreeMap<String, ConcurrentLinkedQueue<String>>();
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		//qualsiasi parametro ?show   ?1234 va bene
		if(req.getParameterNames().hasMoreElements() && req.getParameterNames().nextElement().length()==4)
		{
		
			resp.setContentType("text/html");
			PrintWriter writer = resp.getWriter();
			writer.println("<!DOCTYPE html><html><body>");
			writer.println("<h1> SSE Relay Analytics </h1>");
			writer.println("<p style=\"font-family:'Lucida Console', monospace;font-size:80%;\"/>");
			writer.println("<input type=\"button\" value=\"SCROLL DOWN\" onClick=\"document.getElementById('fondo').scrollIntoView();\" />");
			writer.println(viewDump(true));
			
			writer.println( "<div id=\"fondo\">EOF</div>" );
			writer.println("</body></html>");
	        writer.close(); 
		}
		else {
			String sid = req.getSession().getId();
			queueBySession.put(sid, new ConcurrentLinkedQueue<String>());
			resp.setContentType("text/event-stream");
			resp.setCharacterEncoding("UTF-8");
			PrintWriter writer = resp.getWriter();

			//writer.print("-["+clq.size()+"]-\n");
			writer.print(":ok\n\n");
			resp.setStatus(200);
			resp.flushBuffer();
			
				while(true) {
					try {
							if(!queueBySession.get(sid).isEmpty()) {
						
								writer.println("event: message");
								
								queueBySession.get(sid).forEach(epc -> writer.println(getTimeStamp()+"---"+ queueBySession.get(sid).poll() ) );
								writer.print("\n\n");
								
								//writer.flush();//useless
							}
							else {
								writer.flush();
								resp.flushBuffer();
							}
							
							Thread.sleep(100);
						
					} catch (Exception e) {
						
						writer.println("X");
						writer.close();
					} 
				}
			
		}
	}
	

	public static void main(String[] args){
		
		
	}
	
	static void dump(String tbw) throws IOException {

		Path path = Paths.get("/chroot/home/cxntcom/cxnt48.com/hookdump.txt");
		     
		Files.write(path, tbw.getBytes(), java.nio.file.StandardOpenOption.APPEND );
	
	}
	
	static String viewDump(boolean isHtmlOutput) {
		Path path = Paths.get("/chroot/home/cxntcom/cxnt48.com/hookdump.txt");
	     
		String res = "";
		try {
			res =  new String( Files.readAllBytes(path) );
		} catch (IOException e) {
			res = "EXCP";
		}	
		if(isHtmlOutput) {
			res = res.replace("<", "&lt;");
			res = res.replace(">", "&gt;");
			res = res.replace("\n",	"<br><br>"); //place this after <> substitution!
		}
		
		return res;
	}
	
}
