package Roguelike.Quests.Input;

import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import java.util.HashMap;

/**
 * Created by Philip on 23-Jan-16.
 */
public abstract class AbstractQuestInput
{
	// ----------------------------------------------------------------------
	public String key;
	public String value;
	public boolean not;

	// ----------------------------------------------------------------------
	public abstract boolean evaluate();

	// ----------------------------------------------------------------------
	public abstract void parse( XmlReader.Element xml );

	// ----------------------------------------------------------------------
	public static AbstractQuestInput load( XmlReader.Element xml )
	{
		Class<AbstractQuestInput> c = ClassMap.get( xml.getName().toUpperCase() );
		AbstractQuestInput type = null;

		try
		{
			type = ClassReflection.newInstance( c );
		}
		catch ( Exception e )
		{
			System.err.println(xml.getName());
			e.printStackTrace();
		}

		type.parse( xml );

		return type;
	}

	// ----------------------------------------------------------------------
	protected static HashMap<String, Class> ClassMap = new HashMap<String, Class>();

	// ----------------------------------------------------------------------
	static
	{
		ClassMap.put( "FLAGPRESENT", QuestInputFlagPresent.class );
		ClassMap.put( "FLAGEQUALS", QuestInputFlagEquals.class );
	}
}
