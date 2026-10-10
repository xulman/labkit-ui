package sc.fiji.labkit.ui.brush;

import bdv.interactive.behaviours.BdvPromptsBehaviours;
import bdv.interactive.services.BdvPromptsService;
import bdv.interactive.services.ConvertedViewsService;
import bdv.interactive.services.OriginalViewsService;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.type.numeric.RealType;
import java.util.HashMap;
import java.util.Map;

/**
 * This class governs the BdvPrompts package in Labkit.
 *
 * It makes sure exactly one instance of BdvPromptsServices is started,
 * it is the point of contact to get a reference on these services.
 * It further registers the keys, actions, so that downstream code can
 * register their listeners to deliver the actual work...
 */
public class BdvPromptsController implements AutoCloseable {
	// -------------- singleton stuff --------------
	//TODO: possibly don't use singleton
	private BdvPromptsController() {
		//to make IDE happy to see all paths initiating final attribs
		bdvHandle = null;
		bdvPromptsService = null;
		originalViewsService = null;
	}

	public static BdvPromptsController obtain() {
		if (instance == null) {
			throw new IllegalStateException("BdvPromptsController has not yet been initialized");
		}
		return instance;
	}

	private static BdvPromptsController instance = null;

	// -------------- object stuff --------------
	public BdvPromptsController(final BdvHandle bdvHandle)
	{
		this.bdvHandle = bdvHandle;

		// --- the three services, created once, handed to whoever needs them
		bdvPromptsService = new BdvPromptsService(bdvHandle);
		originalViewsService = new OriginalViewsService(bdvHandle);

		// --- named actions; triggers come from BDV's keymap (defaults if unknown there)
		BdvPromptsBehaviours.addPromptActionsTo(bdvPromptsService);
		// --- extra 3D navigation: ctrl I/J/K side views, ctrl N/M slicing
		BdvPromptsBehaviours.addNavigationActionsTo(bdvPromptsService);

		instance = this;
	}

	public <T extends RealType<T>> void registerNewSAC(SourceAndConverter<T> sac) {
		convertedViewsServices.putIfAbsent(sac, new ConvertedViewsService(bdvHandle, sac));
	}
	public void unregisterSAC(SourceAndConverter<?> sac) {
		convertedViewsServices.remove(sac);
	}

	/**
	 * Also {@link #registerNewSAC(SourceAndConverter)} if needed.
	 */
	public <T extends RealType<T>> ConvertedViewsService getConvViewServiceFor(SourceAndConverter<T> sac) {
		if (!convertedViewsServices.containsKey(sac)) registerNewSAC(sac);
		return convertedViewsServices.get(sac);
	}

	public OriginalViewsService getOrigViewService() {
		return originalViewsService;
	}

	public BdvPromptsService getBdvPromptsService() {
		return bdvPromptsService;
	}

	private final BdvHandle bdvHandle;
	private final BdvPromptsService bdvPromptsService;
	private final OriginalViewsService originalViewsService;
	private final Map<SourceAndConverter<?>, ConvertedViewsService> convertedViewsServices
			  = new HashMap<>(10);

	@Override
	public void close() throws Exception {
		bdvPromptsService.getActions().forEach(bdvPromptsService::removeAction);
		originalViewsService.close();
		convertedViewsServices.clear();
	}
}
