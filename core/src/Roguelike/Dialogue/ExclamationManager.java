package Roguelike.Dialogue;

import java.util.HashMap;

import com.badlogic.gdx.audio.Sound;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import Roguelike.Global;
import Roguelike.Global.Statistic;
import Roguelike.Entity.GameEntity;
import Roguelike.Sound.SoundInstance;
import Roguelike.Tiles.GameTile;
import Roguelike.Tiles.Point;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader.Element;

import exp4j.Helpers.EquationHelper;

public class ExclamationManager
{
	public ExclamationEventWrapper seePlayer;
	public ExclamationEventWrapper seeAlly;
	public ExclamationEventWrapper seeEnemy;
	public ExclamationEventWrapper lowHealth;
	public ExclamationEventWrapper inCombat;

	public void update( float delta )
	{
		if ( seePlayer != null && seePlayer.cooldownAccumulator > 0 )
		{
			seePlayer.cooldownAccumulator -= delta;
		}

		if ( seeAlly != null && seeAlly.cooldownAccumulator > 0 )
		{
			seeAlly.cooldownAccumulator -= delta;
		}

		if ( seeEnemy != null && seeEnemy.cooldownAccumulator > 0 )
		{
			seeEnemy.cooldownAccumulator -= delta;
		}

		if ( inCombat != null && inCombat.cooldownAccumulator > 0 )
		{
			inCombat.cooldownAccumulator -= delta;
		}
	}

	public void process( Array<GameTile> tiles, GameEntity entity )
	{
		if ( seePlayer != null )
		{
			processSeePlayer( tiles, entity );
		}

		if ( seeAlly != null )
		{
			processSeeAlly( tiles, entity );
		}

		if ( seeEnemy != null )
		{
			processSeeEnemy( tiles, entity );
		}

		if ( lowHealth != null )
		{
			processLowHealth( tiles, entity );
		}

		if ( inCombat != null )
		{
			processInCombat( tiles, entity );
		}
	}

	private void processSeePlayer( Array<GameTile> tiles, GameEntity entity )
	{
		boolean canSeePlayer = false;
		for ( GameTile tile : tiles )
		{
			if ( tile.entity != null && tile.entity == Global.CurrentLevel.player )
			{
				canSeePlayer = true;
				break;
			}
		}

		if ( canSeePlayer )
		{
			if ( seePlayer.cooldownAccumulator <= 0 )
			{
				seePlayer.process( entity, null, null );
			}

			seePlayer.cooldownAccumulator = EquationHelper.evaluate( seePlayer.cooldown );
		}
	}

	private void processSeeAlly( Array<GameTile> tiles, GameEntity entity )
	{
		boolean canSeeAlly = false;
		for ( GameTile tile : tiles )
		{
			if ( tile.entity != null && tile.entity.isAllies( entity ) )
			{
				canSeeAlly = true;
				break;
			}
		}

		if ( canSeeAlly )
		{
			if ( seeAlly.cooldownAccumulator <= 0 )
			{
				seeAlly.process( entity, null, null );
			}

			seeAlly.cooldownAccumulator = EquationHelper.evaluate( seeAlly.cooldown );
		}
	}

	private void processSeeEnemy( Array<GameTile> tiles, GameEntity entity )
	{
		Point pos = null;

		boolean canSeeEnemy = false;
		for ( GameTile tile : tiles )
		{
			if ( tile.entity != null && !tile.entity.isAllies( entity ) )
			{
				pos = new Point().set( tile );
				canSeeEnemy = true;
				break;
			}
		}

		if ( canSeeEnemy )
		{
			if ( seeEnemy.cooldownAccumulator <= 0 )
			{
				seeEnemy.process( entity, "EnemyPos", pos );
			}

			seeEnemy.cooldownAccumulator = EquationHelper.evaluate( seeEnemy.cooldown );
		}
	}

	private void processInCombat( Array<GameTile> tiles, GameEntity entity )
	{
		Point pos = null;

		boolean canSeeEnemy = false;
		for ( GameTile tile : tiles )
		{
			if ( tile.entity != null && !tile.entity.isAllies( entity ) )
			{
				pos = new Point().set( tile );
				canSeeEnemy = true;
				break;
			}
		}

		if ( canSeeEnemy )
		{
			if ( inCombat.cooldownAccumulator <= 0 )
			{
				inCombat.process( entity, "EnemyPos", pos );
				inCombat.cooldownAccumulator = EquationHelper.evaluate( inCombat.cooldown );
			}
		}
	}

