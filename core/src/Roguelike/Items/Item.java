package Roguelike.Items;

import Roguelike.Ability.AbilityTree;
import Roguelike.Ability.ActiveAbility.ActiveAbility;
import Roguelike.Ability.IAbility;
import Roguelike.Ability.PassiveAbility.PassiveAbility;
import Roguelike.AssetManager;
import Roguelike.Entity.Entity;
import Roguelike.Entity.GameEntity;
import Roguelike.GameEvent.GameEventHandler;
import Roguelike.Global;
import Roguelike.Global.Statistic;
import Roguelike.Lights.Light;
import Roguelike.Sound.SoundInstance;
import Roguelike.Sprite.Sprite;
import Roguelike.Sprite.SpriteAnimation.MoveAnimation;
import Roguelike.Tiles.Point;
import Roguelike.UI.Seperator;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;
import exp4j.Helpers.EquationHelper;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.io.IOException;

public final class Item extends GameEventHandler
{
	/*
	 * IDEAS:
	 *
	 * Unlock extra power after condition (absorb x essence, kill x enemy)
	 */

	public String name = "";
	public String description = "";
	public Sprite icon;
	public Sprite hitEffect;
	public Array<EquipmentSlot> slots = new Array<EquipmentSlot>();
	public ItemCategory category;
	public String type = "";
	public boolean canStack;
	public int count = 1;
	public Light light;
	public boolean canDrop = true;
	public String dropChanceEqn;
	public AbilityTree ability;
	public WeaponDefinition wepDef;
	public int quality = 1;

	// ----------------------------------------------------------------------
	public Item()
	{

	}

	// ----------------------------------------------------------------------
	public static Item load( String name )
	{
		Item item = new Item();

		item.internalLoad( name );

		return item;
	}

	// ----------------------------------------------------------------------
	public static Item load( Element xml )
	{
		Item item = null;

		if ( xml.getChildByName( "Recipe" ) != null )
		{
			String recipe = Global.capitalizeString( xml.getChildByName( "Recipe" ).getText() );
			String material = xml.getChildByName( "Material" ).getText();

			Item materialItem = Item.load( "Material/"+material );

			item = Recipe.createRecipe( recipe, materialItem );

			Element prefixElement = xml.getChildByName( "Prefix" );
			if ( prefixElement != null )
			{
				String[] prefixes = prefixElement.getText().split( "," );

				for (String prefix : prefixes)
				{
					Recipe.applyModifer( item, prefix, materialItem.quality, true );
				}
			}

			Element suffixElement = xml.getChildByName( "Suffix" );
			if ( suffixElement != null )
			{
				String[] suffixes = suffixElement.getText().split( "," );

				for (String suffix : suffixes)
				{
					Recipe.applyModifer( item, suffix, materialItem.quality, false );
				}
			}
		}
		else
		{
			item = new Item();
			item.internalLoad( xml );
		}

		return item;
	}

	// ----------------------------------------------------------------------
	public boolean shouldDrop()
	{
		if ( dropChanceEqn == null ) { return true; }

		ExpressionBuilder expB = EquationHelper.createEquationBuilder( dropChanceEqn );

		Expression exp = EquationHelper.tryBuild( expB );
		if ( exp == null ) { return false; }

		double conditionVal = exp.evaluate();

		return conditionVal == 1;
	}

	// ----------------------------------------------------------------------
	public Table createTable( Skin skin, GameEntity entity )
	{
		if ( ability != null ) { return ability.current.current.createTable( skin, entity ); }

		Inventory inventory = entity.getInventory();

		if ( slots.contains( EquipmentSlot.WEAPON, true ) )
		{
			Item other = inventory.getEquip( EquipmentSlot.WEAPON );
			return createWeaponTable( other, entity, skin );
		}
		else if ( slots.size > 0 )
		{
			Item other = inventory.getEquip( slots.get( 0 ) );
			return createArmourTable( other, entity, skin );
		}

		Table table = new Table();

		table.add( new Label( getName(), skin, "title" ) ).left();
		if ( count > 1 )
		{
			table.add( new Label( "x" + count, skin ) ).left().padLeft( 20 );
		}

		table.row();

		Label descLabel = new Label( description, skin );
		descLabel.setWrap( true );
		table.add( descLabel ).expand().left().width( com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth( 1, table ) );
		table.row();

		return table;
	}

