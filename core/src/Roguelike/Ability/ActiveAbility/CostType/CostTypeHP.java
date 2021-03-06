package Roguelike.Ability.ActiveAbility.CostType;

import java.util.HashMap;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import Roguelike.Global;
import Roguelike.Ability.ActiveAbility.ActiveAbility;

import com.badlogic.gdx.utils.XmlReader.Element;

import exp4j.Helpers.EquationHelper;

public class CostTypeHP extends AbstractCostType
{
	private String[] reliesOn;
	private String equation;

	@Override
	public boolean isCostAvailable( ActiveAbility aa )
	{
		int raw = calculateHPCost( aa );
		if ( raw == -1 ) { return false; }

		return raw < aa.getCaster().HP;
	}

	@Override
	public void spendCost( ActiveAbility aa )
	{
		int raw = calculateHPCost( aa );
		if ( raw == -1 ) { return; }

		aa.getCaster().applyDamage( raw, aa.getCaster() );
	}

	private int calculateHPCost( ActiveAbility aa )
	{
		HashMap<String, Integer> variableMap = aa.getVariableMap();

		for ( String name : reliesOn )
		{
			if ( !variableMap.containsKey( name.toLowerCase() ) )
			{
				variableMap.put( name.toLowerCase(), 0 );
			}
		}

		return EquationHelper.evaluate( equation, variableMap );
	}

	@Override
	public void parse( Element xml )
	{
		reliesOn = xml.getAttribute( "ReliesOn", "" ).split( "," );
		equation = xml.getText().toLowerCase();
	}

	@Override
	public AbstractCostType copy()
	{
		CostTypeHP hp = new CostTypeHP();

		hp.reliesOn = reliesOn;
		hp.equation = equation;

		return hp;
	}

	@Override
	public String toString( ActiveAbility aa )
	{
		int raw = calculateHPCost( aa );
		boolean valid = raw < aa.getCaster().HP;
		String colour = valid ? "[GREEN]" : "[RED]";

		return colour + "Costs " + raw + " HP";
	}

}
