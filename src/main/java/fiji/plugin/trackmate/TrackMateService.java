package fiji.plugin.trackmate;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.ScriptService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import net.imagej.ImageJService;

@Plugin( type = Service.class )
public class TrackMateService extends AbstractService implements ImageJService
{

	@Parameter
	private ScriptService scriptService;

	@Override
	public void initialize()
	{
		scriptService.addAlias( TrackMate.class );
	}

}
