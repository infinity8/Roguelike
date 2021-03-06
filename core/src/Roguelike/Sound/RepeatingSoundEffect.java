package Roguelike.Sound;

import Roguelike.AssetManager;

import Roguelike.Global;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.XmlReader.Element;

public class RepeatingSoundEffect
{
	public enum Type
	{
		CONTINUOUS,
		INTERVAL
	}

	public float minPitch = 0.8f;
	public float maxPitch = 1.2f;
	public float volume = 0.5f;

	private long soundID;
	private Sound sound;
	private boolean isPlaying = false;

	private Type type;
	private float repeatMin;
	private float repeatMax;

	private float nextRepeat;

	private float timeAccumulator;

	public void update(float delta)
	{
		if (type == Type.CONTINUOUS)
		{
			if (!isPlaying)
			{
				soundID = sound.loop(volume * Global.AmbientVolume);
			}
		}
		else
		{
			timeAccumulator += delta;

			if (timeAccumulator >= nextRepeat)
			{
				if (Global.AmbientVolume > 0)
				{
					soundID = sound.play( volume * Global.AmbientVolume, minPitch + MathUtils.random() * ( maxPitch - minPitch ), 0 );
					isPlaying = true;
				}

				nextRepeat = repeatMin + MathUtils.random() * (repeatMax - repeatMin);
				timeAccumulator = 0;
			}
		}
	}

	public void updateVolume()
	{
		if (isPlaying)
		{
			sound.setVolume( soundID, volume * Global.AmbientVolume );
		}
	}

	public void stop()
	{
		if (isPlaying)
		{
			sound.stop(soundID);

			isPlaying = false;
			timeAccumulator = 0;
		}
	}

	public static RepeatingSoundEffect parse(Element xml)
	{
		RepeatingSoundEffect sound = new RepeatingSoundEffect();

		sound.sound = AssetManager.loadSound(xml.get("Name"));
		sound.type = Type.valueOf(xml.get("Type", "Interval").toUpperCase());

		sound.repeatMin = xml.getFloat("Repeat", 30);
		sound.repeatMax = sound.repeatMin * 2;
		sound.repeatMin = xml.getFloat("RepeatMin", sound.repeatMin);
		sound.repeatMax = xml.getFloat("RepeatMax", sound.repeatMax);

		sound.nextRepeat = sound.repeatMin + MathUtils.random() * (sound.repeatMax - sound.repeatMin);

		sound.timeAccumulator = sound.repeatMin * MathUtils.random();

		return sound;
	}
}
