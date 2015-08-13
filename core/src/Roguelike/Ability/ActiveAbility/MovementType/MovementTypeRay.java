package Roguelike.Ability.ActiveAbility.MovementType;

import Roguelike.Ability.ActiveAbility.ActiveAbility;
import Roguelike.Global.Passability;
import Roguelike.Pathfinding.BresenhamLine;
import Roguelike.Tiles.GameTile;
import Roguelike.Tiles.Point;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader.Element;

public class MovementTypeRay extends AbstractMovementType
{
	private static final Array<Passability> RayPassability = new Array<Passability>(new Passability[]{Passability.LEVITATE});

	@Override
	public void parse(Element xml)
	{
	}

	@Override
	public void init(ActiveAbility ab, int endx, int endy)
	{
		Array<Point> path = BresenhamLine.line(ab.source.x, ab.source.y, endx, endy, ab.source.level.getGrid(), false, ab.range, RayPassability);
		
		ab.AffectedTiles.clear();
		
		for (int i = 1; i < path.size; i++)
		{
			Point p = path.get(i);
			GameTile tile = ab.source.level.getGameTile(p);
			ab.AffectedTiles.add(tile);
			
			if (!tile.getPassable(RayPassability)) { break; }
		}	
	}

	@Override
	public boolean update(ActiveAbility ab)
	{
		return true;
	}

	
	@Override
	public AbstractMovementType copy()
	{
		MovementTypeRay t = new MovementTypeRay();
		return t;
	}

	@Override
	public void updateAccumulators(float cost)
	{
	}

	
	@Override
	public boolean needsUpdate()
	{
		return false;
	}

}