	// ----------------------------------------------------------------------
	private Table createArmourTable( Item other, GameEntity entity, Skin skin )
	{
		Table table = new Table();

		table.add( new Label( name, skin, "title" ) ).expandX().left();

		if ( type != null && type.length() > 0 )
		{
			Label label = new Label( type, skin );
			label.setFontScale( 0.7f );
			table.add( label ).expandX().right();
		}

		table.row();

		Label descLabel = new Label( description, skin );
		descLabel.setWrap( true );
		table.add( descLabel ).expand().left().width( com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth( 1, table ) );
		table.row();

		table.add( new Seperator( skin, false ) ).expandX().fillX();
		table.row();

		for (Statistic stat : Statistic.values())
		{
			int val = getStatistic( entity.getVariableMap(), stat );
			int otherVal = other != null ? other.getStatistic( entity.getVariableMap(), stat ) : 0;

			if (val > 0 || val != otherVal)
			{
				String value = ""+val;

				if (val < otherVal)
				{
					value = value + "  [RED]" + (val - otherVal) + "[]";
				}
				else if (val > otherVal)
				{
					value = value + "  [GREEN]+" + (val - otherVal) + "[]";
				}

				Label statLabel = new Label( Global.capitalizeString( stat.toString() ) + ": " + value, skin );
				statLabel.setWrap( true );
				table.add( statLabel ).expandX().left().width( com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth( 1, table ) );
				table.row();
			}
		}

		Array<String> lines = toString( entity.getVariableMap(), true );
		for (String line : lines)
		{
			if (line.equals( "---" ))
			{
				table.add( new Seperator( skin, false ) ).expandX().fillX();
			}
			else
			{
				Label lineLabel = new Label( line, skin );
				lineLabel.setWrap( true );
				table.add( lineLabel ).expandX().left().width( com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth( 1, table ) );
				table.row();
			}

			table.row();
		}

		return table;
	}

	// ----------------------------------------------------------------------
	private Table createWeaponTable( Item other, GameEntity entity, Skin skin )
	{
		Table table = new Table();

		table.add( new Label( name, skin, "title" ) ).expandX().left();

		{
			Label label = new Label( type, skin );
			label.setFontScale( 0.7f );
			table.add( label ).expandX().right();
		}

		table.row();

		Label descLabel = new Label( description, skin );
		descLabel.setWrap( true );
		table.add( descLabel ).expand().left().width( com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth( 1, table ) );
		table.row();

		table.add( new Seperator( skin, false ) ).expandX().fillX();
		table.row();

		int oldDam = other != null ? Global.calculateScaledAttack( Statistic.statsBlockToVariableBlock( other.getStatistics( entity.getVariableMap() ) ), entity.getVariableMap() ) : 0;
		int newDam = Global.calculateScaledAttack( Statistic.statsBlockToVariableBlock( getStatistics( entity.getVariableMap() ) ), entity.getVariableMap() );

		String damText = "Damage: " + newDam;
		if ( newDam != oldDam )
		{
			int diff = newDam - oldDam;

			if ( diff > 0 )
			{
				damText += "   [GREEN]+" + diff;
			}
			else
			{
				damText += "   [RED]" + diff;
			}
		}

		table.add( new Label( damText, skin ) ).expandX().left();
		table.row();

		table.add( new Seperator( skin, false ) ).expandX().fillX();
		table.row();

		table.add( new Label( "Scales with:", skin ) ).expandX().left();
		table.row();

		for (Statistic stat : Statistic.values())
		{
			int val = getStatistic( entity.getVariableMap(), stat );
			int otherVal = other != null ? other.getStatistic( entity.getVariableMap(), stat ) : 0;

			if ( stat == Statistic.ATTACK || stat == Statistic.DEFENSE )
			{
				continue;
			}

			if (val > 0 || val != otherVal)
			{
				String scale = val > 0 ? Global.ScaleLevel.values()[ val - 1 ].toString() : "--";

				if (val < otherVal)
				{
					scale = "[RED]" + scale + "[]";
				}
				else if (val > otherVal)
				{
					scale = "[GREEN]" + scale + "[]";
				}

				Label statLabel = new Label( Global.capitalizeString( stat.toString() ) + ": " + scale, skin );
				statLabel.setWrap( true );
				table.add( statLabel ).expandX().left().width( com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth( 1, table ) );
				table.row();
			}
		}

		Array<String> lines = toString( entity.getVariableMap(), true );
		for (String line : lines)
		{
			if (line.equals( "---" ))
			{
				table.add( new Seperator( skin, false ) ).expandX().fillX();
			}
			else
			{
				Label lineLabel = new Label( line, skin );
				lineLabel.setWrap( true );
				table.add( lineLabel ).expandX().left().width( com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth( 1, table ) );
				table.row();
			}

			table.row();
		}

		return table;
	}

