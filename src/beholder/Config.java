package beholder;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
public class Config {
	private int x,
				y,
				width,
				height;
	
	private boolean visible = true;
	
	public Config() {}

	@XmlElement
	public void setX( int loc ) { x = loc; }
	public int getX() { return x; }

	@XmlElement
	public void setY( int loc ) { y = loc; }
	public int getY() { return y; }

	@XmlElement
	public void setWidth( int w ) { width = w; }
	public int getWidth() { return width; }

	@XmlElement
	public void setHeight( int h ) { height = h; }
	public int getHeight() { return height; }

	@XmlElement
	public void setVisible( boolean h ) { visible = h; }
	public boolean getVisible() { return visible; }
}