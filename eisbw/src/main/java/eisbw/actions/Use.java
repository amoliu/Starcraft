package eisbw.actions;

import eis.iilang.Action;
import eis.iilang.Identifier;
import eis.iilang.Parameter;
import eisbw.BwapiUtility;
import jnibwapi.JNIBWAPI;
import jnibwapi.Unit;
import jnibwapi.types.TechType;

import java.util.LinkedList;

public class Use extends StarcraftTechAction {

  public Use(JNIBWAPI api) {
    super(api);
  }

  @Override
  public void execute(Unit unit, Action action) {
    LinkedList<Parameter> parameters = action.getParameters();
    TechType techType = BwapiUtility.getTechType(((Identifier) parameters.get(0)).getValue());
    unit.useTech(techType);
  }

  @Override
  public String toString() {
    return "ability(Type)";
  }
}
