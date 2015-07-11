package Roguelike.GameEvent.Constant;

import java.util.EnumMap;
import java.util.HashMap;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import Roguelike.Global.Statistics;
import Roguelike.Global.Tier1Element;
import Roguelike.Entity.Entity;

import com.badlogic.gdx.utils.XmlReader.Element;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import exp4j.Functions.RandomFunction;
import exp4j.Helpers.EquationHelper;
import exp4j.Operators.BooleanOperators;

public class ConstantEvent
{
	private EnumMap<Statistics, String> equations = new EnumMap<Statistics, String>(Statistics.class);
	private String[] reliesOn;
	
	public void parse(Element xml)
	{
		reliesOn = xml.getAttribute("ReliesOn", "").split(",");
		
		for (int i = 0; i < xml.getChildCount(); i++)
		{
			Element sEl = xml.getChild(i);
			
			Statistics el = Statistics.valueOf(sEl.getName().toUpperCase());
			equations.put(el, sEl.getText());
		}
	}
	
	public int getStatistic(Entity entity, Statistics stat)
	{
		String eqn = equations.get(stat);
		
		if (eqn == null)
		{
			return 0;
		}
		
		HashMap<String, Integer> variableMap = entity.getBaseVariableMap();
		for (String name : reliesOn)
		{
			if (!variableMap.containsKey(name.toUpperCase()))
			{
				variableMap.put(name.toUpperCase(), 0);
			}
		}
		
		ExpressionBuilder expB = EquationHelper.createEquationBuilder(eqn);
		EquationHelper.setVariableNames(expB, variableMap, "");
				
		Expression exp = EquationHelper.tryBuild(expB);
		if (exp == null)
		{
			return 0;
		}
		
		EquationHelper.setVariableValues(exp, variableMap, "");
		
		int val = (int)exp.evaluate();
		
		return val;
	}
	
	public static ConstantEvent load(Element xml)
	{		
		ConstantEvent ce = new ConstantEvent();
		
		ce.parse(xml);
		
		return ce;
	}	
}