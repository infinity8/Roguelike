package Roguelike.GameEvent.OnTask;

import java.util.HashMap;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import Roguelike.Global;
import Roguelike.Entity.Entity;
import Roguelike.Entity.Tasks.AbstractTask;
import Roguelike.GameEvent.IGameObject;
import Roguelike.StatusEffect.StatusEffect;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader.Element;

import exp4j.Helpers.EquationHelper;

public final class StatusTaskEvent extends AbstractOnTaskEvent
{
	private String condition;
	private String[] reliesOn;
	public String stacksEqn;
	public Element status;

	@Override
	public boolean handle( Entity entity, AbstractTask task, IGameObject parent )
	{
		HashMap<String, Integer> variableMap = entity.getVariableMap();
		for ( String name : reliesOn )
		{
			if ( !variableMap.containsKey( name.toLowerCase() ) )
			{
				variableMap.put( name.toLowerCase(), 0 );
			}
		}

		if ( condition != null )
		{
			int conditionVal = EquationHelper.evaluate( condition, variableMap );
			if ( conditionVal == 0 ) { return false; }
		}

		int stacks = 1;

		if ( stacksEqn != null )
		{
			stacks = EquationHelper.evaluate( stacksEqn, variableMap );
		}

		for ( int i = 0; i < stacks; i++ )
		{
			entity.addStatusEffect( StatusEffect.load( status, parent ) );
		}

		return true;
	}

	@Override
	public void parse( Element xml )
	{
		reliesOn = xml.getAttribute( "ReliesOn", "" ).split( "," );
		condition = xml.getAttribute( "Condition", null );
		if ( condition != null )
		{
			condition = condition.toLowerCase();
		}
		stacksEqn = xml.getAttribute( "Stacks", null );
		if ( stacksEqn != null )
		{
			stacksEqn = stacksEqn.toLowerCase();
		}

		status = xml;

	}

	@Override
	public Array<String> toString( HashMap<String, Integer> variableMap, String eventType, IGameObject parent )
	{
		Array<String> lines = new Array<String>();

		StatusEffect s = StatusEffect.load( status, parent );

		lines.add( "Spawn a status:" );
		lines.addAll( s.toString( variableMap, false ) );

		return lines;
	}

}
