package Roguelike.Ability.ActiveAbility.HitType;

import Roguelike.Ability.ActiveAbility.ActiveAbility;
import Roguelike.Entity.Entity;
import com.badlogic.gdx.utils.XmlReader;

/**
 * Created by Philip on 02-Jan-16.
 */
public class HitTypeAny extends AbstractHitType
{
	@Override
	public AbstractHitType copy()
	{
		return new HitTypeAny();
	}

	@Override
	public void parse( XmlReader.Element xml )
	{

	}

	@Override
	public boolean isTargetValid( ActiveAbility aa, Entity entity )
	{
		return true;
	}
}
