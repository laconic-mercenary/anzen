/**
 * 
 */
package anzen.app.ui;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import org.apache.commons.lang3.RandomUtils;

/**
 * @author mlcs05
 *
 */
@Named
@RequestScoped
public class RandomColor {

	private static final String[] COLORS = { 
		"red",  
		"blue",  
		"white",
		"purple"
	};
	
	public String getColor() {
		int idx = RandomUtils.nextInt(0, COLORS.length);
		return COLORS[idx];
	}
}
