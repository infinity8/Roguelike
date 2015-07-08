package Roguelike.GameEvent.OnTurn;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import Roguelike.Entity.Entity;

import com.badlogic.gdx.utils.XmlReader.Element;

public class HealOverTimeEvent extends AbstractOnTurnEvent
{
	String hps;
	float remainder;
	
	@Override
	public boolean handle(Entity entity, float time)
	{
		ExpressionBuilder expB = new ExpressionBuilder(hps);
		Expression exp = entity.fillExpressionWithValues(expB, "");
		
		float raw = (float)exp.evaluate() * time + remainder;
		
		int rounded = (int)Math.floor(raw);
		
		remainder = raw - rounded;
		
		entity.applyHealing(rounded);
		
		return true;
	}

	@Override
	public void parse(Element xml)
	{
		hps = xml.get("Heal");
	}

}