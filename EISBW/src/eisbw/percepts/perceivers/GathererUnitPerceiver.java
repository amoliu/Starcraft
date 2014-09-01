package eisbw.percepts.perceivers;

import eis.iilang.Percept;
import eisbw.percepts.GatheringPercept;
import eisbw.percepts.VespeneGeyserPercept;
import java.util.*;
import jnibwapi.*;
import jnibwapi.types.UnitType;

public class GathererUnitPerceiver extends UnitPerceiver {

    public GathererUnitPerceiver(JNIBWAPI api, Unit unit) {
        super(api, unit);
    }

    @Override
    public List<Percept> perceive() {
        List<Percept> result = new ArrayList<>();
        if (unit.isGatheringGas() || unit.isGatheringMinerals()) {
            Percept p = new GatheringPercept(unit.isGatheringGas());
            result.add(p);
        }
		
		for (Unit u : api.getNeutralUnits()) {
			if (u.getType() == UnitType.UnitTypes.Resource_Vespene_Geyser) {
				Percept p = new VespeneGeyserPercept(u.getID(), u.getResources(), u.getResourceGroup());
				result.add(p);
			}
		}
		
        return result;
    }
}