	// ----------------------------------------------------------------------
	@Override
	public String getName()
	{
		if ( ability != null ) { return ability.current.current.getName(); }

		return name;
	}

	// ----------------------------------------------------------------------
	@Override
	public String getDescription()
	{
		if ( ability != null ) { return ability.current.current.getDescription(); }

		return description;
	}

	// ----------------------------------------------------------------------
	@Override
	public Sprite getIcon()
	{
		if ( icon != null ) { return icon; }

		EquipmentSlot slot = getMainSlot();

		if ( slot == EquipmentSlot.WEAPON )
		{
			if ( type.equals( "sword" ) )
			{
				icon = AssetManager.loadSprite( "Oryx/uf_split/uf_items/weapon_sword" );
			}
			else if ( type.equals( "spear" ) )
			{
				icon = AssetManager.loadSprite( "Oryx/uf_split/uf_items/weapon_spear" );
			}
			else if ( type.equals( "axe" ) )
			{
				icon = AssetManager.loadSprite( "Oryx/uf_split/uf_items/weapon_handaxe" );
			}
			else if ( type.equals( "bow" ) )
			{
				icon = AssetManager.loadSprite( "Oryx/uf_split/uf_items/weapon_longbow" );
			}
			else if ( type.equals( "wand" ) )
			{
				icon = AssetManager.loadSprite( "Oryx/uf_split/uf_items/weapon_staff_jeweled" );
			}
		}
		else if ( slot == EquipmentSlot.HEAD )
		{
			icon = AssetManager.loadSprite( "Oryx/uf_split/uf_items/armor_chain_helm" );
		}
		else if ( slot == EquipmentSlot.BODY )
		{
			icon = AssetManager.loadSprite( "Oryx/uf_split/uf_items/armor_chain_chest" );
		}
		else if ( slot == EquipmentSlot.LEGS )
		{
			icon = AssetManager.loadSprite( "Oryx/uf_split/uf_items/armor_chain_leg" );
		}
		else if ( category == ItemCategory.MATERIAL )
		{
			if ( type.equals( "fabric" ) )
			{
				icon = AssetManager.loadSprite( "GUI/Fabric" );
			}
			else if ( type.equals( "hide" ) )
			{
				icon = AssetManager.loadSprite( "GUI/Hide" );
			}
			else if ( type.equals( "leather" ) )
			{
				icon = AssetManager.loadSprite( "GUI/Leather" );
			}
			else if ( type.equals( "ore" ) )
			{
				icon = AssetManager.loadSprite( "GUI/Ore" );
			}
			else if ( type.equals( "ingot" ) )
			{
				icon = AssetManager.loadSprite( "GUI/Ingot" );
			}
			else if ( type.equals( "log" ) )
			{
				icon = AssetManager.loadSprite( "GUI/Log" );
			}
			else if ( type.equals( "plank" ) )
			{
				icon = AssetManager.loadSprite( "GUI/Plank" );
			}
			else if ( type.equals( "bone" ) )
			{
				icon = AssetManager.loadSprite( "GUI/Bone" );
			}
			else if ( type.equals( "claw" ) )
			{
				icon = AssetManager.loadSprite( "GUI/Claw" );
			}
			else if ( type.equals( "fang" ) )
			{
				icon = AssetManager.loadSprite( "GUI/Fang" );
			}
			else if ( type.equals( "spine" ) )
			{
				icon = AssetManager.loadSprite( "GUI/Spine" );
			}
			else if ( type.equals( "scale" ) )
			{
				icon = AssetManager.loadSprite( "GUI/Scale" );
			}
			else if ( type.equals( "feather" ) )
			{
				icon = AssetManager.loadSprite( "GUI/Feather" );
			}
			else if ( type.equals( "shell" ) )
			{
				icon = AssetManager.loadSprite( "GUI/Shell" );
			}
			else if ( type.equals( "vial" ) )
			{
				icon = AssetManager.loadSprite( "GUI/Vial" );
			}
			else if ( type.equals( "sac" ) )
			{
				icon = AssetManager.loadSprite( "GUI/Sac" );
			}
			else if ( type.equals( "powder" ) )
			{
				icon = AssetManager.loadSprite( "GUI/Powder" );
			}
			else if ( type.equals( "crystal" ) )
			{
				icon = AssetManager.loadSprite( "GUI/Crystal" );
			}
			else if ( type.equals( "gem" ) )
			{
				icon = AssetManager.loadSprite( "GUI/Gem" );
			}
		}
		else if ( ability != null )
		{
			icon = ability.current.current.getIcon();
		}

		if ( icon == null )
		{
			icon = AssetManager.loadSprite( "white" );
		}
		return icon;
	}

