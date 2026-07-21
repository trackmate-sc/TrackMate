package fiji.plugin.trackmate.io.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import fiji.plugin.trackmate.FeatureModel;

public class FeatureModelIO
{
	private static Gson getGson()
	{
		return new GsonBuilder()
				.setPrettyPrinting()
				.create();
	}

	public static JsonElement toJsonTree( final FeatureModel featureModel )
	{
		return getGson().toJsonTree( featureModel );
	}
}
