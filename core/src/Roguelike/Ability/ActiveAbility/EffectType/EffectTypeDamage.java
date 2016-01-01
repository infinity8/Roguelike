package Roguelike.Ability.ActiveAbility.EffectType;

import Roguelike.Ability.ActiveAbility.ActiveAbility;
import Roguelike.Entity.Entity;
import Roguelike.Global;
import Roguelike.Global.Statistic;
import Roguelike.Tiles.GameTile;
import Roguelike.Util.FastEnumMap;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader.Element;
import exp4j.Helpers.EquationHelper;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.HashMap;

public class EffectTypeDamage extends AbstractEffectType
{
	private FastEnumMap<Statistic, String> equations = new FastEnumMap<Statistic, String>( Statistic.class );
	private String[] reliesOn;

	@Override
	public void update( ActiveAbility aa, float time, GameTile tile )
	{
		if ( tile.entity != null || tile.environmentEntity != null )
		{
			HashMap<String, Integer> variableMap = calculateVariableMap( aa );

			if ( tile.entity != null )
			{
				applyToEntity( tile.entity, aa, variableMap );
			}

			if ( tile.environmentEntity != null )
			{
				applyToEntity( tile.environmentEntity, aa, variableMap );
			}
		}
	}

	@Override
	public void parse( Element xml )
	{
		reliesOn = xml.getAttribute( "ReliesOn", "" ).split( "," );

		for ( int i = 0; i < xml.getChildCount(); i++ )
		{
			Element sEl = xml.getChild( i );

			Statistic stat = Statistic.valueOf( sEl.getName().toUpperCase() );
			equations.put( stat, sEl.getText().toLowerCase() );
		}
	}

	@Override
	public AbstractEffectType copy()
	{
		EffectTypeDamage e = new EffectTypeDamage();
		e.equations = equations;
		e.reliesOn = reliesOn;
		return e;
	}

	@Override
	public Array<String> toString( ActiveAbility aa )
	{
		HashMap<String, Integer> variableMap = calculateVariableMap( aa );

		Array<String> lines = new Array<String>();

		float damage = Global.calculateScaledAttack( variableMap, aa.getVariableMap() );
		damage /= 100.0f;
		damage *= aa.getCaster().getVariable( Statistic.ATTACK );

		lines.add( "Total Damage: " + (int)damage );

		lines.add( "---" );

		lines.add( "Weapon Damage: " + variableMap.get( Statistic.ATTACK.toString().toLowerCase() ) + "%" );

		lines.add( "---" );

		lines.add( "Scales By:" );

		for ( Statistic stat : Statistic.ModifierValues )
		{
			int val = variableMap.get( stat.toString().toLowerCase() );

			if ( val > 0 )
			{
				Global.ScaleLevel scale = Global.ScaleLevel.values()[val-1];

				lines.add( Global.capitalizeString( stat.toString() ) + " : " + scale );
			}
		}

		return lines;
	}

	private void applyToEntity( Entity target, ActiveAbility aa, HashMap<String, Integer> variableMap )
	{
		float damage = Global.calculateScaledAttack( variableMap, aa.getVariableMap() );
		damage /= 100.0f;
		damage *= aa.getCaster().getVariable( Statistic.ATTACK );

		Global.calculateDamage( aa.getCaster(), target, (int)damage, target.getVariable( Statistic.DEFENSE ), true );
	}

	private HashMap<String, Integer> calculateVariableMap( ActiveAbility aa )
	{
		HashMap<String, Integer> variableMap = aa.getVariableMap();

		for ( String name : reliesOn )
		{
			if ( !variableMap.containsKey( name.toLowerCase() ) )
			{
				variableMap.put( name.toLowerCase(), 0 );
			}
		}

		FastEnumMap<Statistic, Integer> stats = Statistic.getStatisticsBlock();

		for ( Statistic stat : Statistic.values() )
		{
			if ( equations.containsKey( stat ) )
			{
				int raw = 0;
				String eqn = equations.get( stat );

				if ( Global.isNumber( eqn ) )
				{
					raw = Integer.parseInt( eqn );
				}
				else
				{
					ExpressionBuilder expB = EquationHelper.createEquationBuilder( eqn );
					EquationHelper.setVariableNames( expB, variableMap, "" );

					Expression exp = EquationHelper.tryBuild( expB );
					if ( exp == null )
					{
						continue;
					}

					EquationHelper.setVariableValues( exp, variableMap, "" );

					raw = (int) exp.evaluate();
				}

				stats.put( stat, raw );
			}
		}

		variableMap = Statistic.statsBlockToVariableBlock( stats );

		return variableMap;
	}
}