	private void processLowHealth( Array<GameTile> tiles, GameEntity entity )
	{
		boolean isLowHealth = entity.HP < entity.getMaxHP() / 2;

		if ( isLowHealth )
		{
			if ( lowHealth.cooldownAccumulator <= 0 )
			{
				lowHealth.process( entity, "LowHealth", new Point().set( entity ) );
			}

			lowHealth.cooldownAccumulator = EquationHelper.evaluate( lowHealth.cooldown );
		}
	}

	public void parse( Element xml )
	{
		Element seePlayerElement = xml.getChildByName( "SeePlayer" );
		if ( seePlayerElement != null )
		{
			seePlayer = ExclamationEventWrapper.load( seePlayerElement );
		}

		Element seeAllyElement = xml.getChildByName( "SeeAlly" );
		if ( seeAllyElement != null )
		{
			seeAlly = ExclamationEventWrapper.load( seeAllyElement );
		}

		Element seeEnemyElement = xml.getChildByName( "SeeEnemy" );
		if ( seeEnemyElement != null )
		{
			seeEnemy = ExclamationEventWrapper.load( seeEnemyElement );
		}

		Element lowHealthElement = xml.getChildByName( "LowHealth" );
		if ( lowHealthElement != null )
		{
			lowHealth = ExclamationEventWrapper.load( lowHealthElement );
		}

		Element inCombatElement = xml.getChildByName( "InCombat" );
		if ( inCombatElement != null )
		{
			inCombat = ExclamationEventWrapper.load( inCombatElement );
		}
	}

	public static ExclamationManager load( Element xml )
	{
		ExclamationManager manager = new ExclamationManager();
		manager.parse( xml );
		return manager;
	}

	public static class ExclamationEventWrapper
	{
		public Array<ExclamationWrapper> groups = new Array<ExclamationWrapper>();
		public String cooldown;
		public float cooldownAccumulator;

		public void process( GameEntity entity, String key, Object value )
		{
			for ( ExclamationWrapper wrapper : groups )
			{
				if ( processCondition( entity.dialogue.data, wrapper.condition, wrapper.reliesOn ) )
				{
					entity.setPopupText( Global.expandNames( wrapper.text ), 2 );

					SoundInstance sound = wrapper.sound;
					if ( sound == null )
					{
						sound = entity.sprite.sound;
					}

					if ( sound != null )
					{
						sound.shoutFaction = entity.factions;
						sound.key = key;
						sound.value = value;

						sound.play( entity.tile[0][0] );

						sound.shoutFaction = null;
						sound.key = null;
						sound.value = null;
					}

					break;
				}
			}
		}

		// ----------------------------------------------------------------------
		public boolean processCondition( HashMap<String, Integer> data, String condition, String[] reliesOn )
		{
			for ( String name : reliesOn )
			{
				if ( !data.containsKey( name ) )
				{
					String flag = "";
					if ( Global.WorldFlags.containsKey( name ) )
					{
						flag = Global.WorldFlags.get( name );
					}
					else if ( Global.RunFlags.containsKey( name ) )
					{
						flag = Global.RunFlags.get( name );
					}
					else
					{
						flag = "0";
					}

					if (Global.isNumber( flag ))
					{
						data.put( name, Integer.parseInt( flag ) );
					}
					else
					{
						data.put( name, 1 );
					}
				}
			}

			return EquationHelper.evaluate( condition, data ) > 0;
		}

		public void parse( Element xml )
		{
			cooldown = xml.get( "Cooldown", "5+rnd(5)" );

			for ( int i = 0; i < xml.getChildCount(); i++ )
			{
				groups.add( ExclamationWrapper.load( xml.getChild( i ) ) );
			}
		}

		public static ExclamationEventWrapper load( Element xml )
		{
			ExclamationEventWrapper event = new ExclamationEventWrapper();
			event.parse( xml );
			return event;
		}
	}

	public static class ExclamationWrapper
	{
		public String condition;
		public String[] reliesOn;
		public SoundInstance sound;
		public String text;

		public void parse( Element xml )
		{
			condition = xml.getAttribute( "Condition", "1" ).toLowerCase();
			reliesOn = xml.getAttribute( "ReliesOn", "" ).toLowerCase().split( "," );

			if ( xml.getChildCount() == 0 )
			{
				text = xml.getText();
			}
			else
			{
				Element soundEl = xml.getChildByName( "Sound" );
				if ( soundEl != null )
				{
					sound = SoundInstance.load( soundEl );
				}

				text = xml.get( "Text" );
			}
		}

		public static ExclamationWrapper load( Element xml )
		{
			ExclamationWrapper wrapper = new ExclamationWrapper();
			wrapper.parse( xml );
			return wrapper;
		}
	}
}