	// ----------------------------------------------------------------------
	private void internalLoad( String name )
	{
		XmlReader xml = new XmlReader();
		Element xmlElement = null;

		try
		{
			xmlElement = xml.parse( Gdx.files.internal( "Items/" + name + ".xml" ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		internalLoad( xmlElement );
	}

	// ----------------------------------------------------------------------
	private void internalLoad( Element xmlElement )
	{
		String extendsElement = xmlElement.getAttribute( "Extends", null );
		if ( extendsElement != null )
		{
			internalLoad( extendsElement );
		}

		name = xmlElement.get( "Name", name );
		description = xmlElement.get( "Description", description );
		canStack = xmlElement.getBoolean( "CanStack", canStack );

		String countEqn = xmlElement.get( "Count", "" + count );
		count = EquationHelper.evaluate( countEqn, Statistic.emptyMap );

		Element iconElement = xmlElement.getChildByName( "Icon" );
		if ( iconElement != null )
		{
			icon = AssetManager.loadSprite( iconElement );
		}

		Element hitElement = xmlElement.getChildByName( "HitEffect" );
		if ( hitElement != null )
		{
			hitEffect = AssetManager.loadSprite( hitElement );
		}

		Element eventsElement = xmlElement.getChildByName( "Events" );
		if ( eventsElement != null )
		{
			super.parse( eventsElement );
		}

		Element lightElement = xmlElement.getChildByName( "Light" );
		if ( lightElement != null )
		{
			light = Light.load( lightElement );
		}

		String slotsElement = xmlElement.get( "Slot", null );
		if ( slotsElement != null )
		{
			String[] split = slotsElement.split( "," );
			for ( String s : split )
			{
				slots.add( EquipmentSlot.valueOf( s.toUpperCase() ) );
			}
		}
		category = xmlElement.get( "Category", null ) != null ? ItemCategory.valueOf( xmlElement.get( "Category" ).toUpperCase() ) : category;
		type = xmlElement.get( "Type", type ).toLowerCase();
		quality = xmlElement.getInt( "Quality", quality );

		// Load the wep def
		if (slots.contains( EquipmentSlot.WEAPON, true ) && type != null)
		{
			wepDef = WeaponDefinition.load( type );
		}

		Element abilityElement = xmlElement.getChildByName( "Ability" );
		if ( abilityElement != null )
		{
			ability = new AbilityTree(abilityElement.getText());
		}

		// Preload sprites
		if ( type != null )
		{
			getWeaponHitEffect();
			getIcon();
		}
	}

	// ----------------------------------------------------------------------
	public EquipmentSlot getMainSlot()
	{
		return slots.size > 0 ? slots.get( 0 ) : null;
	}

	// ----------------------------------------------------------------------
	public Sprite getWeaponHitEffect()
	{
		if ( hitEffect != null ) { return hitEffect; }

		if ( type.equals( "sword" ) )
		{
			return AssetManager.loadSprite( "slash/slash", 0.1f );
		}
		else if ( type.equals( "spear" ) )
		{
			return AssetManager.loadSprite( "thrust/thrust", 0.1f );
		}
		else if ( type.equals( "axe" ) )
		{
			return AssetManager.loadSprite( "slash/slash", 0.1f );
		}
		else if ( type.equals( "bow" ) )
		{
			Sprite sprite = AssetManager.loadSprite( "arrow", 0.1f );
			sprite.spriteAnimation = new MoveAnimation(  );
			return sprite;
		}
		else if ( type.equals( "wand" ) )
		{
			Sprite sprite = AssetManager.loadSprite( "bolt", 0.1f );
			sprite.spriteAnimation = new MoveAnimation(  );
			return sprite;
		}

		return AssetManager.loadSprite( "strike/strike", 0.1f );
	}

	// ----------------------------------------------------------------------
	public SoundInstance getWeaponSound()
	{
		if ( hitEffect != null && hitEffect.sound != null ) { return hitEffect.sound; }

		if ( type.equals( "sword" ) )
		{
			return new SoundInstance( AssetManager.loadSound( "knife_stab" ) );
		}
		else if ( type.equals( "spear" ) )
		{
			return new SoundInstance( AssetManager.loadSound( "knife_stab" ) );
		}
		else if ( type.equals( "axe" ) )
		{
			return new SoundInstance( AssetManager.loadSound( "knife_stab" ) );
		}
		else if ( type.equals( "bow" ) )
		{
			return new SoundInstance( AssetManager.loadSound( "arrow_approaching_and_hitting_target" ) );
		}
		else if ( type.equals( "wand" ) )
		{
			return new SoundInstance( AssetManager.loadSound( "arrow_approaching_and_hitting_target" ) );
		}

		return new SoundInstance( AssetManager.loadSound( "knife_stab" ) );
	}

	// ----------------------------------------------------------------------
	public enum EquipmentSlot
	{
		// Armour
		HEAD,
		BODY,
		LEGS,

		// Jewelry
		FETISH,
		TOTEM,
		RING,
		RUNE,

		// Weapons
		WEAPON
	}

	// ----------------------------------------------------------------------
	public enum ItemCategory
	{
		ARMOUR,
		WEAPON,
		JEWELRY,
		TREASURE,
		MATERIAL,
		MISC,

		ALL
	}

	// ----------------------------------------------------------------------
	public static class WeaponDefinition
	{
		public enum HitType
		{
			ALL,
			CLOSEST,
			RANDOM
		}

		public HitType hitType;
		public String hitData;

		public Array<Point> hitPoints = new Array<Point>(  );

		public static WeaponDefinition load( String name )
		{
			WeaponDefinition wepDef = new WeaponDefinition();

			name = Global.capitalizeString( name );

			XmlReader reader = new XmlReader();
			XmlReader.Element xml = null;

			try
			{
				xml = reader.parse( Gdx.files.internal( "Items/Weapons/" + name + ".xml" ) );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}

			String[] hitTypeData = xml.get( "HitType" ).split( "[\\(\\)]" );
			wepDef.hitType = HitType.valueOf( hitTypeData[0].toUpperCase() );
			wepDef.hitData = hitTypeData.length > 1 ? hitTypeData[1] : null;

			Element hitPatternElement = xml.getChildByName( "HitPattern" );

			char[][] hitGrid = new char[hitPatternElement.getChildCount()][];
			Point centralPoint = new Point();

			for (int y = 0; y < hitPatternElement.getChildCount(); y++)
			{
				Element lineElement = hitPatternElement.getChild( y );
				String text = lineElement.getText();

				hitGrid[ y ] = text.toCharArray();

				for (int x = 0; x < hitGrid[ y ].length; x++)
				{
					if (hitGrid[ y ][ x ] == 'x')
					{
						centralPoint.x = x;
						centralPoint.y = y;
					}
				}
			}

			for (int y = 0; y < hitGrid.length; y++)
			{
				for (int x = 0; x < hitGrid[0].length; x++)
				{
					if (hitGrid[y][x] == '#')
					{
						int dx = x - centralPoint.x;
						int dy = centralPoint.y - y;

						wepDef.hitPoints.add( new Point(dx, dy) );
					}
				}
			}

			return wepDef;
		}
	}
}
