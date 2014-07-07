package beholder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.font.TextAttribute;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.mythicscape.batclient.interfaces.BatClientPlugin;
import com.mythicscape.batclient.interfaces.BatClientPluginCommandTrigger;
import com.mythicscape.batclient.interfaces.BatClientPluginTrigger;
import com.mythicscape.batclient.interfaces.BatClientPluginUtil;
import com.mythicscape.batclient.interfaces.BatWindow;
import com.mythicscape.batclient.interfaces.ParsedResult;

public class Beholder extends BatClientPlugin implements BatClientPluginTrigger, 
														 BatClientPluginCommandTrigger,
														 BatClientPluginUtil {
	BatWindow window = null;
	Config config = null;
	String configPath = null;
	
	private static final Color TEXT_COLOR = new Color( 238, 238, 238 );
	private static final Color RED = new Color( 242, 70, 17 );

	final static int NW = 0,
			  		 N = 1,
			  		 NE = 2,
			  		 W = 3,
			  		 C = 4,
			  		 E = 5,
			  		 SW = 6,
			  		 S = 7,
			  		 SE = 8;
	
	final static String labels[] = { "NW", "N", "NE", "W", "*", "E", "SW", "S", "SE" };
	boolean displayButton[] = { false, false, false, false, false, false, false, false, false };
	boolean fireInExit[] = { false, false, false, false, false, false, false, false, false };
	
	Map<String, Integer> map = new HashMap<String, Integer>(),
						 upperCaseMap = new HashMap<String, Integer>();
	
	JButton buttons[] = new JButton[ 9 ];
	
	Pattern obviousExitsPattern = Pattern.compile( "^Obvious exits? (is|are): (.*)$" );
	Pattern newWallPattern = Pattern.compile( "A wall of searing flames raises up from the ground to the (.*)\\." );
	Pattern existingWallPattern = Pattern.compile( "A curtain of flames covers the ([a-zA-Z, ]+) exit" );
	
	boolean exitsHandledByExistingWallTrigger = false;
	
	ParsedResult fireMessage = new ParsedResult( " FIRE! \n" );
	
	public void loadPlugin() {
		config = new Config();
		configPath = this.getClientGUI().getBaseDirectory() + "/conf/beholder.xml";
		
		JAXBContext jaxbContext;
		try {
			jaxbContext = JAXBContext.newInstance(Config.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			File XMLfile = new File( configPath );
			config = (Config) jaxbUnmarshaller.unmarshal(XMLfile);
		} catch (JAXBException e) {
			this.getClientGUI().printText("debug", e.getStackTrace().toString());
		}
		
		window = this.getClientGUI().createBatWindow("Beholder", config.getX(), config.getY(), 
														config.getWidth(), config.getHeight());
		window.setVisible(true);
		createButtons(window);
		
		fireMessage.addAttribute( TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD );
		fireMessage.addAttribute( TextAttribute.FOREGROUND, Color.RED );
		
		buildMap();
		
		this.getClientGUI().printText("generic", "--- Loaded BeholderMappingPlugin ---\n");
	}

	private void buildMap() {
		map.put( "northwest", 0 );
		map.put( "north", 1 );
		map.put( "northeast", 2 );
		map.put( "west", 3 );
		map.put( "east", 5 );
		map.put( "southwest", 6 );
		map.put( "south", 7 );
		map.put( "southeast", 8 );
		map.put( "nw", 0 );
		map.put( "n", 1 );
		map.put( "ne", 2 );
		map.put( "w", 3 );
		map.put( "e", 5 );
		map.put( "sw", 6 );
		map.put( "s", 7 );
		map.put( "se", 8 );
		
		upperCaseMap.put( "NW", 0 );
		upperCaseMap.put( "N", 1 );
		upperCaseMap.put( "NE", 2 );
		upperCaseMap.put( "W", 3 );
		upperCaseMap.put( "E", 5 );
		upperCaseMap.put( "SW", 6 );
		upperCaseMap.put( "S", 7 );
		upperCaseMap.put( "SE", 8 );
	}

	private void createButtons(BatWindow temp) {
		JPanel panel = temp.getPanelAtTab(0);
		panel.setLayout( new GridLayout( 3, 3 ) );
		
		for (int i = 0; i <= SE; i++) {
			if ( i != C ) {
				buttons[ i ] = new JButton();
				buttons[ i ].setText(labels[i]);
				buttons[ i ].setBackground( Color.BLACK );
				buttons[ i ].setForeground( TEXT_COLOR );
				buttons[ i ].setEnabled( false );
				
				panel.add( buttons[ i ] );
			}
			else{
				panel.add( new JLabel("") );
			}
		}
	}
	
	@Override
	public void process(Object data){
		buttons[0].setText( "Whee" );
	}
	
	public String getName(){
		return "beholder";
	}

	@Override
	public ParsedResult trigger(ParsedResult arg0) {
		String strippedText = arg0.getStrippedText();
		Matcher obviousExitsMatcher = obviousExitsPattern.matcher( strippedText );

		if ( obviousExitsMatcher.find() ) {
			    enableButtons( obviousExitsMatcher.group( 2 ).replaceAll( "(and )|,|\\.", "" ).split(" ") );
			    
			    if ( !exitsHandledByExistingWallTrigger ) {
			    	clearFireInExits();
					updateButtonColors();
			    }
			    else
			    	exitsHandledByExistingWallTrigger = false;
		}
		else {
			Matcher newWallMatcher = newWallPattern.matcher( strippedText );
			
			if ( newWallMatcher.find() && map.containsKey( newWallMatcher.group( 1 ) ) ) {
				fireInExit[ map.get( newWallMatcher.group( 1 ) ) ]  = true;
				buttons[ map.get( newWallMatcher.group( 1 ) ) ].setBackground( RED );
			}
			else {
				Matcher existingWallMatcher = existingWallPattern.matcher( strippedText );
				
				if ( existingWallMatcher.find() ) {
				    exitsHandledByExistingWallTrigger = true;
					setButtonColors( existingWallMatcher.group( 1 ).replaceAll( "(and )|,|\\.", "" ).split(" ") );
				}
			}
		}
		
		return null;
	}

	private void setButtonColors(String[] exits) {
		clearFireInExits();
		
		for ( int i = 0; i < exits.length; i++ ) {
			if ( map.containsKey( exits[ i ] ) )
				fireInExit[ map.get( exits[ i ] ) ] = true;
		}
		
		updateButtonColors();
	}

	private void updateButtonColors() {
		for ( int i = 0; i <= SE; i++){
			if ( i != C ) {
				if ( fireInExit[ i ] )
					buttons[ i ].setBackground( RED );
				else
					buttons[ i ].setBackground( Color.BLACK );
			}
		}
	}

	private void enableButtons(String[] directions) {
		for ( int i = 0; i <= SE; i++ ) {
			displayButton[ i ] = false;
		}
		
		for ( int i = 0; i < directions.length; i++ ) {
			if ( map.containsKey( directions[i] ) )
				displayButton[ map.get( directions[ i ] ) ] = true;
		}
		
		for ( int i = 0; i <= SE; i++ ) {
			if ( buttons[ i ] != null )
				buttons[ i ].setVisible( displayButton[ i ] );
		}
	}

	private void clearFireInExits() {
		for ( int i = 0; i <= SE; i++ )
			fireInExit[ i ] = false;
	}
	
	@Override
	public String trigger(String arg0) {
		if ( arg0.length() < 3 ) {
			if ( upperCaseMap.containsKey( arg0 ) && fireInExit[ upperCaseMap.get( arg0 ) ] ) {
				return arg0.toLowerCase();
			}
			else if ( map.containsKey( arg0 ) && fireInExit[ map.get( arg0 ) ] ) {
				this.getClientGUI().printAttributedString( "generic", fireMessage, false, false );
				return "";
			}
		}
		
		return null;
	}

	@Override
	public void clientExit() {
		Point location = window.getLocation();
		Dimension size = window.getSize();
		
		config.setX(location.x);
		config.setY(location.y);
		config.setWidth(size.width);
		config.setHeight(size.height);
		config.setVisible(window.isVisible());
		
		File file = new File(configPath);
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(Config.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			jaxbMarshaller.marshal(config, file);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
}
