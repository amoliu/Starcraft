package eisbw;

import eis.exceptions.ActException;
import eis.iilang.Action;
import eisbw.actions.ActionProvider;
import eisbw.actions.StarcraftAction;
import eisbw.debugger.DebugWindow;
import eisbw.units.StarcraftUnitFactory;
import jnibwapi.JNIBWAPI;
import jnibwapi.Unit;
import jnibwapi.types.RaceType.RaceTypes;
import jnibwapi.types.UnitType.UnitTypes;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Danny & Harm - The Listener of the BWAPI Events.
 *
 */
public class BwapiListener extends BwapiEvents {
	protected Logger logger = Logger.getLogger("StarCraft Logger");
	protected JNIBWAPI bwapi;
	protected Game game;
	protected ActionProvider actionProvider;
	protected Map<Unit, Action> pendingActions;
	protected StarcraftUnitFactory factory;
	protected UpdateThread updateThread;
	protected boolean debugmode;
	protected boolean invulnerable;
	protected int speed;
	protected int count = 0;
	protected DebugWindow debug;

	/**
	 * Event listener for BWAPI.
	 * 
	 * @param game
	 *            - the game data class
	 * @param debugmode
	 *            - true iff debugger should be attached
	 */
	public BwapiListener(Game game, boolean debugmode, boolean invulnerable, int speed) {
		bwapi = new JNIBWAPI(this, true);
		this.game = game;
		actionProvider = new ActionProvider();
		actionProvider.loadActions(bwapi);
		pendingActions = new ConcurrentHashMap<>();
		factory = new StarcraftUnitFactory(bwapi);
		this.debugmode = debugmode;
		this.invulnerable = invulnerable;
		this.speed = speed;

		new Thread() {
			@Override
			public void run() {
				Thread.currentThread().setPriority(MAX_PRIORITY);
				Thread.currentThread().setName("BWAPI thread");
				bwapi.start();
			}
		}.start();

	}

	@Override
	public void matchStart() {
		updateThread = new UpdateThread(game, bwapi);
		updateThread.start();
		game.updateConstructionSites(bwapi);
		game.updateMap(bwapi);

		// SET INIT SPEED (DEFAULT IS 50 FPS, WHICH IS 20 SPEED)
		if (speed > 0)
			bwapi.setGameSpeed(1000 / speed);
		else if (speed == 0)
			bwapi.setGameSpeed(speed);
		
		// SET INIT INVULNERABLE PARAMETER
		if(invulnerable)
			bwapi.sendText("power overwhelming");

		// START THE DEBUG TOOLS.
		if (debugmode) {
			debug = new DebugWindow(game);
			bwapi.drawTargets(true);
			bwapi.enableUserInput();
		}
	}

	@Override
	public void matchFrame() {
		Iterator<Entry<Unit, Action>> it = pendingActions.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Unit, Action> entry = it.next();
			Unit unit = entry.getKey();
			Action act = entry.getValue();

			StarcraftAction action = getAction(act);
			action.execute(unit, act);

			it.remove();
		}

		if (debug != null) {
			debug.debug(bwapi);
		}

		if (count == 200) {
			game.updateConstructionSites(bwapi);
			count = 0;
		}
		count++;
	}

	@Override
	public void unitDestroy(int id) {
		if (game.getUnits().getUnitNames().containsKey(id)) {
			String unitName = game.getUnits().getUnitNames().get(id);
			game.getUnits().deleteUnit(unitName, id);
		}
	}

	@Override
	public void unitComplete(int unitId) {
		Unit unit = bwapi.getUnit(unitId);
		if (bwapi.getMyUnits().contains(unit) && !game.getUnits().getUnitNames().containsKey(unitId)) {
			game.getUnits().addUnit(unit, factory);
		}
	}

	@Override
	public void unitMorph(int id) {
		if (bwapi.getSelf().getRace().getID() != RaceTypes.Zerg.getID()) {
			return;
		}
		unitDestroy(id);
		if (bwapi.getUnit(id).getType() != UnitTypes.Zerg_Zergling) {
			unitComplete(id);
		}
	}

	@Override
	public void matchEnd(boolean winner) {
		if (winner) {
			game.updateEndGamePerceiver(bwapi);
			game.setEndGame(winner);
		}
		// have the winner percept perceived for 2 seconds before all agents
		// are removed
		try {
			TimeUnit.SECONDS.sleep(2);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		updateThread.terminate();
		try {
			updateThread.join();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		pendingActions = new ConcurrentHashMap<>();
		if (debug != null) {
			debug.dispose();
		}
		game.clean();
	}

	protected boolean isSupportedByEntity(Action act, String name) {
		Unit unit = game.getUnits().getUnits().get(name);

		StarcraftAction action = getAction(act);
		return action != null && action.isValid(act) && action.canExecute(unit, act);
	}

	/**
	 * @param action
	 *            The inserted requested action.
	 * @return The requested Starcraft Action.
	 */
	public StarcraftAction getAction(Action action) {
		return actionProvider.getAction(action.getName() + "/" + action.getParameters().size());
	}

	/**
	 * Returns the current FPS.
	 * 
	 * @return the current FPS.
	 */
	public int getFPS() {
		return (this.debug == null) ? this.speed : this.debug.getFPS();
	}

	/**
	 * Adds an action to the action queue, the action is then executed on the
	 * next frame.
	 * 
	 * @param name
	 *            - the name of the unit.
	 * @param act
	 *            - the action.
	 * @throws ActException
	 *             - mandatory from EIS
	 */
	public void performEntityAction(String name, Action act) throws ActException {
		Unit unit = game.getUnits().getUnits().get(name);

		// cant act during construction
		// if (!unit.isBeingConstructed()) {
		StarcraftAction action = getAction(act);
		// Action might be invalid
		if (action.isValid(act) && isSupportedByEntity(act, name)) {
			pendingActions.put(unit, act);
		} else {
			logger.log(Level.WARNING, "The Entity: " + name + " is not able to perform the action: " + act.getName());
		}
		// }
	}
}
