/**
 * 
 */
package de.champonthis.ena.eke.proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 
 * @author Lurkars
 *
 */
@SpringBootApplication
@EnableScheduling
public class Application extends SpringBootServletInitializer {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
